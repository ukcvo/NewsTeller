package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.embeddings.EmbeddingsProvider;
import edu.stanford.nlp.simple.Sentence;

public class EmbeddingsFeature extends RankingFeature {

	private static Log log = LogFactory.getLog(EmbeddingsFeature.class);
	
	private static final int AGGREGATION_TYPE_AVG = 0;
	private static final int AGGREGATION_TYPE_MIN = 1;
	private static final int AGGREGATION_TYPE_MAX = 2;
	private static final int AGGREGATION_TYPE_GEOM = 3;
	
	private int sentenceAggregationType;
	private int keywordAggregationType;
	
	private boolean useTitleInsteadOfSentence;
	
	private EmbeddingsProvider embeddings;
	
	public void setSentenceAggregationType(int sentenceAggregationType) {
		this.sentenceAggregationType = sentenceAggregationType;
	}
	
	public void setKeywordAggregationType(int keywordAggregationType) {
		this.keywordAggregationType = keywordAggregationType;
	}
	
	public void setUseTitleInsteadOfSentence(boolean useTitleInsteadOfSentence) {
		this.useTitleInsteadOfSentence = useTitleInsteadOfSentence;
	}
	
	public void setEmbeddings(EmbeddingsProvider embeddings) {
		this.embeddings = embeddings;
	}
		
	@Override
	public double getValue(String eventURI, List<Keyword> keywords, UserModel userModel) {
		
		double result;
		switch (this.keywordAggregationType) {
		case AGGREGATION_TYPE_MIN:
			result = Double.POSITIVE_INFINITY;
			break;
		case AGGREGATION_TYPE_GEOM:
			result = 1.0;
			break;
		case AGGREGATION_TYPE_AVG:
		case AGGREGATION_TYPE_MAX:
		default:
			result = 0.0;
			break;
		}
		
		// TODO: can make this also applicable to userModel.getInterest() --> boolean flag in class
		List<Keyword> keywordsToUse = keywords;
		
		Set<String> sentences = new HashSet<String>();
		if (this.useTitleInsteadOfSentence) // use titles
			sentences.addAll(ksAdapter.getResourceTitlesFromEvent(eventURI, keywords.get(0).getWord()));
		else // use actual sentences
			sentences.addAll(ksAdapter.retrieveSentencesFromEvent(eventURI, keywords.get(0).getWord()));
		
		if (sentences.isEmpty()) {
			if (log.isWarnEnabled())
				log.warn(String.format("no sentences for event '%s', returning 0", eventURI));
			return 0;
		}
		
		List<double[]> sentenceVectors = new ArrayList<double[]>();
		for (String sentence : sentences) {
			if (sentence.isEmpty())
				continue;
			String preprocessed = embeddings.getUseLowercase() ? sentence.toLowerCase() : sentence;
			Sentence s = new Sentence(preprocessed);
			double[] vector = embeddings.wordsToVector(s.words());
			if (vector != null)
				sentenceVectors.add(vector);
		}
		
		for (Keyword keyword : keywordsToUse) {
			double keywordResult;
			switch (this.keywordAggregationType) {
			case AGGREGATION_TYPE_MIN:
				keywordResult = Double.POSITIVE_INFINITY;
				break;
			case AGGREGATION_TYPE_GEOM:
				keywordResult = 1.0;
				break;
			case AGGREGATION_TYPE_AVG:
			case AGGREGATION_TYPE_MAX:
			default:
				keywordResult = 0.0;
				break;
			}
			
			// preprocess the keyword string as required by embeddings
			String preprocessed = embeddings.getUseLowercase() ? keyword.getWord().toLowerCase() : keyword.getWord();
			List<String> keywordTokens = new ArrayList<String>();
			if (embeddings.getSplitKeywordsIntoTokens())
				keywordTokens.addAll(Arrays.asList(preprocessed.split(" ")));
			else
				keywordTokens.add(preprocessed.replace(" ", "_"));
			double[] keywordVector = embeddings.wordsToVector(keywordTokens);
			
			if (keywordVector == null)
				continue;
			
			for (double[] sentenceVector : sentenceVectors) {
				double similarity = EmbeddingsProvider.cosineSimilarity(keywordVector, sentenceVector);
				
				switch (this.sentenceAggregationType) {
				case AGGREGATION_TYPE_AVG:
					keywordResult += similarity / sentenceVectors.size();
					break;
				case AGGREGATION_TYPE_MAX:
					keywordResult = Math.max(keywordResult, similarity);
					break;
				case AGGREGATION_TYPE_MIN:
					keywordResult = Math.min(keywordResult, similarity);
					break;
				case AGGREGATION_TYPE_GEOM:
					keywordResult *= similarity;
					break;
				default:
					break;
				}
			}
			if (this.sentenceAggregationType == AGGREGATION_TYPE_GEOM)
				keywordResult = Math.pow(keywordResult, (1.0 / sentenceVectors.size()));
			
			switch (this.keywordAggregationType) {
			case AGGREGATION_TYPE_AVG:
				result += keywordResult / keywordsToUse.size();
				break;
			case AGGREGATION_TYPE_MAX:
				result = Math.max(result, keywordResult);
				break;
			case AGGREGATION_TYPE_MIN:
				result = Math.min(result, keywordResult);
				break;
			case AGGREGATION_TYPE_GEOM:
				result *= keywordResult;
				break;
			default:
				break;
			}
		}
		
		if (this.keywordAggregationType == AGGREGATION_TYPE_GEOM)
			result = Math.pow(result, (1.0 / keywordsToUse.size()));
		
		return result;
	}
}
