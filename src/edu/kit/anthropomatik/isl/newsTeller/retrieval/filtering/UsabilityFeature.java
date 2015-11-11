package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.HashMap;
import java.util.Map;

import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Represents one feature used for usability classification.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class UsabilityFeature {

	private String name;
	
	protected String sparqlQuery;
	
	protected KnowledgeStoreAdapter ksAdapter;
	
	private Map<Integer, Double> logProbabilities;
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
			
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public UsabilityFeature(String queryFileName, String probabilityFileName) {
		this.sparqlQuery = Util.readStringFromFile(queryFileName);
		this.logProbabilities = new HashMap<Integer, Double>(); //TODO: change
	}
	
	public abstract int getValue(String eventURI);
	
	public double getLogProbability(int value) {
		return logProbabilities.get(value);
	}
}
