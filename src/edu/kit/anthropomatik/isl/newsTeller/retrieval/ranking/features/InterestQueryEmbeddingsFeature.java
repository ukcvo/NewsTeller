package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

/**
 * Feature that compares the user interests to the user query.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class InterestQueryEmbeddingsFeature extends EmbeddingsFeature {

	public InterestQueryEmbeddingsFeature(String stopWordsFileName) {
		super(stopWordsFileName);
	}

	@Override
	protected Set<String> getComparisonStrings(String eventURI, List<Keyword> keywords, UserModel userModel) {
		Set<String> comparisonStrings = new HashSet<String>();
		
		for (Keyword keyword : userModel.getInterests())
			comparisonStrings.add(keyword.getWord());
		
		return comparisonStrings;
	}

}
