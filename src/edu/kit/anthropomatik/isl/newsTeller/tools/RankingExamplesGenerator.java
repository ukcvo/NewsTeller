package edu.kit.anthropomatik.isl.newsTeller.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.IEventFilter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.IEventRanker;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.search.EventSearcher;
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class RankingExamplesGenerator {

	private static Log log = LogFactory.getLog(RankingExamplesGenerator.class);
	
	private KnowledgeStoreAdapter ksAdapter;
	
	private EventSearcher searcher;
	
	private IEventFilter filter;
	
	private IEventRanker ranker;
	
	private int totalNumberOfEvents;

	private List<List<String>> queries;
	
	private List<String> interests;
	
	private String outputFolder;
	
	private String userID;
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}

	public void setSearcher(EventSearcher searcher) {
		this.searcher = searcher;
	}

	public void setFilter(IEventFilter filter) {
		this.filter = filter;
	}

	public void setRanker(IEventRanker ranker) {
		this.ranker = ranker;
	}

	public void setTotalNumberOfEvents(int totalNumberOfEvents) {
		this.totalNumberOfEvents = totalNumberOfEvents;
	}

	public void setQueries(List<List<String>> queries) {
		this.queries = queries;
	}

	public void setInterests(List<String> interests) {
		this.interests = interests;
	}

	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public void run() {
		ksAdapter.openConnection();
		
		UserModel dummyUserModel = new DummyUserModel();
		
		int numberOfEventsPerQuery = (int) Math.ceil(totalNumberOfEvents / queries.size());
		int numberOfEventsForNextQuery = numberOfEventsPerQuery;
		int numberOfAlreadyCollectedEvents = 0;
		int i = 0;
		
		Map<String, List<String>> fileNameToKeywordMap = new HashMap<String, List<String>>();
		
		for (List<String> query : this.queries) {
			if (numberOfAlreadyCollectedEvents >= totalNumberOfEvents)
				break;
			
			List<Keyword> queryKeywords = new ArrayList<Keyword>();
			for (String word : query) 
				queryKeywords.add(new Keyword(word));
			
			Set<NewsEvent> events = searcher.findEvents(queryKeywords, dummyUserModel);
			Set<NewsEvent> filteredEvents = filter.filterEvents(events, queryKeywords);
			List<NewsEvent> rankedEvents = ranker.rankEvents(filteredEvents, queryKeywords, dummyUserModel);
			
			List<NewsEvent> selectedEvents = rankedEvents.subList(0, Math.min(numberOfEventsForNextQuery, rankedEvents.size()));
			if (selectedEvents.size() == numberOfEventsForNextQuery)
				numberOfEventsForNextQuery = numberOfEventsPerQuery;
			else
				numberOfEventsForNextQuery = numberOfEventsPerQuery + (numberOfEventsForNextQuery - selectedEvents.size());
			numberOfAlreadyCollectedEvents += selectedEvents.size();
			
			String folderName = String.format("%s/%s", outputFolder, userID);
			File folder = new File(folderName);
			if (!folder.exists())
				folder.mkdirs();
			
			String queryFileName = String.format("%s/query_%d.csv", folderName, i);
			Map<String, String> eventToSentenceMap = new HashMap<String, String>();

			for (NewsEvent event : selectedEvents) {
				String eventURI = event.getEventURI();
				List<String> sentences = ksAdapter.retrieveSentencesFromEvent(eventURI, query.get(0));
				Collections.shuffle(sentences);
				String sentence = sentences.get(0);
				eventToSentenceMap.put(eventURI, sentence);
			}
			
			Util.writeQueryFile(queryFileName, eventToSentenceMap);
			
			fileNameToKeywordMap.put(queryFileName, query);
			i++;
			if (log.isInfoEnabled())
				log.info(queryFileName);
		}
		
		Util.writeUserConfigFile(String.format("%s/%s.csv", outputFolder, userID), interests, fileNameToKeywordMap);
		
		filter.shutDown();
		ksAdapter.closeConnection();
	}
	
	public static void main(String[] args) {
		System.setProperty("java.util.logging.config.file", "./config/logging.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(RankingExamplesGenerator.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/rankingExamples.xml");
		RankingExamplesGenerator generator = (RankingExamplesGenerator) context.getBean("rankingExamplesGenerator");
		((AbstractApplicationContext) context).close();

		generator.run();
	}

}
