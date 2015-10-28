package edu.kit.anthropomatik.isl.newsTeller.data;

import java.util.ArrayList;
import java.util.List;

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

	public void setEventURI(String eventURI) {
		this.eventURI = eventURI;
	}

	public List<Scoring> getScorings() {
		return scorings;
	}

	public double getTotalScore() {
		return totalScore;
	}

	public void setTotalScore(double totalScore) {
		this.totalScore = totalScore;
	}
	
	public NewsEvent(String eventURI) {
		this.eventURI = eventURI;
		this.scorings = new ArrayList<Scoring>();
		this.totalScore = Double.NaN; // encodes that there is no total score, yet
	}

	public void addScoring(Scoring scoring) {
		scorings.add(scoring);
	}
}
