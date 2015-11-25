package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

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
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
			
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public UsabilityFeature(String queryFileName) {
		this.sparqlQuery = Util.readStringFromFile(queryFileName);
	}
	
	public abstract double getValue(String eventURI);
}
