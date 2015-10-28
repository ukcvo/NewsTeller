package edu.kit.anthropomatik.isl.newsTeller.data;

/**
 * Represents a score assigned to an event by a certain heuristic.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class Scoring {

	private String nameOfHeuristic;
	
	private double score;

	public String getNameOfHeuristic() {
		return nameOfHeuristic;
	}

	public void setNameOfHeuristic(String nameOfHeuristic) {
		this.nameOfHeuristic = nameOfHeuristic;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}
	
	public Scoring(String nameOfHeuristic, double score) {
		this.nameOfHeuristic = nameOfHeuristic;
		this.score = score;
	}
}
