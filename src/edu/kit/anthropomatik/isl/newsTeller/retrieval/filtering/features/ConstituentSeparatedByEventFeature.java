package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.KSMention;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class ConstituentSeparatedByEventFeature extends UsabilityFeature {

	public static final int OPERATION_TYPE_AVG = 0;
	public static final int OPERATION_TYPE_MIN = 1;
	public static final int OPERATION_TYPE_MAX = 2;

	private int operationType;

	private String mentionQuery;

	private String mentionQueryName;
	
	public void setOperationType(int operationType) {
		this.operationType = operationType;
	}

	public ConstituentSeparatedByEventFeature(String queryFileName, String mentionQueryFileName) {
		super(queryFileName);
		this.mentionQuery = Util.readStringFromFile(mentionQueryFileName);
		this.mentionQueryName = Util.queryNameFromFileName(mentionQueryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {

		double result = (this.operationType == OPERATION_TYPE_MIN) ? Double.POSITIVE_INFINITY : 0;

		Set<String> constituents = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_CONSTITUENT + this.sparqlQueryName, eventURI);
		Set<KSMention> constituentMentions = new HashSet<KSMention>();
		for (String constituent : constituents) {
			Set<String> mentionURIs = 
					ksAdapter.getBufferedValues(Util.RELATION_NAME_CONSTITUENT_MENTION + this.sparqlQueryName + this.mentionQueryName, constituent);
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

		Set<String> mentionURIs = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_MENTION + this.mentionQueryName, eventURI);
		
		for (String mentionURI : mentionURIs) {
			KSMention sentenceMention = ksAdapter.retrieveKSMentionFromMentionURI(mentionURI, true);
			KSMention eventMention = ksAdapter.retrieveKSMentionFromMentionURI(mentionURI, false);

			List<KSMention> allEventMentions = ksAdapter.getAllEventMentions(sentenceMention.getResourceURI());
			allEventMentions.remove(eventMention);

			int numberOfCheckedMentions = 0;
			double mentionResult = 0;
			for (KSMention constituentMention : constituentMentions) {
				if (sentenceMention.overlap(constituentMention) > 0) {

					double numberOfEventsBetween = 0;
					numberOfCheckedMentions++;

					if (eventMention.compareTo(constituentMention) == 0)
						continue;

					for (KSMention otherEventMention : allEventMentions) {
						double directionEventOther = eventMention.compareTo(otherEventMention);
						double directionOtherConstitutent = otherEventMention.compareTo(constituentMention);
						if ((directionEventOther == directionOtherConstitutent) && (directionOtherConstitutent != 0))
							numberOfEventsBetween++;
					}

					switch (this.operationType) {
					case OPERATION_TYPE_AVG:
						mentionResult += numberOfEventsBetween;
						break;
					case OPERATION_TYPE_MIN:
						mentionResult = Math.min(mentionResult, numberOfEventsBetween);
						break;
					case OPERATION_TYPE_MAX:
						mentionResult = Math.max(mentionResult, numberOfEventsBetween);
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

	@Override
	public void runBulkQueries(Set<String> eventURIs, List<Keyword> keywords) {
		ksAdapter.runKeyValueQuery(mentionQuery, Util.RELATION_NAME_EVENT_MENTION + this.mentionQueryName, Util.VARIABLE_EVENT, Util.VARIABLE_MENTION, eventURIs);
		ksAdapter.runKeyValueQuery(sparqlQuery, Util.RELATION_NAME_EVENT_CONSTITUENT + this.sparqlQueryName, Util.VARIABLE_EVENT, Util.VARIABLE_ENTITY, eventURIs);
		Set<String> constituents = ksAdapter.getAllRelationValues(Util.RELATION_NAME_EVENT_CONSTITUENT);
		ksAdapter.runKeyValueQuery(mentionQuery, Util.RELATION_NAME_CONSTITUENT_MENTION + this.sparqlQueryName + this.mentionQueryName, Util.VARIABLE_EVENT, Util.VARIABLE_MENTION, constituents);
	}

}
