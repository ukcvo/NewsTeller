package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.KSMention;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class ConstituentsPerMentionFeature extends UsabilityFeature {

	public static final int OPERATION_TYPE_AVG = 0;
	public static final int OPERATION_TYPE_MAX = 1;
	public static final int OPERATION_TYPE_MIN = 2;
	public static final int OPERATION_TYPE_ZERO_FRACTION = 3;
	public static final int OPERATION_TYPE_COUNT_NONZERO = 4;

	private int operationType;

	public void setOperationType(int operationType) {
		this.operationType = operationType;
	}

	public ConstituentsPerMentionFeature() {
		super();
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {

		double result = (this.operationType == OPERATION_TYPE_MIN) ? Double.POSITIVE_INFINITY : 0;
		String arbitraryKeyword = keywords.get(0).getWord();

		Set<String> mentionURIs = ksAdapter.getBufferedValues(Util.getRelationName("event", "mention", arbitraryKeyword), eventURI);
		Set<String> resourceURIs = Util.resourceURIsFromMentionURIs(mentionURIs);
		
		Set<String> constituents = ksAdapter.getBufferedValues(Util.getRelationName("event", "entity", arbitraryKeyword), eventURI);
		List<List<KSMention>> constituentMentions = new ArrayList<List<KSMention>>();
		for (String constituent : constituents) {
			List<KSMention> mentions = new ArrayList<KSMention>();
			for (String resourceURI : resourceURIs) 
				mentions.addAll(ksAdapter.getEntityMentions(constituent, resourceURI));
			constituentMentions.add(mentions);
		}
		
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
	public Set<String> getRequiredMentionProperties() {
		return new HashSet<String>();
	}

}
