package edu.kit.anthropomatik.isl.newsTeller.retrieval.aggregating;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.Scoring;

/**
 * Aggregates event scores by taking a simple average.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SimpleAverageAggregator extends ScoreAggregator implements IScoreAggregator{

	private static Log log = LogFactory.getLog(SimpleAverageAggregator.class);
	
	@Override
	protected void aggregateScoresForEvent(NewsEvent event) {
		
		double relevanceSum = 0;
		for (Scoring scoring : event.getRelevanceScorings())
			relevanceSum += scoring.getScore();
		double totalRelevanceScore = relevanceSum / event.getRelevanceScorings().size();
		
		event.setTotalRelevanceScore(totalRelevanceScore);
	}

	public double getTotalScore(List<Scoring> scorings) {
		if (log.isTraceEnabled())
			log.trace(String.format("getTotalScore(scorings = <%s>)", StringUtils.collectionToCommaDelimitedString(scorings)));
			
		double sum = 0;
		for (Scoring scoring : scorings)
			sum += scoring.getScore();
		double totalScore = sum / scorings.size();
		
		if (log.isTraceEnabled())
			log.trace(String.format("total score: %f", totalScore));
		
		return totalScore;
	}

}
