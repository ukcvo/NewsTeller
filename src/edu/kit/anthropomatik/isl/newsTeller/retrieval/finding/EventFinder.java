package edu.kit.anthropomatik.isl.newsTeller.retrieval.finding;

import java.util.ArrayList;
import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.ConversationCycle;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Represents one way to find events.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class EventFinder {

	// access to KnowledgeStore
	private KnowledgeStoreAdapter ksAdapter;
	
	private List<String> queryQueries;		// SPARQL queries based on user query keyword
	
	@SuppressWarnings("unused")
	private List<String> interestQueries;	// SPARQL queries based on user interests keyword
	
	@SuppressWarnings("unused")
	private List<String> historyQueries;	// SPARQL queries based on conversation history event
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public EventFinder(String queryQueryFolder, String interestQueryFolder, String historyQueryFolder) {
		this.queryQueries = Util.readQueriesFromFolder(queryQueryFolder);
		this.interestQueries = Util.readQueriesFromFolder(interestQueryFolder);
		this.historyQueries = Util.readQueriesFromFolder(historyQueryFolder);
	}
	
	private List<NewsEvent> processUserQuery(List<Keyword> userQuery) {
		
		List<NewsEvent> events = new ArrayList<NewsEvent>();
		
		for (String sparqlQuery : queryQueries) {
			// TODO: generalize to multiple keywords (Scope 3)
			events.addAll(ksAdapter.runSingleVariableEventQuery(sparqlQuery.replace("*k*", userQuery.get(0).getWord()), "event"));
		}
		return events;
	}
	
	private List<NewsEvent> processUserInterests(List<Keyword> userInterests) {
		// TODO: implement (Scope 4)
		return new ArrayList<NewsEvent>();
	}
	
	private List<NewsEvent> processConversationHistory(List<ConversationCycle> converstaionHistory) {
		// TODO: implement (Scope 7)
		return new ArrayList<NewsEvent>();
	}
	
	public List<NewsEvent> findEvents(List<Keyword> userQuery, UserModel userModel) {
		List<NewsEvent> events = new ArrayList<NewsEvent>();
		
		events.addAll(processUserQuery(userQuery));
		events.addAll(processUserInterests(userModel.getInterests()));
		events.addAll(processConversationHistory(userModel.getHistory()));
		
		return events;
	}
	
}
