package edu.kit.anthropomatik.isl.newsTeller.userModel;

import java.util.ArrayList;
import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.ConversationCycle;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;

/**
 * Dummy implementation - only returns empty lists. Can be used when testing the system w/o user model.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class DummyUserModel extends UserModel {

	@Override
	public List<Keyword> getInterests() {
		return new ArrayList<Keyword>();
	}

	@Override
	public List<ConversationCycle> getHistory() {
		return new ArrayList<ConversationCycle>();
	}

}
