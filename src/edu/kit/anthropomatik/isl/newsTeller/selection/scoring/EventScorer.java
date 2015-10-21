package edu.kit.anthropomatik.isl.newsTeller.selection.scoring;

import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;

/**
 * Scores an event based on certain criteria.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class EventScorer {

	// access to KnowledgeStore if necessary
	protected KnowledgeStoreAdapter ksAdapter;
	
	//TODO: public abstract double scoreEvent(Event e);
}
