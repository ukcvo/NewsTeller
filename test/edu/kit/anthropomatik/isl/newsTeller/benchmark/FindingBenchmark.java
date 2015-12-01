package edu.kit.anthropomatik.isl.newsTeller.benchmark;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.BenchmarkEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.GroundTruth;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.finding.EventFinder;
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class FindingBenchmark {

	private static Log log = LogFactory.getLog(FindingBenchmark.class);
	
	private Map<List<Keyword>, Set<NewsEvent>> keywordsToEventsMap;

	private EventFinder finder;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	public void setFinder(EventFinder finder) {
		this.finder = finder;
	}
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public FindingBenchmark(String configFileName) {
		this.keywordsToEventsMap = new HashMap<List<Keyword>, Set<NewsEvent>>();
		
		Map<String,List<Keyword>> configFile = Util.readBenchmarkConfigFile(configFileName);
		
		for (Map.Entry<String, List<Keyword>> entry: configFile.entrySet()) {
			String fileName = entry.getKey();
			List<Keyword> keywords = entry.getValue();
			
			Map<BenchmarkEvent, GroundTruth> fileMap = Util.readBenchmarkQueryFromFile(fileName);
			Set<NewsEvent> events = new HashSet<NewsEvent>();
			for (BenchmarkEvent event : fileMap.keySet()) 
				events.add(new NewsEvent(event.getEventURI()));
			
			this.keywordsToEventsMap.put(keywords, events);
		}
		
	}
	
	// find events; compare to expectation and report differences
	private void checkQuery(List<Keyword> keywords, Set<NewsEvent> expectedEvents) {
		if (log.isInfoEnabled())
			log.info(StringUtils.collectionToCommaDelimitedString(keywords));
		
		Set<NewsEvent> foundEvents = finder.findEvents(keywords, new DummyUserModel());
		
		if (foundEvents.size() != expectedEvents.size()) {
			if (log.isWarnEnabled())
				log.warn(String.format("expected %d events, but found %d events", expectedEvents.size(), foundEvents.size()));
		}
		
		for (NewsEvent event : foundEvents) {
			if (!expectedEvents.contains(event)) {
				if (log.isWarnEnabled())
					log.warn(String.format("found additional event: '%s'", event.getEventURI()));
			}
		}
		
		for (NewsEvent event : expectedEvents) {
			if (!foundEvents.contains(event)) {
				if (log.isWarnEnabled())
					log.warn(String.format("missing expected event: '%s'", event.getEventURI()));
			}
		}
		
	}
	
	public void run() {
		this.ksAdapter.openConnection();
		
		for (Map.Entry<List<Keyword>, Set<NewsEvent>> entry : this.keywordsToEventsMap.entrySet())
			checkQuery(entry.getKey(), entry.getValue());
		
		this.ksAdapter.closeConnection();
	}
	
	public static void main(String[] args) {
		System.setProperty("java.util.logging.config.file", "./config/logging-benchmark.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(FindingBenchmark.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		FindingBenchmark test = (FindingBenchmark) context.getBean("findingBenchmark");
		((AbstractApplicationContext) context).close();
		
		test.run();
	}
	
}
