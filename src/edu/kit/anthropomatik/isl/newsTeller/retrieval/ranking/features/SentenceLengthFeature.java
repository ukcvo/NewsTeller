package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.List;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

/**
 * Looks at the sentence length.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SentenceLengthFeature extends RankingFeature {

	private static final int AGGREGATION_TYPE_AVG = 0;
	private static final int AGGREGATION_TYPE_MIN = 1;
	private static final int AGGREGATION_TYPE_MAX = 2;
	
	private boolean useWordCount;
	
	private int aggregationType;
	
	public void setUseWordCount(boolean useWordCount) {
		this.useWordCount = useWordCount;
	}
	
	public void setAggregationType(int aggregationType) {
		this.aggregationType = aggregationType;
	}
	
	@Override
	public double getValue(String eventURI, List<Keyword> keywords, UserModel userModel) {
		
		double result = (this.aggregationType == AGGREGATION_TYPE_MIN) ? Double.POSITIVE_INFINITY : 0;
		
		List<String> sentences = ksAdapter.retrieveSentencesFromEvent(eventURI, keywords.get(0).getWord());
		
		for (String sentence : sentences) {
			double sentenceResult = this.useWordCount ? sentence.split(" ").length : sentence.length();
			
			switch (this.aggregationType) {
			case AGGREGATION_TYPE_AVG:
				result += sentenceResult / sentences.size();
				break;
			case AGGREGATION_TYPE_MIN:
				result = Math.min(result, sentenceResult);
				break;
			case AGGREGATION_TYPE_MAX:
				result = Math.max(result, sentenceResult);
				break;
			default:
				break;
			}
		}
		
		return result;
	}

}
