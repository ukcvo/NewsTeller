package edu.kit.anthropomatik.isl.newsTeller.data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Represents a news event being processed by the NewsTeller. Includes the event URI and scoring information.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class NewsEvent implements Comparable<NewsEvent>{

	// URI of the event - identifies the event unambiguously
	private String eventURI;

	// values for the usability features
	private ConcurrentMap<String, Double> usabilityFeatureValues;
	
	// probability that this event is usable -- as judged by the classifier based on the usability features
	private double usabilityProbability;

	// values for the relevance ranking features
	private ConcurrentMap<String, Double> relevanceFeatureValues;
	
	// relevance as predicted by the regression based on the relevance ranking features
	private double expectedRelevance;
	
	//region getters & setters
	public String getEventURI() {
		return eventURI;
	}

	public double getUsabilityProbability() {
		return usabilityProbability;
	}

	public void setUsabilityProbability(double usabilityProbability) {
		this.usabilityProbability = usabilityProbability;
	}

	public double getExpectedRelevance() {
		return expectedRelevance;
	}

	public void setExpectedRelevance(double expectedRelevance) {
		this.expectedRelevance = expectedRelevance;
	}
	
	/**
	 * Instead of returning the raw value used for the internal regression, return the score projected to the scale used to label the data.
	 */
	public double getExpectedRelevanceScoring() {
		return Math.log(expectedRelevance + 1) / Math.log(2);
	}
	//endregion

	public NewsEvent(String eventURI) {
		this.eventURI = eventURI;
		this.usabilityFeatureValues = new ConcurrentHashMap<String, Double>();
		this.relevanceFeatureValues = new ConcurrentHashMap<String, Double>();
	}
	
	public void addUsabilityFeatureValue(String featureName, Double featureValue) {
		this.usabilityFeatureValues.putIfAbsent(featureName, featureValue);
	}
	
	public double getUsabilityFeatureValue(String featureName) {
		return this.usabilityFeatureValues.getOrDefault(featureName, Double.NaN);
	}
	
	public void addRelevanceFeatureValue(String featureName, Double featureValue) {
		this.relevanceFeatureValues.putIfAbsent(featureName, featureValue);
	}
	
	public double getRelevanceFeatureValue(String featureName) {
		return this.relevanceFeatureValues.getOrDefault(featureName, Double.NaN);
	}
	
	@Override
	public String toString() {
		return eventURI;
	}
	
	public String toVerboseString() {
		return String.format("[%s|%.2f|%.2f]", eventURI, usabilityProbability, expectedRelevance);
	}
	
	@Override
	public boolean equals(Object o) {
		return ((o != null) && this.toString().equals(o.toString()));
	}
	
	@Override
    public int hashCode() {
        return this.toString().hashCode();
    }

	public int compareTo(NewsEvent o) {
		if (this.expectedRelevance > o.getExpectedRelevance())
			return -1;
		else if (this.expectedRelevance < o.getExpectedRelevance())
			return 1;
		else
			return 0;
	}
}
