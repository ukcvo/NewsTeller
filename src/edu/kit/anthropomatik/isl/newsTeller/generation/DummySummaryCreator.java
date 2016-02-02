package edu.kit.anthropomatik.isl.newsTeller.generation;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Dummy class, always returns fixed string (only a placeholder, should not be used in actual system!)
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class DummySummaryCreator extends SummaryCreator {

	private static Log log = LogFactory.getLog(SummaryCreator.class);
	
	@Override
	public String createSummary(NewsEvent event, List<Keyword> keywords) {
		if (log.isTraceEnabled()) 
			log.trace(String.format("summarizeEvent(URI = '%s')", (event == null) ? "null" : event.getEventURI()));
		
		return "dummySummary";
	}
}
