package edu.kit.anthropomatik.isl.newsTeller.retrieval.finding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import edu.kit.anthropomatik.isl.newsTeller.data.ConversationCycle;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Responsible for finding many potentially relevant events.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class EventFinder {

	private class QueryWorker implements Callable<List<NewsEvent>> {

		private String query;
		
		public QueryWorker(String query) {
			this.query = query;
		}
		
		public List<NewsEvent> call() throws Exception {
			long t1 = System.currentTimeMillis();
			List<NewsEvent> result = ksAdapter.runSingleVariableEventQuery(query, Util.VARIABLE_EVENT);
			long t2 = System.currentTimeMillis();
			if (log.isInfoEnabled())
				log.info(t2-t1);
			return result;
		}
		
	}
	
	private static Log log = LogFactory.getLog(EventFinder.class);

	// access to KnowledgeStore
	private KnowledgeStoreAdapter ksAdapter;

	private ExecutorService threadPool;
	
	private List<String> userQuerySPARQLTemplates; // SPARQL queries based on user query keyword

	@SuppressWarnings("unused")
	private List<String> userInterestSPARQLTemplates; // SPARQL queries based on user interests keyword

	@SuppressWarnings("unused")
	private List<String> previousEventSPARQLTemplates; // SPARQL queries based on conversation history event

	private SnowballStemmer stemmer;
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public void setNThreads(int nThreads) {
		if (nThreads == 0)
			this.threadPool = Executors.newCachedThreadPool();
		else
			this.threadPool = Executors.newFixedThreadPool(nThreads);
	}

	public EventFinder(String userQueryConfigFileName, String userInterestConfigFileName, String previousEventConfigFileName) {
		this.userQuerySPARQLTemplates = Util.readQueriesFromConfigFile(userQueryConfigFileName);
		this.userInterestSPARQLTemplates = Util.readQueriesFromConfigFile(userInterestConfigFileName);
		this.previousEventSPARQLTemplates = Util.readQueriesFromConfigFile(previousEventConfigFileName);
		this.stemmer = new englishStemmer();
	}

	// stem the keyword before applying the query
	private String stemKeyword(String keyword) {
		stemmer.setCurrent(keyword);
		stemmer.stem();
		String result = stemmer.getCurrent();
		if (result.endsWith("i"))
			result = result.substring(0, result.length()-1) + "(i|y)";
		return result;
	}
	
	// use keywords from user query to find events
	private Set<NewsEvent> processUserQuery(List<Keyword> userQuery) {
		if (log.isTraceEnabled())
			log.trace(String.format("processUserQuery(userQuery = <%s>)", StringUtils.collectionToCommaDelimitedString(userQuery)));

		Set<NewsEvent> events = new HashSet<NewsEvent>();

		long stemmingTime = 0;
		long setUpTime = 0;
		long waitTime = 0;
		long mergeTime = 0;
		long t1 = 0;
		long t2 = 0;
		
		t1 = System.currentTimeMillis();
		List<String> stemmedKeywords = new ArrayList<String>();
		for (Keyword keyword : userQuery)
			stemmedKeywords.add(stemKeyword(keyword.getWord()));
		t2 = System.currentTimeMillis();
		stemmingTime = (t2 - t1);
		
		t1 = System.currentTimeMillis();
		List<Future<List<NewsEvent>>> futures = new ArrayList<Future<List<NewsEvent>>>();
		for (String sparqlQuery : userQuerySPARQLTemplates) {
			// TODO: generalize to multiple keywords (Scope 3)
			String keywordStem = stemmedKeywords.get(0);
			
			QueryWorker w = new QueryWorker(sparqlQuery.replace("*k*", keywordStem));
			futures.add(threadPool.submit(w));
		}
		t2 = System.currentTimeMillis();
		setUpTime = (t2 - t1);
		
		t1 = System.currentTimeMillis();
		for (Future<List<NewsEvent>> f : futures) {
			try {
				f.get();
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("thread execution somehow failed!");
				if (log.isDebugEnabled())
					log.debug("thread execution exception", e);
			} 
		}
		t2 = System.currentTimeMillis();
		waitTime = (t2 - t1);
		
		t1 = System.currentTimeMillis();
		for (Future<List<NewsEvent>> f : futures) {
			try {
				events.addAll(f.get());
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("thread execution somehow failed!");
				if (log.isDebugEnabled())
					log.debug("thread execution exception", e);
			} 
		}
		t2 = System.currentTimeMillis();
		mergeTime = (t2 - t1);
		if (log.isInfoEnabled())
			log.info(String.format("stem: %d, setUp: %d, wait: %d, merge: %d, TOTAL: %d", 
					stemmingTime, setUpTime, waitTime, mergeTime, stemmingTime + setUpTime + waitTime + mergeTime));
		
		return events;
	}

	// use keywords from user interests to find events
	private Set<NewsEvent> processUserInterests(List<Keyword> userInterests) {
		if (log.isTraceEnabled())
			log.trace(String.format("processUserInterests(userInterests = <%s>)", StringUtils.collectionToCommaDelimitedString(userInterests)));
		// TODO: implement (Scope 4)
		return new HashSet<NewsEvent>();
	}

	// use events from previous conversation cycles to find events
	private Set<NewsEvent> processConversationHistory(List<ConversationCycle> conversationHistory) {
		if (log.isTraceEnabled())
			log.trace(String.format("processConversationHistory(conversationHistory = <%s>)", StringUtils.collectionToCommaDelimitedString(conversationHistory)));
		// TODO: implement (Scope 7)
		return new HashSet<NewsEvent>();
	}

	/**
	 * Find potentially relevant events.
	 */
	public Set<NewsEvent> findEvents(List<Keyword> userQuery, UserModel userModel) {
		if (log.isTraceEnabled())
			log.trace(String.format("findEvents(userQuery = <%s>, userModel = %s)", StringUtils.collectionToCommaDelimitedString(userQuery), userModel.toString()));

		Set<NewsEvent> events = new HashSet<NewsEvent>();
		
		if (userQuery != null && !userQuery.isEmpty()) //TODO: temporary fix, remove in Scope 3
			events.addAll(processUserQuery(userQuery));
		events.addAll(processUserInterests(userModel.getInterests()));
		events.addAll(processConversationHistory(userModel.getHistory()));
		
		return events;
	}

	/**
	 * Closes the threadpool.
	 */
	public void shutDown() {
		this.threadPool.shutdown();
	}
	
}
