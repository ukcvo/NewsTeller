package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Gets a coefficient by executing a SPARQL count query.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SPARQLCoefficientDeterminer extends CoefficientDeterminer {

	private static Log log = LogFactory.getLog(SPARQLCoefficientDeterminer.class);
	
	public SPARQLCoefficientDeterminer(String queryFileName) {
		super(queryFileName);
	}
	
	public double getCoefficient(String eventURI, String keyword, String historicalEventURI) {
		
		List<String> retrievalResults = executeQuery(eventURI, keyword, historicalEventURI, Util.VARIABLE_NUMBER);
		
		double number = 0.0;
		if (retrievalResults == null && log.isWarnEnabled())
			log.warn("executeQuery returned null; returning zero");
		else if (retrievalResults.size() == 0 && log.isErrorEnabled())
			log.error("executeQuery returned empty result; returning zero");
		else
			number = Util.parseXMLDouble(retrievalResults.get(0));
		return number;
	}

}
