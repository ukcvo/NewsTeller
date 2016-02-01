package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Counts average number of results for a given property.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class MentionPropertyFeature extends UsabilityFeature {

	private String propertyURI;
	
	public MentionPropertyFeature(String queryFileName, String propertyURI) {
		super(queryFileName);
		this.propertyURI = propertyURI;
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		
		double result = 0;
		
		Set<String> mentionURIs = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_MENTION, eventURI);
		
		for (String mentionURI : mentionURIs) {
			Set<String> propertyValues = ksAdapter.getBufferedValues(Util.RELATION_NAME_MENTION_PROPERTY + propertyURI, mentionURI);
			result += propertyValues.size();
		}
		result = result / mentionURIs.size();
		
		return result;
	}

	@Override
	public void runBulkQueries(Set<String> eventURIs, List<Keyword> keywords) {
		// nothing to do
	}

	@Override
	public Set<String> getRequiredMentionProperties() {
		return Sets.newHashSet(propertyURI);
	}

}
