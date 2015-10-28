package edu.kit.anthropomatik.isl.newsTeller.data;

import java.net.URI;
import java.util.List;

import org.springframework.util.StringUtils;

/**
 * Represents one cycle in the conversation (user query - event selected - system response).
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class ConversationCycle {

	private List<Keyword> userQuery;
	
	private String eventURI;
	
	private String systemResponse;

	public List<Keyword> getUserQuery() {
		return userQuery;
	}

	public void setUserQuery(List<Keyword> userQuery) {
		this.userQuery = userQuery;
	}

	public String getEventURI() {
		return eventURI;
	}

	public void setEventURI(String eventURI) {
		this.eventURI = eventURI;
	}

	public String getSystemResponse() {
		return systemResponse;
	}

	public void setSystemResponse(String systemResponse) {
		this.systemResponse = systemResponse;
	}
	
	@Override
	public String toString() {
		return String.format("[userQuery = <%s>, event = '%s', systemResponse = '%s']", 
								StringUtils.collectionToCommaDelimitedString(userQuery), eventURI, systemResponse);
	}
}
