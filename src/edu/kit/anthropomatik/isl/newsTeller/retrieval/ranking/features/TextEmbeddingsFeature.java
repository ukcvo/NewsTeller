package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

/**
 * Feature for comparing keyword embeddings to text embeddings (i.e. either sentence or title).
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class TextEmbeddingsFeature extends EmbeddingsFeature {

	private boolean useTitleInsteadOfSentence;
	
	public void setUseTitleInsteadOfSentence(boolean useTitleInsteadOfSentence) {
		this.useTitleInsteadOfSentence = useTitleInsteadOfSentence;
	}
	
	public TextEmbeddingsFeature(String stopWordsFileName) {
		super(stopWordsFileName);
	}

	@Override
	protected Set<String> getComparisonStrings(String eventURI, List<Keyword> keywords, UserModel userModel) {
		Set<String> comparisonStrings = new HashSet<String>();
		if (this.useTitleInsteadOfSentence) // use titles
			comparisonStrings.addAll(ksAdapter.getResourceTitlesFromEvent(eventURI, keywords.get(0).getWord()));
		else // use actual sentences
			comparisonStrings.addAll(ksAdapter.retrieveSentencesFromEvent(eventURI, keywords.get(0).getWord()));
		return comparisonStrings;
	}
}
