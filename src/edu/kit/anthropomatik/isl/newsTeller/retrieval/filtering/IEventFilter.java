package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Filters events such that only usable ones remain.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public interface IEventFilter {
	
	/**
	 * Return the subset of usable events for further processing.
	 */
	public Set<NewsEvent> filterEvents(Set<NewsEvent> events);
	
	public void shutDown();
}
