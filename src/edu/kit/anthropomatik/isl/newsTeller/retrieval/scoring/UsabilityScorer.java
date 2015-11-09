package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.Scoring;

/**
 * EventScorer for usability of event (i.e. collecting well-formedness scores).
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class UsabilityScorer extends EventScorer {

	@Override
	protected void addScoringToEvent(NewsEvent event, Scoring scoring) {
		event.addUsabilityScoring(scoring);
	}

}
