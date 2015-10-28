package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Heuristic based only on the event.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class EventHeuristic extends ScoringHeuristic {
	
	public double getScore(NewsEvent event) {
		return getScore(event, null, null);
	}
}
