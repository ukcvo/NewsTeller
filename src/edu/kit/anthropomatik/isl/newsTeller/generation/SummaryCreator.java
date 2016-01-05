package edu.kit.anthropomatik.isl.newsTeller.generation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Takes care of mapping the abstractly represented event to an output string.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class SummaryCreator {

	private static Log log = LogFactory.getLog(SummaryCreator.class);
	
	// access to KnowledgeStore
	protected KnowledgeStoreAdapter ksAdapter;

	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	} 
	
	// 
	protected abstract String createSummary(NewsEvent event);
	
	/**
	 * Summarize the given event.
	 */
	public String summarizeEvent(NewsEvent event) {
		
		String result;
		
		if (event == null) {
			if (log.isWarnEnabled())
				log.warn("no event picked for summary generation, returning placeholder string.");
			result = Util.EMPTY_EVENT_RESPONSE;
		} else {
			if (log.isTraceEnabled())
				log.trace(String.format("summarizeEvent(event = %s)", event.toVerboseString()));
			
			result = createSummary(event);
			if (result.isEmpty())
				result = Util.EMPTY_EVENT_RESPONSE;
		}
		
		return result;
	}
}
