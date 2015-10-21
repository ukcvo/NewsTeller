package edu.kit.anthropomatik.isl.newsTeller.generation;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Dummy class, always returns empty string (only a placeholder, should not be used in actual system!)
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class DummySummaryCreator extends SummaryCreator {

	static Log log = LogFactory.getLog(SummaryCreator.class);
	
	@Override
	public String summarizeEvent(URI event) {
		if (log.isTraceEnabled())
			log.trace("dummy summarization");;
		
		return "dummySummary";
	}

}
