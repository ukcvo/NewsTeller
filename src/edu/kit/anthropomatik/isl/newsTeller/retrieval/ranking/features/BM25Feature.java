package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class BM25Feature extends RankingFeature {

	private boolean useTextInsteadOfSentence;
	
	private boolean useTitleInsteadOfSentence;
	
	private double k1;
	
	private boolean useUserInterestsInsteadOfQuery;
	
	public void setUseTextInsteadOfSentence(boolean useTextInsteadOfSentence) {
		this.useTextInsteadOfSentence = useTextInsteadOfSentence;
	}
	
	public void setUseTitleInsteadOfSentence(boolean useTitleInsteadOfSentence) {
		this.useTitleInsteadOfSentence = useTitleInsteadOfSentence;
	}
	
	public void setK1(double k1) {
		this.k1 = k1;
	}
	
	public void setUseUserInterestsInsteadOfQuery(boolean useUserInterestsInsteadOfQuery) {
		this.useUserInterestsInsteadOfQuery = useUserInterestsInsteadOfQuery;
	}
	
	// computes the tf for the given queryWord with respect to all the documents of the current event
	private double termFrequency(String queryWord, Set<List<String>> eventDocuments) {
		double result = 0;
		for (List<String> document : eventDocuments) {
			for (String token : document) {
				if (token.equalsIgnoreCase(queryWord))
					result++;
			}
		}
		return result;
	}
	
	// computes the idf for the given queryWord with respect to allDocuments
	private double inverseDocumentFrequency(String queryWord, Set<List<String>> allDocuments) {
		double numberOfDocuments = allDocuments.size();
		double numberOfPositiveDocuments = 0;
		
		for (List<String> document : allDocuments) {
			for (String token : document) {
				if (token.equalsIgnoreCase(queryWord)) {
					numberOfPositiveDocuments++;
					break;
				}
			}
		}
		
		return Util.log2(numberOfDocuments/numberOfPositiveDocuments);
	}
	
	// computes the average document length
	private double computeAverageLength(Set<List<String>> allDocuments) {
		double sum = 0;
		int numberOfDocuments = 0;
		
		for (List<String> document : allDocuments) {
			sum += document.size();
			numberOfDocuments++;
		}
		
		return (sum/numberOfDocuments);
	}
	
	@Override
	public double getValue(String eventURI, List<Keyword> keywords, UserModel userModel) {
		
		String arbitraryKeyword = keywords.get(0).getWord();
		
		List<String> keywordsToUse = new ArrayList<String>();
		List<Keyword> originalKeywordsToUse = this.useUserInterestsInsteadOfQuery ? userModel.getInterests() : keywords;
		for (Keyword k : originalKeywordsToUse)
			keywordsToUse.addAll(Arrays.asList(k.getWord().split(" ")));
		
		Set<List<String>> eventDocuments;
		if (this.useTextInsteadOfSentence)
			eventDocuments = ksAdapter.retrieveOriginalTextTokens(eventURI, arbitraryKeyword);
		else if (this.useTitleInsteadOfSentence)
			eventDocuments = ksAdapter.retrieveTitleTokensFromEvent(eventURI, arbitraryKeyword);
		else
			eventDocuments = ksAdapter.retrieveSentenceTokensFromEvent(eventURI, arbitraryKeyword);
		
		Set<List<String>> allDocuments;
		if (this.useTextInsteadOfSentence)
			allDocuments = ksAdapter.getAllQueryTextTokens(arbitraryKeyword);
		else
			allDocuments = ksAdapter.getAllQuerySentenceTokens(arbitraryKeyword);
		
		double averageDocumentLength = computeAverageLength(allDocuments);
		
		double result = 0;
		
		for (String keyword : keywordsToUse) {
			double tf = termFrequency(keyword, eventDocuments);
			double idf = inverseDocumentFrequency(keyword, allDocuments);
			
			double localResult = idf * tf * (k1 + 1);
			localResult = localResult / (tf + k1 * (0.25 + 0.75 * (allDocuments.size()/averageDocumentLength)));
			result += localResult;
		}
		
		return result;
	}

}
