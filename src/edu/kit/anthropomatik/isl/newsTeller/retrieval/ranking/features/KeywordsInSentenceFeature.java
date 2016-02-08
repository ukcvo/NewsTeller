package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class KeywordsInSentenceFeature extends RankingFeature {

	private static final int AGGREGATION_TYPE_AVG = 0;
	private static final int AGGREGATION_TYPE_MIN = 1;
	private static final int AGGREGATION_TYPE_MAX = 2;
	
	private int aggregationTypeKeyword;
	
	private int aggregationTypeSentence;
	
	private boolean useTitleInsteadOfSentence;
	
	private boolean useContainsInsteadOfRegex;
	
	private boolean useStemInsteadOfWord;
	
	private boolean splitKeywords;
	
	public void setAggregationTypeKeyword(int aggregationTypeKeyword) {
		this.aggregationTypeKeyword = aggregationTypeKeyword;
	}
	
	public void setAggregationTypeSentence(int aggregationTypeSentence) {
		this.aggregationTypeSentence = aggregationTypeSentence;
	}
	
	public void setUseTitleInsteadOfSentence(boolean useTitleInsteadOfSentence) {
		this.useTitleInsteadOfSentence = useTitleInsteadOfSentence;
	}
	
	public void setUseContainsInsteadOfRegex(boolean useConstainsInsteadOfRegex) {
		this.useContainsInsteadOfRegex = useConstainsInsteadOfRegex;
	}

	public void setUseStemInsteadOfWord(boolean useStemInsteadOfWord) {
		this.useStemInsteadOfWord = useStemInsteadOfWord;
	}
	
	public void setSplitKeywords(boolean splitKeywords) {
		this.splitKeywords = splitKeywords;
	}
	
	@Override
	public double getValue(String eventURI, List<Keyword> keywords, UserModel userModel) {
		
		double result = (this.aggregationTypeKeyword == AGGREGATION_TYPE_MIN) ? Double.POSITIVE_INFINITY : 0.0;
		
		// TODO: can also use userModel.getInterests()
		List<Keyword> keywordsToUse = keywords;
		
		Set<String> sentences = new HashSet<String>();
		if (this.useTitleInsteadOfSentence)
			sentences.addAll(ksAdapter.getResourceTitlesFromEvent(eventURI, keywords.get(0).getWord()));
		else
			sentences.addAll(ksAdapter.retrieveSentencesFromEvent(eventURI, keywords.get(0).getWord()));
		
		for (Keyword keyword : keywordsToUse) {
			
			// set up the keyword tokens to match in the text according to the setup
			Set<String> keywordTokens = new HashSet<String>();
			String base = this.useStemInsteadOfWord ? keyword.getStem() : keyword.getWord(); // use stem or word?
			if (this.splitKeywords) {
				String[] labelParts = base.split(" ");
				if (this.useContainsInsteadOfRegex) { // split keywords, use contains --> just need to escape
					for (int i = 0; i < labelParts.length; i++)
						labelParts[i] = Util.escapeText(labelParts[i]);
				} else if (this.useStemInsteadOfWord) { // split keywords, use regex and stem --> need to append regex letters
					for (int i = 0; i < labelParts.length; i++)
						labelParts[i] += Util.KEYWORD_REGEX_LETTERS_JAVA;
				}
				keywordTokens.addAll(Sets.newHashSet(labelParts));
			} else { // don't split keywords
				String toAdd = base;
				if (this.useContainsInsteadOfRegex) // use contains --> escape
					toAdd = Util.escapeText(toAdd);
				else if (this.useStemInsteadOfWord) // use stem and regex --> append regex letters
					toAdd = toAdd + Util.KEYWORD_REGEX_LETTERS_JAVA;
				keywordTokens.add(toAdd);
			}
			
			double keywordResult = (this.aggregationTypeSentence == AGGREGATION_TYPE_MIN) ? Double.POSITIVE_INFINITY : 0.0;
			
			for (String sentence : sentences) {
				double sentenceResult = 0;
				
				for (String keywordToken : keywordTokens) {
					String regex = Util.KEYWORD_REGEX_PREFIX_JAVA + keywordToken.toLowerCase() + Util.KEYWORD_REGEX_SUFFIX_JAVA;
					if ((this.useContainsInsteadOfRegex && sentence.contains(keywordToken)) || (sentence.matches(regex))) 
							sentenceResult++;
				}
				
				sentenceResult /= keywordTokens.size();
				
				// aggregate over sentences
				switch (this.aggregationTypeSentence) {
				case AGGREGATION_TYPE_AVG:
					keywordResult += sentenceResult / sentences.size();
					break;
				case AGGREGATION_TYPE_MAX:
					keywordResult = Math.max(keywordResult, sentenceResult);
					break;
				case AGGREGATION_TYPE_MIN:
					keywordResult = Math.min(keywordResult, sentenceResult);
					break;
				default:
					break;
				}
			}
			
			// aggregate over keywords
			switch (this.aggregationTypeKeyword) {
			case AGGREGATION_TYPE_AVG:
				result += keywordResult / keywordsToUse.size();
				break;
			case AGGREGATION_TYPE_MAX:
				result = Math.max(result, keywordResult);
				break;
			case AGGREGATION_TYPE_MIN:
				result = Math.min(result, keywordResult);
				break;
			default:
				break;
			}
		}
		
		return result;
	}

}
