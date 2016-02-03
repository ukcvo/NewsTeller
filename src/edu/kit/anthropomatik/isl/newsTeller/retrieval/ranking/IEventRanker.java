package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking;

import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Takes care of ranking the events according to their total relevance score.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public interface IEventRanker {

	/**
	 * Ranks the given set of events according to the expected relevance and returns them as list (sorted by relevance in descending order).
	 */
	public List<NewsEvent> rankEvents(Set<NewsEvent> events);
}
