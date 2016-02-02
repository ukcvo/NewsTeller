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

//	private class QueryWorker implements Callable<List<NewsEvent>> {
//
//		private String query;
//		
//		public QueryWorker(String query) {
//			this.query = query;
//		}
//		
//		public List<NewsEvent> call() throws Exception {
//			List<NewsEvent> result = ksAdapter.runSingleVariableEventQuery(query, Util.VARIABLE_EVENT);
//			return result;
//		}
//		
//	}
	
	private static Log log = LogFactory.getLog(EventSearcher.class);

	// access to KnowledgeStore
	private KnowledgeStoreAdapter ksAdapter;

	private ExecutorService threadPool;
	
	private String standardSparqlQuery;
	
	private String fallbackSparqlQuery;
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public void setNThreads(int nThreads) {
		if (nThreads == 0)
			this.threadPool = Executors.newCachedThreadPool();
		else
			this.threadPool = Executors.newFixedThreadPool(nThreads);
	}

	public EventSearcher(String standardSparqlQueryFileName, String fallbackSparqlQueryFileName) {
		this.standardSparqlQuery = Util.readStringFromFile(standardSparqlQueryFileName);
		this.fallbackSparqlQuery = Util.readStringFromFile(fallbackSparqlQueryFileName);
	}

	/**
	 * Find potentially relevant events.
	 */
	public Set<NewsEvent> findEvents(List<Keyword> userQuery, UserModel userModel) {
		if (log.isTraceEnabled())
			log.trace(String.format("findEvents(userQuery = <%s>, userModel = %s)", StringUtils.collectionToCommaDelimitedString(userQuery), userModel.toString()));

		Set<NewsEvent> events = new HashSet<NewsEvent>();
		
		for (Keyword keyword : userQuery) {
			Util.stemKeyword(keyword);
			String keywordRegex = keyword.getStemmedRegex();
			String bifContainsString = keyword.getBifContainsString();
			events.addAll(ksAdapter.runSingleVariableEventQuery(
					standardSparqlQuery.replace(Util.PLACEHOLDER_BIF_CONTAINS, bifContainsString).replace(Util.PLACEHOLDER_KEYWORD, keywordRegex), 
					Util.VARIABLE_EVENT, 5000L));
			if (events.isEmpty())
				events.addAll(ksAdapter.runSingleVariableEventQuery(
						fallbackSparqlQuery.replace(Util.PLACEHOLDER_KEYWORD, keywordRegex), Util.VARIABLE_EVENT, 5000L));
		}
			
		
//		List<Future<List<NewsEvent>>> futures = new ArrayList<Future<List<NewsEvent>>>();
		
		
//		for (String sparqlQuery : userQuerySPARQLTemplates) {
//			// TODO: generalize to multiple keywords (Scope 3)
//			String keywordRegex = userQuery.get(0).getStemmedRegex();
//			
//			QueryWorker w = new QueryWorker(sparqlQuery.replace("*k*", keywordRegex));
//			futures.add(threadPool.submit(w));
//		}
//		
//		for (Future<List<NewsEvent>> f : futures) {
//			try {
//				f.get();
//			} catch (Exception e) {
//				if (log.isErrorEnabled())
//					log.error("thread execution somehow failed!");
//				if (log.isDebugEnabled())
//					log.debug("thread execution exception", e);
//			} 
//		}
//		
//		for (Future<List<NewsEvent>> f : futures) {
//			try {
//				events.addAll(f.get());
//			} catch (Exception e) {
//				if (log.isErrorEnabled())
//					log.error("thread execution somehow failed!");
//				if (log.isDebugEnabled())
//					log.debug("thread execution exception", e);
//			} 
//		}
		
		return events;
	}

	/**
	 * Closes the threadpool.
	 */
	public void shutDown() {
		this.threadPool.shutdown();
	}
	
}
