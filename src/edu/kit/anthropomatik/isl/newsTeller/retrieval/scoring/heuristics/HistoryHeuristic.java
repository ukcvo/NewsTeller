package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics;

import edu.kit.anthropomatik.isl.newsTeller.data.ConversationCycle;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Heuristic based on event and one previous conversation cycle.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class HistoryHeuristic extends ScoringHeuristic {

	public double getScore(NewsEvent event, ConversationCycle cycle) {
		return getScore(event, null, cycle);
	}
}
