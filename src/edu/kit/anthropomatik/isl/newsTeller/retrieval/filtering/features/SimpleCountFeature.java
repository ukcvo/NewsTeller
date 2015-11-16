package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Represents a feature based on a COUNT query.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
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
		
		if (!this.possibleValues.isEmpty() && !this.possibleValues.contains(value))
			value = this.defaultValue;
		
		return value;
	}

}
