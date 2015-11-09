package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.Scoring;

/**
 * EventScorer for event relevance.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class RelevanceScorer extends EventScorer {

	@Override
	protected void addScoringToEvent(NewsEvent event, Scoring scoring) {
		event.addRelevanceScoring(scoring);
	}

}
