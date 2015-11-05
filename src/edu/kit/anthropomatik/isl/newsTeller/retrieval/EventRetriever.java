package edu.kit.anthropomatik.isl.newsTeller.retrieval;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.aggregating.ScoreAggregator;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.finding.EventFinder;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.EventScorer;
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
	
	private EventScorer eventScorer;
	
	private ScoreAggregator scoreAggregator;
	
	private EventSelector eventSelector;
	
	//region setters
	public void setEventFinder(EventFinder eventFinder) {
		this.eventFinder = eventFinder;
	}

	public void setEventScorer(EventScorer eventScorer) {
		this.eventScorer = eventScorer;
	}

	public void setScoreAggregator(ScoreAggregator scoreAggregator) {
		this.scoreAggregator = scoreAggregator;
	}
	
	public void setEventSelector(EventSelector eventSelector) {
		this.eventSelector = eventSelector;
	}
	//endregion
	
	public NewsEvent retrieveEvent(List<Keyword> userQuery, UserModel userModel) {
		if (log.isTraceEnabled())
			log.trace(String.format("retrieveEvents(userQuery = <%s>, userModel = %s)", 
										StringUtils.collectionToCommaDelimitedString(userQuery) , userModel.toString()));
		
		List<NewsEvent> events = eventFinder.findEvents(userQuery, userModel);
		eventScorer.scoreEvents(events, userQuery, userModel);
		scoreAggregator.aggregateScores(events);
		NewsEvent event = eventSelector.selectEvent(events);
				
		return event;
	}
}
