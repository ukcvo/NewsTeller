package edu.kit.anthropomatik.isl.newsTeller.data;

/**
 * Represents one line of a benchmark query file (i.e. one event and its ground truth annotations).
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class GroundTruth {

	private double usabilityRating;
	
	private int relevanceRank;

	
	public double getUsabilityRating() {
		return usabilityRating;
	}

	public int getRelevanceRank() {
		return relevanceRank;
	}
	
	public GroundTruth(double usabilityRating, int relevanceRank) {
		this.usabilityRating = usabilityRating;
		this.relevanceRank = relevanceRank;
	}
}
