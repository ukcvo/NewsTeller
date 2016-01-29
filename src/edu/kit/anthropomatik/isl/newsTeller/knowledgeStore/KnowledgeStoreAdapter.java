package edu.kit.anthropomatik.isl.newsTeller.knowledgeStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import edu.kit.anthropomatik.isl.newsTeller.data.KSMention;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import eu.fbk.knowledgestore.KnowledgeStore;
import eu.fbk.knowledgestore.Session;
import eu.fbk.knowledgestore.client.Client;
import eu.fbk.knowledgestore.data.Record;
import eu.fbk.knowledgestore.data.Stream;
import eu.fbk.knowledgestore.vocabulary.KS;

/**
 * Adapter class to facilitate KnowledgeStore access.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class KnowledgeStoreAdapter {
	
	private static Log log = LogFactory.getLog(KnowledgeStoreAdapter.class);
	
	private static final int MAXIMUM_QUERY_LENGTH = 6700;
	
	private String serverURL;
	
	private int timeoutMsec;
	
	private int maxNumberOfConnections;
	
	private boolean isConnectionOpen = false;
	
	private KnowledgeStore knowledgeStore;
	
	private String getMentionFromEventTemplate;
	
	private String getEventFromMentionTemplate;
	
	private ConcurrentMap<String, String> resourceCache;
	
	private ConcurrentMap<String, List<KSMention>> eventMentionCache;
	
	private ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache; // relationship-id --> key --> values
	
	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}

	public void setTimeoutMsec(int timeoutMsec) {
		this.timeoutMsec = timeoutMsec;
	}
	
	public void setMaxNumberOfConnections(int maxNumberOfConnections) {
		this.maxNumberOfConnections = maxNumberOfConnections;
	}
	
	public int getMaxNumberOfConnections() {
		return this.maxNumberOfConnections;
	}

	public boolean isConnectionOpen() {
		return isConnectionOpen;
	}
	
	public KnowledgeStoreAdapter(String getMentionFromEventFileName, String getEventFromMentionFileName) {
		this.getMentionFromEventTemplate = Util.readStringFromFile(getMentionFromEventFileName);
		this.getEventFromMentionTemplate = Util.readStringFromFile(getEventFromMentionFileName);
		this.resourceCache = new ConcurrentHashMap<String, String>();
		this.eventMentionCache = new ConcurrentHashMap<String, List<KSMention>>();
		this.sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
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
			this.knowledgeStore = Client.builder(serverURL).compressionEnabled(true).maxConnections(maxNumberOfConnections).validateServer(false)
					.connectionTimeout(timeoutMsec).build();
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
			this.knowledgeStore.close();
			this.isConnectionOpen = false;
		} else {
			if (log.isWarnEnabled())
				log.warn("Trying to close nonexistent connection. Request ignored.");
		}
		
	}
	
	//region filling the buffer
	
	/**
	 * Runs a key-value query. 
	 * Inserts the keyValues into the template, fires the query and stores the resulting key-value pairs in the internal cache.
	 */
	public void runKeyValueQuery(String sparqlQueryTemplate, String relationName, String variableNameKey, 
									String variableNameValue, Set<String> keyValues) {
		
		if (!isConnectionOpen) {
			if (log.isWarnEnabled())
				log.warn("Trying to access KnowledgeStore without having an open connection. Request ignored."); 
			return;
		}
		
		try {
			ConcurrentMap<String, Set<String>> relationMap = new ConcurrentHashMap<String, Set<String>>();
			
			List<String> queries = new ArrayList<String>();
			StringBuilder sb = new StringBuilder();
			for (String uri : keyValues) {
				String s = String.format("<%s> ", uri);
				if (sb.length() + s.length() + sparqlQueryTemplate.length() > MAXIMUM_QUERY_LENGTH) {
					queries.add(sparqlQueryTemplate.replace(Util.PLACEHOLDER_KEYS, sb.toString().trim()));
					sb = new StringBuilder();
				}
				sb.append(s);
			}
			queries.add(sparqlQueryTemplate.replace(Util.PLACEHOLDER_KEYS, sb.toString().trim()));
			
			for (String query : queries) {
				Session session = this.knowledgeStore.newSession();
				Stream<BindingSet> stream = session.sparql(query).timeout((long) this.timeoutMsec).execTuples();
				List<BindingSet> tuples = stream.toList();
				stream.close();
				session.close();
				for (BindingSet tuple : tuples) {
					String key = tuple.getValue(variableNameKey).toString();
					String value = tuple.getValue(variableNameValue).toString();
					if (value.startsWith("\""))
						value = value.substring(1, value.lastIndexOf('"'));
					Set<String> values = relationMap.containsKey(key) ? relationMap.get(key) : new HashSet<String>();
					values.add(value);
					relationMap.put(key, values);
				}
			}
			
			this.sparqlCache.put(relationName, relationMap);
			
		} catch (Exception e) {
			if(log.isErrorEnabled())
				log.error(String.format("Query execution failed. Query: '%s' Key: '%s' Value: %s", 
							sparqlQueryTemplate, variableNameKey, variableNameValue));
			if(log.isDebugEnabled())
					log.debug("Query execution exception", e);
		}
	}
	
	//endregion
	
	//region accessing the buffer
	/**
	 * Retrieves the values from the internal buffer, given the name of the key-value relation and the key.
	 */
	public Set<String> getBufferedValues(String relationName, String key) {
		
		if (!this.sparqlCache.containsKey(relationName)) {
			if (log.isErrorEnabled())
				log.error(String.format("unknown relationName '%s'. Returning empty set.", relationName));
			return new HashSet<String>();
		}
		ConcurrentMap<String, Set<String>> relationMap = this.sparqlCache.get(relationName);
		if (!relationMap.containsKey(key)) {
			if (log.isErrorEnabled())
				log.error(String.format("relation '%s' does not contain key '%s'. Returning empty set.", relationName, key));
			return new HashSet<String>();
		}
		return relationMap.get(key);
	}
	//endregion
	
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
				Session session = this.knowledgeStore.newSession();
				Stream<BindingSet> stream = session.sparql(sparqlQuery).timeout(timeoutMillisec).execTuples();
				List<BindingSet> tuples = stream.toList();
				stream.close();
				session.close();
				for (BindingSet tuple : tuples) {
					String value = tuple.getValue(variableName).toString();
					if (value.startsWith("\""))
						value = value.substring(1, value.lastIndexOf('"'));
					result.add(value);
				}
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
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with the standard timeout).
	 * Returns the retrieved results as Strings.
	 * Don't report empty results as errors if flag is set.
	 */
	public List<String> runSingleVariableStringQuery(String sparqlQuery, String variableName, boolean isEmptyResultExpected) {
		return runSingleVariableStringQuery(sparqlQuery, variableName, this.timeoutMsec, isEmptyResultExpected);
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
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with the standard timeout).
	 * Returns the first retrieved result as String.
	 */
	public String runSingleVariableStringQuerySingleResult(String sparqlQuery, String variableName) {
		return runSingleVariableStringQuerySingleResult(sparqlQuery, variableName, this.timeoutMsec);
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
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with the standard timeout).
	 * Returns the retrieved results as NewsEvents.
	 */
	public List<NewsEvent> runSingleVariableEventQuery(String sparqlQuery, String variableName) {
		return runSingleVariableEventQuery(sparqlQuery, variableName, this.timeoutMsec);
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
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with the standard timeout).
	 * Returns the retrieved results as Doubles.
	 * Empty results are expected and passed on.
	 */
	public List<Double> runSingleVariableDoubleQuery(String sparqlQuery, String variableName) {
		return runSingleVariableDoubleQuery(sparqlQuery, variableName, this.timeoutMsec);
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
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with the standard timeout).
	 * Returns the first retrieved result as Double.
	 * Empty results are expected and dealt with by returning Double.NaN
	 */
	public double runSingleVariableDoubleQuerySingleResult(String sparqlQuery, String variableName) {
		return runSingleVariableDoubleQuerySingleResult(sparqlQuery, variableName, this.timeoutMsec);
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
				Session session = this.knowledgeStore.newSession();
				result = session.download(new URIImpl(resourceURI)).timeout((long) this.timeoutMsec).exec().writeToString();
				session.close();
				resourceCache.putIfAbsent(resourceURI, result);
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
	public Set<String> retrieveOriginalTexts(String eventURI) {
		Set<String> originalTexts = new HashSet<String>();
		
		List<String> mentionURIs;
		if (this.sparqlCache.containsKey(Util.RELATION_NAME_MENTION) && this.sparqlCache.get(Util.RELATION_NAME_MENTION).containsKey(eventURI))
			mentionURIs = new ArrayList<String>(this.sparqlCache.get(Util.RELATION_NAME_MENTION).get(eventURI));
		else
		 mentionURIs = runSingleVariableStringQuery(getMentionFromEventTemplate.replace(Util.PLACEHOLDER_EVENT, eventURI), 
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
	 * Returns the phrase the mention is referring to from the original text.
	 */
	public String retrievePhraseFromMention(String mentionURI) {
		return retrievePhraseFromMention(mentionURI, false);
	}
	
	/**
	 * Returns the phrase the mention is referring to from the original text. If wholeSentence is set to true, will expand phrase to whole sentence.
	 */
	public String retrievePhraseFromMention(String mentionURI, boolean wholeSentence) {
		
		KSMention mention = retrieveKSMentionFromMentionURI(mentionURI, wholeSentence);
		
		// get original text
		String originalText = getOriginalText(mention.getResourceURI());
		if (originalText.isEmpty())
			return "";
		
		return originalText.substring(mention.getStartIdx(), mention.getEndIdx());
	}
	
	/**
	 * Returns the phrases of the mentions of the given entity.
	 */
	public List<String> retrievePhrasesFromEntity(String entityURI) {
		return retrievePhrasesFromEntity(entityURI, false);
	}
	
	/**
	 * Returns the phrases of the mentions of the given entity. If wholeSentence is set, returns whole sentences.
	 */
	public List<String> retrievePhrasesFromEntity(String entityURI, boolean wholeSentence) {
		List<String> result = new ArrayList<String>();
		
		List<String> mentions;
		if (this.sparqlCache.containsKey(Util.RELATION_NAME_MENTION) && this.sparqlCache.get(Util.RELATION_NAME_MENTION).containsKey(entityURI))
			mentions = new ArrayList<String>(this.sparqlCache.get(Util.RELATION_NAME_MENTION).get(entityURI));
		else mentions = runSingleVariableStringQuery(getMentionFromEventTemplate.replace(Util.PLACEHOLDER_EVENT, entityURI), 
																		Util.VARIABLE_MENTION);

		if (mentions.isEmpty()) {
			if(log.isErrorEnabled())
				log.error(String.format("Could not retrieve mentions for entity, returning empty list: '%s'", entityURI));
			return result;
		}
		
		for (String mention : mentions) {
			
			String sentence = retrievePhraseFromMention(mention, wholeSentence);
			if (!sentence.isEmpty())
				result.add(sentence);
		}
		
		return result;
	}
	
	/**
	 * Converts the given mentionURI into a KSMention object; if wholeSentence is set, expands the mention to a complete sentence.
	 */
	public KSMention retrieveKSMentionFromMentionURI(String mentionURI, boolean wholeSentence) {
		String resourceURI = mentionURI.substring(0, mentionURI.indexOf("#"));
		int startIdx = Integer.parseInt(mentionURI.substring(mentionURI.indexOf("=")+1, mentionURI.indexOf(",", mentionURI.indexOf("="))));
		int endIdx = Integer.parseInt(mentionURI.substring(mentionURI.indexOf(",", mentionURI.indexOf("="))+1));
		
		if (wholeSentence) {
			// search for sentence boundaries using a very simple heuristic
			String originalText = getOriginalText(resourceURI);
			if (originalText.isEmpty()) {
				if (log.isWarnEnabled())
					log.warn(String.format("empty original text, cannot find sentence boundaries for mention '%s'", mentionURI));
				return new KSMention(mentionURI);
			}
			List<Character> sentenceDelimiters = Arrays.asList('.', '!', '?');
			List<Character> skipChars = Arrays.asList(' ', '\n', '\t');
			while((startIdx > 0) && (!sentenceDelimiters.contains(originalText.charAt(startIdx-1))))
				startIdx--;
			while (skipChars.contains(originalText.charAt(startIdx)))
				startIdx++;
			
			while((endIdx < originalText.length()) && (!sentenceDelimiters.contains(originalText.charAt(endIdx-1))))
				endIdx++;
		}
		
		return new KSMention(resourceURI, startIdx, endIdx);
	}
	
	/**
	 * Returns the sentence corresponding to the given mention.
	 */
	public String retrieveSentenceFromMention(String mentionURI) {
		return retrievePhraseFromMention(mentionURI, true);
	}
	
	/**
	 * Given the eventURI, picks the first mention and extracts the surrounding sentence from the original resource.
	 */
	public String retrieveSentencefromEvent(String eventURI) {
		List<String> results = retrieveSentencesfromEvent(eventURI);
		if (results.isEmpty())
			return "";
		else
			return results.get(0);
	}
	
	/**
	 * Given the eventURI, returns a list of all sentences mentioning this event.
	 */
	public List<String> retrieveSentencesfromEvent(String eventURI) {
		return retrievePhrasesFromEntity(eventURI, true);
	}
	//endregion

	// region handling mentions
	
	/**
	 * Retrieves the given property for the given mention within the standard timeout and returns it as list of Strings.
	 */
	public List<String> getMentionProperty(String mentionURI, String propertyURI) {
		return getMentionProperty(mentionURI, propertyURI, this.timeoutMsec);
	}
	
	/**
	 * Retrieves the given property for the given mention within the given timeout and returns it as list of Strings.
	 */
	public List<String> getMentionProperty(String mentionURI, String propertyURI, long timeoutMillisec) {
		List<String> result = new ArrayList<String>();
		Session session = this.knowledgeStore.newSession();
		
		try {
			Stream<Record> stream = session.retrieve(KS.MENTION).ids(new URIImpl(mentionURI)).timeout(timeoutMillisec).exec();
			List<Record> records = stream.toList();
			stream.close();
			
			for (Record r : records) {
				List<String> values = r.get(new URIImpl(propertyURI), String.class);
				result.addAll(values);
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(String.format("Cannot retrieve property '%s' of mention '%s'. Returning empty result", propertyURI, mentionURI));
			if (log.isDebugEnabled())
				log.debug("Cannot retrieve mention property", e);
		}
		
		session.close();
		return result;
	}
	
	/**
	 * Retrieves the given property for the given mention within the standard timeout and returns the first result as String.
	 */
	public String getUniqueMentionProperty(String mentionURI, String propertyURI) {
		return getUniqueMentionProperty(mentionURI, propertyURI, this.timeoutMsec);
	}
	
	/**
	 * Retrieves the given property for the given mention within the given timeout and returns the first result as String.
	 */
	public String getUniqueMentionProperty(String mentionURI, String propertyURI, long timeoutMillisec) {
		List<String> results = getMentionProperty(mentionURI, propertyURI, timeoutMillisec);
		if (results.size() > 0)
			return results.get(0);
		else
			return "";
	}
	
	/**
	 * Takes the given resourceURI and returns all mentions of this resource that link to an event.
	 */
	public List<KSMention> getAllEventMentions(String resourceURI) {
		
		if (this.eventMentionCache.containsKey(resourceURI)) 
			return this.eventMentionCache.get(resourceURI);
		
		List<KSMention> result = new ArrayList<KSMention>();

		try {
			Session session = knowledgeStore.newSession();
			Stream<Record> stream = session.retrieve(KS.RESOURCE).ids(new URIImpl(resourceURI)).timeout((long) this.timeoutMsec).exec();
			List<Record> records = stream.toList();
			stream.close();
			session.close();
			
			for (Record r : records) {
				List<String> mentionURIs = r.get(new URIImpl("http://dkm.fbk.eu/ontologies/knowledgestore#hasMention"), String.class);
				
				for (String mentionURI : mentionURIs) {
					String event = runSingleVariableStringQuerySingleResult(this.getEventFromMentionTemplate.replace(Util.PLACEHOLDER_MENTION, mentionURI), Util.VARIABLE_EVENT);
					if (!event.isEmpty())
						result.add(new KSMention(mentionURI));
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 		
		
		this.eventMentionCache.putIfAbsent(resourceURI, result);
		
		return result;
	}
	
	// endregion
}
