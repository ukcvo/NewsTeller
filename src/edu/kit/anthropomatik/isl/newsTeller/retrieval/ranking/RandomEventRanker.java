package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

/**
 * Orders the events randomly by putting them into a list and shuffling.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class RandomEventRanker implements IEventRanker {

	@Override
	public List<NewsEvent> rankEvents(Set<NewsEvent> events, List<Keyword> userQuery, UserModel userModel) {
		List<NewsEvent> result = new ArrayList<NewsEvent>(events);
		Collections.shuffle(result);
		return result;
	}

}
