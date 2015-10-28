package edu.kit.anthropomatik.isl.newsTeller.retrieval.aggregating;

import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Responsible for aggregating all the scores into one total score for each event.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class ScoreAggregator {

	protected abstract void aggregateScoresForEvent(NewsEvent event);
	
	public void aggregateScores(List<NewsEvent> events) {
		for (NewsEvent event : events) {
			aggregateScoresForEvent(event);
		}
	}
}
