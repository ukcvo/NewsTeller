package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Checks the assigned part of speech for all the mentions and looka how many of them are tagged as verb.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class POSFeature extends UsabilityFeature {

	public POSFeature() {
		super();
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {

		Set<String> mentionURIs = ksAdapter.getBufferedValues(Util.getRelationName("event", "mention", keywords.get(0).getWord()), eventURI);
		
		double result = 0;
		for (String mentionURI : mentionURIs) {
			String posTag = 
					ksAdapter.getFirstBufferedValue(Util.RELATION_NAME_MENTION_PROPERTY + Util.MENTION_PROPERTY_POS, mentionURI);
			if (posTag.equals(Util.MENTION_PROPERTY_POS_VERB))
				result++;
		}
		result = result / mentionURIs.size();
		
		return result;
	}

	@Override
	public Set<String> getRequiredMentionProperties() {
		return Sets.newHashSet(Util.MENTION_PROPERTY_POS);
	}

}
