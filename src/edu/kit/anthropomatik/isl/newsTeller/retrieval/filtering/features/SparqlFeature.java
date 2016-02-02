package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Represents a feature based on a simple SPARQL query w/o any post-processing.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SparqlFeature extends UsabilityFeature {

	private String valueName;
	
	public void setValueName(String valueName) {
		this.valueName = valueName;
	}
	
	public SparqlFeature() {
		super();
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		// calculate weighted average over all keywords, based on their weight
		double sum = 0;
		double weightSum = 0;
		for (Keyword keyword : keywords) {
			weightSum += keyword.getWeight();
			sum += Util.parseXMLDoubleFromSet(ksAdapter.getBufferedValues(Util.getRelationName("event", valueName, keyword.getWord()), eventURI));
		}
		double result = sum / weightSum;
		return result;
	}

	@Override
	public Set<String> getRequiredMentionProperties() {
		return new HashSet<String>();
	}

}
