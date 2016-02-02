package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;

/**
 * Represents one feature used for usability classification.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class UsabilityFeature {

	private String name;
	
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
	
	public UsabilityFeature() {
	}
	
	/**
	 * Get the feature value for the given event; may also make use of the given keyword list (note: not all features do so!).
	 */
	public abstract double getValue(String eventURI, List<Keyword> keywords);
	
	/**
	 * Returns the mention properties being used by this feature for central bulk aggregation.
	 */
	public abstract Set<String> getRequiredMentionProperties();
}
