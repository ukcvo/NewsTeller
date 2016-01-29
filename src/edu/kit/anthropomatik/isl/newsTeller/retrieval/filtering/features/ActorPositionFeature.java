package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class ActorPositionFeature extends UsabilityFeature {

	public static int DIRECTION_LEFT = 0;
	public static int DIRECTION_RIGHT = 1;
	
	private int directionToLookAt;
	
	private String mentionQuery;
	
	private String mentionQueryName;
	
	private String labelQuery;
	
	private String labelQueryName;
	
	public void setDirectionToLookAt(int directionToLookAt) {
		this.directionToLookAt = directionToLookAt;
	}
	
	public ActorPositionFeature(String queryFileName, String mentionQueryFileName, String labelQueryFileName) {
		super(queryFileName);
		this.mentionQuery = Util.readStringFromFile(mentionQueryFileName);
		this.labelQuery = Util.readStringFromFile(labelQueryFileName);
		this.mentionQueryName = Util.queryNameFromFileName(mentionQueryFileName);
		this.labelQueryName = Util.queryNameFromFileName(labelQueryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		
		double result = 0;
		
		Set<String> mentionURIs = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_MENTION + this.mentionQueryName, eventURI);
		
		Set<String> actors = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_CONSTITUENT + this.sparqlQueryName, eventURI);
		Set<Set<String>> actorLabels = new HashSet<Set<String>>();
		for (String actor : actors) {
			actorLabels.add(ksAdapter.getBufferedValues(Util.RELATION_NAME_CONSTITUENT_LABEL + this.sparqlQueryName + this.labelQueryName, actor));
		}
				
		for (String mentionURI : mentionURIs) {
			String sentence = ksAdapter.retrieveSentenceFromMention(mentionURI);
			String eventLabel = ksAdapter.getUniqueMentionProperty(mentionURI, Util.MENTION_PROPERTY_ANCHOR_OF);
			
			String[] sentenceParts = sentence.split(eventLabel);
			
			// catch if event label is the first or last word in the sentence and we want to look into the "empty" direction
			if (sentenceParts.length <= directionToLookAt) 
				continue;
			
			String sentencePart = sentenceParts[directionToLookAt].toLowerCase(); // case-insensitive
			
			double mentionResult = 0;
			
			for (Set<String> actor : actorLabels) {
				
				double actorResult = 0;
				for (String label : actor) {
					if (sentencePart.contains(label.toLowerCase())) { // case-insensitive
						actorResult = 1;
						break;
					}
				}
				mentionResult += actorResult;
			}
			if (!actors.isEmpty())
				result += mentionResult / actors.size();
		}
		
		result /= mentionURIs.size();
		
		return result;
	}

	@Override
	public void runBulkQueries(Set<String> eventURIs, List<Keyword> keywords) {
		ksAdapter.runKeyValueQuery(mentionQuery, Util.RELATION_NAME_EVENT_MENTION + this.mentionQueryName, Util.VARIABLE_EVENT, 
				Util.VARIABLE_MENTION, eventURIs);
		ksAdapter.runKeyValueQuery(sparqlQuery, Util.RELATION_NAME_EVENT_CONSTITUENT + this.sparqlQueryName, Util.VARIABLE_EVENT, 
				Util.VARIABLE_ENTITY, eventURIs);
		Set<String> actors = ksAdapter.getAllRelationValues(Util.RELATION_NAME_EVENT_CONSTITUENT + this.sparqlQueryName);
		ksAdapter.runKeyValueQuery(labelQuery, Util.RELATION_NAME_CONSTITUENT_LABEL + this.sparqlQueryName + this.labelQueryName, 
				Util.VARIABLE_ENTITY, Util.VARIABLE_LABEL, actors);
	}

	

}
