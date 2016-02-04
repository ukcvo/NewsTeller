package edu.kit.anthropomatik.isl.newsTeller.data.benchmark;

import java.util.Set;

/**
 * Represents one line of a benchmark query file (i.e. one event and its ground truth annotations).
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class GroundTruth {

	private double usabilityRating;
	
	private int relevanceRank;
	
	private Set<UsabilityRatingReason> reasons;
	
	public double getUsabilityRating() {
		return usabilityRating;
	}

	public int getRelevanceRank() {
		return relevanceRank;
	}
	
	/**
	 * Returns (2^relevanceRank - 1) which will be used as target value for regression.
	 */
	public double getRegressionRelevanceValue() {
		return (Math.pow(2, this.relevanceRank) - 1.0);
	}
	
	public Set<UsabilityRatingReason> getReasons() {
		return reasons;
	}
	
	public GroundTruth(double usabilityRating, int relevanceRank, Set<UsabilityRatingReason> reasons) {
		this.usabilityRating = usabilityRating;
		this.relevanceRank = relevanceRank;
		this.reasons = reasons;
	}
}
