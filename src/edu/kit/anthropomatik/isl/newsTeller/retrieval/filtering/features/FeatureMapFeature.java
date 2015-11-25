package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.HashMap;
import java.util.Map;

import edu.kit.anthropomatik.isl.newsTeller.data.BenchmarkEvent;

/**
 * Dummy implementation: looks up feature values from given feature map. Used for benchmark testing purposes only (to speed up computation).
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class FeatureMapFeature extends UsabilityFeature {

	private Map<String, Integer> featureMap;
	
	public FeatureMapFeature(Map<BenchmarkEvent, Map<String, Integer>> featureMap, String currentFile, String featureName) {
		super("", "");
		this.featureMap = new HashMap<String, Integer>();
		this.setName(featureName);
		for (BenchmarkEvent event : featureMap.keySet()) {
			if (event.getFileName().equals(currentFile)) {
				this.featureMap.put(event.getEventURI(), featureMap.get(event).get(featureName));
			}
		}
	}

	@Override
	public int getValue(String eventURI) {
		return featureMap.get(eventURI);
	}

}
