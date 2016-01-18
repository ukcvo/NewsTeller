package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class PrepPhraseFeature extends UsabilityFeature {

	private List<String> prepositions;
	
	public PrepPhraseFeature(String queryFileName, String prepositionFileName) {
		super(queryFileName);
		this.prepositions = Util.readStringListFromFile(prepositionFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		double result = 0;
		
		List<String> actors = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_LABEL);
		for (String label : actors) {
			String[] tokens = label.split(" ");
			if (prepositions.contains(tokens[0]))
				result++;
		}
		result /= actors.size();
		
		return result;
	}

}
