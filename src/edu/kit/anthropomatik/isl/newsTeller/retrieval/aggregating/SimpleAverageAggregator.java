package edu.kit.anthropomatik.isl.newsTeller.retrieval.aggregating;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.Scoring;

/**
 * Aggregates event scores by taking a simple average.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SimpleAverageAggregator extends ScoreAggregator {

	@Override
	protected void aggregateScoresForEvent(NewsEvent event) {
		
		double relevanceSum = 0;
		for (Scoring scoring : event.getRelevanceScorings())
			relevanceSum += scoring.getScore();
		double totalRelevanceScore = relevanceSum / event.getRelevanceScorings().size();
		
		event.setTotalRelevanceScore(totalRelevanceScore);
	}

}
