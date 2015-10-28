package edu.kit.anthropomatik.isl.newsTeller.retrieval.finding;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.ConversationCycle;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Responsible for finding many potentially relevant events.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class EventFinder {

	private static Log log = LogFactory.getLog(EventFinder.class);
	
	// access to KnowledgeStore
	private KnowledgeStoreAdapter ksAdapter;
	
	private List<String> userQuerySPARQLTemplates;		// SPARQL queries based on user query keyword
	
	@SuppressWarnings("unused")
	private List<String> userInterestSPARQLTemplates;	// SPARQL queries based on user interests keyword
	
	@SuppressWarnings("unused")
	private List<String> previousEventSPARQLTemplates;	// SPARQL queries based on conversation history event
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public EventFinder(String userQueryFolder, String userInterestFolder, String previousEventFolder) {
		this.userQuerySPARQLTemplates = Util.readStringsFromFolder(userQueryFolder);
		this.userInterestSPARQLTemplates = Util.readStringsFromFolder(userInterestFolder);
		this.previousEventSPARQLTemplates = Util.readStringsFromFolder(previousEventFolder);
	}
	
	// use keywords from user query to find events
	private List<NewsEvent> processUserQuery(List<Keyword> userQuery) {
		if (log.isTraceEnabled())
			log.trace(String.format("processUserQuery(userQuery = <%s>)", StringUtils.collectionToCommaDelimitedString(userQuery)));
		
		List<NewsEvent> events = new ArrayList<NewsEvent>();
		
		for (String sparqlQuery : userQuerySPARQLTemplates) {
			// TODO: generalize to multiple keywords (Scope 3)
			events.addAll(ksAdapter.runSingleVariableEventQuery(sparqlQuery.replace("*k*", userQuery.get(0).getWord()), "event"));
		}
		return events;
	}
	
	// use keywords from user interests to find events
	private List<NewsEvent> processUserInterests(List<Keyword> userInterests) {
		if (log.isTraceEnabled())
			log.trace(String.format("processUserInterests(userInterests = <%s>)", StringUtils.collectionToCommaDelimitedString(userInterests)));
		// TODO: implement (Scope 4)
		return new ArrayList<NewsEvent>();
	}
	
	// use events from previous conversation cycles to find events
	private List<NewsEvent> processConversationHistory(List<ConversationCycle> converstaionHistory) {
		if (log.isTraceEnabled())
			log.trace(String.format("processConversationHistory(converstaionHistory = <%s>)", 
					StringUtils.collectionToCommaDelimitedString(converstaionHistory)));
		// TODO: implement (Scope 7)
		return new ArrayList<NewsEvent>();
	}
	
	/**
	 * Find potentially relevant events.
	 */
	public List<NewsEvent> findEvents(List<Keyword> userQuery, UserModel userModel) {
		if (log.isInfoEnabled())
			log.info(String.format("findEvents(userQuery = <%s>, userModel = %s)", 
					StringUtils.collectionToCommaDelimitedString(userQuery), userModel.toString()));
		
		List<NewsEvent> events = new ArrayList<NewsEvent>();
		
		events.addAll(processUserQuery(userQuery));
		events.addAll(processUserInterests(userModel.getInterests()));
		events.addAll(processConversationHistory(userModel.getHistory()));
		
		return events;
	}
	
}
