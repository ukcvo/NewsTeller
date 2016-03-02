package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class SparqlCountFeature extends RankingFeature {

	private static final int AGGREGATION_TYPE_AVG = 0;
	private static final int AGGREGATION_TYPE_MIN = 1;
	private static final int AGGREGATION_TYPE_MAX = 2;
	
	private int aggregationType;
	
	public void setAggregationType(int aggregationType) {
		this.aggregationType = aggregationType;
	}
	
	private String valueName;
	
	public void setValueName(String valueName) {
		this.valueName = valueName;
	}
	
	private boolean useUserInterestsInsteadOfQuery;
	
	public void setUseUserInterestsInsteadOfQuery(boolean useUserInterestsInsteadOfQuery) {
		this.useUserInterestsInsteadOfQuery = useUserInterestsInsteadOfQuery;
	}
	
	@Override
	public double getValue(String eventURI, List<Keyword> keywords, UserModel userModel) {
		
		List<Keyword> keywordsToUse = this.useUserInterestsInsteadOfQuery ? userModel.getInterests() : keywords;
		double result = (this.aggregationType == AGGREGATION_TYPE_MIN) ? Double.POSITIVE_INFINITY : 0.0;
		
		for (Keyword keyword : keywordsToUse) {
			double keywordResult = Util.parseXMLDouble(ksAdapter.getFirstBufferedValue(Util.getRelationName("event", valueName, keyword.getWord()), eventURI));

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
		
		if (Double.isInfinite(result)) // dirty fix to avoid Infinities
			result = Double.NaN;
		
		return result;
	}

}
