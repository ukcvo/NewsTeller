package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Dummy implementation of IEventRanker: simply converts the set of events into a list without caring about the ordering at all.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class DummyEventRanker implements IEventRanker {

	@Override
	public List<NewsEvent> rankEvents(Set<NewsEvent> events) {
		return new ArrayList<NewsEvent>(events);
	}

}
