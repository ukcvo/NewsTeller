package edu.kit.anthropomatik.isl.newsTeller.retrieval.aggregating;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Responsible for aggregating all the scores into one total score for each event.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class ScoreAggregator {

	protected static Log log = LogFactory.getLog(ScoreAggregator.class);
	/**
	 * Aggregate scores of a single event and attaches this information to the event.
	 */
	protected abstract void aggregateScoresForEvent(NewsEvent event);
	
	/**
	 * For each event aggregates the scores into a total score.
	 */
	public void aggregateScores(List<NewsEvent> events) {
		if (log.isInfoEnabled())
			log.info(String.format("aggregateScores(#events = %d)", events.size()));
		
		for (NewsEvent event : events) {
			aggregateScoresForEvent(event);
			if(log.isDebugEnabled())
				log.debug(String.format("event total score: [%s|%f]", event.getEventURI(), event.getTotalScore()));
		}
	}
}
