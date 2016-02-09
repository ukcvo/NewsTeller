package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class KeywordEntitiesFeature extends RankingFeature {

	private static final int AGGREGATION_TYPE_AVG = 0;
	private static final int AGGREGATION_TYPE_MIN = 1;
	private static final int AGGREGATION_TYPE_MAX = 2;
	
	private int aggregationType;
	
	private String labelType;
	
	private boolean useStemInsteadOfWord;
	
	private boolean normalizeResult;
	
	public void setAggregationType(int aggregationType) {
		this.aggregationType = aggregationType;
	}
	
	public void setLabelType(String labelType) {
		this.labelType = labelType;
	}
	
	public void setUseStemInsteadOfWord(boolean useStemInsteadOfWord) {
		this.useStemInsteadOfWord = useStemInsteadOfWord;
	}
	
	public void setNormalizeResult(boolean normalizeResult) {
		this.normalizeResult = normalizeResult;
	}
	
	@Override
	public double getValue(String eventURI, List<Keyword> keywords, UserModel userModel) {
		double result = (this.aggregationType == AGGREGATION_TYPE_MIN) ? Double.POSITIVE_INFINITY : 0.0;
		
		// TODO: userModel
		List<Keyword> keywordsToUse = keywords;
				
		Set<String> entities = 	ksAdapter.getBufferedValues(Util.getRelationName("event", "entity", keywords.get(0).getWord()), eventURI);
		
		Set<Set<String>> entityLabels = new HashSet<Set<String>>();
		for (String entity : entities)
			entityLabels.add(ksAdapter.getBufferedValues(Util.getRelationName("entity", this.labelType, keywords.get(0).getWord()), entity));
		
		for (Keyword keyword : keywordsToUse) {
			double keywordResult = 0;
			String keywordString = this.useStemInsteadOfWord ? keyword.getStem().toLowerCase() : keyword.getWord().toLowerCase();
			
			for (Set<String> entity : entityLabels) {
				for (String label :entity) {
					if (label.toLowerCase().contains(keywordString)) {
						keywordResult++;
						break;
					}	
				}
			}
			
			if (this.normalizeResult)
				keywordResult /= entities.size();
			
			switch (this.aggregationType) {
			case AGGREGATION_TYPE_AVG:
				result += keywordResult / keywordsToUse.size();
				break;
			case AGGREGATION_TYPE_MIN:
				result = Math.min(result, keywordResult);
				break;
			case AGGREGATION_TYPE_MAX:
				result = Math.max(result, keywordResult);
				break;
			default:
				break;
			}
		}
		
		return result;
	}

}
