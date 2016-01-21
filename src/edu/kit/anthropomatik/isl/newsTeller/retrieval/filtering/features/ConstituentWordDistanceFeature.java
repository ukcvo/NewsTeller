package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.KSMention;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Counts the word distance between the constituents and the event label.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class ConstituentWordDistanceFeature extends UsabilityFeature {

	private String mentionQuery;
	
	public static final int OPERATION_TYPE_AVG = 0;
	public static final int OPERATION_TYPE_MIN = 1;
	public static final int OPERATION_TYPE_MAX = 2;
	
	private int operationType;
	
	private boolean normalize;
	
	public void setOperationType(int operationType) {
		this.operationType = operationType;
	}
	
	public void setNormalize(boolean normalize) {
		this.normalize = normalize;
	}
	
	public ConstituentWordDistanceFeature(String queryFileName, String mentionQueryFileName) {
		super(queryFileName);
		this.mentionQuery = Util.readStringFromFile(mentionQueryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		
		double result = (this.operationType == OPERATION_TYPE_MIN) ? Double.POSITIVE_INFINITY : 0;
		
		List<Character> stopChars = Arrays.asList('.', '!', '?', ' ');
		
		List<String> constituents = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_ENTITY);
		List<KSMention> constituentMentions = new ArrayList<KSMention>();
		for (String constituent : constituents) {
			List<String> mentionURIs =  ksAdapter.runSingleVariableStringQuery(mentionQuery.replace(Util.PLACEHOLDER_EVENT, constituent), Util.VARIABLE_MENTION);
			List<KSMention> mentions = new ArrayList<KSMention>();
			for (String mentionURI : mentionURIs) {
				boolean shouldAdd = true;
				KSMention newMention = new KSMention(mentionURI);
				List<KSMention> toRemove = new ArrayList<KSMention>();
				for (KSMention m : mentions) {
					if (m.contains(newMention)) {
						shouldAdd = false;
					} else if (newMention.contains(m)) {
						toRemove.add(m);
					}
				}
				mentions.removeAll(toRemove);
				if (shouldAdd)
					mentions.add(newMention);
			}
			constituentMentions.addAll(mentions);
		}
		
		List<String> mentionURIs = ksAdapter.runSingleVariableStringQuery(mentionQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_MENTION);
		for (String mentionURI : mentionURIs) {
			KSMention sentenceMention = ksAdapter.retrieveKSMentionFromMentionURI(mentionURI, true);
			KSMention eventMention = ksAdapter.retrieveKSMentionFromMentionURI(mentionURI, false);
			String sentenceString = ksAdapter.retrieveSentenceFromMention(mentionURI);
			
			int sentenceLength = sentenceString.split("[ .,;:?!\"'\\[\\]()]").length;
			
			int eventStartIdx = eventMention.getStartIdx() - sentenceMention.getStartIdx();
			int eventEndIdx = eventMention.getEndIdx() - sentenceMention.getStartIdx();
			
			int numberOfCheckedMentions = 0;
			double mentionResult = (this.operationType == OPERATION_TYPE_MIN) ? Double.POSITIVE_INFINITY : 0;
			
			for (KSMention mention : constituentMentions) {
				if (sentenceMention.overlap(mention) > 0) {
					
					int startIdx = mention.getStartIdx() - sentenceMention.getStartIdx();
					int endIdx = mention.getEndIdx() - sentenceMention.getStartIdx();
					numberOfCheckedMentions++;
					double numberOfWords = 0;
					boolean inWord = false;
					
					int order = eventMention.compareTo(mention);
					if (order > 0) { // event mention comes AFTER entity mention
						for (int idx = endIdx; idx < eventStartIdx; idx++) { // start walking at end of entity mention until you hit start of event mention
							if (stopChars.contains(sentenceString.charAt(idx)))
								inWord = false;
							else {
								if (!inWord) {
									inWord = true;
									numberOfWords++;
								}
							}
						}
					} else if (order < 0) {	// event mention comes BEFORE entity mention
						for (int idx = startIdx - 1; idx >= eventEndIdx; idx--) { // start walking at end of entity mention until you hit start of event mention
							if (stopChars.contains(sentenceString.charAt(idx)))
								inWord = false;
							else {
								if (!inWord) {
									inWord = true;
									numberOfWords++;
								}
							}
						}
					}
					
					if (this.normalize)
						numberOfWords /= sentenceLength;
					
					switch (this.operationType) {
					case OPERATION_TYPE_AVG:
						mentionResult += numberOfWords;
						break;
					case OPERATION_TYPE_MIN:
						mentionResult = Math.min(mentionResult, numberOfWords);
						break;
					case OPERATION_TYPE_MAX:
						mentionResult = Math.max(mentionResult, numberOfWords);
						break;
					default:
						break;
					}
				}
			}
			
			switch (this.operationType) {
			case OPERATION_TYPE_AVG:
				result += mentionResult / numberOfCheckedMentions;
				break;
			case OPERATION_TYPE_MIN:
				result = Math.min(result, mentionResult);
				break;
			case OPERATION_TYPE_MAX:
				result = Math.max(result, mentionResult);
				break;
			default:
				break;
			}
		}
		if (this.operationType == OPERATION_TYPE_AVG)
			result /= mentionURIs.size();
		
		return result;
	}

}
