package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public abstract class ContainsStringsFeature extends RankingFeature {

	protected List<String> stringsToCheck;

	public ContainsStringsFeature(String stringFileName) {
		this.stringsToCheck = Util.readStringListFromFile(stringFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords, UserModel userModel) {
		
		double result = 0.0;
		
		Set<List<String>> sentences = getText(eventURI, keywords, userModel);
		
		for (List<String> tokens : sentences) {
			double counter = 0.0;
			
			for (String c : this.stringsToCheck) {
				for (String token : tokens) {
					if (token.equalsIgnoreCase(c))
						counter++;
				}
			}
			result = Math.max(result, counter);
		}
		
		return result;
	}

	protected abstract Set<List<String>> getText(String eventURI, List<Keyword> keywords, UserModel userModel);
	
}