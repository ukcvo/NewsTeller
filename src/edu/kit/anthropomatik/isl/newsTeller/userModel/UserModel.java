package edu.kit.anthropomatik.isl.newsTeller.userModel;

import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.ConversationCycle;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;

/**
 * Encapsulates the user model: interests (keywords the user is interested in) and the conversation history.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class UserModel {

	public abstract List<Keyword> getInterests();
	
	public abstract List<ConversationCycle> getHistory();
}
