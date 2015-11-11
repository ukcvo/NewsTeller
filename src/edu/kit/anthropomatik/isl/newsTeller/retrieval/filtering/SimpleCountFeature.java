package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class SimpleCountFeature extends UsabilityFeature {

	public SimpleCountFeature(String queryFileName, String probabilityFileName) {
		super(queryFileName, probabilityFileName);
	}

	@Override
	public int getValue(String eventURI) {
		double result = this.ksAdapter.runSingleVariableDoubleQuerySingleResult(
				sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_NUMBER);

		return ((int) result);
	}

}
