package edu.kit.anthropomatik.isl.newsTeller.knowledgeStore;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openrdf.query.BindingSet;

import eu.fbk.knowledgestore.KnowledgeStore;
import eu.fbk.knowledgestore.Session;
import eu.fbk.knowledgestore.client.Client;
import eu.fbk.knowledgestore.data.Stream;

/**
 * Adapter class to facilitate KnowledgeStore access.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class KnowledgeStoreAdapter {
	
	private static Log log = LogFactory.getLog(KnowledgeStoreAdapter.class);
	
	private String serverURL;
	
	private int timeoutMsec;
	
	private boolean isConnectionOpen = false;
	
	private KnowledgeStore knowledgeStore;
	
	private Session session;
	
	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}

	public void setTimeoutMsec(int timeoutMsec) {
		this.timeoutMsec = timeoutMsec;
	}

	public boolean isConnectionOpen() {
		return isConnectionOpen;
	}
	
	/**
	 * Open a connection to the given KnowledgeStore (configuration done via Spring). Only works if there is no other open connection!
	 * Remember to call closeConnection() when done querying.
	 */
	public void openConnection() {
		if (this.isConnectionOpen) {
			if (log.isWarnEnabled())
				log.warn("Trying to open a second connection before closing the first one. Request ignored.");
		} else {
			this.knowledgeStore = Client.builder(serverURL).compressionEnabled(true).maxConnections(2).validateServer(false)
					.connectionTimeout(timeoutMsec).build();
			this.session = knowledgeStore.newSession();
			this.isConnectionOpen = true;
		}
	}
	
	/**
	 * Closing the connection to the KnowledgeStore.
	 */
	public void closeConnection() {
		if (this.isConnectionOpen) {
			this.session.close();
			this.knowledgeStore.close();
			this.isConnectionOpen = false;
		} else {
			if (log.isWarnEnabled())
				log.warn("Trying to close nonexistent connection. Request ignored.");
		}
		
	}
	
	/**
	 * Send the given sparqlQuery to the KnowledgeStore instance (using the given timeout in milliseconds) and return the retrieved events.
	 */
	public List<URI> getEvents(String sparqlQuery, String eventVariableName, long timeoutMillisec) {
		List<URI> result = new ArrayList<URI>();
		
		if (isConnectionOpen) {
			 
			try {
				Stream<BindingSet> stream = this.session.sparql(sparqlQuery).timeout((long) (timeoutMillisec)).execTuples();
				List<BindingSet> tuples = stream.toList();
				for (BindingSet tuple : tuples) {
					result.add(URI.create(tuple.getValue(eventVariableName).toString()));
				}
				stream.close();
			} catch (Exception e) {
				if(log.isErrorEnabled())
					log.error("Query execution failed. Query: " + sparqlQuery + " Exception: " + e.getMessage());					
				e.printStackTrace();
			}
			
		} else {
			if (log.isWarnEnabled())
				log.warn("Trying to access KnowledgeStore without having an open connection. Request ignored, returning empty list."); 
		}
				
		return result;
	}

}
