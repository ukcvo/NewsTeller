package edu.kit.anthropomatik.isl.newsTeller.retrieval.finders;

import java.net.URI;
import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

/**
 * Represents one way to find events.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class EventFinder {

	// access to KnowledgeStore
	protected KnowledgeStoreAdapter ksAdapter;
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public abstract List<URI> findEvents(List<Keyword> userQuery, UserModel userModel);
	
}
