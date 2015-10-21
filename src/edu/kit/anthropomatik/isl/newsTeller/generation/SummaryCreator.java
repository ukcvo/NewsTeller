package edu.kit.anthropomatik.isl.newsTeller.generation;

import java.net.URI;

import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;

/**
 * Takes care of mapping the abstractly represented event to an output string.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class SummaryCreator {

	// access to KnowledgeStore
	protected KnowledgeStoreAdapter ksAdapter;

	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	} 
	
	// summarize the given event
	public abstract String summarizeEvent(URI event);
}
