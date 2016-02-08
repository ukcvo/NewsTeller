package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

public class SentenceContainsCharactersFeature extends RankingFeature {

	private List<String> chars;
	
	public void setChars(List<String> chars) {
		this.chars = chars;
	}
	
	@Override
	public double getValue(String eventURI, List<Keyword> keywords, UserModel userModel) {
		
		double result = 0.0;
		
		Set<String> sentences = new HashSet<String>(ksAdapter.retrieveSentencesFromEvent(eventURI, keywords.get(0).getWord()));
		
		for (String sentence : sentences) {
			double counter = 0.0;
			for (String c : this.chars) {
				if (sentence.contains(c))
					counter++;
			}
			
			result = Math.max(result, counter);
		}
		
		return result;
	}

}
