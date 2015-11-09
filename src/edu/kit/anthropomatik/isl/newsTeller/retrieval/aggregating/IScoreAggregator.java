package edu.kit.anthropomatik.isl.newsTeller.retrieval.aggregating;

import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Scoring;

/**
 * Aggregates all the given scorings into one total score.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public interface IScoreAggregator {

	/**
	 * Aggregates all the given scorings into one total score.
	 */
	public double getTotalScore(List<Scoring> scorings);
}
