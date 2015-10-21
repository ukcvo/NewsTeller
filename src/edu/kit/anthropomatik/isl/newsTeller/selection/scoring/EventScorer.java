package edu.kit.anthropomatik.isl.newsTeller.selection.scoring;

import java.net.URI;
import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

/**
 * Scores an event based on certain criteria.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class EventScorer {

	// access to KnowledgeStore if necessary
	protected KnowledgeStoreAdapter ksAdapter;
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public abstract double scoreEvent(URI event, List<Keyword> userQuery, UserModel userModel);
}
