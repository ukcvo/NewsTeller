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
		double result = Double.NEGATIVE_INFINITY;
		for (Keyword keyword : keywords) {
			double keywordResult = Util.parseXMLDouble(ksAdapter.getFirstBufferedValue(Util.getRelationName("event", valueName, keyword.getWord()), eventURI));
			result = Math.max(result, keywordResult);
		}
		return result;
	}

	@Override
	public Set<String> getRequiredMentionProperties() {
		return new HashSet<String>();
	}

}
