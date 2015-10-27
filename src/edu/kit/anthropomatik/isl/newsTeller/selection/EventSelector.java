package edu.kit.anthropomatik.isl.newsTeller.selection;

import java.net.URI;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

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

	private static Log log = LogFactory.getLog(EventSelector.class);
	
	private List<EventScorer> eventScorers;
	
	public void setEventScorers(List<EventScorer> eventScorers) {
		this.eventScorers = eventScorers;
	}
		
	public URI selectEvent(List<URI> events, List<Keyword> userQuery, UserModel userModel) {
		if (log.isInfoEnabled())
			log.info(String.format("selectEvent(events = <%s>, userQuery = <%s>, userModel = %s)", 
										StringUtils.collectionToCommaDelimitedString(events), 
										StringUtils.collectionToCommaDelimitedString(userQuery),
										userModel.toString()));
		
		for (URI event : events) {
			for (EventScorer scorer : eventScorers) {
				//TODO: figure out how to combine scores and pick highest element (might need a bit of infrastructure)
				scorer.scoreEvent(event, userQuery, userModel);
			}
		}
		
		return null;
	}

}
