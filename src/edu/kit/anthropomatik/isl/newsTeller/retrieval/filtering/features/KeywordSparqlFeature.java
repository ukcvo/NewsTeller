package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class KeywordSparqlFeature extends UsabilityFeature {

	public KeywordSparqlFeature(String queryFileName) {
		super(queryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		// calculate weighted average over all features, based on their weight
		double sum = 0;
		double weightSum = 0;
		for (Keyword keyword : keywords) {
			weightSum += keyword.getWeight();
			sum += this.ksAdapter.runSingleVariableDoubleQuerySingleResult(
					sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI).replace(Util.PLACEHOLDER_KEYWORD, keyword.getWord()), Util.VARIABLE_NUMBER);
		}
		double result = sum/weightSum;
		return result;
	}

}
