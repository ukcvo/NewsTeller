package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.List;

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
	
	private String labelQuery;
	
	public ConstituentOverlapFeature(String queryFileName, String mentionQueryFileName, String labelQueryFileName) {
		super(queryFileName);
		this.mentionQuery = Util.readStringFromFile(mentionQueryFileName);
		this.labelQuery = Util.readStringFromFile(labelQueryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		
		double result = 0;
		
		List<String> constituents = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_ENTITY);
		List<List<String>> constituentLabels = new ArrayList<List<String>>();
		for (String constituent : constituents) {
			constituentLabels.add(ksAdapter.runSingleVariableStringQuery(labelQuery.replace(Util.PLACEHOLDER_ENTITY, constituent), Util.VARIABLE_LABEL));
		}
		
		List<String> mentions = ksAdapter.runSingleVariableStringQuery(mentionQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_MENTION);
		for (String mention : mentions) {
			List<String> checkForOverlap = new ArrayList<String>();
			checkForOverlap.add(ksAdapter.getUniqueMentionProperty(mention, Util.MENTION_PROPERTY_PRED));
			String mentionSentence = ksAdapter.retrieveSentenceFromMention(mention);
			
			// collect all the labels to be used
			for (List<String> labelSet : constituentLabels) {
				List<String> relevantLabels = new ArrayList<String>();
				for (String label : labelSet) {
					if (mentionSentence.contains(label)) { // get all that are in the sentence
						relevantLabels.add(label);
					}
				}
				List<String> pickedLabels = new ArrayList<String>(relevantLabels);
				for (int i = 0; i < relevantLabels.size(); i++) {
					for (int j = i + 1; j < relevantLabels.size(); j++) { // now eliminate the ones overlapping like "Bob Hawke" and "Hawke"
						String first = relevantLabels.get(i);
						String second = relevantLabels.get(j);
						if (first.contains(second))
							pickedLabels.remove(second);
						else if (second.contains(first))
							pickedLabels.remove(first);
					}
				}
				checkForOverlap.addAll(pickedLabels);
			}
			
			// actually check for overlaps
			for (int i = 0; i < checkForOverlap.size(); i++) {
				for (int j = i + 1; j < checkForOverlap.size(); j++) {
					String[] first = checkForOverlap.get(i).split(" ");
					String[] second = checkForOverlap.get(j).split(" ");
					double overlap = 0;
					for (int a = 0; a < first.length; a++) {
						for (int b = 0; b < second.length; b++) {
							if (first[a].equals(second[b]))
								overlap++;
						}
					}
					result += overlap / (Math.min(first.length, second.length));
				}
			}
		}
		
		return result;
	}

}
