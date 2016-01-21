package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Checks if the entities matching the keyword-stems actually match the original keyword.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class KeywordEntityMatchingKeywordFeature extends UsabilityFeature {

	public KeywordEntityMatchingKeywordFeature(String queryFileName) {
		super(queryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {

		double result = 0;
		
		for (Keyword keyword : keywords) {
			List<String> keywordEntityLabels = 
					ksAdapter.runSingleVariableStringQuery(
						sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI).replace(Util.PLACEHOLDER_KEYWORD, keyword.getStemmedRegex()), 
						Util.VARIABLE_LABEL);
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

}
