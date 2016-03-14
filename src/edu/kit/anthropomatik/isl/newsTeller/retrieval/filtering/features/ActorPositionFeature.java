package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import jersey.repackaged.com.google.common.collect.Sets;

public class ActorPositionFeature extends UsabilityFeature {

	public static int DIRECTION_LEFT = 0;
	public static int DIRECTION_RIGHT = 1;
	
	private int directionToLookAt;
	
	public void setDirectionToLookAt(int directionToLookAt) {
		this.directionToLookAt = directionToLookAt;
	}
	
	public ActorPositionFeature() {
		super();
	}

	public long preparationTime = 0;
	public long mentionPrepTime = 0;
	public long actorIterationTime = 0;
	
	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		
		double result = 0;
		
		long t = System.currentTimeMillis();
		String arbitraryKeyword = keywords.get(0).getWord();
		
		Set<String> mentionURIs = ksAdapter.getBufferedValues(Util.getRelationName("event", "mention", arbitraryKeyword), eventURI);
		
		Set<String> actors = ksAdapter.getBufferedValues(Util.getRelationName("event", "actor", arbitraryKeyword), eventURI);
		Set<Set<String>> actorLabels = new HashSet<Set<String>>();
		for (String actor : actors) {
			actorLabels.add(ksAdapter.getBufferedValues(Util.getRelationName("entity", "entityPrefLabel", arbitraryKeyword), actor));
		}
		this.preparationTime += System.currentTimeMillis() - t;
		
		for (String mentionURI : mentionURIs) {
			
			t = System.currentTimeMillis();
			String sentence = ksAdapter.retrieveSentenceFromMention(mentionURI);
			String eventLabel = ksAdapter.getFirstBufferedValue(Util.RELATION_NAME_MENTION_PROPERTY + Util.MENTION_PROPERTY_ANCHOR_OF, mentionURI);
			
			String[] sentenceParts = sentence.split(eventLabel);
			
			// catch if event label is the first or last word in the sentence and we want to look into the "empty" direction
			if (sentenceParts.length <= directionToLookAt) 
				continue;
			
			String sentencePart = sentenceParts[directionToLookAt].toLowerCase(); // case-insensitive
			
			this.mentionPrepTime += System.currentTimeMillis() - t;
			t = System.currentTimeMillis();
			
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
			
			this.actorIterationTime += System.currentTimeMillis() - t;
		}
		
		result /= mentionURIs.size();
		
		return result;
	}

	@Override
	public Set<String> getRequiredMentionProperties() {
		return Sets.newHashSet(Util.MENTION_PROPERTY_ANCHOR_OF);
	}

	

}
