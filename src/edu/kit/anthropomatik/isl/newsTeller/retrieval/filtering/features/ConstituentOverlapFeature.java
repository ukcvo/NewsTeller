package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.List;

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

	private String mentionQuery;
		
	public ConstituentOverlapFeature(String queryFileName, String mentionQueryFileName) {
		super(queryFileName);
		this.mentionQuery = Util.readStringFromFile(mentionQueryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		
		double result = 0;
		
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
			List<KSMention> toCheckForOverlap = new ArrayList<KSMention>();
			toCheckForOverlap.add(new KSMention(mentionURI));
			KSMention sentenceMention = ksAdapter.retrieveKSMentionFromMentionURI(mentionURI, true);
			
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

}
