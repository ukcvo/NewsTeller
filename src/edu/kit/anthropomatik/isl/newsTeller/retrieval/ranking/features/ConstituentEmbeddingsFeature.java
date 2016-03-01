package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class ConstituentEmbeddingsFeature extends EmbeddingsFeature {

	private String valueName;
	
	private String entityName;
	
	private boolean needsKeywordIteration;
	
	public void setValueName(String valueName) {
		this.valueName = valueName;
	}
	
	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
	
	public void setNeedsKeywordIteration(boolean needsKeywordIteration) {
		this.needsKeywordIteration = needsKeywordIteration;
	}
	
	public ConstituentEmbeddingsFeature(String stopWordsFileName) {
		super(stopWordsFileName);
		this.shouldWarnIfNoComparisonStrings = false;
	}

	@Override
	protected Set<String> getComparisonStrings(String eventURI, List<Keyword> keywords, UserModel userModel) {
		Set<String> comparisonStrings = new HashSet<String>();
		
		List<Keyword> keywordsToUse = this.useUserInterestsInsteadOfQuery ? userModel.getInterests() : keywords;
		if (keywordsToUse.isEmpty())
			return comparisonStrings;
		
		if (this.needsKeywordIteration) {
			
			for (Keyword keyword : keywordsToUse) {
				Set<String> constituents = ksAdapter.getBufferedValues(Util.getRelationName("event", entityName, keyword.getWord()), eventURI);
				
				for (String entity : constituents)
					comparisonStrings.addAll(ksAdapter.getBufferedValues(Util.getRelationName("entity", valueName, keyword.getWord()), entity));
			}
			
		} else {
			String arbitraryKeyword = keywordsToUse.get(0).getWord();
			Set<String> constituents = ksAdapter.getBufferedValues(Util.getRelationName("event", entityName, arbitraryKeyword), eventURI);
			for (String entity : constituents)
				comparisonStrings.addAll(ksAdapter.getBufferedValues(Util.getRelationName("entity", valueName, arbitraryKeyword), entity));
		}
		
		return comparisonStrings;
	}

}
