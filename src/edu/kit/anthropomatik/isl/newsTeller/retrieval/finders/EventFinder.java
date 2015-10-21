package edu.kit.anthropomatik.isl.newsTeller.retrieval.finders;

import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;

/**
 * Represents one way to find events.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class EventFinder {

	// access to KnowledgeStore
	protected KnowledgeStoreAdapter ksAdapter;
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	//TODO: findEvents() once we know what events can be represented as
}
