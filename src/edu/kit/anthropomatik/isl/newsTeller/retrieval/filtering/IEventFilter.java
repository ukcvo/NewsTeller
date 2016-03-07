package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

/**
 * Filters events such that only usable ones remain.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public interface IEventFilter {
	
	/**
	 * Return the subset of usable events for further processing. May use the user query for filtering purposes.
	 */
	public Set<NewsEvent> filterEvents(Set<NewsEvent> events, List<Keyword> userQuery, UserModel userModel);
	
	public void shutDown();
}
