package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features.UsabilityFeature;
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
	
	public void setPriorProbabilityMap(Map<String, Double> priorProbabilityMap) {
		this.priorProbabilityMap = priorProbabilityMap;
	}
	
	public NaiveBayesFusion(String fileName) {
		this.priorProbabilityMap = Util.readPriorProbabilityMapFromFile(fileName);
	}
	
	private double getLogAccumulatedProbability(Map<UsabilityFeature,Integer> featureValues, String type) {
		double logProb = 0;
		
		logProb += priorProbabilityMap.get(type);
		
		for (UsabilityFeature feature : features) {
			logProb += feature.getLogProbability(featureValues.get(feature), type);
		}
		
		return logProb;
	}
	
	/**
	 * Returns the posterior probability of an event, given the prior probability and the conditional probabilities from the features.
	 */
	public double getProbabilityOfEvent(NewsEvent event) {
		
		Map<UsabilityFeature, Integer> featureValues = new HashMap<UsabilityFeature, Integer>();
		for (UsabilityFeature f : features)
			featureValues.put(f, f.getValue(event.getEventURI()));
			
		double nominator = getLogAccumulatedProbability(featureValues, Util.COLUMN_NAME_POSITIVE_PROBABILITY);
		double denominator = getLogAccumulatedProbability(featureValues, Util.COLUMN_NAME_OVERALL_PROBABILITY);
		
		double posteriorProbability = Math.exp(nominator - denominator);
		
		return posteriorProbability;
	}
}
