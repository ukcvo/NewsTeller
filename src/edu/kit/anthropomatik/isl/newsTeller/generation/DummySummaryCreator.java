package edu.kit.anthropomatik.isl.newsTeller.generation;

import java.net.URI;

/**
 * Dummy class, always returns empty string (only a placeholder, should not be used in actual system!)
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class DummySummaryCreator extends SummaryCreator {

	@Override
	public String summarizeEvent(URI event) {
		return "dummySummary";
	}

}
