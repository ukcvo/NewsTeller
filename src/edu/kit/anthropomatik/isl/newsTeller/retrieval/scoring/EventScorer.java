package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring;

import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.ConversationCycle;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.Scoring;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics.EventHeuristic;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics.HistoryHeuristic;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics.KeywordHeuristic;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

/**
 * Responsible for scoring all the events according to different scoring heuristics (only collecting individual scores, not aggregating them).
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class EventScorer {

	private static Log log = LogFactory.getLog(EventScorer.class);
	
	private List<EventHeuristic>	eventHeuristics;	// heuristics working only on the event itself
	@SuppressWarnings("unused")
	private List<KeywordHeuristic> 	queryHeuristics;	// heuristics working on the event and a keyword from the user query
	@SuppressWarnings("unused")
	private List<KeywordHeuristic> 	interestHeuristics;	// heuristics working on the event and a keyword from the user interests
	@SuppressWarnings("unused")
	private List<HistoryHeuristic>	historyHeuristics;	// heuristics working on the event and an event from previous conversation cycles
	
	//region setters
	public void setEventHeuristics(List<EventHeuristic> eventHeuristics) {
		this.eventHeuristics = eventHeuristics;
	}

	public void setQueryHeuristics(List<KeywordHeuristic> queryHeuristics) {
		this.queryHeuristics = queryHeuristics;
	}

	public void setInterestHeuristics(List<KeywordHeuristic> interestHeuristics) {
		this.interestHeuristics = interestHeuristics;
	}

	public void setHistoryHeuristics(List<HistoryHeuristic> historyHeuristics) {
		this.historyHeuristics = historyHeuristics;
	}
	//endregion

	/**
	 * Apply all eventHeuristics to a certain event.
	 */
	private void applyEventHeuristics(NewsEvent event) {
		if (log.isTraceEnabled())
			log.trace(String.format("applyEventHeuristics(event = %s)", event.toString()));
		
		for (EventHeuristic eventHeuristic : eventHeuristics) {
			double score = eventHeuristic.getScore(event);
			Scoring scoring = new Scoring(eventHeuristic.getName(), score);
			event.addScoring(scoring);
		}
	}
	
	/**
	 * Apply all queryHeuristics to a certain event and the set of query keywords.
	 */
	private void applyQueryHeuristics(NewsEvent event, List<Keyword> userQuery) {
		if (log.isTraceEnabled())
			log.trace(String.format("applyQueryHeuristics(event = %s, userQuery = <%s>)", 
					event.toString(), StringUtils.collectionToCommaDelimitedString(userQuery)));
		//TODO: implement (Scope 3) --> weighted average! (keyword weights)
	}
	
	/**
	 * Apply all interestHeuristics to a certain event and the set of user interests.
	 */
	private void applyInterestHeuristics(NewsEvent event, List<Keyword> userInterests) {
		if (log.isTraceEnabled())
			log.trace(String.format("applyInterestHeuristics(event = %s, userInterests = <%s>)", 
					event.toString(), StringUtils.collectionToCommaDelimitedString(userInterests)));
		//TODO: implement (Scope 4) --> weighted average! (keyword weights)
	}
	
	/**
	 * Apply all historyHeuristics to a certain event and the list of previous conversation cycles.
	 */
	private void applyHistoryHeuristics(NewsEvent event, List<ConversationCycle> conversationHistory) {
		if (log.isTraceEnabled())
			log.trace(String.format("applyHistoryHeuristics(event = %s, conversationHistory = <%s>)", 
					event.toString(), StringUtils.collectionToCommaDelimitedString(conversationHistory)));
		//TODO: implement (Scope 6/7) --> weighted average! (decaying over time)
	}
	
	/**
	 * Score all the events based on all the available heuristics.
	 */
	public void scoreEvents(Set<NewsEvent> events, List<Keyword> userQuery, UserModel userModel) {
		if (log.isTraceEnabled())
			log.trace(String.format("scoreEvents(events = <%s>, userQuery = <%s>, userModel = %s)", 
					StringUtils.collectionToCommaDelimitedString(events),
					StringUtils.collectionToCommaDelimitedString(userQuery),
					userModel.toString()));
		
		for (NewsEvent event : events) {
			applyEventHeuristics(event);
			applyQueryHeuristics(event, userQuery);
			applyInterestHeuristics(event, userModel.getInterests());
			applyHistoryHeuristics(event, userModel.getHistory());
			
			if (log.isDebugEnabled())
				log.debug(String.format("event scored: %s", event.toVerboseString()));
		}
		
	}
}
