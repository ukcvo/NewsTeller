package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Represents a feature based on an AVG query (returns a double).
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SimpleAverageFeature extends UsabilityFeature {

	private static Log log = LogFactory.getLog(SimpleAverageFeature.class);
	
	private Set<ValueBin> bins;
	
	public void setBins(Set<ValueBin> bins) {
		this.bins = bins;
	}
	
	public SimpleAverageFeature(String queryFileName, String probabilityFileName) {
		super(queryFileName, probabilityFileName);
	}
	
	@Override
	public int getValue(String eventURI) {
		double result = this.ksAdapter.runSingleVariableDoubleQuerySingleResult(
				sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_NUMBER);
		
		for (ValueBin bin : bins) {
			if (bin.contains(result)) {
				return bin.getLabel();
			}
		}
		if (Double.isNaN(result))
			return -1;
		
		if (log.isErrorEnabled())
			log.error(String.format("result does not fit any bin: %f '%s'", result, eventURI));
		return -2;
	}

}
