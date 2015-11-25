package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Represents a feature based on a simple SPARQL query w/o any post-processing.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SimpleSparqlFeature extends UsabilityFeature {

	public SimpleSparqlFeature(String queryFileName) {
		super(queryFileName);
	}
	
	@Override
	public double getValue(String eventURI) {
		// simply run the query
		return this.ksAdapter.runSingleVariableDoubleQuerySingleResult(
				sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_NUMBER);
	}

}
