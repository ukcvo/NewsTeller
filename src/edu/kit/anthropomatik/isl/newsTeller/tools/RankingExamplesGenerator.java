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
import org.springframework.util.StringUtils;

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

	private class EventSentenceTuple {
		
		private NewsEvent event;
		private String sentence;
		
		public NewsEvent getEvent() {
			return event;
		}
		
		public String getSentence() {
			return sentence;
		}
		
		public EventSentenceTuple(NewsEvent event, String sentence) {
			this.event = event;
			this.sentence = sentence;
		}
	}
	
	private class QueryResultTuple implements Comparable<QueryResultTuple> {
		
		private List<String> query;
		private List<EventSentenceTuple> result;
		
		public List<String> getQuery() {
			return this.query;
		}
		
		public List<EventSentenceTuple> getResult() {
			return this.result;
		}
		
		public QueryResultTuple(List<String> query, List<EventSentenceTuple> result) {
			this.query = query;
			this.result = result;
		}

		@Override
		public int compareTo(QueryResultTuple arg0) {
			return Integer.compare(this.result.size(), arg0.getResult().size());
		}
	}
	
	public void run() {
		ksAdapter.openConnection();
		
		UserModel dummyUserModel = new DummyUserModel();

		Map<String, List<String>> fileNameToKeywordMap = new HashMap<String, List<String>>();
		
		List<QueryResultTuple> queryResultList = new ArrayList<QueryResultTuple>();
		
		for (List<String> query : this.queries) {
			if (log.isInfoEnabled())
				log.info(StringUtils.collectionToCommaDelimitedString(query));
			List<Keyword> queryKeywords = new ArrayList<Keyword>();
			for (String word : query) 
				queryKeywords.add(new Keyword(word));
			
			Set<NewsEvent> events = searcher.findEvents(queryKeywords, dummyUserModel);
			Set<NewsEvent> filteredEvents = filter.filterEvents(events, queryKeywords);
			List<NewsEvent> rankedEvents = ranker.rankEvents(filteredEvents, queryKeywords, dummyUserModel);

			List<EventSentenceTuple> sentenceTuples = new ArrayList<EventSentenceTuple>();
			for (NewsEvent event : rankedEvents) {
				String eventURI = event.getEventURI();
				List<String> sentences = ksAdapter.retrieveSentencesFromEvent(eventURI, query.get(0));
				Collections.shuffle(sentences);
				String sentence = sentences.get(0);
				sentenceTuples.add(new EventSentenceTuple(event, sentence));
			}
			
			if (!rankedEvents.isEmpty())
				queryResultList.add(new QueryResultTuple(query, sentenceTuples));
		}
		
		Collections.sort(queryResultList);
		
		String folderName = String.format("%s/%s", outputFolder, userID);
		File folder = new File(folderName);
		if (!folder.exists())
			folder.mkdirs();

		
		int numberOfEventsStillMissing = this.totalNumberOfEvents;
				
		for (int i = 0; (i < queryResultList.size()) && (numberOfEventsStillMissing > 0); i++) {
			List<String> query = queryResultList.get(i).getQuery();
			List<EventSentenceTuple> events = queryResultList.get(i).getResult();
			
			// take 50% as sorted by best feature, remaining 50% random
			int numberOfEventToTake = Math.min(events.size(), (int) Math.ceil(numberOfEventsStillMissing / (queryResultList.size() - i)));
			List<EventSentenceTuple> selectedEvents = new ArrayList<EventSentenceTuple>(events.subList(0, (int) Math.round(numberOfEventToTake / 2.0)));
			List<EventSentenceTuple> remainder = new ArrayList<EventSentenceTuple>(events.subList((int) Math.round(numberOfEventToTake / 2.0), events.size()));
			Collections.shuffle(remainder);
			selectedEvents.addAll(remainder.subList(0, numberOfEventToTake - selectedEvents.size()));
			
			Collections.shuffle(selectedEvents); // make sure that there's no pattern in the data
			
			numberOfEventsStillMissing = numberOfEventsStillMissing - numberOfEventToTake;
			
			String queryFileName = String.format("%s/query_%d.csv", folderName, i);
			Map<String, String> eventToSentenceMap = new HashMap<String, String>();

			for (EventSentenceTuple event : selectedEvents) {
				String eventURI = event.getEvent().getEventURI();
				String sentence = event.getSentence();
				eventToSentenceMap.put(eventURI, sentence);
			}
			
			Util.writeQueryFile(queryFileName, eventToSentenceMap);
			
			fileNameToKeywordMap.put(queryFileName, query);
			if (log.isInfoEnabled())
				log.info(String.format("%s: %d events", queryFileName, numberOfEventToTake));
		}
		
		if ((numberOfEventsStillMissing > 0) && log.isWarnEnabled())
			log.warn(String.format("Still missing %d events!", numberOfEventsStillMissing));
		
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

		ApplicationContext context = new FileSystemXmlApplicationContext("config/tools-noEmbeddings.xml");
		RankingExamplesGenerator generator = (RankingExamplesGenerator) context.getBean("rankingExamplesGenerator");
		((AbstractApplicationContext) context).close();

		generator.run();
	}

}
