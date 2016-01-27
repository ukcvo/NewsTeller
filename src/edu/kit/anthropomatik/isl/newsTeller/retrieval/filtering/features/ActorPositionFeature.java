package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class ActorPositionFeature extends UsabilityFeature {

	public static int DIRECTION_LEFT = 0;
	public static int DIRECTION_RIGHT = 1;
	
	private int directionToLookAt;
	
	private String mentionQuery;
	
	private String labelQuery;
	
	public void setDirectionToLookAt(int directionToLookAt) {
		this.directionToLookAt = directionToLookAt;
	}
	
	public ActorPositionFeature(String queryFileName, String mentionQueryFileName, String labelQueryFileName) {
		super(queryFileName);
		this.mentionQuery = Util.readStringFromFile(mentionQueryFileName);
		this.labelQuery = Util.readStringFromFile(labelQueryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		
		double result = 0;
		
		List<String> mentionURIs = ksAdapter.runSingleVariableStringQuery(mentionQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), 
				Util.VARIABLE_MENTION, false);
		
		List<String> actors = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_ENTITY);
		List<List<String>> actorLabels = new ArrayList<List<String>>();
		for (String actor : actors) {
			actorLabels.add(ksAdapter.runSingleVariableStringQuery(labelQuery.replace(Util.PLACEHOLDER_ENTITY, actor), Util.VARIABLE_LABEL));
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
			
			for (List<String> actor : actorLabels) {
				
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

	

}
