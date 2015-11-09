package edu.kit.anthropomatik.isl.newsTeller.retrieval;

import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.EventFilter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.finding.EventFinder;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.EventRanker;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.RelevanceScorer;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.UsabilityScorer;
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
	
	private EventFinder eventFinder;
	
	private UsabilityScorer usabilityScorer;
	
	private EventFilter eventFilter;
	
	private RelevanceScorer relevanceScorer;
	
	private EventRanker eventRanker;

	private EventSelector eventSelector;
	
	//region setters
	public void setEventFinder(EventFinder eventFinder) {
		this.eventFinder = eventFinder;
	}

	public void setUsabilityScorer(UsabilityScorer usabilityScorer) {
		this.usabilityScorer = usabilityScorer;
	}

	public void setEventFilter(EventFilter eventFilter) {
		this.eventFilter = eventFilter;
	}
	
	public void setRelevanceScorer(RelevanceScorer relevanceScorer) {
		this.relevanceScorer = relevanceScorer;
	}

	public void setEventRanker(EventRanker eventRanker) {
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
		
		Set<NewsEvent> events = eventFinder.findEvents(userQuery, userModel);
		usabilityScorer.scoreEvents(events, userQuery, userModel);
		Set<NewsEvent> filteredEvents = eventFilter.filterEvents(events);
		relevanceScorer.scoreEvents(filteredEvents, userQuery, userModel);
		List<NewsEvent> rankedEvents = eventRanker.getRankedEvents(filteredEvents);
		NewsEvent event = eventSelector.selectEvent(rankedEvents);
				
		return event;
	}
}
