package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

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
		
		Set<String> mentionURIs = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_MENTION + sparqlQueryName, eventURI);
		
		for (String mentionURI : mentionURIs) {
			Set<String> propertyValues = ksAdapter.getBufferedValues(Util.RELATION_NAME_MENTION_PROPERTY + sparqlQueryName + propertyURI, mentionURI);
			result += propertyValues.size();
		}
		result = result / mentionURIs.size();
		
		return result;
	}

	@Override
	public void runBulkQueries(Set<String> eventURIs, List<Keyword> keywords) {
		ksAdapter.runKeyValueSparqlQuery(sparqlQuery, Util.RELATION_NAME_EVENT_MENTION + sparqlQueryName, Util.VARIABLE_EVENT, 
				Util.VARIABLE_MENTION, eventURIs);
		Set<String> mentionURIs = ksAdapter.getAllRelationValues(Util.RELATION_NAME_EVENT_MENTION + sparqlQueryName);
		ksAdapter.runKeyValueMentionPropertyQuery(propertyURI, Util.RELATION_NAME_MENTION_PROPERTY + sparqlQueryName, mentionURIs);
	}

}
