package edu.kit.anthropomatik.isl.newsTeller.retrieval.search;

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

		Set<NewsEvent> events = new HashSet<NewsEvent>();
		
		for (Keyword keyword : userQuery) {
			Util.stemKeyword(keyword);
			String keywordRegex = keyword.getStemmedRegex();
			String bifContainsString = keyword.getBifContainsString();
			events.addAll(ksAdapter.runSingleVariableEventQuery(
					standardSparqlQuery.replace(Util.PLACEHOLDER_BIF_CONTAINS, bifContainsString).replace(Util.PLACEHOLDER_KEYWORD, keywordRegex), 
					Util.VARIABLE_EVENT, 5000L));
			if (events.isEmpty())
				events.addAll(ksAdapter.runSingleVariableEventQuery(
						fallbackSparqlQuery.replace(Util.PLACEHOLDER_KEYWORD, keywordRegex), Util.VARIABLE_EVENT, 5000L));
		}
		
		if (events.size() > maxNumberOfEvents) {
			// throw away some of the events to make further processing faster
			Set<NewsEvent> filteredEvents = new HashSet<NewsEvent>();
			for (NewsEvent e : events) {
				filteredEvents.add(e);
				if (filteredEvents.size() == maxNumberOfEvents)
					break;
			}
			if (log.isDebugEnabled())
				log.debug(String.format("found %d events, selected first %d for further processing", events.size(), filteredEvents.size()));
			events = filteredEvents;
		}
		
		return events;
	}	
}
