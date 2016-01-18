package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Represents a feature based on a simple SPARQL query w/o any post-processing.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SparqlFeature extends UsabilityFeature {

	public SparqlFeature(String queryFileName) {
		super(queryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		// calculate weighted average over all keywords, based on their weight
		double sum = 0;
		double weightSum = 0;
		for (Keyword keyword : keywords) {
			weightSum += keyword.getWeight();
			sum += this.ksAdapter.runSingleVariableDoubleQuerySingleResult(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI).replace(Util.PLACEHOLDER_KEYWORD, keyword.getStemmedRegex()), Util.VARIABLE_NUMBER);
		}
		double result = sum / weightSum;
		return result;
	}

}
