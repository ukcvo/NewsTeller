package edu.kit.anthropomatik.isl.newsTeller.retrieval;

import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.IEventFilter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.IEventRanker;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.search.EventSearcher;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.selecting.EventSelector;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

/**
 * Takes care of retrieving the most relevant event from the KnowledgeStore (IR task).
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class EventRetriever {

	private static Log log = LogFactory.getLog(EventRetriever.class);
	
	private EventSearcher eventSearcher;
	
	private IEventFilter eventFilter;
	
	private IEventRanker eventRanker;

	private EventSelector eventSelector;
	
	//region setters
	public void setEventSearcher(EventSearcher eventSearcher) {
		this.eventSearcher = eventSearcher;
	}

	public void setEventFilter(IEventFilter eventFilter) {
		this.eventFilter = eventFilter;
	}
	
	public void setEventRanker(IEventRanker eventRanker) {
		this.eventRanker = eventRanker;
	}
	
	public void setEventSelector(EventSelector eventSelector) {
		this.eventSelector = eventSelector;
	}
	//endregion
	
	public NewsEvent retrieveEvent(List<Keyword> userQuery, UserModel userModel) {
		if (log.isTraceEnabled())
			log.trace(String.format("retrieveEvents(userQuery = <%s>, userModel = %s)", 
										StringUtils.collectionToCommaDelimitedString(userQuery) , userModel.toString()));
		//region time logging
		long t0 = System.currentTimeMillis();
		long old = System.currentTimeMillis();
		//endregion
		Set<NewsEvent> events = eventSearcher.findEvents(userQuery, userModel);
		//region time logging
		if (log.isDebugEnabled()) {
			long l = System.currentTimeMillis();
			log.debug(String.format("search for events: % d ms", l - old));
			log.debug(String.format("found %d events", events.size()));
			old = l;
		}
		//endregion
		Set<NewsEvent> filteredEvents = eventFilter.filterEvents(events, userQuery);
		//region time logging
		if (log.isDebugEnabled()) {
			long l = System.currentTimeMillis();
			log.debug(String.format("filter events: % d ms", l - old));
			log.debug(String.format("keeping %d of %d events", events.size(), filteredEvents.size()));
			old = l;
		}
		//endregion
		List<NewsEvent> rankedEvents = eventRanker.rankEvents(filteredEvents, userQuery, userModel);
		//region time logging
		if (log.isDebugEnabled()) {
			long l = System.currentTimeMillis();
			log.debug(String.format("relevance ranking: % d ms", l - old));
			old = l;
		}
		//endregion
		NewsEvent event = eventSelector.selectEvent(rankedEvents);
		//region time logging
		if (log.isDebugEnabled()) {
			long l = System.currentTimeMillis();
			log.debug(String.format("select event: % d ms", l - old));
			log.debug(String.format("total: %d", l - t0));
		}
		//endregion		
		return event;
	}
	
	public void shutDown() {
		this.eventFilter.shutDown();
	}
}
