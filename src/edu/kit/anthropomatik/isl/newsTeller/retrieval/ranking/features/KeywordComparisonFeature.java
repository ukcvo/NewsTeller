package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.embeddings.EmbeddingsProvider;

public class KeywordComparisonFeature extends RankingFeature {

	private static final int AGGREGATION_TYPE_AVG = 0;
	private static final int AGGREGATION_TYPE_MIN = 1;
	private static final int AGGREGATION_TYPE_MAX = 2;
	private static final int AGGREGATION_TYPE_GEOM = 3;
	
	private int aggregationType;
	
	private EmbeddingsProvider embeddings;
	
	private boolean useUserInterestsInsteadOfQuery;
	
	public void setAggregationType(int aggregationType) {
		this.aggregationType = aggregationType;
	}
	
	public void setEmbeddings(EmbeddingsProvider embeddings) {
		this.embeddings = embeddings;
	}
	
	public void setUseUserInterestsInsteadOfQuery(boolean useUserInterestsInsteadOfQuery) {
		this.useUserInterestsInsteadOfQuery = useUserInterestsInsteadOfQuery;
	}
	
	@Override
	public double getValue(String eventURI, List<Keyword> keywords, UserModel userModel) {

		double result;
		switch (this.aggregationType) {
		case AGGREGATION_TYPE_MIN:
			result = Double.POSITIVE_INFINITY;
			break;
		case AGGREGATION_TYPE_GEOM:
			result = 1.0;
			break;
		case AGGREGATION_TYPE_MAX:
			result = Double.NEGATIVE_INFINITY;
			break;
		case AGGREGATION_TYPE_AVG:
		default:
			result = 0.0;
			break;
		}
		
		List<Keyword> keywordsToUse = this.useUserInterestsInsteadOfQuery ? userModel.getInterests() : keywords;
		
		// create keyword vectors
		List<double[]> keywordVectors = new ArrayList<double[]>();
		for (Keyword keyword : keywordsToUse) {
			// preprocess the keyword string as required by embeddings
			String preprocessed = embeddings.getUseLowercase() ? keyword.getWord().toLowerCase() : keyword.getWord();
			List<String> keywordTokens = new ArrayList<String>();
			if (embeddings.getSplitKeywordsIntoTokens()) 
				keywordTokens.addAll(Arrays.asList(preprocessed.split(" ")));
			else {
				if (embeddings.hasWord(preprocessed.replace(" ", "_")))
					keywordTokens.add(preprocessed.replace(" ", "_"));
				else
					keywordTokens.addAll(Arrays.asList(preprocessed.split(" ")));
			}
			
			double[] keywordVector = embeddings.wordsToVector(keywordTokens);
			keywordVectors.add(keywordVector);
		}
		
		int numberOfComparisons = (keywordsToUse.size() * (keywordsToUse.size() - 1))/2;
		
		// now compare them pair-wise
		for (int i = 0; i < keywordVectors.size(); i++) {
			for (int j = i + 1; j < keywordVectors.size(); j++) {
				double similarity = EmbeddingsProvider.cosineSimilarity(keywordVectors.get(i), keywordVectors.get(j));
				
				switch (this.aggregationType) {
				case AGGREGATION_TYPE_AVG:
					result += similarity / numberOfComparisons;
					break;
				case AGGREGATION_TYPE_MAX:
					result = Math.max(result, similarity);
					break;
				case AGGREGATION_TYPE_MIN:
					result = Math.min(result, similarity);
					break;
				case AGGREGATION_TYPE_GEOM:
					result *= similarity;
					break;
				default:
					break;
				}
			}
		}
		
		if (this.aggregationType == AGGREGATION_TYPE_GEOM)
			result = Math.pow((result > 0) ? result : -result, (1.0 / numberOfComparisons));
		
		if (Double.isInfinite(result))
			result = 0;
		
		return result;
	}

}
