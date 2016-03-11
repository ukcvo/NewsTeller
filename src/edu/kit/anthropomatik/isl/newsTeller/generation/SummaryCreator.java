package edu.kit.anthropomatik.isl.newsTeller.generation;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.data.ConversationCycle;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
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
	protected abstract String createSummary(NewsEvent event, List<Keyword> keywords);
	
	/**
	 * Summarize the given event.
	 */
	public String summarizeEvent(NewsEvent event, List<Keyword> keywords, UserModel userModel) {
		
		String result;
		
		if (event == null) {
			if (log.isWarnEnabled())
				log.warn("no event picked for summary generation, returning placeholder string.");
			result = Util.EMPTY_EVENT_RESPONSE;
		} else {
			if (log.isTraceEnabled())
				log.trace(String.format("summarizeEvent(event = %s)", event.toVerboseString()));
			
			result = createSummary(event, keywords);
			if (result.isEmpty())
				result = Util.EMPTY_EVENT_RESPONSE;
		}
		
		ConversationCycle cycle = new ConversationCycle(keywords, (event == null) ? "" : event.getEventURI(), result);
		userModel.addCycleToHistory(cycle);
		
		return result;
	}
}
