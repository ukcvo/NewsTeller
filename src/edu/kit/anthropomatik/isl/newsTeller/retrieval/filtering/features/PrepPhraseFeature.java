package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class PrepPhraseFeature extends UsabilityFeature {

	private List<String> prepositions;
	
	private String labelQuery;
	
	private String labelQueryName;
	
	private boolean doSearchInsidePhrase;
	
	public void setDoSearchInsidePhrase(boolean doSearchInsidePhrase) {
		this.doSearchInsidePhrase = doSearchInsidePhrase;
	}
		
	public PrepPhraseFeature(String queryFileName, String labelQueryFileName, String prepositionFileName) {
		super(queryFileName);
		this.prepositions = Util.readStringListFromFile(prepositionFileName);
		this.labelQuery = Util.readStringFromFile(labelQueryFileName);
		this.labelQueryName = Util.queryNameFromFileName(labelQueryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		double result = 0;
		
		Set<String> actors = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_CONSTITUENT + sparqlQueryName, eventURI);
		for (String actor : actors) {
			
			Set<String> labels = ksAdapter.getBufferedValues(Util.RELATION_NAME_CONSTITUENT_LABEL + sparqlQueryName + labelQueryName, actor);
			
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

	@Override
	public void runBulkQueries(Set<String> eventURIs, List<Keyword> keywords) {
		ksAdapter.runKeyValueSparqlQuery(sparqlQuery, Util.RELATION_NAME_EVENT_CONSTITUENT + sparqlQueryName, Util.VARIABLE_EVENT, 
				Util.VARIABLE_ENTITY, eventURIs);
		Set<String> constituents = ksAdapter.getAllRelationValues(Util.RELATION_NAME_EVENT_CONSTITUENT + sparqlQueryName);
		ksAdapter.runKeyValueSparqlQuery(labelQuery, Util.RELATION_NAME_CONSTITUENT_LABEL + sparqlQueryName + labelQueryName, Util.VARIABLE_ENTITY, 
				Util.VARIABLE_LABEL, constituents);
	}

	@Override
	public Set<String> getRequiredMentionProperties() {
		return new HashSet<String>();
	}

}
