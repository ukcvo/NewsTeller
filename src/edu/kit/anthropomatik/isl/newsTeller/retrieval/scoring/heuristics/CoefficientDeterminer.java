package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Retrieves some sort of coefficient/characteristic number based on the
 * KnowledgeStore to use for scoring purposes.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class CoefficientDeterminer {

	private static Log log = LogFactory.getLog(CoefficientDeterminer.class);
	
	private KnowledgeStoreAdapter ksAdapter;

	private String query; // the SPARQL-query

	private boolean queryContainsEvent;
	private boolean queryContainsKeyword;
	private boolean queryContainsHistoricalEvent;

	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}

	protected CoefficientDeterminer(String queryFileName) {
		this.query = Util.readStringFromFile(queryFileName);
		this.queryContainsEvent = query.contains(Util.PLACEHOLDER_EVENT);
		this.queryContainsKeyword = query.contains(Util.PLACEHOLDER_KEYWORD);
		this.queryContainsHistoricalEvent = query.contains(Util.PLACEHOLDER_HISTORICAL_EVENT);
	}

	protected List<String> executeQuery(String eventURI, String keyword, String historicalEventURI, String variableName) {

		// region checking correct parameters for query
		if ((eventURI != null) && !queryContainsEvent) {
			if (log.isErrorEnabled())
				log.error(String.format("giving eventURI, but query does not contain event placeholder; returning null: '%s'", query));
			return null;
		}
		if ((eventURI == null) && queryContainsEvent) {
			if (log.isErrorEnabled())
				log.error(String.format("query contains event placeholder, but not giving eventURI; returning null: '%s'", query));
			return null;
		}
		if ((keyword != null) && !queryContainsKeyword) {
			if (log.isErrorEnabled())
				log.error(String.format("giving keyword, but query does not contain keyword placeholder; returning null: '%s'", query));
			return null;
		}
		if ((keyword == null) && queryContainsKeyword) {
			if (log.isErrorEnabled())
				log.error(String.format("query contains keyword placeholder, but not giving keyword; returning null: '%s'", query));
			return null;
		}
		if ((historicalEventURI != null) && !queryContainsHistoricalEvent) {
			if (log.isErrorEnabled())
				log.error(String.format("giving historicalEventURI, but query does not contain historical event placeholder; returning null: '%s'", query));
			return null;
		}
		if ((historicalEventURI == null) && queryContainsHistoricalEvent) {
			if (log.isErrorEnabled())
				log.error(String.format("query contains historical event placeholder, but not giving historicalEventURI; returning null: '%s'", query));
			return null;
		}
		// endregion

		String modifiedQuery = query;
		if (queryContainsEvent)
			modifiedQuery = modifiedQuery.replace(Util.PLACEHOLDER_EVENT, eventURI);
		if (queryContainsKeyword)
			modifiedQuery = modifiedQuery.replace(Util.PLACEHOLDER_KEYWORD, keyword);
		if (queryContainsHistoricalEvent)
			modifiedQuery = modifiedQuery.replace(Util.PLACEHOLDER_HISTORICAL_EVENT, historicalEventURI);

		ksAdapter.openConnection();
		// TODO: maybe just picking first result is too simple
		List<String> result = ksAdapter.runSingleVariableStringQuery(modifiedQuery, variableName);
		ksAdapter.closeConnection();

		return result;
	}

	/**
	 * Determine a coefficient based on the parameters.
	 */
	public abstract double getCoefficient(String eventURI, String keyword, String historicalEventURI);

}
