package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class SimpleCountFeature extends UsabilityFeature {

	private Set<Integer> possibleValues;
	
	private int defaultValue;
	
	public void setPossibleValues(Set<Integer> possibleValues) {
		this.possibleValues = possibleValues;
	}
	
	public void setDefaultValue(int defaultValue) {
		this.defaultValue = defaultValue;
	}
	
	public SimpleCountFeature(String queryFileName, String probabilityFileName) {
		super(queryFileName, probabilityFileName);
	}

	@Override
	public int getValue(String eventURI) {
		double result = this.ksAdapter.runSingleVariableDoubleQuerySingleResult(
				sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_NUMBER);

		int value = (int) result;
		
		if (!this.possibleValues.contains(value))
			value = this.defaultValue;
		
		return value;
	}

}
