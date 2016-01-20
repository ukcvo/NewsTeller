package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.KSMention;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class MentionComparisonFeature extends UsabilityFeature {

	public static final int OPERATION_TYPE_TEXT = 0;
	public static final int OPERATION_TYPE_DISTANCE = 1;
	public static final int OPERATION_TYPE_SENTENCE = 2;
	public static final int OPERATION_TYPE_INTERSECTION = 3;

	private int operationType;

	public void setOperationType(int operationType) {
		this.operationType = operationType;
	}

	public MentionComparisonFeature(String queryFileName) {
		super(queryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {

		double result = 0;

		List<String> mentionURIs = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_MENTION);
		List<KSMention> mentions = new ArrayList<KSMention>();
		List<KSMention> mentionSentences = new ArrayList<KSMention>();
		List<String[]> mentionSentenceStrings = new ArrayList<String[]>();
		for (String mentionURI : mentionURIs) {
			mentions.add(new KSMention(mentionURI));
			mentionSentences.add(ksAdapter.retrieveKSMentionFromMentionURI(mentionURI, true));
			mentionSentenceStrings.add(ksAdapter.retrieveSentenceFromMention(mentionURI).split(" "));
		}

		for (int i = 0; i < mentionURIs.size(); i++) {
			for (int j = i + 1; j < mentionURIs.size(); j++) {

				switch (operationType) {
				case OPERATION_TYPE_TEXT:
					if (mentions.get(i).hasSameResourceURI(mentions.get(j)))
						result++;
					break;

				case OPERATION_TYPE_DISTANCE:
					KSMention first = mentions.get(i);
					KSMention second = mentions.get(j);
					if (first.hasSameResourceURI(second))
						result += first.distanceTo(second);
					break;

				case OPERATION_TYPE_SENTENCE:
					if (mentionSentences.get(i).equals(mentionSentences.get(j)))
						result++;
					break;

				case OPERATION_TYPE_INTERSECTION:
					String[] firstText = mentionSentenceStrings.get(i);
					String[] secondText = mentionSentenceStrings.get(j);
					double count = 0;
					for (int a = 0; a < firstText.length; a++) {
						for (int b = 0; b < secondText.length; b++) {
							if (firstText[a].equalsIgnoreCase(secondText[b])) {
								count++;
								break;
							}
						}
					}
					result += count / Math.min(firstText.length, secondText.length);
					break;

				default:
					break;
				}

			}
		}
		result /= (0.5 * mentionURIs.size() * (mentionURIs.size() - 1));

		return result;
	}

}
