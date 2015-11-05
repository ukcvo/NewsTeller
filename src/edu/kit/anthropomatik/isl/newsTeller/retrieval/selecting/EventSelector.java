package edu.kit.anthropomatik.isl.newsTeller.retrieval.selecting;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Selects one event to be summarized to the user.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class EventSelector {

	private static Log log = LogFactory.getLog(EventSelector.class);
	
	/**
	 * Select the highest scoring event.
	 */
	public NewsEvent selectEvent(List<NewsEvent> events) {
		if (log.isTraceEnabled())
			log.trace(String.format("selectEvent(events = <%s>)", StringUtils.collectionToCommaDelimitedString(events)));
		
		double maxValue = Double.NEGATIVE_INFINITY;
		NewsEvent selectedEvent = null;
		for (NewsEvent event : events) {
			if (event.getTotalScore() > maxValue) {
				maxValue = event.getTotalScore();
				selectedEvent = event;
			}
		}
		
		if (log.isTraceEnabled())
			log.trace(String.format("selected event: %s", (selectedEvent == null ? "null" : selectedEvent.toVerboseString())));
		return selectedEvent;
		
	}

}
