package edu.kit.anthropomatik.isl.newsTeller.knowledgeStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
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
	
	private String getMentionFromEventTemplate;
	
	private Map<String, String> resourceCache;
	
	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}

	public void setTimeoutMsec(int timeoutMsec) {
		this.timeoutMsec = timeoutMsec;
	}

	public boolean isConnectionOpen() {
		return isConnectionOpen;
	}
	
	public KnowledgeStoreAdapter(String getMentionFromEventFileName) {
		this.getMentionFromEventTemplate = Util.readStringFromFile(getMentionFromEventFileName);
		this.resourceCache = new HashMap<String, String>();
	}
	
	/**
	 * Open a connection to the given KnowledgeStore (configuration done via Spring). Only works if there is no other open connection!
	 * Remember to call closeConnection() when done querying.
	 */
	public void openConnection() {
		if (log.isTraceEnabled())
			log.trace("openConnection()");
		
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
		if (log.isTraceEnabled())
			log.trace("closeConnection()");
		
		if (this.isConnectionOpen) {
			this.session.close();
			this.knowledgeStore.close();
			this.isConnectionOpen = false;
		} else {
			if (log.isWarnEnabled())
				log.warn("Trying to close nonexistent connection. Request ignored.");
		}
		
	}
	
	//region String query
	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (using the given timeout in milliseconds).
	 * Returns the retrieved results as Strings.
	 * Don't report empty results as errors if flag is set.
	 */
	public List<String> runSingleVariableStringQuery(String sparqlQuery, String variableName, long timeoutMillisec, boolean isEmptyResultExpected) {
		if (log.isTraceEnabled())
			log.trace(String.format("runSingleVariableStringQuery(sparqlQuery = '%s', variableName = '%s', timeoutMillisec = %d)", 
									sparqlQuery, variableName, timeoutMillisec));
		
		
		List<String> result = new ArrayList<String>();
		
		if (isConnectionOpen) {
			 
			try {
				Stream<BindingSet> stream = this.session.sparql(sparqlQuery).timeout(timeoutMillisec).execTuples();
				List<BindingSet> tuples = stream.toList();
				for (BindingSet tuple : tuples) {
					result.add(tuple.getValue(variableName).toString());
				}
				stream.close();
			} catch (Exception e) {
				if (!isEmptyResultExpected) {
					if(log.isErrorEnabled())
						log.error(String.format("Query execution failed. Query: '%s' Variable: '%s' Timeout: %d", 
								sparqlQuery, variableName, timeoutMillisec));
					if(log.isDebugEnabled())
						log.debug("Query execution exception", e);
				}
			}
			
		} else {
			if (log.isWarnEnabled())
				log.warn("Trying to access KnowledgeStore without having an open connection. Request ignored, returning empty list."); 
		}
		
		return result;
	}
	
	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with a standard timeout of 10 seconds).
	 * Returns the retrieved results as Strings.
	 * Don't report empty results as errors if flag is set.
	 */
	public List<String> runSingleVariableStringQuery(String sparqlQuery, String variableName, boolean isEmptyResultExpected) {
		return runSingleVariableStringQuery(sparqlQuery, variableName, 10000, isEmptyResultExpected);
	}
	
	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with a standard timeout of 10 seconds).
	 * Returns the retrieved results as Strings.
	 * Does not expect empty results.
	 */
	public List<String> runSingleVariableStringQuery(String sparqlQuery, String variableName) {
		return runSingleVariableStringQuery(sparqlQuery, variableName, false);
	}
	
	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (using the given timeout in milliseconds).
	 * Returns the first retrieved result as String.
	 * Handles empty results by returning empty String.
	 */
	public String runSingleVariableStringQuerySingleResult(String sparqlQuery, String variableName, long timeoutMillisec) {
		List<String> results = runSingleVariableStringQuery(sparqlQuery, variableName, timeoutMillisec, true);
		if (results.size() > 0)
			return results.get(0);
		else
			return "";
	}
	
	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with a standard timeout of 10 seconds).
	 * Returns the first retrieved result as String.
	 */
	public String runSingleVariableStringQuerySingleResult(String sparqlQuery, String variableName) {
		return runSingleVariableStringQuerySingleResult(sparqlQuery, variableName, 10000);
	}
	//endregion
	
	//region NewsEvent query
	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (using the given timeout in milliseconds).
	 * Returns the retrieved results as NewsEvents.
	 * Empty results are expected and handed over.
	 */
	public List<NewsEvent> runSingleVariableEventQuery(String sparqlQuery, String variableName, long timeoutMillisec) {
		
		List<NewsEvent> result = new ArrayList<NewsEvent>();
		
		List<String> stringResults = runSingleVariableStringQuery(sparqlQuery, variableName, timeoutMillisec, true);
	
		for (String str : stringResults) {
			result.add(new NewsEvent(str));
		}
		
		return result;
	}

	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with a standard timeout of 10 seconds).
	 * Returns the retrieved results as NewsEvents.
	 */
	public List<NewsEvent> runSingleVariableEventQuery(String sparqlQuery, String variableName) {
		return runSingleVariableEventQuery(sparqlQuery, variableName, 10000);
	}
	
	//endregion
	
	//region Double query
	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (using the given timeout in milliseconds).
	 * Returns the retrieved results as Doubles.
	 * Empty results are expected and passed on.
	 */
	public List<Double> runSingleVariableDoubleQuery(String sparqlQuery, String variableName, long timeoutMillisec) {
		
		List<Double> results = new ArrayList<Double>();
		
		List<String> stringResults = runSingleVariableStringQuery(sparqlQuery, variableName, timeoutMillisec, true);
		
		for (String str : stringResults) {
			results.add(Util.parseXMLDouble(str));
		}
		
		return results;
	}
	
	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with a standard timeout of 10 seconds).
	 * Returns the retrieved results as Doubles.
	 * Empty results are expected and passed on.
	 */
	public List<Double> runSingleVariableDoubleQuery(String sparqlQuery, String variableName) {
		return runSingleVariableDoubleQuery(sparqlQuery, variableName, 10000);
	}
	
	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (using the given timeout in milliseconds).
	 * Returns the first retrieved result as Double.
	 * Empty results are expected and dealt with by returning Double.NaN
	 */
	public double runSingleVariableDoubleQuerySingleResult(String sparqlQuery, String variableName, long timeoutMillisec) {
		List<Double> results = runSingleVariableDoubleQuery(sparqlQuery, variableName, timeoutMillisec);
		if (results.size() > 0)
			return results.get(0);
		else
			return Double.NaN;
	}
	
	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with a standard timeout of 10 seconds).
	 * Returns the first retrieved result as Double.
	 * Empty results are expected and dealt with by returning Double.NaN
	 */
	public double runSingleVariableDoubleQuerySingleResult(String sparqlQuery, String variableName) {
		return runSingleVariableDoubleQuerySingleResult(sparqlQuery, variableName, 10000);
	}
	//endregion
	
	//region retrieve text
	// get the news story text - either from the cache or by looking it up
	private String getOriginalText(String resourceURI) {
		String result = "";
		if (resourceCache.containsKey(resourceURI))
			result = resourceCache.get(resourceURI);
		else {
			try {
				result = session.download(new URIImpl(resourceURI)).exec().writeToString();
				resourceCache.put(resourceURI, result);
			} catch (Exception e) {
				if(log.isErrorEnabled())
					log.error(String.format("Could not retrieve resource, returning empty String. URI: '%s'", resourceURI));
				if(log.isDebugEnabled())
					log.debug("Resource download failed", e);
			}
		}
		
		return result;
	}
	
	/**
	 * Returns a list of all news stories in which the given event is mentioned.
	 */
	public List<String> retrieveOriginalTexts(String eventURI) {
		List<String> originalTexts = new ArrayList<String>();
		
		List<String> mentionURIs = runSingleVariableStringQuery(getMentionFromEventTemplate.replace(Util.PLACEHOLDER_EVENT, eventURI), 
				Util.VARIABLE_MENTION);
		
		for (String mentionURI : mentionURIs) {
			String resourceURI = mentionURI.substring(0, mentionURI.indexOf("#"));
			String originalText = getOriginalText(resourceURI);
			if (!originalText.isEmpty())
				originalTexts.add(originalText);
		}
		
		return originalTexts;
	}
	
	/**
	 * Given the eventURI, picks the first mention and extracts the surrounding sentence from the original resource.
	 */
	public String retrieveSentencefromEvent(String eventURI) {
		
		// get mention TODO: instead of picking first mention, maybe use nwr:factualityConfidence?
		String mentionURI = runSingleVariableStringQuerySingleResult(getMentionFromEventTemplate.replace(Util.PLACEHOLDER_EVENT, eventURI), 
																		Util.VARIABLE_MENTION);

		if (mentionURI.isEmpty()) {
			if(log.isErrorEnabled())
				log.error(String.format("Could not retrieve mention for event, returning empty string: '%s'", eventURI));
			return "";
		}
		// get mention information TODO: better way to access this?
		String resourceURI = mentionURI.substring(0, mentionURI.indexOf("#"));
		int startIdx = Integer.parseInt(mentionURI.substring(mentionURI.indexOf("=")+1, mentionURI.indexOf(",", mentionURI.indexOf("="))));
		int endIdx = Integer.parseInt(mentionURI.substring(mentionURI.indexOf(",", mentionURI.indexOf("="))+1));
		
		// get original text
		String originalText = getOriginalText(resourceURI);
		if (originalText.isEmpty())
			return "";
		
		// search for sentence boundaries using a very simple heuristic
		List<Character> sentenceDelimiters = Arrays.asList('.', '!', '?');
		while((startIdx > 0) && (!sentenceDelimiters.contains(originalText.charAt(startIdx-1))))
			startIdx--;
		while((endIdx < originalText.length() - 1) && (!sentenceDelimiters.contains(originalText.charAt(endIdx-1))))
			endIdx++;
		
		// pick correct substring
		return originalText.substring(startIdx, endIdx).trim();
	}
	//endregion
}
