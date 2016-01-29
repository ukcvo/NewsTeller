package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.List;

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
		
		List<String> mentionURIs = new ArrayList<String>();
		mentionURIs.addAll(ksAdapter.getBufferedValues(Util.RELATION_NAME_MENTION, eventURI));
		if (mentionURIs.isEmpty())
			mentionURIs = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), 
				Util.VARIABLE_MENTION, false);
		
		for (String mentionURI : mentionURIs) {
			List<String> propertyValues = ksAdapter.getMentionProperty(mentionURI, propertyURI); //TODO: use Record.count instead? --> faster?
			result += propertyValues.size();
		}
		result = result / mentionURIs.size();
		
		return result;
	}

}
