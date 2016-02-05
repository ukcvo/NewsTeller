package edu.kit.anthropomatik.isl.newsTeller.benchmark;

import java.util.ArrayList;
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

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.BenchmarkEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.GroundTruth;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Looks through all benchmark files and collects the corresponding sentences for all the events. 
 * Outputs the event-sentence pairs for easier rank-labeling.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class RankingSentenceExtractor {

	private static Log log = LogFactory.getLog(RankingSentenceExtractor.class);
	
	private String configFileName;
	
	private String outputFileName;
	
	private char delimiter;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	public void setConfigFileName(String configFileName) {
		this.configFileName = configFileName;
	}
	
	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}
	
	public void setDelimiter(char delimiter) {
		this.delimiter = delimiter;
	}
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public RankingSentenceExtractor() { }
	
	public void run() {
		
		this.ksAdapter.openConnection();
		
		Map<String, Map<BenchmarkEvent, List<String>>> outputMap = new HashMap<String, Map<BenchmarkEvent, List<String>>>();
		Map<String,List<Keyword>> configFile = Util.readBenchmarkConfigFile(this.configFileName);
		
		for (Map.Entry<String, List<Keyword>> entry: configFile.entrySet()) {
			
			this.ksAdapter.flushBuffer();
			
			Map<BenchmarkEvent, List<String>> localOutputMap = new HashMap<BenchmarkEvent, List<String>>();
			String filePath = entry.getKey();
			String fileName = filePath.substring(filePath.lastIndexOf('/') + 1).toLowerCase();
			
			List<Keyword> keywords = entry.getValue();
			Set<String> eventURIs = new HashSet<String>();
			
			Map<BenchmarkEvent, GroundTruth> fileMap = Util.readBenchmarkQueryFromFile(filePath, this.delimiter);
			for (BenchmarkEvent event : fileMap.keySet())  
				eventURIs.add(event.getEventURI());
			
			// retrieve the original texts for the events
			ksAdapter.runKeyValueMentionFromEventQuery(eventURIs, keywords);
			for (Keyword k : keywords) {
				Set<String> resourceURIs = Util.resourceURIsFromMentionURIs(ksAdapter.getAllRelationValues(
						Util.getRelationName("event", "mention", k.getWord())));
				ksAdapter.runKeyValueResourceTextQuery(resourceURIs);

			}
			
			for (BenchmarkEvent event : fileMap.keySet()) {
				Set<String> sentences = new HashSet<String>();
				for (Keyword k : keywords) 
					sentences.addAll(ksAdapter.retrieveSentencesfromEvent(event.getEventURI(), k.getWord()));
				
				localOutputMap.put(event, new ArrayList<String>(sentences));
			}
			outputMap.put(fileName, localOutputMap);
			
			if(log.isInfoEnabled())
				log.info(fileName);
		}
		
		this.ksAdapter.closeConnection();
		
		Util.writeRankingSentencesToCsv(outputFileName, outputMap);
	}
	
	public static void main(String[] args) {
		System.setProperty("java.util.logging.config.file", "./config/logging-benchmark.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(RuntimeTester.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/benchmark.xml");
		RankingSentenceExtractor extractor = (RankingSentenceExtractor) context.getBean("rankingSentenceExtractor");
		((AbstractApplicationContext) context).close();

		extractor.run();
	}

}
