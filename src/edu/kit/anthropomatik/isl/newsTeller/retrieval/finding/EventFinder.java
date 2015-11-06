package edu.kit.anthropomatik.isl.newsTeller.retrieval.finding;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

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

	private List<String> userQuerySPARQLTemplates; // SPARQL queries based on user query keyword

	@SuppressWarnings("unused")
	private List<String> userInterestSPARQLTemplates; // SPARQL queries based on user interests keyword

	@SuppressWarnings("unused")
	private List<String> previousEventSPARQLTemplates; // SPARQL queries based on conversation history event

	private SnowballStemmer stemmer;
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}

	public EventFinder(String userQueryConfigFileName, String userInterestConfigFileName, String previousEventConfigFileName) {
		this.userQuerySPARQLTemplates = Util.readQueriesFromConfigFile(userQueryConfigFileName);
		this.userInterestSPARQLTemplates = Util.readQueriesFromConfigFile(userInterestConfigFileName);
		this.previousEventSPARQLTemplates = Util.readQueriesFromConfigFile(previousEventConfigFileName);
		this.stemmer = new englishStemmer();
	}

	// stem the keyword before applying the query
	private String stemKeyword(String keyword) {
		stemmer.setCurrent(keyword);
		stemmer.stem();
		String result = stemmer.getCurrent();
		if (result.endsWith("i"))
			result = result.substring(0, result.length()-1) + "(i|y)";
		return result;
	}
	
	// use keywords from user query to find events
	private Set<NewsEvent> processUserQuery(List<Keyword> userQuery) {
		if (log.isTraceEnabled())
			log.trace(String.format("processUserQuery(userQuery = <%s>)", StringUtils.collectionToCommaDelimitedString(userQuery)));

		Set<NewsEvent> events = new HashSet<NewsEvent>();

		for (String sparqlQuery : userQuerySPARQLTemplates) {
			// TODO: generalize to multiple keywords (Scope 3)
			String keywordStem = userQuery.get(0).getWord();
			keywordStem = stemKeyword(keywordStem);
			events.addAll(ksAdapter.runSingleVariableEventQuery(sparqlQuery.replace("*k*", keywordStem), "event"));
		}
		return events;
	}

	// use keywords from user interests to find events
	private Set<NewsEvent> processUserInterests(List<Keyword> userInterests) {
		if (log.isTraceEnabled())
			log.trace(String.format("processUserInterests(userInterests = <%s>)", StringUtils.collectionToCommaDelimitedString(userInterests)));
		// TODO: implement (Scope 4)
		return new HashSet<NewsEvent>();
	}

	// use events from previous conversation cycles to find events
	private Set<NewsEvent> processConversationHistory(List<ConversationCycle> conversationHistory) {
		if (log.isTraceEnabled())
			log.trace(String.format("processConversationHistory(conversationHistory = <%s>)", StringUtils.collectionToCommaDelimitedString(conversationHistory)));
		// TODO: implement (Scope 7)
		return new HashSet<NewsEvent>();
	}

	/**
	 * Find potentially relevant events.
	 */
	public Set<NewsEvent> findEvents(List<Keyword> userQuery, UserModel userModel) {
		if (log.isTraceEnabled())
			log.trace(String.format("findEvents(userQuery = <%s>, userModel = %s)", StringUtils.collectionToCommaDelimitedString(userQuery), userModel.toString()));

		Set<NewsEvent> events = new HashSet<NewsEvent>();

		this.ksAdapter.openConnection();
		
		if (userQuery != null && !userQuery.isEmpty()) //TODO: temporary fix, remove in Scope 3
			events.addAll(processUserQuery(userQuery));
		events.addAll(processUserInterests(userModel.getInterests()));
		events.addAll(processConversationHistory(userModel.getHistory()));

		this.ksAdapter.closeConnection();
		
		return events;
	}

}
