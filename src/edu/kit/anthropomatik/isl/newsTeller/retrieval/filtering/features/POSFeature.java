package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Checks the assigned part of speech for all the mentions and looka how many of them are tagged as verb.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class POSFeature extends UsabilityFeature {

	public POSFeature(String queryFileName) {
		super(queryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {

		List<String> mentionURIs = new ArrayList<String>();
		mentionURIs.addAll(ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_MENTION, eventURI));
		if (mentionURIs.isEmpty())
			mentionURIs = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), 
				Util.VARIABLE_MENTION, false);
		
		double result = 0;
		for (String mentionURI : mentionURIs) {
			String posTag = ksAdapter.getUniqueMentionProperty(mentionURI, Util.MENTION_PROPERTY_POS);
			if (posTag.equals(Util.MENTION_PROPERTY_POS_VERB))
				result++;
		}
		result = result / mentionURIs.size();
		
		return result;
	}

}
