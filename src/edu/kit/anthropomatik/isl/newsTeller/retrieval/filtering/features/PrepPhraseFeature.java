package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class PrepPhraseFeature extends UsabilityFeature {

	private List<String> prepositions;
	
	private String labelQuery;
	
	private boolean doSearchInsidePhrase;
	
	public void setDoSearchInsidePhrase(boolean doSearchInsidePhrase) {
		this.doSearchInsidePhrase = doSearchInsidePhrase;
	}
		
	public PrepPhraseFeature(String queryFileName, String labelQueryFileName, String prepositionFileName) {
		super(queryFileName);
		this.prepositions = Util.readStringListFromFile(prepositionFileName);
		this.labelQuery = Util.readStringFromFile(labelQueryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		double result = 0;
		
		List<String> actors = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_ENTITY);
		for (String actor : actors) {
			
			List<String> labels = ksAdapter.runSingleVariableStringQuery(labelQuery.replace(Util.PLACEHOLDER_ENTITY, actor), Util.VARIABLE_LABEL);
			
			double actorResult = 0;
			for (String label : labels) {
				String[] tokens = label.split(Util.SPLIT_REGEX);
				if (this.doSearchInsidePhrase) {
					for (int i = 1; i < tokens.length; i++) { // look at all tokens
						if (prepositions.contains(tokens[i])) {
							actorResult++;
							break;
						}
					}
				} else if (prepositions.contains(tokens[0])) // look at first token
					actorResult++;
			}
			result += actorResult/labels.size();
		}
		if (!actors.isEmpty())
			result /= actors.size();
		
		return result;
	}

}
