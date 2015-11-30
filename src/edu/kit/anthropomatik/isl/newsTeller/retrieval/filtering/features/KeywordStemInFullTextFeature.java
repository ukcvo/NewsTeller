package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.List;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;

/**
 * Checks if the keywords appear anywhere in the orginal text.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class KeywordStemInFullTextFeature extends FullTextFeature {

	public KeywordStemInFullTextFeature(String queryFileName) {
		super(queryFileName);
	}

	@Override
	protected List<List<String>> getLabels(String eventURI, List<Keyword> keywords) {
		List<List<String>> result = new ArrayList<List<String>>();
		
		for (Keyword keyword : keywords) {
			List<String> list = new ArrayList<String>();
			list.add(keyword.getStem());
			result.add(list);
		}
		
		return result;
	}

}
