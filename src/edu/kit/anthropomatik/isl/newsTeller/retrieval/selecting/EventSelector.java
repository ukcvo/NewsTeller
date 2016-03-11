package edu.kit.anthropomatik.isl.newsTeller.retrieval.selecting;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

/**
 * Selects one event to be summarized to the user.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class EventSelector {

	private static Log log = LogFactory.getLog(EventSelector.class);
	
	private double threshold;
	
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	/**
	 * Select the highest scoring event. (assume that list is sorted in descending order)
	 */
	public NewsEvent selectEvent(List<NewsEvent> events, UserModel userModel) {
		if (log.isTraceEnabled())
			log.trace(String.format("selectEvent(events = <%s>)", StringUtils.collectionToCommaDelimitedString(events)));
		
		NewsEvent selectedEvent = null;
		if (events.isEmpty()) {
			if(log.isErrorEnabled())
				log.error("empty event list, cannot select anything!");
		} else {
			boolean isEventSelected = false;
			for (int i = 0; (i < events.size()) && !isEventSelected; i++) {
				selectedEvent = events.get(i);
				if (selectedEvent.getExpectedRelevanceScoring() < this.threshold) {
					if (log.isInfoEnabled())
						log.info("Not confident enough in top event, returning nothing.");
					selectedEvent = null;
					isEventSelected = true;
				} else if (!userModel.historyContainsEvent(selectedEvent))
					isEventSelected = true;
			}
			
		}
		
		if (log.isInfoEnabled())
			log.info(String.format("selected event: %s", (selectedEvent == null ? "null" : selectedEvent.toVerboseString())));
		return selectedEvent;
		
	}

}
