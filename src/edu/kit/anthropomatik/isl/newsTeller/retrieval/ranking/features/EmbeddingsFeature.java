package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import edu.stanford.nlp.simple.Sentence;

public class EmbeddingsFeature extends RankingFeature {

	private static Log log = LogFactory.getLog(EmbeddingsFeature.class);
	
	private static final int AGGREGATION_TYPE_AVG = 0;
	private static final int AGGREGATION_TYPE_MIN = 1;
	private static final int AGGREGATION_TYPE_MAX = 2;
	private static final int AGGREGATION_TYPE_GEOM = 3;
	
	private int aggregationType;
	
	private boolean useTitleInsteadOfSentence;
	
	private WordVectors wordVectors;
	
	public void setAggregationType(int aggregationType) {
		this.aggregationType = aggregationType;
	}
	
	public void setUseTitleInsteadOfSentence(boolean useTitleInsteadOfSentence) {
		this.useTitleInsteadOfSentence = useTitleInsteadOfSentence;
	}
	
	public EmbeddingsFeature(String embeddingsFileName) {
		try {
			if (embeddingsFileName.endsWith(".txt"))
				this.wordVectors = WordVectorSerializer.loadTxtVectors(new File(embeddingsFileName));
			else
				this.wordVectors = WordVectorSerializer.loadGoogleModel(new File(embeddingsFileName), true);
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("Could not load word vectors!");
			if (log.isDebugEnabled())
				log.debug("Could not load word vectors", e);
		}
	}
	
	private double[] addVectors(double[] vectorA, double[] vectorB) {
		double[] result = new double[vectorA.length];
		
		for (int i = 0; i < result.length; i++) {
			result[i] = vectorA[i] + vectorB[i];
		}
		
		return result;
	}
	
	private double[] wordsToVector(Collection<String> words) {
		double[] resultVector = null;
		
		for (String word : words) {
			if (wordVectors.hasWord(word)) {
				double[] wordVector = wordVectors.getWordVector(word);
				if (resultVector == null)
					resultVector = wordVector;
				else
					resultVector = addVectors(resultVector, wordVector);
			}
		}
		
		return resultVector;
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
		
		List<double[]> sentenceVectors = new ArrayList<double[]>();
		for (String sentence : sentences) {
			if (sentence.isEmpty())
				continue;
			Sentence s = new Sentence(sentence.toLowerCase());
			double[] vector = wordsToVector(s.words());
			if (vector != null)
				sentenceVectors.add(vector);
		}
		
		int numberOfComparisons = sentenceVectors.size() * keywordsToUse.size();
		
		for (Keyword keyword : keywordsToUse) {
			// wordVector stuff needs lowerCase, tokenized input
			String[] keywordTokens = keyword.getWord().toLowerCase().split(" ");
			double[] keywordVector = wordsToVector(Arrays.asList(keywordTokens));
			
			if (keywordVector == null)
				continue;
			
			for (double[] sentenceVector : sentenceVectors) {
				double similarity = Util.cosineSimilarity(keywordVector, sentenceVector);
				
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
			result = Math.pow(result, (1.0 / numberOfComparisons));
		
		return result;
	}
}
