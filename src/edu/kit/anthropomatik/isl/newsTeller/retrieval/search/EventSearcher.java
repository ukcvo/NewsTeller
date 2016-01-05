package edu.kit.anthropomatik.isl.newsTeller.retrieval.search;

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
public class EventSearcher {

	private class QueryWorker implements Callable<List<NewsEvent>> {

		private String query;
		
		public QueryWorker(String query) {
			this.query = query;
		}
		
		public List<NewsEvent> call() throws Exception {
			List<NewsEvent> result = ksAdapter.runSingleVariableEventQuery(query, Util.VARIABLE_EVENT);
			return result;
		}
		
	}
	
	private static Log log = LogFactory.getLog(EventSearcher.class);

	// access to KnowledgeStore
	private KnowledgeStoreAdapter ksAdapter;

	private ExecutorService threadPool;
	
	private List<String> userQuerySPARQLTemplates; // SPARQL queries based on user query keyword

	@SuppressWarnings("unused")
	private List<String> userInterestSPARQLTemplates; // SPARQL queries based on user interests keyword

	@SuppressWarnings("unused")
	private List<String> previousEventSPARQLTemplates; // SPARQL queries based on conversation history event

	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public void setNThreads(int nThreads) {
		if (nThreads == 0)
			this.threadPool = Executors.newCachedThreadPool();
		else
			this.threadPool = Executors.newFixedThreadPool(nThreads);
	}

	public EventSearcher(String userQueryConfigFileName, String userInterestConfigFileName, String previousEventConfigFileName) {
		this.userQuerySPARQLTemplates = Util.readQueriesFromConfigFile(userQueryConfigFileName);
		this.userInterestSPARQLTemplates = Util.readQueriesFromConfigFile(userInterestConfigFileName);
		this.previousEventSPARQLTemplates = Util.readQueriesFromConfigFile(previousEventConfigFileName);
	}

	// use keywords from user query to find events
	private Set<NewsEvent> processUserQuery(List<Keyword> userQuery) {
		if (log.isTraceEnabled())
			log.trace(String.format("processUserQuery(userQuery = <%s>)", StringUtils.collectionToCommaDelimitedString(userQuery)));

		Set<NewsEvent> events = new HashSet<NewsEvent>();

		for (Keyword keyword : userQuery)
			Util.stemKeyword(keyword);
		
		List<Future<List<NewsEvent>>> futures = new ArrayList<Future<List<NewsEvent>>>();
		for (String sparqlQuery : userQuerySPARQLTemplates) {
			// TODO: generalize to multiple keywords (Scope 3)
			String keywordRegex = userQuery.get(0).getStemmedRegex();
			
			QueryWorker w = new QueryWorker(sparqlQuery.replace("*k*", keywordRegex));
			futures.add(threadPool.submit(w));
		}
		
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
