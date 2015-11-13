package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.Map;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Takes care of fusing the a priori probability and the conditional probabilities into a posterior probability.
 * Assumes that all feature probabilities and the prior probabilities are given as log-probabilities.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class NaiveBayesFusion {

	private Set<UsabilityFeature> features;
	
	private Map<String, Double> priorProbabilityMap;
	
	public void setFeatures(Set<UsabilityFeature> features)	{
		this.features = features;
	}
	
	public NaiveBayesFusion(String fileName) {
		this.priorProbabilityMap = Util.readPriorProbabilityMapFromFile(fileName);
	}
	
	private double getNominator(NewsEvent event, String type) {
		double logNominator = 0;
		
		logNominator += priorProbabilityMap.get(type);
		
		for (UsabilityFeature feature : features) {
			logNominator += feature.getLogProbability(feature.getValue(event.getEventURI()), type);
		}
		
		double nominator = Math.exp(logNominator);
		return nominator;
	}
	
	/**
	 * Returns the posterior probability of an event, given the prior probability and the conditional probabilities from the features.
	 */
	public double getProbabilityOfEvent(NewsEvent event) {
		
		double positiveNominator = getNominator(event, Util.COLUMN_NAME_POSITIVE_PROBABILITY);
		double negativeNominator = getNominator(event, Util.COLUMN_NAME_NEGATIVE_PROBABILITY);
		
		double posteriorProbability = positiveNominator / (positiveNominator + negativeNominator);
		
		return posteriorProbability;
	}
}
