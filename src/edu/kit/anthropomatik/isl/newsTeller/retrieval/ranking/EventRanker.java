package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.aggregating.IScoreAggregator;

/**
 * Takes care of ranking the events according to their total relevance score.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class EventRanker {

	private static Log log = LogFactory.getLog(EventRanker.class);
	
	private IScoreAggregator scoreAggregator;
	
	public void setScoreAggregator(IScoreAggregator scoreAggregator) {
		this.scoreAggregator = scoreAggregator;
	}
	
	public List<NewsEvent> getRankedEvents(Set<NewsEvent> events) {
		if (log.isTraceEnabled())
			log.trace(String.format("getRankedEvents(events = <%s>)", StringUtils.collectionToCommaDelimitedString(events)));
		
		ArrayList<NewsEvent> result = new ArrayList<NewsEvent>();
		
		for (NewsEvent event : events) {
			event.setTotalRelevanceScore(scoreAggregator.getTotalScore(event.getRelevanceScorings()));
			result.add(event);
		}
		
		Collections.sort(result);
		
		return result;
	}
}
