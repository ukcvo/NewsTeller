package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.List;
import java.util.Set;

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

		Set<String> mentionURIs = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_MENTION + sparqlQueryName, eventURI);
		
		double result = 0;
		for (String mentionURI : mentionURIs) {
			String posTag = 
					ksAdapter.getFirstBufferedValue(Util.RELATION_NAME_MENTION_PROPERTY + sparqlQueryName + Util.MENTION_PROPERTY_POS, mentionURI);
			if (posTag.equals(Util.MENTION_PROPERTY_POS_VERB))
				result++;
		}
		result = result / mentionURIs.size();
		
		return result;
	}

	@Override
	public void runBulkQueries(Set<String> eventURIs, List<Keyword> keywords) {
		ksAdapter.runKeyValueSparqlQuery(sparqlQuery, Util.RELATION_NAME_EVENT_MENTION + sparqlQueryName, Util.VARIABLE_EVENT, 
				Util.VARIABLE_MENTION, eventURIs);
		Set<String> mentionURIs = ksAdapter.getAllRelationValues(Util.RELATION_NAME_EVENT_MENTION + sparqlQueryName);
		ksAdapter.runKeyValueMentionPropertyQuery(Util.MENTION_PROPERTY_POS, Util.RELATION_NAME_MENTION_PROPERTY + sparqlQueryName, mentionURIs);
	}

}
