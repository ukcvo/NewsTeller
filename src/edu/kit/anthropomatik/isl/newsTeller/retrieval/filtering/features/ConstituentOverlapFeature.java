package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.KSMention;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Counts constituent overlaps.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class ConstituentOverlapFeature extends UsabilityFeature {

	public ConstituentOverlapFeature() {
		super();
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		
		double result = 0;
		String arbitraryKeyword = keywords.get(0).getWord();
		
		Set<String> constituents = ksAdapter.getBufferedValues(Util.getRelationName("event", "entity", arbitraryKeyword), eventURI);
//		Set<KSMention> constituentMentions = new HashSet<KSMention>();
//		for (String constituent : constituents) {
//			Set<String> mentionURIs =  ksAdapter.getBufferedValues(Util.getRelationName("entity", "mention", arbitraryKeyword), constituent);
//			List<KSMention> mentions = new ArrayList<KSMention>();
//			for (String mentionURI : mentionURIs) {
//				boolean shouldAdd = true;
//				KSMention newMention = new KSMention(mentionURI);
//				List<KSMention> toRemove = new ArrayList<KSMention>();
//				for (KSMention m : mentions) {
//					if (m.contains(newMention)) {
//						shouldAdd = false;
//					} else if (newMention.contains(m)) {
//						toRemove.add(m);
//					}
//				}
//				mentions.removeAll(toRemove);
//				if (shouldAdd)
//					mentions.add(newMention);
//			}
//			constituentMentions.addAll(mentions);
//		}
		
		Set<String> mentionURIs = ksAdapter.getBufferedValues(Util.getRelationName("event", "mention", arbitraryKeyword), eventURI);
		
		for (String mentionURI : mentionURIs) {
			List<KSMention> toCheckForOverlap = new ArrayList<KSMention>();
			toCheckForOverlap.add(new KSMention(mentionURI));
			KSMention sentenceMention = ksAdapter.retrieveKSMentionFromMentionURI(mentionURI, true);
			
			String resourceURI = Util.resourceURIFromMentionURI(mentionURI);
//			toCheckForOverlap.addAll(ksAdapter.getEntityMentions(eventURI, resourceURI));
			Set<KSMention> constituentMentions = new HashSet<KSMention>();
			for (String entityURI : constituents)
				constituentMentions.addAll(ksAdapter.getEntityMentions(entityURI, resourceURI));
			
			for (KSMention mention : constituentMentions) {
				if (sentenceMention.overlap(mention) > 0)	// only check for overlap those who are contained in the sentence
					toCheckForOverlap.add(mention);
			}
			
			for (int i = 0; i < toCheckForOverlap.size(); i++) {
				for (int j = i + 1; j < toCheckForOverlap.size(); j++) {
					KSMention first = toCheckForOverlap.get(i);
					KSMention second = toCheckForOverlap.get(j);
					result += first.overlap(second);
				}
			}
		}
		
		return result;
	}

	@Override
	public Set<String> getRequiredMentionProperties() {
		return new HashSet<String>();
	}

}
