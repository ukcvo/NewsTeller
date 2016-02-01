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

	private String mentionQuery;
	
	private String mentionQueryName;
	
	public ConstituentOverlapFeature(String queryFileName, String mentionQueryFileName) {
		super(queryFileName);
		this.mentionQuery = Util.readStringFromFile(mentionQueryFileName);
		this.mentionQueryName = Util.queryNameFromFileName(mentionQueryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		
		double result = 0;
		
		Set<String> constituents = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_CONSTITUENT + this.sparqlQueryName, eventURI);
		Set<KSMention> constituentMentions = new HashSet<KSMention>();
		for (String constituent : constituents) {
			Set<String> mentionURIs =  ksAdapter.getBufferedValues(Util.RELATION_NAME_CONSTITUENT_MENTION + this.sparqlQueryName + this.mentionQueryName, constituent);
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

	@Override
	public void runBulkQueries(Set<String> eventURIs, List<Keyword> keywords) {
		ksAdapter.runKeyValueSparqlQuery(mentionQuery, Util.RELATION_NAME_EVENT_MENTION + this.mentionQueryName, Util.VARIABLE_EVENT, Util.VARIABLE_MENTION, eventURIs);
		ksAdapter.runKeyValueSparqlQuery(sparqlQuery, Util.RELATION_NAME_EVENT_CONSTITUENT + this.sparqlQueryName, Util.VARIABLE_EVENT, Util.VARIABLE_ENTITY, eventURIs);
		Set<String> constituents = ksAdapter.getAllRelationValues(Util.RELATION_NAME_EVENT_CONSTITUENT + this.sparqlQueryName);
		ksAdapter.runKeyValueSparqlQuery(mentionQuery, Util.RELATION_NAME_CONSTITUENT_MENTION + this.sparqlQueryName + this.mentionQueryName, Util.VARIABLE_EVENT, Util.VARIABLE_MENTION, constituents);
	}

}
