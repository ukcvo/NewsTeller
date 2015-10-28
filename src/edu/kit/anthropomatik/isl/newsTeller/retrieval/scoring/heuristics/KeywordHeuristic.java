package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Heuristic based on the event and one keyword.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class KeywordHeuristic extends ScoringHeuristic {

	public double getScore(NewsEvent event, Keyword keyword) {
		return getScore(event, keyword, null);
	}
}
