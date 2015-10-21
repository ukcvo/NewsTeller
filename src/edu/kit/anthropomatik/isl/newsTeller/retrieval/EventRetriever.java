package edu.kit.anthropomatik.isl.newsTeller.retrieval;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.finders.EventFinder;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

/**
 * Takes care of retrieving potentially relevant events from the KnowledgeStore.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class EventRetriever {

	private static Log log = LogFactory.getLog(EventRetriever.class);
	
	private List<EventFinder> eventFinders;
	
	public void setEventFinders(List<EventFinder> eventFinders) {
		this.eventFinders = eventFinders;
	}

	public List<URI> retrieveEvents(List<Keyword> userQuery, UserModel userModel) {
		
		if (log.isTraceEnabled())
			log.trace("retrieve events");
		
		List<URI> events = new ArrayList<URI>();
		
		for (EventFinder finder : eventFinders) {
			events.addAll(finder.findEvents(userQuery, userModel));
		}
		
		return events;
	}
}
