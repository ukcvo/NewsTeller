package edu.kit.anthropomatik.isl.newsTeller.selection;

import java.net.URI;
import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.selection.scoring.EventScorer;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

/**
 * Selects one event to be summarized to the user.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class EventSelector {

	private List<EventScorer> eventScorers;
	
	public void setEventScorers(List<EventScorer> eventScorers) {
		this.eventScorers = eventScorers;
	}
		
	public URI selectEvent(List<URI> events, List<Keyword> userQuery, UserModel userModel) {
		
		//TODO: figure out how to combine scores and pick highest element (might need a bit of infrastructure)
		return null;
	}

}
