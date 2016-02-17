package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class SameDocumentFeature extends RankingFeature {

	private static final int AGGREGATION_TYPE_AVG = 0;
	private static final int AGGREGATION_TYPE_MIN = 1;
	private static final int AGGREGATION_TYPE_MAX = 2;
	
	private int aggregationType;
	
	private boolean useSentenceInsteadOfText;
	
	private boolean normalizeOverNumberOfTotalDocuments;
	
	public void setAggregationType(int aggregationType) {
		this.aggregationType = aggregationType;
	}
	
	public void setUseSentenceInsteadOfText(boolean useSentenceInsteadOfText) {
		this.useSentenceInsteadOfText = useSentenceInsteadOfText;
	}
	
	public void setNormalizeOverNumberOfTotalDocuments(boolean normalizeOverNumberOfTotalDocuments) {
		this.normalizeOverNumberOfTotalDocuments = normalizeOverNumberOfTotalDocuments;
	}
	
	@Override
	public double getValue(String eventURI, List<Keyword> keywords, UserModel userModel) {
		
		String arbitraryKeyword = keywords.get(0).getWord();
		
		List<Set<String>> documents;
		if (this.useSentenceInsteadOfText)
			documents = ksAdapter.getAllQuerySentences(arbitraryKeyword);
		else
			documents = ksAdapter.getAllQueryResourceURIs(arbitraryKeyword);
		
		Set<String> thisDocuments;
		if (this.useSentenceInsteadOfText)
			thisDocuments = new HashSet<String>(ksAdapter.retrieveSentencesFromEvent(eventURI, arbitraryKeyword));
		else 
			thisDocuments = Util.resourceURIsFromMentionURIs(ksAdapter.getBufferedValues(Util.getRelationName("event", "mention", arbitraryKeyword), eventURI));		
		
		Map<String,Integer> documentCounts = new HashMap<String, Integer>();
		for (String doc : thisDocuments)
			documentCounts.put(doc, 0);
		
		// do the counting
		for (Set<String> eventDocs : documents) {
			for (String doc : eventDocs) {
				if (thisDocuments.contains(doc))
					documentCounts.put(doc, documentCounts.get(doc) + 1);
			}
		}
		
		// now aggregate
		double result = (this.aggregationType == AGGREGATION_TYPE_MIN) ? Double.POSITIVE_INFINITY : 0.0;
		for (Integer i : documentCounts.values()) {
			double normalized = this.normalizeOverNumberOfTotalDocuments ? (1.0 * i) / documents.size() : i;
			
			switch (this.aggregationType) {
			case AGGREGATION_TYPE_AVG:
				result += normalized / thisDocuments.size();
				break;
			case AGGREGATION_TYPE_MIN:
				result = Math.min(result, normalized);
				break;
			case AGGREGATION_TYPE_MAX:
				result = Math.max(result, normalized);
				break;
			default:
				break;
			}
		}
		
		return result;
	}

}
