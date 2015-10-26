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

	private static Log log = LogFactory.getLog(SummaryCreator.class);
	
	@Override
	public String summarizeEvent(URI event) {
		if (log.isInfoEnabled()) {
			if (event == null)
				log.info("summarizeEvent(URI = null)");
			else
				log.info(String.format("summarizeEvent(URI = <%s>)", event.toString()));
		}
		
		return "dummySummary";
	}

}
