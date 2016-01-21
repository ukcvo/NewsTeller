package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.KSMention;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class EntityMentionSurroundingsFeature extends UsabilityFeature {

	private String mentionQuery;

	private List<String> targetWords;

	public static final int OPERATION_TYPE_NEXT_WORD = 0;
	public static final int OPERATION_TYPE_PREVIOUS_WORD = 1;

	private int operationType;

	public void setOperationType(int operationType) {
		this.operationType = operationType;
	}

	public EntityMentionSurroundingsFeature(String queryFileName, String mentionQueryFileName, String targetWordsFileName) {
		super(queryFileName);
		this.mentionQuery = Util.readStringFromFile(mentionQueryFileName);
		this.targetWords = Util.readStringListFromFile(targetWordsFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {

		double result = 0;

		List<String> entities = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_ENTITY);
		List<List<KSMention>> entityMentions = new ArrayList<List<KSMention>>();
		for (String entity : entities) {
			List<String> mentionURIs = ksAdapter.runSingleVariableStringQuery(mentionQuery.replace(Util.PLACEHOLDER_EVENT, entity), Util.VARIABLE_MENTION);
			List<KSMention> mentions = new ArrayList<KSMention>();
			for (String mention : mentionURIs) {
				mentions.add(new KSMention(mention));
			}

			entityMentions.add(mentions);
		}

		List<String> mentionURIs = ksAdapter.runSingleVariableStringQuery(mentionQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_MENTION);
		for (String mentionURI : mentionURIs) {
			KSMention sentenceMention = ksAdapter.retrieveKSMentionFromMentionURI(mentionURI, true);
			
			String sentence = ksAdapter.retrieveSentenceFromMention(mentionURI);

			for (int i = 0; i < entityMentions.size(); i++) {
				List<KSMention> entity = entityMentions.get(i);
				
				for (int j = 0; j < entity.size(); j++) {
					KSMention mention = entity.get(j);
					if (sentenceMention.contains(mention)) {
						
						int idx = mention.getStartIdx() - sentenceMention.getStartIdx(); 
						if (idx == 0 && this.operationType == OPERATION_TYPE_PREVIOUS_WORD)
							continue;
						int startIdx;
						int endIdx;
						
						switch (this.operationType) {
						
						case OPERATION_TYPE_PREVIOUS_WORD:
							
							endIdx = idx - 1;
							if (endIdx > 0 && Util.STOP_CHARS.contains(sentence.charAt(endIdx-1)))
								endIdx--;
							for (startIdx = endIdx - 1; (startIdx >= 0) && !Util.STOP_CHARS.contains(sentence.charAt(startIdx)); startIdx--) { }
							startIdx++;
							break;

						case OPERATION_TYPE_NEXT_WORD:
							startIdx = idx + mention.getLength();
							if (startIdx >= sentence.length()) {
								// nothing to look for after end of the sentence.
								startIdx = 0;
								endIdx = 0;
								break;
							}
							if (sentence.charAt(startIdx) == ' ')
								startIdx++;
							for (endIdx = startIdx + 1; (endIdx < sentence.length()) && !Util.STOP_CHARS.contains(sentence.charAt(endIdx)); endIdx++) { }
							if (endIdx == sentence.length() - 1)
								endIdx++;
							break;

						default:
							// should never happen; don't do anything
							startIdx = 0;
							endIdx = 0;
							break;
						}
						
						String word = sentence.substring(startIdx, endIdx);
						if (targetWords.contains(word)) {
							result++;
							break;
						}
							
					}
				}
			}
		}
		result /= entities.size();

		return result;
	}

}
