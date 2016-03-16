package edu.kit.anthropomatik.isl.newsTeller.retrieval.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
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
public class EventSearcher {

	private static Log log = LogFactory.getLog(EventSearcher.class);

	// access to KnowledgeStore
	private KnowledgeStoreAdapter ksAdapter;

	private String standardSparqlQuery;
	
	private String fallbackSparqlQuery;
	
	private int maxNumberOfEvents = 100;
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public void setMaxNumberOfEvents(int maxNumberOfEvents) {
		this.maxNumberOfEvents = maxNumberOfEvents;
	}
	
	public EventSearcher(String standardSparqlQueryFileName, String fallbackSparqlQueryFileName) {
		this.standardSparqlQuery = Util.readStringFromFile(standardSparqlQueryFileName);
		this.fallbackSparqlQuery = Util.readStringFromFile(fallbackSparqlQueryFileName);
	}

	/**
	 * Find potentially relevant events.
	 */
	public Set<NewsEvent> findEvents(List<Keyword> userQuery, UserModel userModel) {
		if (log.isTraceEnabled())
			log.trace(String.format("findEvents(userQuery = <%s>, userModel = %s)", StringUtils.collectionToCommaDelimitedString(userQuery), userModel.toString()));

		Set<NewsEvent> result = new HashSet<NewsEvent>();
		List<NewsEvent> events = new ArrayList<NewsEvent>();
		for (Keyword k : userQuery)
			Util.stemKeyword(k);
		
		// create the replacement strings
		StringBuilder bifString = new StringBuilder();
		StringBuilder regexString = new StringBuilder();
		regexString.append(Util.KEYWORD_REGEX_PREFIX);
		regexString.append("(");
		for (int i = 0; i < userQuery.size(); i++) {
			Keyword k = userQuery.get(i);
			if (i > 0) {
				bifString.append(" or ");
				regexString.append("|");
			}
			bifString.append("(");
			bifString.append(k.getBifContainsString());
			bifString.append(")");
			String regex = k.getStemmedRegex().replace(Util.KEYWORD_REGEX_PREFIX, "").replace(Util.KEYWORD_REGEX_SUFFIX, "");
			regexString.append("(");
			regexString.append(regex);
			regexString.append(")");
		}
		regexString.append(")");
		regexString.append(Util.KEYWORD_REGEX_SUFFIX);
		
		// try with standard query first, if this doesn't work: use fallback query
		events.addAll(ksAdapter.runSingleVariableEventQuery(
				standardSparqlQuery.replace(Util.PLACEHOLDER_BIF_CONTAINS, bifString.toString()).replace(Util.PLACEHOLDER_KEYWORD, regexString.toString()), 
				Util.VARIABLE_EVENT));
		if (events.isEmpty())
			events.addAll(ksAdapter.runSingleVariableEventQuery(
				fallbackSparqlQuery.replace(Util.PLACEHOLDER_BIF_CONTAINS, bifString.toString()).replace(Util.PLACEHOLDER_KEYWORD, regexString.toString()), 
				Util.VARIABLE_EVENT));
		
		// throw away some of the events to make further processing faster - if necessary
		Collections.shuffle(events);
		int numberOfEventsToKeep = Math.min(events.size(), maxNumberOfEvents);
		if (log.isDebugEnabled())
			log.debug(String.format("found %d events, selecting randomly %d for further processing", events.size(), numberOfEventsToKeep));
		result = new HashSet<NewsEvent>(events.subList(0, numberOfEventsToKeep));
		
		return result;
	}	
}
