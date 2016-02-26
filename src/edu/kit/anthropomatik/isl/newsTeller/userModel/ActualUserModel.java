package edu.kit.anthropomatik.isl.newsTeller.userModel;

import java.util.ArrayList;
import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.ConversationCycle;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;

/**
 * Implementation of an actual user model.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class ActualUserModel extends UserModel {

	private List<Keyword> interests;
	
	private List<ConversationCycle> history;
	
	public ActualUserModel(List<Keyword> interests) {
		this.interests = interests;
		this.history = new ArrayList<ConversationCycle>();
	}
	
	public void addCycleToHistory(ConversationCycle cycle) {
		this.history.add(cycle);
	}
	
	@Override
	public List<Keyword> getInterests() {
		return this.interests;
	}

	@Override
	public List<ConversationCycle> getHistory() {
		return this.history;
	}

}
