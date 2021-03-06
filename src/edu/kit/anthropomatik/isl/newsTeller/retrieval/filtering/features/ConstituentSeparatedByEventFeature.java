package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

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

	public void setOperationType(int operationType) {
		this.operationType = operationType;
	}

	public ConstituentSeparatedByEventFeature() {
		super();
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {

		double result = (this.operationType == OPERATION_TYPE_MIN) ? Double.POSITIVE_INFINITY : 0;
		String arbitraryKeyword = keywords.get(0).getWord();
		
		Set<String> constituents = ksAdapter.getBufferedValues(Util.getRelationName("event", "entity", arbitraryKeyword), eventURI);
		Set<String> mentionURIs = ksAdapter.getBufferedValues(Util.getRelationName("event", "mention", arbitraryKeyword), eventURI);
		
		for (String mentionURI : mentionURIs) {
			KSMention sentenceMention = ksAdapter.retrieveKSMentionFromMentionURI(mentionURI, true);
			KSMention eventMention = ksAdapter.retrieveKSMentionFromMentionURI(mentionURI, false);

			Set<KSMention> allEventMentions = ksAdapter.getAllEventMentions(sentenceMention.getResourceURI());
			allEventMentions.remove(eventMention);

			String resourceURI = Util.resourceURIFromMentionURI(mentionURI);
			Set<KSMention> constituentMentions = new HashSet<KSMention>();
			for (String entityURI : constituents)
				constituentMentions.addAll(ksAdapter.getEntityMentions(entityURI, resourceURI));
			
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
	public Set<String> getRequiredMentionProperties() {
		return new HashSet<String>();
	}

}
