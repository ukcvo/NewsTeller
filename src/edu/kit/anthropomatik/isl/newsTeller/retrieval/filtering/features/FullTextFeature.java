package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
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

	public FullTextFeature(String queryFileName) {
		super(queryFileName);
	}

	private double checkLabel(String label, Set<String> originalTexts) {
		for (String text : originalTexts) {
			if (text.toLowerCase().contains(label.toLowerCase())) //ignoring case
				return 1.0;
		}
		return 0.0;
	}

	private List<Double> checkLabels(List<List<String>> entityLabels, Set<String> originalTexts) {
		List<Double> result = new ArrayList<Double>();
		
		for (List<String> labels : entityLabels) {
			
			double appeared = 0;
			for (String label : labels) {
				appeared = checkLabel(label, originalTexts);
				if (appeared > 0)
					break;
			}
			result.add(appeared);
		}
		
		return result;
	}

	
	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		List<List<String>> labels = getLabels(eventURI, keywords);
		Set<String> originalTexts = ksAdapter.retrieveOriginalTexts(eventURI);
		List<Double> appearances = checkLabels(labels, originalTexts);
		double averageAppearance = Util.averageFromCollection(appearances);
		
		return averageAppearance;
	}
	
	protected abstract List<List<String>> getLabels(String eventURI, List<Keyword> keywords);
}