package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

public class SentenceContainsStringsFeature extends ContainsStringsFeature {

	public SentenceContainsStringsFeature(String stringFileName) {
		super(stringFileName);
	}

	@Override
	protected Set<List<String>> getText(String eventURI, List<Keyword> keywords, UserModel userModel) {
		return ksAdapter.retrieveSentenceTokensFromEvent(eventURI, keywords.get(0).getWord());
	}

}
