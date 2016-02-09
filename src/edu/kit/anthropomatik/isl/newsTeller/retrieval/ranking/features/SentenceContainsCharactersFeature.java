package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.stanford.nlp.simple.Sentence;

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
			
			Sentence s = new Sentence(sentence.toLowerCase());
			List<String> tokens = s.words();
			
			for (String c : this.chars) {
				if (tokens.contains(c.toLowerCase()))
					counter++;
			}
			
			result = Math.max(result, counter);
		}
		
		return result;
	}

}
