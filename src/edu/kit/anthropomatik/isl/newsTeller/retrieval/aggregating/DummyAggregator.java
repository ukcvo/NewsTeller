package edu.kit.anthropomatik.isl.newsTeller.retrieval.aggregating;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Sets the overall score to zero. Only for intial testing purposes.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class DummyAggregator extends ScoreAggregator {

	@Override
	protected void aggregateScoresForEvent(NewsEvent event) {
		event.setTotalUsabilityScore(0);
		event.setTotalRelevanceScore(0);
	}

}
