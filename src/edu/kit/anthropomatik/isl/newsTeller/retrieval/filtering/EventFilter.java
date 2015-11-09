package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.aggregating.IScoreAggregator;

/**
 * Filters events based on their usability score and a threshold.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class EventFilter {

	private static Log log = LogFactory.getLog(EventFilter.class);
	
	private double threshold;
	
	private IScoreAggregator scoreAggregator;
	
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public void setScoreAggregator(IScoreAggregator scoreAggregator) {
		this.scoreAggregator = scoreAggregator;
	}

	/**
	 * Filter the given set of events based on the aggregated scores.
	 */
	public Set<NewsEvent> filterEvents(Set<NewsEvent> events) {
		if (log.isTraceEnabled())
			log.trace(String.format("filterEvents(events = <%s>)", StringUtils.collectionToCommaDelimitedString(events)));
			
		Set<NewsEvent> result = new HashSet<NewsEvent>();
		
		for (NewsEvent event : events) {
			double totalUsabilityScore = scoreAggregator.getTotalScore(event.getUsabilityScorings());
			event.setTotalUsabilityScore(totalUsabilityScore);
			if (totalUsabilityScore >= this.threshold)
				result.add(event);
		}
		
		if(log.isDebugEnabled())
			log.debug(String.format("keeping %d out of %d events", result.size(), events.size()));
		
		if(log.isTraceEnabled())
			log.trace(String.format("events kept: <%s>", StringUtils.collectionToCommaDelimitedString(result)));
		
		return result;
	}
}
