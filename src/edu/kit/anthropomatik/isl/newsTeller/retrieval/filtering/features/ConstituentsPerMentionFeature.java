package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.KSMention;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class ConstituentsPerMentionFeature extends UsabilityFeature {

	private String mentionQuery;

	public static final int OPERATION_TYPE_AVG = 0;
	public static final int OPERATION_TYPE_MAX = 1;
	public static final int OPERATION_TYPE_MIN = 2;
	public static final int OPERATION_TYPE_ZERO_FRACTION = 3;
	public static final int OPERATION_TYPE_COUNT_NONZERO = 4;

	private int operationType;

	public void setOperationType(int operationType) {
		this.operationType = operationType;
	}

	public ConstituentsPerMentionFeature(String queryFileName, String mentionQueryFileName) {
		super(queryFileName);
		this.mentionQuery = Util.readStringFromFile(mentionQueryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {

		double result = (this.operationType == OPERATION_TYPE_MIN) ? Double.POSITIVE_INFINITY : 0;

		List<String> constituents = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_ENTITY);
		List<List<KSMention>> constituentMentions = new ArrayList<List<KSMention>>();
		for (String constituent : constituents) {
			List<String> mentionURIs = ksAdapter.runSingleVariableStringQuery(mentionQuery.replace(Util.PLACEHOLDER_EVENT, constituent), Util.VARIABLE_MENTION);
			List<KSMention> mentions = new ArrayList<KSMention>();
			for (String mentionURI : mentionURIs) {
				mentions.add(new KSMention(mentionURI));
			}
			constituentMentions.add(mentions);
		}

		List<String> mentionURIs = new ArrayList<String>();
		mentionURIs.addAll(ksAdapter.getBufferedValues(Util.RELATION_NAME_MENTION, eventURI));
		if (mentionURIs.isEmpty())
			mentionURIs = ksAdapter.runSingleVariableStringQuery(mentionQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_MENTION);
		for (String mentionURI : mentionURIs) {
			KSMention sentenceMention = ksAdapter.retrieveKSMentionFromMentionURI(mentionURI, true);

			double constituentFraction = 0;
			for (List<KSMention> constituent : constituentMentions) {
				for (KSMention mention : constituent) {
					if (sentenceMention.contains(mention)) {
						constituentFraction++;
						break;
					}
				}
			}
			constituentFraction /= constituents.size();

			switch (this.operationType) {
			case OPERATION_TYPE_AVG:
				result += (constituentFraction / mentionURIs.size());
				break;
				
			case OPERATION_TYPE_MAX:
				result = Math.max(result, constituentFraction);
				break;

			case OPERATION_TYPE_MIN:
				result = Math.min(result, constituentFraction);
				break;
			
			case OPERATION_TYPE_ZERO_FRACTION:
				if (constituentFraction == 0)
					result += (1.0 / mentionURIs.size());
				break;
				
			case OPERATION_TYPE_COUNT_NONZERO:
				if (constituentFraction != 0)
					result++;
				break;
			
			default:
				// should never happen; don't do anything
				break;
			}
		}

		return result;
	}

}
