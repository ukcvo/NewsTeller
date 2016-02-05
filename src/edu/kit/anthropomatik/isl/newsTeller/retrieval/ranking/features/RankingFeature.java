package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

/**
 * A features used for relevance ranking.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class RankingFeature {

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
	
	/**
	 * Get the feature value for the given event; may also make use of the given keyword list and/or the given user model (note: not all features do so!).
	 */
	public abstract double getValue(String eventURI, List<Keyword> keywords, UserModel userModel);
	
}
