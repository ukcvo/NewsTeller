package edu.kit.anthropomatik.isl.newsTeller.userModel;

import java.util.List;

import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.ConversationCycle;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Encapsulates the user model: interests (keywords the user is interested in) and the conversation history.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class UserModel {

	public abstract List<Keyword> getInterests();
	
	public abstract List<ConversationCycle> getHistory();
	
	public abstract boolean historyContainsEvent(NewsEvent event);
	
	public abstract void addCycleToHistory(ConversationCycle cycle);
	
	@Override
	public String toString() {
		return String.format("[UM: interests = <%s>, history = <%s>]", 
								StringUtils.collectionToCommaDelimitedString(getInterests()), 
								StringUtils.collectionToCommaDelimitedString(getHistory()));
	}
}
