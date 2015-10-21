package edu.kit.anthropomatik.isl.newsTeller.data;

import java.net.URI;
import java.util.List;

/**
 * Represents one cycle in the conversation (user query - event selected - system response).
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class ConversationCycle {

	private List<Keyword> userQuery;
	
	private URI event;
	
	private String systemResponse;

	public List<Keyword> getUserQuery() {
		return userQuery;
	}

	public void setUserQuery(List<Keyword> userQuery) {
		this.userQuery = userQuery;
	}

	public URI getEvent() {
		return event;
	}

	public void setEvent(URI event) {
		this.event = event;
	}

	public String getSystemResponse() {
		return systemResponse;
	}

	public void setSystemResponse(String systemResponse) {
		this.systemResponse = systemResponse;
	}
}
