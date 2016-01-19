package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class PrepPhraseFeature extends UsabilityFeature {

	private List<String> prepositions;
	
	private String labelQuery;
	
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
				String[] tokens = label.split(" ");
				if (prepositions.contains(tokens[0]))
					actorResult++;
			}
			result += actorResult/labels.size();
		}
		if (!actors.isEmpty())
			result /= actors.size();
		
		return result;
	}

}
