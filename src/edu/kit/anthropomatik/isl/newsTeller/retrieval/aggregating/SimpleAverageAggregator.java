package edu.kit.anthropomatik.isl.newsTeller.retrieval.aggregating;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.Scoring;

/**
 * Aggregates event scores by taking a simple average.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SimpleAverageAggregator implements IScoreAggregator{

	private static Log log = LogFactory.getLog(SimpleAverageAggregator.class);
	
	public double getTotalScore(List<Scoring> scorings) {
		if (log.isTraceEnabled())
			log.trace(String.format("getTotalScore(scorings = <%s>)", StringUtils.collectionToCommaDelimitedString(scorings)));
		
		double totalScore = 0;
		
		if (!scorings.isEmpty()) {
			double sum = 0;
			for (Scoring scoring : scorings)
				sum += scoring.getScore();
			totalScore = sum / scorings.size();
		}
		
		if (log.isTraceEnabled())
			log.trace(String.format("total score: %f", totalScore));
		
		return totalScore;
	}

}
