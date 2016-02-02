package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Checks if the entities matching the keyword-stems actually match the original keyword.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class KeywordEntityMatchingKeywordFeature extends UsabilityFeature {

	public KeywordEntityMatchingKeywordFeature() {
		super();
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {

		double result = 0;
		
		for (Keyword keyword : keywords) {
			Set<String> entities = ksAdapter.getBufferedValues(Util.getRelationName("event", "keywordEntity", keyword.getWord()), eventURI);
			Set<String> keywordEntityLabels =  new HashSet<String>();
			for (String entity : entities)
				ksAdapter.getBufferedValues(Util.getRelationName("entity", "matchingEntityLabel", keyword.getWord()), entity);
					
			for (String label : keywordEntityLabels) {
				if (label.toLowerCase().matches(keyword.getWordRegex())) {
					result++;
					break;
				}
			}
		}
		result /= keywords.size();
		
		return result;
	}

	@Override
	public Set<String> getRequiredMentionProperties() {
		return new HashSet<String>();
	}

}
