package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Represents a feature based on an AVG query (returns a double).
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SimpleAverageFeature extends BinBasedFeature {

	public SimpleAverageFeature(String queryFileName, String probabilityFileName) {
		super(queryFileName, probabilityFileName);
	}
	
	@Override
	protected double getRawValue(String eventURI) {
		// simply run the query
		return this.ksAdapter.runSingleVariableDoubleQuerySingleResult(
				sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_NUMBER);
	}

}
