package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Dummy implementation, doesn't filter at all.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class DummyEventFilter implements IEventFilter {

	public Set<NewsEvent> filterEvents(Set<NewsEvent> events) {
		return events;
	}

	public void shutDown() {
		// nothing to do here
	}
}
