package edu.kit.anthropomatik.isl.newsTeller.data;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

/**
 * Represents a news event being processed by the NewsTeller. Includes the event URI and scoring information.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class NewsEvent {

	// URI of the event - identifies the event unambiguously
	private String eventURI;

	// scorings with respect to usability of the event (i.e. well-formedness)
	List<Scoring> usabilityScorings;
	
	double totalUsabilityScore;
	
	// scorings with respect to relevance of the event
	List<Scoring> relevanceScorings;
	
	double totalRelevanceScore;

	//region getters & setters
	public String getEventURI() {
		return eventURI;
	}

	public List<Scoring> getUsabilityScorings() {
		return usabilityScorings;
	}

	public void addUsabilityScoring(Scoring usabilityScoring) {
		usabilityScorings.add(usabilityScoring);
	}
	
	public double getTotalUsabilityScore() {
		return totalUsabilityScore;
	}

	public void setTotalUsabilityScore(double totalUsabilityScore) {
		this.totalUsabilityScore = totalUsabilityScore;
	}
	
	public List<Scoring> getRelevanceScorings() {
		return relevanceScorings;
	}

	public void addRelevanceScoring(Scoring relevanceScoring) {
		relevanceScorings.add(relevanceScoring);
	}
	
	public double getTotalRelevanceScore() {
		return totalRelevanceScore;
	}

	public void setTotalRelevanceScore(double totalRelevanceScore) {
		this.totalRelevanceScore = totalRelevanceScore;
	}
	//endregion
	
	public NewsEvent(String eventURI, List<Scoring> relevanceScorings) {
		this.eventURI = eventURI;
		this.relevanceScorings = relevanceScorings;
		this.totalRelevanceScore = Double.NaN; // encodes that there is no total score, yet
	}
	
	public NewsEvent(String eventURI) { this(eventURI, new ArrayList<Scoring>());}
	
	@Override
	public String toString() {
		return eventURI;
	}
	
	public String toVerboseString() {
		return String.format("[%s|<%s>|%f]", eventURI, StringUtils.collectionToCommaDelimitedString(relevanceScorings), totalRelevanceScore);
	}
	
	@Override
	public boolean equals(Object o) {
		return ((o != null) && this.toString().equals(o.toString()));
	}
	
	@Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
