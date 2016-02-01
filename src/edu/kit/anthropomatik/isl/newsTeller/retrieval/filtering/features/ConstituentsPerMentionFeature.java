package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.KSMention;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class ConstituentsPerMentionFeature extends UsabilityFeature {

	private String mentionQuery;

	private String mentionQueryName;
	
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
		this.mentionQueryName = Util.queryNameFromFileName(mentionQueryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {

		double result = (this.operationType == OPERATION_TYPE_MIN) ? Double.POSITIVE_INFINITY : 0;

		Set<String> constituents = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_CONSTITUENT + this.sparqlQueryName, eventURI);
		List<List<KSMention>> constituentMentions = new ArrayList<List<KSMention>>();
		for (String constituent : constituents) {
			Set<String> mentionURIs = 
					ksAdapter.getBufferedValues(Util.RELATION_NAME_CONSTITUENT_MENTION + this.sparqlQueryName + this.mentionQueryName, constituent);
			List<KSMention> mentions = new ArrayList<KSMention>();
			for (String mentionURI : mentionURIs) {
				mentions.add(new KSMention(mentionURI));
			}
			constituentMentions.add(mentions);
		}

		Set<String> mentionURIs = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_MENTION, eventURI);
		
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

	@Override
	public void runBulkQueries(Set<String> eventURIs, List<Keyword> keywords) {
		ksAdapter.runKeyValueSparqlQuery(sparqlQuery, Util.RELATION_NAME_EVENT_CONSTITUENT + this.sparqlQueryName, Util.VARIABLE_EVENT, Util.VARIABLE_ENTITY, eventURIs);
		Set<String> constituents = ksAdapter.getAllRelationValues(Util.RELATION_NAME_EVENT_CONSTITUENT + this.sparqlQueryName);
		ksAdapter.runKeyValueSparqlQuery(mentionQuery, Util.RELATION_NAME_CONSTITUENT_MENTION + this.sparqlQueryName + this.mentionQueryName, Util.VARIABLE_EVENT, Util.VARIABLE_MENTION, constituents);
	}

	@Override
	public Set<String> getRequiredMentionProperties() {
		return new HashSet<String>();
	}

}
