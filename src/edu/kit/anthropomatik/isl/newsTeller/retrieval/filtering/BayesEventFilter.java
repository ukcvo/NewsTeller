package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;

/**
 * Filters events based on the posterior probability estimated by naive bayes and a threshold.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class BayesEventFilter implements IEventFilter {

	private static Log log = LogFactory.getLog(BayesEventFilter.class);
	
	private double threshold;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	private NaiveBayesFusion bayes;
	
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public void setBayes(NaiveBayesFusion bayes) {
		this.bayes = bayes;
	}
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	/**
	 * Filter the given set of events based on the aggregated scores.
	 */
	public Set<NewsEvent> filterEvents(Set<NewsEvent> events) {
		if (log.isTraceEnabled())
			log.trace(String.format("filterEvents(events = <%s>)", StringUtils.collectionToCommaDelimitedString(events)));
			
		Set<NewsEvent> result = new HashSet<NewsEvent>();
		
		ksAdapter.openConnection();
		for (NewsEvent event : events) {
			double probability = bayes.getProbabilityOfEvent(event);
			if (probability >= threshold)
				result.add(event);
		}
		ksAdapter.closeConnection();
		
		if(log.isDebugEnabled())
			log.debug(String.format("keeping %d out of %d events", result.size(), events.size()));
		
		if(log.isTraceEnabled())
			log.trace(String.format("events kept: <%s>", StringUtils.collectionToCommaDelimitedString(result)));
		
		return result;
	}
}
