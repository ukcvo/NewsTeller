package edu.kit.anthropomatik.isl.newsTeller.generation;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;

/**
 * Produces an output by picking an arbitrary sentence in which the event is mentioned.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SentencePicker extends SummaryCreator {

	private KnowledgeStoreAdapter ksAdapter;
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	@Override
	public String createSummary(NewsEvent event) {
		
		String result = ksAdapter.retrieveSentencefromEvent(event.getEventURI());
		return result;
	}

}
