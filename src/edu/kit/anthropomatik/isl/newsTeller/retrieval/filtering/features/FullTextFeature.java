package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Checks if some kind of Strings (to be determined by subclass) appear in the full text.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class FullTextFeature extends UsabilityFeature {

	private boolean doOnlyUseSentence;
	
	protected boolean doUseContainsInsteadOfRegex;
	
	public void setDoOnlyUseSentence(boolean doOnlyUseSentence) {
		this.doOnlyUseSentence = doOnlyUseSentence;
	}
	
	public void setDoUseContainsInsteadOfRegex(boolean doUseContainsInsteadOfRegex) {
		this.doUseContainsInsteadOfRegex = doUseContainsInsteadOfRegex;
	}
	
	public FullTextFeature(String queryFileName) {
		super(queryFileName);
	}

	private double checkLabel(List<String> labelParts, Set<String> originalTexts) {
		double max = 0.0;
		for (String text : originalTexts) { // take max over all texts
			String[] lowerCaseText = text.toLowerCase().split("\n");
			double sum = 0.0;
			for (String labelPart : labelParts) { // compute fraction of label parts that are actually mentioned
				if (labelPart.isEmpty())
					continue;
				String regex = Util.KEYWORD_REGEX_PREFIX_JAVA + labelPart.toLowerCase() + Util.KEYWORD_REGEX_SUFFIX_JAVA;
				for (String line : lowerCaseText) {
					if (line.isEmpty())
						continue;
					if ((this.doUseContainsInsteadOfRegex && line.contains(labelPart)) || (line.matches(regex))) { // ignoring case
						sum++;
						break;
					}
				}
				
			}
			sum /= labelParts.size();
			if (sum > max)
				max = sum;
		}
		return max;
	}

	// entityLabels: keyword-dbPediaLabels-labelParts
	private List<Double> checkLabels(List<List<List<String>>> entityLabels, Set<String> originalTexts) {
		List<Double> result = new ArrayList<Double>();
		
		for (List<List<String>> labels : entityLabels) {
			
			double appeared = 0;
			for (List<String> labelParts : labels) {
				appeared = checkLabel(labelParts, originalTexts);
				if (appeared > 0)
					break;
			}
			result.add(appeared);
		}
		
		return result;
	}

	
	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		List<List<List<String>>> labels = getLabels(eventURI, keywords);
		Set<String> originalTexts;
		if (this.doOnlyUseSentence)
			originalTexts = new HashSet<String>(ksAdapter.retrieveSentencesfromEvent(eventURI)); //use only sentence
		else
			originalTexts = ksAdapter.retrieveOriginalTexts(eventURI); //use complete text
		List<Double> appearances = checkLabels(labels, originalTexts);
		double averageAppearance = appearances.isEmpty() ? 1.0 : Util.averageFromCollection(appearances);
		
		return averageAppearance;
	}
	
	/**
	 * Finds the labels to check. Outermost list is for different keywords, middle-layer list is for different labels, innermost list is for label parts.
	 */
	protected abstract List<List<List<String>>> getLabels(String eventURI, List<Keyword> keywords);
}