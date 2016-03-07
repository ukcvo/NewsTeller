package edu.kit.anthropomatik.isl.newsTeller.userModel;

import java.util.ArrayList;
import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.ConversationCycle;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

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
	
	@Override
	public List<Keyword> getInterests() {
		return this.interests;
	}

	@Override
	public List<ConversationCycle> getHistory() {
		return this.history;
	}

	@Override
	public void addCycleToHistory(ConversationCycle cycle) {
		this.history.add(cycle);
	}
	
	@Override
	public boolean historyContainsEvent(NewsEvent event) {
		
		for (ConversationCycle cycle : this.history) {
			if (cycle.getEventURI().equals(event.getEventURI()))
				return true;
		}
		
		return false;
	}

}
