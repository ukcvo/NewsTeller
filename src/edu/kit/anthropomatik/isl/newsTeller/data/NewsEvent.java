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

	private String eventURI;
	
	List<Scoring> scorings;
	
	double totalScore;

	public String getEventURI() {
		return eventURI;
	}

	public List<Scoring> getScorings() {
		return scorings;
	}

	public void addScoring(Scoring scoring) {
		scorings.add(scoring);
	}
	
	public double getTotalScore() {
		return totalScore;
	}

	public void setTotalScore(double totalScore) {
		this.totalScore = totalScore;
	}
	
	public NewsEvent(String eventURI, List<Scoring> scorings) {
		this.eventURI = eventURI;
		this.scorings = scorings;
		this.totalScore = Double.NaN; // encodes that there is no total score, yet
	}
	
	public NewsEvent(String eventURI) { this(eventURI, new ArrayList<Scoring>());}
	
	@Override
	public String toString() {
		return eventURI;
	}
	
	public String toVerboseString() {
		return String.format("[%s|<%s>|%f]", eventURI, StringUtils.collectionToCommaDelimitedString(scorings), totalScore);
	}
}
