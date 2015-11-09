package edu.kit.anthropomatik.isl.newsTeller.retrieval.aggregating;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.Scoring;

/**
 * Returns zero. Only for intial testing purposes.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class DummyAggregator implements IScoreAggregator{

	private static Log log = LogFactory.getLog(DummyAggregator.class);
	
	public double getTotalScore(List<Scoring> scorings) {
		if (log.isTraceEnabled())
			log.trace(String.format("getTotalScore(scorings = <%s>)", StringUtils.collectionToCommaDelimitedString(scorings)));
		
		return 0;
	}

}
