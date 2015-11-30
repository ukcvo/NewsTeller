package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Represents a feature based on a simple SPARQL query using only the eventURI and using the SPARQL result as feature value.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SimpleSparqlFeature extends UsabilityFeature {

	public SimpleSparqlFeature(String queryFileName) {
		super(queryFileName);
	}
	
	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		// simply run the query
		return this.ksAdapter.runSingleVariableDoubleQuerySingleResult(
				sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_NUMBER);
	}

}
