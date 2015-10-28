package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Gets a number by executing a SPARQL query.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SPARQLNumberGetter implements INumberGetter {

	private static Log log = LogFactory.getLog(SPARQLNumberGetter.class);
	
	private KnowledgeStoreAdapter ksAdapter;
	
	private String query; // the SPARQL-query
	
	private boolean queryContainsEvent;
	private boolean queryContainsKeyword;
	private boolean queryContainsHistoricalEvent;
	
	public SPARQLNumberGetter(String queryFileName) {
		this.query = Util.readQueryFromFile(queryFileName);
		this.queryContainsEvent = query.contains(Util.PLACEHOLDER_EVENT);
		this.queryContainsKeyword = query.contains(Util.PLACEHOLDER_KEYWORD);
		this.queryContainsHistoricalEvent = query.contains(Util.PLACEHOLDER_HISTORICAL_EVENT);
	}
	
	public double getNumber(String eventURI, String keyword, String historicalEventURI) {
		
		//region checking correct parameters for query
		if ((eventURI != null) && !queryContainsEvent) {
			if(log.isErrorEnabled())
				log.error(String.format("giving eventURI, but query does not contain event placeholder; returning zero: '%s'", query));
			return 0;
		}
		if ((eventURI == null) && queryContainsEvent) {
			if(log.isErrorEnabled())
				log.error(String.format("query contains event placeholder, but not giving eventURI; returning zero: '%s'", query));
			return 0;
		}
		if ((keyword != null) && !queryContainsKeyword) {
			if(log.isErrorEnabled())
				log.error(String.format("giving keyword, but query does not contain keyword placeholder; returning zero: '%s'", query));
			return 0;
		}
		if ((keyword == null) && queryContainsKeyword) {
			if(log.isErrorEnabled())
				log.error(String.format("query contains keyword placeholder, but not giving keyword; returning zero: '%s'", query));
			return 0;
		}
		if ((historicalEventURI != null) && !queryContainsHistoricalEvent) {
			if(log.isErrorEnabled())
				log.error(String.format("giving historicalEventURI, but query does not contain historical event placeholder; returning zero: '%s'", 
						query));
			return 0;
		}
		if ((historicalEventURI == null) && queryContainsHistoricalEvent) {
			if(log.isErrorEnabled())
				log.error(String.format("query contains historical event placeholder, but not giving historicalEventURI; returning zero: '%s'", 
						query));
			return 0;
		}
		//endregion
		
		String modifiedQuery = query;
		if (queryContainsEvent)
			modifiedQuery = modifiedQuery.replace(Util.PLACEHOLDER_EVENT, eventURI);
		if (queryContainsKeyword)
			modifiedQuery = modifiedQuery.replace(Util.PLACEHOLDER_KEYWORD, keyword);
		if (queryContainsHistoricalEvent)
			modifiedQuery = modifiedQuery.replace(Util.PLACEHOLDER_HISTORICAL_EVENT, historicalEventURI);
		
		// TODO: maybe just picking first result is too simple
		double number = ksAdapter.runSingleVariableDoubleQuerySingleResult(modifiedQuery, Util.VARIABLE_NUMBER);
		
		return number;
	}

}
