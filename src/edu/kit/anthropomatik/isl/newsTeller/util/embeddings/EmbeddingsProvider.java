package edu.kit.anthropomatik.isl.newsTeller.util.embeddings;

import java.io.File;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

/**
 * Provides access to an embeddings file. 
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class EmbeddingsProvider {

	private static Log log = LogFactory.getLog(EmbeddingsProvider.class);
	
	private WordVectors wordVectors;
	
	private boolean useLowercase;
	
	private boolean splitKeywordsIntoTokens;
	
	public boolean getUseLowercase() {
		return useLowercase;
	}

	public void setUseLowercase(boolean needsLowerCase) {
		this.useLowercase = needsLowerCase;
	}

	public boolean getSplitKeywordsIntoTokens() {
		return splitKeywordsIntoTokens;
	}

	public void setSplitKeywordsIntoTokens(boolean splitKeywordsIntoTokens) {
		this.splitKeywordsIntoTokens = splitKeywordsIntoTokens;
	}

	public EmbeddingsProvider(String embeddingsFileName) {
		try {
			if (embeddingsFileName.endsWith(".txt"))
				this.wordVectors = WordVectorSerializer.loadTxtVectors(new File(embeddingsFileName));
			else
				this.wordVectors = WordVectorSerializer.loadGoogleModel(new File(embeddingsFileName), true);
		} catch (Exception e) {
			if (log.isFatalEnabled())
				log.fatal("Could not load word vectors!");
			if (log.isDebugEnabled())
				log.debug("Could not load word vectors", e);
		}
	}
	
	/**
	 * Returns whether the word is in the vocabulary.
	 */
	public boolean hasWord(String word) {
		return wordVectors.hasWord(word);
	}
	
	/**
	 * Converts the given collection of words into a single vector by adding up their respective individual vectors.
	 */
	public double[] wordsToVector(Collection<String> words) {
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
		
		if (resultVector == null)
			resultVector = new double[wordVectors.getWordVector("dog").length];	// dirty hack to return 0 if none of the words is in the dictionary
		
		return resultVector;
	}
	
	/**
	 * Computes the sum of two vectors.
	 */
	public static double[] addVectors(double[] vectorA, double[] vectorB) {
		double[] result = new double[vectorA.length];
		
		for (int i = 0; i < result.length; i++) {
			result[i] = vectorA[i] + vectorB[i];
		}
		
		return result;
	}
	
	/**
	 * Compute the cosine similarity of the two given vectors.
	 */
	public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
	    double dotProduct = 0.0;
	    double normA = 0.0;
	    double normB = 0.0;
	    for (int i = 0; i < vectorA.length; i++) {
	        dotProduct += vectorA[i] * vectorB[i];
	        normA += Math.pow(vectorA[i], 2);
	        normB += Math.pow(vectorB[i], 2);
	    }   
	    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	}
	
//	public static void main(String[] args) {
//	EmbeddingsFeature f = new EmbeddingsFeature("resources/wordvectors/glove.6B.300d.txt");
//
//	System.out.println(String.format("contains erupted: %b", f.wordVectors.hasWord("erupted")));
//	System.out.println(String.format("erupt = erupted ? %f", f.wordVectors.similarity("erupted", "erupt")));

	
//	System.out.println(String.format("contains barack: %b", f.wordVectors.hasWord("barack")));
//	System.out.println(String.format("contains obama: %b", f.wordVectors.hasWord("obama")));
//	System.out.println(String.format("contains Barack: %b", f.wordVectors.hasWord("Barack")));
//	System.out.println(String.format("contains Obama: %b", f.wordVectors.hasWord("Obama")));
//	System.out.println(String.format("contains barack_obama: %b", f.wordVectors.hasWord("barack_obama")));
//	System.out.println(String.format("contains Barack_Obama: %b", f.wordVectors.hasWord("Barack_Obama")));
//	
//	System.out.println(String.format("barack = obama ? %f", f.wordVectors.similarity("barack", "obama")));
//	System.out.println(String.format("Barack = Obama ? %f", f.wordVectors.similarity("Barack", "Obama")));
//	System.out.println(String.format("barack = Barack ? %f", f.wordVectors.similarity("barack", "Barack")));
//	System.out.println(String.format("obama = Obama ? %f", f.wordVectors.similarity("obama", "Obama")));
//	System.out.println(String.format("barack_obama = Barack_Obama ? %f", f.wordVectors.similarity("barack_obama", "Barack_Obama")));
//	
//	System.out.println(String.format("barack + obama = barack_obama ? %f", 
//			Util.cosineSimilarity(f.wordsToVector(Sets.newHashSet("barack", "obama")), f.wordVectors.getWordVector("barack_obama"))));
//	System.out.println(String.format("Barack + Obama = Barack_Obama ? %f", 
//			Util.cosineSimilarity(f.wordsToVector(Sets.newHashSet("Barack", "Obama")), f.wordVectors.getWordVector("Barack_Obama"))));
//	System.out.println(String.format("barack + obama = Barack_Obama ? %f", 
//			Util.cosineSimilarity(f.wordsToVector(Sets.newHashSet("barack", "obama")), f.wordVectors.getWordVector("Barack_Obama"))));
//	System.out.println(String.format("Barack + Obama = barack_obama ? %f", 
//			Util.cosineSimilarity(f.wordsToVector(Sets.newHashSet("Barack", "Obama")), f.wordVectors.getWordVector("barack_obama"))));
//	
//	System.out.println(String.format("contains volcano: %b", f.wordVectors.hasWord("volcano")));
//	System.out.println(String.format("contains erupt: %b", f.wordVectors.hasWord("erupt")));
//	System.out.println(String.format("contains erupted: %b", f.wordVectors.hasWord("erupted")));
//	System.out.println(String.format("erupt = erupted ? %f", f.wordVectors.similarity("erupted", "erupt")));
//	System.out.println(String.format("erupt = volcano ? %f", f.wordVectors.similarity("volcano", "erupt")));
	
//	System.out.println(String.format("contains barack: %b", f.wordVectors.hasWord("barack")));
//	System.out.println(String.format("contains obama: %b", f.wordVectors.hasWord("obama")));
//	System.out.println(String.format("contains barack_obama: %b", f.wordVectors.hasWord("barack_obama")));
//	System.out.println(String.format("barack = obama ? %f", f.wordVectors.similarity("barack", "obama")));
//	System.out.println(String.format("barack_obama = obama ? %f", f.wordVectors.similarity("barack_obama", "obama")));
//	System.out.println(String.format("barack + obama = barack_obama ? %f", 
//			Util.cosineSimilarity(f.wordsToVector(Sets.newHashSet("barack", "obama")), f.wordVectors.getWordVector("barack_obama"))));
//	System.out.println(String.format("contains volcano: %b", f.wordVectors.hasWord("volcano")));
//	System.out.println(String.format("contains erupt: %b", f.wordVectors.hasWord("erupt")));
//	System.out.println(String.format("contains erupted: %b", f.wordVectors.hasWord("erupted")));
//	System.out.println(String.format("erupt = erupted ? %f", f.wordVectors.similarity("erupted", "erupt")));
//	System.out.println(String.format("erupt = volcano ? %f", f.wordVectors.similarity("volcano", "erupt")));
	
//	System.out.println(String.format("contains /en/barack: %b", f.wordVectors.hasWord("/en/barack")));
//	System.out.println(String.format("contains /en/obama: %b", f.wordVectors.hasWord("/en/obama")));
//	System.out.println(String.format("/en/barack + /en/obama = /en/barack_obama ? %f", 
//			Util.cosineSimilarity(f.wordsToVector(Sets.newHashSet("/en/barack", "/en/obama")), f.wordVectors.getWordVector("/en/barack_obama"))));
//	System.out.println(String.format("contains /en/volcano: %b", f.wordVectors.hasWord("/en/volcano")));
//	System.out.println(String.format("contains /en/erupt: %b", f.wordVectors.hasWord("/en/erupt")));
//	System.out.println(String.format("/en/erupt = /en/volcano ? %f", f.wordVectors.similarity("/en/volcano", "/en/erupt")));
	
//}
	
}
