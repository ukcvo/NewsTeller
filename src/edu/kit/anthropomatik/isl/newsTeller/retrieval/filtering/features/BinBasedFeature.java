package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides the common binning functionality.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class BinBasedFeature extends UsabilityFeature {

	private static Log log = LogFactory.getLog(BinBasedFeature.class);
	
	protected Set<ValueBin> bins;

	public BinBasedFeature(String queryFileName, String probabilityFileName) {
		super(queryFileName, probabilityFileName);
	}

	public void setBins(Set<ValueBin> bins) {
		this.bins = bins;
	}

	/**
	 * Get the raw feature value which will then be binned.
	 */
	protected abstract double getRawValue(String eventURI);
	
	
	@Override
	public int getValue(String eventURI) {
		double result = getRawValue(eventURI);
		
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