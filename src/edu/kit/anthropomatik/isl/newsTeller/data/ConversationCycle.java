package edu.kit.anthropomatik.isl.newsTeller.data;

import java.util.List;

/**
 * Represents one cycle in the conversation (user query - event selected - system response).
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class ConversationCycle {

	private List<Keyword> userQuery;
	
	private String eventURL; //TODO fix dataType once we know how to represent events
	
	private String systemResponse;

	public List<Keyword> getUserQuery() {
		return userQuery;
	}

	public void setUserQuery(List<Keyword> userQuery) {
		this.userQuery = userQuery;
	}

	public String getEventURL() {
		return eventURL;
	}

	public void setEventURL(String eventURL) {
		this.eventURL = eventURL;
	}

	public String getSystemResponse() {
		return systemResponse;
	}

	public void setSystemResponse(String systemResponse) {
		this.systemResponse = systemResponse;
	}
}
