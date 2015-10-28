package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring;

import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.ConversationCycle;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.Scoring;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics.EventHeuristic;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics.HistoryHeuristic;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics.KeywordHeuristic;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

/**
 * Scores an event based on certain criteria.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class EventScorer {

	private List<EventHeuristic>	eventHeuristics;
	@SuppressWarnings("unused")
	private List<KeywordHeuristic> 	queryHeuristics;
	@SuppressWarnings("unused")
	private List<KeywordHeuristic> 	interestHeuristics;
	@SuppressWarnings("unused")
	private List<HistoryHeuristic>	historyHeuristics;
	
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

	private void applyEventHeuristics(NewsEvent event) {
		for (EventHeuristic eventHeuristic : eventHeuristics) {
			double score = eventHeuristic.getScore(event);
			Scoring scoring = new Scoring(eventHeuristic.getName(), score);
			event.addScoring(scoring);
		}
	}
	
	private void applyQueryHeuristics(NewsEvent event, List<Keyword> userQuery) {
		//TODO: implement (Scope 3)
	}
	
	private void applyInterestHeuristics(NewsEvent event, List<Keyword> userInterests) {
		//TODO: implement (Scope 4)
	}
	
	private void applyHistoryHeuristics(NewsEvent event, List<ConversationCycle> conversationHistory) {
		//TODO: implement (Scope 6/7)
	}
	
	public void scoreEvents(List<NewsEvent> events, List<Keyword> userQuery, UserModel userModel) {
		
		for (NewsEvent event : events) {
			applyEventHeuristics(event);
			applyQueryHeuristics(event, userQuery);
			applyInterestHeuristics(event, userModel.getInterests());
			applyHistoryHeuristics(event, userModel.getHistory());
		}
		
	}
}
