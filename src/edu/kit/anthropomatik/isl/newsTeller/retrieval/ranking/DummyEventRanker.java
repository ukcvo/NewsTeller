package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

/**
 * Dummy implementation of IEventRanker: simply converts the set of events into a list without caring about the ordering at all.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class DummyEventRanker implements IEventRanker {

	@Override
	public List<NewsEvent> rankEvents(Set<NewsEvent> events, List<Keyword> userQuery, UserModel userModel) {
		return new ArrayList<NewsEvent>(events);
	}

}
