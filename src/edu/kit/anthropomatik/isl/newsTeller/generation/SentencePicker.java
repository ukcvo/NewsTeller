package edu.kit.anthropomatik.isl.newsTeller.generation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;

/**
 * Produces an output by picking an arbitrary sentence in which the event is mentioned.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SentencePicker extends SummaryCreator {

	private static Log log = LogFactory.getLog(SentencePicker.class);
	
	public static final String EMPTY_EVENT_RESPONSE = "I'm sorry, but there's nothing I can tell you about this topic.";
	
	private KnowledgeStoreAdapter ksAdapter;
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	@Override
	public String summarizeEvent(NewsEvent event) {
		
		String result;
		
		if (event == null) {
			if (log.isWarnEnabled())
				log.warn("no event picked for summary generation, returning placeholder string.");
			result = EMPTY_EVENT_RESPONSE;
		} else {
			if (log.isTraceEnabled())
				log.trace(String.format("summarizeEvent(event = %s)", event.toVerboseString()));
			
			result = ksAdapter.retrieveSentencefromEvent(event.getEventURI());
			if (result.isEmpty())
				result = EMPTY_EVENT_RESPONSE;
		}
		
		return result;
	}

}
