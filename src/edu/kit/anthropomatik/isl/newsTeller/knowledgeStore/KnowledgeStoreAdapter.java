package edu.kit.anthropomatik.isl.newsTeller.knowledgeStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.KSMention;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import eu.fbk.knowledgestore.KnowledgeStore;
import eu.fbk.knowledgestore.OperationException;
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

	private static final int MAXIMUM_QUERY_LENGTH = 6000;

	private String serverURL;

	private int timeoutMsec;

	private int maxNumberOfConnections;

	private boolean isConnectionOpen = false;

	private KnowledgeStore knowledgeStore;

	private String getMentionFromEventTemplate;

	private String getMentionFromEventTemplateName;

	private String getEventFromMentionTemplate;

	private String getEventFromMentionTemplateName;

	private ConcurrentMap<String, String> resourceCache;

	private ConcurrentMap<String, Set<KSMention>> eventMentionCache;

	private ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache; // relationship-id --> key --> values

	private ConcurrentMap<String, KSMention> sentenceMentionCache;
	
	private ExecutorService threadPool;

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

	public String getMentionFromEventTemplateName() {
		return this.getMentionFromEventTemplateName;
	}
	
	public boolean isConnectionOpen() {
		return isConnectionOpen;
	}

	public KnowledgeStoreAdapter(String getMentionFromEventFileName, String getEventFromMentionFileName) {
		this.getMentionFromEventTemplate = Util.readStringFromFile(getMentionFromEventFileName);
		this.getEventFromMentionTemplate = Util.readStringFromFile(getEventFromMentionFileName);
		this.resourceCache = new ConcurrentHashMap<String, String>();
		this.eventMentionCache = new ConcurrentHashMap<String, Set<KSMention>>();
		this.sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
		this.sentenceMentionCache = new ConcurrentHashMap<String, KSMention>();
		this.getMentionFromEventTemplateName = Util.queryNameFromFileName(getMentionFromEventFileName);
		this.getEventFromMentionTemplateName = Util.queryNameFromFileName(getEventFromMentionFileName);
	}

	/**
	 * Open a connection to the given KnowledgeStore (configuration done via
	 * Spring). Only works if there is no other open connection! Remember to
	 * call closeConnection() when done querying.
	 */
	public void openConnection() {
		if (log.isTraceEnabled())
			log.trace("openConnection()");

		if (this.isConnectionOpen) {
			if (log.isWarnEnabled())
				log.warn("Trying to open a second connection before closing the first one. Request ignored.");
		} else {
			this.knowledgeStore = Client.builder(serverURL).compressionEnabled(true).maxConnections(maxNumberOfConnections).validateServer(false).connectionTimeout(timeoutMsec).build();
			this.threadPool = Executors.newFixedThreadPool(maxNumberOfConnections);
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
			this.threadPool.shutdown();
			this.isConnectionOpen = false;
		} else {
			if (log.isWarnEnabled())
				log.warn("Trying to close nonexistent connection. Request ignored.");
		}

	}

	/**
	 * Flushes the internal buffer. should be called before each new "round" of
	 * processing.
	 */
	public void flushBuffer() {
		this.sparqlCache.clear();
		this.sentenceMentionCache.clear();
		this.eventMentionCache.clear();
	}

	/**
	 * Submit the given task to the internal threadpool.
	 */
	public Future<?> submit(Runnable task) {
		return this.threadPool.submit(task);
	}

	// region filling the buffer

	// takes care of the individual bulk queries
	private class KeyValueWorker implements Runnable {

		private String query;
		private String variableNameKey;
		private String variableNameValue;
		private ConcurrentMap<String, Set<String>> relationMap;

		public KeyValueWorker(String query, String variableNameKey, String variableNameValue, ConcurrentMap<String, Set<String>> relationMap) {
			this.query = query;
			this.variableNameKey = variableNameKey;
			this.variableNameValue = variableNameValue;
			this.relationMap = relationMap;
		}

		@Override
		public void run() {
			try {
				Session session = knowledgeStore.newSession();
				Stream<BindingSet> stream = session.sparql(query).timeout((long) timeoutMsec).execTuples();
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
					relationMap.putIfAbsent(key, values);
				}
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error(String.format("Query execution failed. Query: '%s' Key: '%s' Value: '%s'", query, variableNameKey, variableNameValue));
				if (log.isDebugEnabled())
					log.debug("Query execution exception", e);
			}

		}

	}

	/**
	 * Store all mentions of the given events in the internal map.
	 */
	public void runKeyValueMentionFromEventQuery(Set<String> eventURIs) {
		runKeyValueSparqlQuery(getMentionFromEventTemplate, Util.RELATION_NAME_EVENT_MENTION, Util.VARIABLE_EVENT, Util.VARIABLE_MENTION, eventURIs);
	}
	
	/**
	 * Runs a key-value SPARQL query. Inserts the keyValues into the template, fires
	 * the query and stores the resulting key-value pairs in the internal cache.
	 */
	public void runKeyValueSparqlQuery(String sparqlQueryTemplate, String relationName, String variableNameKey, String variableNameValue, Set<String> keyValues) {

		if (!isConnectionOpen) {
			if (log.isWarnEnabled())
				log.warn("Trying to access KnowledgeStore without having an open connection. Request ignored.");
			return;
		}
		if (sparqlQueryTemplate.isEmpty()) {
			if (log.isWarnEnabled())
				log.warn("Empty query template. Request ignored.");
			return;
		}
		if (keyValues.isEmpty()) {
			if (log.isWarnEnabled())
				log.warn("Empty set of keyValues. Request ignored.");
			return;
		}

		if (this.sparqlCache.containsKey(relationName))
			return; // don't do work twice

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

		List<Future<?>> futures = new ArrayList<Future<?>>();
		for (String query : queries) {
			KeyValueWorker w = new KeyValueWorker(query, variableNameKey, variableNameValue, relationMap);
			futures.add(threadPool.submit(w));
		}

		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("thread execution somehow failed!");
				if (log.isDebugEnabled())
					log.debug("thread execution exception", e);
			}
		}

		for (String key : keyValues) {
			if (!relationMap.containsKey(key))
				relationMap.put(key, new HashSet<String>()); // fill up with empty sets
		}

		this.sparqlCache.put(relationName, relationMap);

	}

	/**
	 * Runs a key-value mentionProperty query for a single property. Stores the resulting mentionURI-propertyValue pairs in the internal cache.
	 */
	public void runKeyValueMentionPropertyQuery(String propertyURI, String relationName, Set<String> mentionURIs) {
		Set<String> propertyURIs = new HashSet<String>();
		propertyURIs.add(propertyURI);
		runKeyValueMentionPropertyQuery(propertyURIs, relationName, mentionURIs);
	}
	
	/**
	 * Runs a key-value mentionProperty query for a set of properties. Stores the resulting mentionURI-propertyValue pairs in the internal cache.
	 */
	public void runKeyValueMentionPropertyQuery(Set<String> propertyURIs, String relationName, Set<String> mentionURIs) {
		if (!isConnectionOpen) {
			if (log.isWarnEnabled())
				log.warn("Trying to access KnowledgeStore without having an open connection. Request ignored.");
			return;
		}
		if (mentionURIs.isEmpty()) {
			if (log.isWarnEnabled())
				log.warn("Empty set of mentionURIs. Request ignored.");
			return;
		}

		
		ConcurrentMap<String, ConcurrentMap<String, Set<String>>> propertyMap = new ConcurrentHashMap<String, ConcurrentMap<String,Set<String>>>();
		
		for (String propertyURI : propertyURIs)
			propertyMap.put(propertyURI, new ConcurrentHashMap<String, Set<String>>());

		boolean areAllPropertiesAlreadyListed = true;
		for (String propertyURI : propertyURIs) {
			if (!sparqlCache.containsKey(relationName + propertyURI)) {
				areAllPropertiesAlreadyListed = false;
				break;
			}
		}
		if (areAllPropertiesAlreadyListed)
			return; // no need to do double work
		
		List<Set<URI>> queryURISets = new ArrayList<Set<URI>>();
		Set<URI> currentSet = new HashSet<URI>();
		int currentLength = 0;
		for (String mentionURI : mentionURIs) {
			if (currentLength + mentionURI.length() > 6000) {
				queryURISets.add(currentSet);
				currentSet = new HashSet<URI>();
				currentLength = 0;
			}
			currentSet.add(new URIImpl(mentionURI));
			currentLength += mentionURI.length();
		}
		queryURISets.add(currentSet);

		Session session = knowledgeStore.newSession();
		for (Set<URI> uriSet : queryURISets) {
			try {
				Stream<Record> stream = session.retrieve(KS.MENTION).ids(uriSet).timeout(10000L).exec();
				List<Record> records = stream.toList();
				stream.close();
				
				for (Record r : records) {
					String key = r.getID().toString();
					for (String propertyURI : propertyURIs) {
						Set<String> values = new HashSet<String>(r.get(new URIImpl(propertyURI), String.class));
						propertyMap.get(propertyURI).put(key, values);
					}
					
				}
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error(String.format("Mention property access failed. Property: '%s'", 
							StringUtils.collectionToCommaDelimitedString(queryURISets)));
				if (log.isDebugEnabled())
					log.debug("Mention property access exception", e);
			}
		}

		for (String propertyURI : propertyURIs) {
			this.sparqlCache.put(relationName + propertyURI, propertyMap.get(propertyURI));
		}
		
	}
	
	private class ResourceTextWorker implements Runnable {

		private String resourceURI;
		private ConcurrentMap<String, Set<String>> relationMap;
		
		public ResourceTextWorker(String resourceURI, ConcurrentMap<String, Set<String>> relationMap) {
			this.resourceURI = resourceURI;
			this.relationMap = relationMap;
		}
		
		@Override
		public void run() {
			Set<String> result = new HashSet<String>();
			try {
				Session session = knowledgeStore.newSession();
				result.add(session.download(new URIImpl(resourceURI)).timeout((long) timeoutMsec).exec().writeToString());
				session.close();
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error(String.format("Could not retrieve resource, returning empty String. URI: '%s'", resourceURI));
				if (log.isDebugEnabled())
					log.debug("Resource download failed", e);
			}
			relationMap.putIfAbsent(resourceURI, result);
		}
		
		
	}
	
	/**
	 * Collects the texts for the given resourceURIs.
	 */
	public void runKeyValueResourceTextQuery(Set<String> resourceURIs) {
		
		if (this.sparqlCache.containsKey(Util.RELATION_NAME_RESOURCE_TEXT))
			return; // don't do double work
		
		ConcurrentMap<String, Set<String>> relationMap = new ConcurrentHashMap<String, Set<String>>();
		List<Future<?>> futures = new ArrayList<Future<?>>();
		for (String resourceURI : resourceURIs) {
			ResourceTextWorker w = new ResourceTextWorker(resourceURI, relationMap);
			futures.add(this.submit(w));
		}
		
		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("thread execution somehow failed!");
				if (log.isDebugEnabled())
					log.debug("thread execution exception", e);
			}
		}
		
		this.sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, relationMap);
	}
	// endregion

	// region accessing the buffer
	/**
	 * Retrieves the values from the internal buffer, given the name of the
	 * key-value relation and the key.
	 */
	public Set<String> getBufferedValues(String relationName, String key) {

		if (!this.sparqlCache.containsKey(relationName)) {
			if (log.isErrorEnabled())
				log.error(String.format("unknown relationName '%s'. Returning empty set.", relationName));
			return new HashSet<String>();
		}
		ConcurrentMap<String, Set<String>> relationMap = this.sparqlCache.get(relationName);
		if (!relationMap.containsKey(key)) {
			if (log.isWarnEnabled())
				log.warn(String.format("relation '%s' does not contain key '%s'. Returning empty set.", relationName, key));
			return new HashSet<String>();
		}
		return relationMap.get(key);
	}

	/**
	 * Retrieves the first value from the internal buffer. Returns empty String in case of empty list.
	 */
	public String getFirstBufferedValue(String relationName, String key) {
		Set<String> result = getBufferedValues(relationName, key);
		for (String s : result)
			return s;
		return ""; // return empty string in case of empty set
	}
	
	/**
	 * Retrieves all the values associated with the given relation. Used for
	 * stacked bulk queries.
	 */
	public Set<String> getAllRelationValues(String relationName) {
		if (!this.sparqlCache.containsKey(relationName)) {
			if (log.isErrorEnabled())
				log.error(String.format("unknown relationName '%s'. Returning empty set.", relationName));
			return new HashSet<String>();
		}

		Collection<Set<String>> allSets = this.sparqlCache.get(relationName).values();
		Set<String> result = new HashSet<String>();
		for (Set<String> set : allSets)
			result.addAll(set);
		return result;
	}
	// endregion

	// region String query
	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (using the
	 * given timeout in milliseconds). Returns the retrieved results as Strings.
	 * Don't report empty results as errors if flag is set.
	 */
	public List<String> runSingleVariableStringQuery(String sparqlQuery, String variableName, long timeoutMillisec, boolean isEmptyResultExpected) {
		if (log.isTraceEnabled())
			log.trace(String.format("runSingleVariableStringQuery(sparqlQuery = '%s', variableName = '%s', timeoutMillisec = %d)", sparqlQuery, variableName, timeoutMillisec));

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
					if (log.isErrorEnabled())
						log.error(String.format("Query execution failed. Query: '%s' Variable: '%s' Timeout: %d", sparqlQuery, variableName, timeoutMillisec));
					if (log.isDebugEnabled())
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
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with the
	 * standard timeout). Returns the retrieved results as Strings. Don't report
	 * empty results as errors if flag is set.
	 */
	public List<String> runSingleVariableStringQuery(String sparqlQuery, String variableName, boolean isEmptyResultExpected) {
		return runSingleVariableStringQuery(sparqlQuery, variableName, this.timeoutMsec, isEmptyResultExpected);
	}

	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with a
	 * standard timeout of 10 seconds). Returns the retrieved results as
	 * Strings. Does not expect empty results.
	 */
	public List<String> runSingleVariableStringQuery(String sparqlQuery, String variableName) {
		return runSingleVariableStringQuery(sparqlQuery, variableName, false);
	}

	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (using the
	 * given timeout in milliseconds). Returns the first retrieved result as
	 * String. Handles empty results by returning empty String.
	 */
	public String runSingleVariableStringQuerySingleResult(String sparqlQuery, String variableName, long timeoutMillisec) {
		List<String> results = runSingleVariableStringQuery(sparqlQuery, variableName, timeoutMillisec, true);
		if (results.size() > 0)
			return results.get(0);
		else
			return "";
	}

	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with the
	 * standard timeout). Returns the first retrieved result as String.
	 */
	public String runSingleVariableStringQuerySingleResult(String sparqlQuery, String variableName) {
		return runSingleVariableStringQuerySingleResult(sparqlQuery, variableName, this.timeoutMsec);
	}
	// endregion

	// region NewsEvent query
	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (using the
	 * given timeout in milliseconds). Returns the retrieved results as
	 * NewsEvents. Empty results are expected and handed over.
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
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with the
	 * standard timeout). Returns the retrieved results as NewsEvents.
	 */
	public List<NewsEvent> runSingleVariableEventQuery(String sparqlQuery, String variableName) {
		return runSingleVariableEventQuery(sparqlQuery, variableName, this.timeoutMsec);
	}

	// endregion

	// region Double query
	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (using the
	 * given timeout in milliseconds). Returns the retrieved results as Doubles.
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
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with the
	 * standard timeout). Returns the retrieved results as Doubles. Empty
	 * results are expected and passed on.
	 */
	public List<Double> runSingleVariableDoubleQuery(String sparqlQuery, String variableName) {
		return runSingleVariableDoubleQuery(sparqlQuery, variableName, this.timeoutMsec);
	}

	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (using the
	 * given timeout in milliseconds). Returns the first retrieved result as
	 * Double. Empty results are expected and dealt with by returning Double.NaN
	 */
	public double runSingleVariableDoubleQuerySingleResult(String sparqlQuery, String variableName, long timeoutMillisec) {
		List<Double> results = runSingleVariableDoubleQuery(sparqlQuery, variableName, timeoutMillisec);
		if (results.size() > 0)
			return results.get(0);
		else
			return Double.NaN;
	}

	/**
	 * Sends the given sparqlQuery to the KnowledgeStore instance (with the
	 * standard timeout). Returns the first retrieved result as Double. Empty
	 * results are expected and dealt with by returning Double.NaN
	 */
	public double runSingleVariableDoubleQuerySingleResult(String sparqlQuery, String variableName) {
		return runSingleVariableDoubleQuerySingleResult(sparqlQuery, variableName, this.timeoutMsec);
	}
	// endregion

	// region retrieve text
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
				if (log.isErrorEnabled())
					log.error(String.format("Could not retrieve resource, returning empty String. URI: '%s'", resourceURI));
				if (log.isDebugEnabled())
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

		Set<String> mentionURIs = getBufferedValues(Util.RELATION_NAME_EVENT_MENTION, eventURI);
		
		for (String mentionURI : mentionURIs) {
			String resourceURI = Util.resourceURIFromMentionURI(mentionURI);
			String originalText = getFirstBufferedValue(Util.RELATION_NAME_RESOURCE_TEXT, resourceURI);
//					getOriginalText(resourceURI);
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
	 * Returns the phrase the mention is referring to from the original text. If
	 * wholeSentence is set to true, will expand phrase to whole sentence.
	 */
	public String retrievePhraseFromMention(String mentionURI, boolean wholeSentence) {

		KSMention mention = retrieveKSMentionFromMentionURI(mentionURI, wholeSentence);

		// get original text
		String originalText = getFirstBufferedValue(Util.RELATION_NAME_RESOURCE_TEXT, mention.getResourceURI());
//				getOriginalText(mention.getResourceURI());
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
	 * Returns the phrases of the mentions of the given entity. If wholeSentence
	 * is set, returns whole sentences.
	 */
	public List<String> retrievePhrasesFromEntity(String entityURI, boolean wholeSentence) {
		List<String> result = new ArrayList<String>();

		Set<String> mentions = getBufferedValues(Util.RELATION_NAME_EVENT_MENTION, entityURI);
		if (mentions.isEmpty())
			mentions = getBufferedValues(Util.RELATION_NAME_CONSTITUENT_MENTION + getMentionFromEventTemplateName, entityURI);

		if (mentions.isEmpty()) {
			if (log.isErrorEnabled())
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
	 * Converts the given mentionURI into a KSMention object; if wholeSentence
	 * is set, expands the mention to a complete sentence.
	 */
	public KSMention retrieveKSMentionFromMentionURI(String mentionURI, boolean wholeSentence) {
		String resourceURI = mentionURI.substring(0, mentionURI.indexOf("#"));
		int startIdx = Integer.parseInt(mentionURI.substring(mentionURI.indexOf("=") + 1, mentionURI.indexOf(",", mentionURI.indexOf("="))));
		int endIdx = Integer.parseInt(mentionURI.substring(mentionURI.indexOf(",", mentionURI.indexOf("=")) + 1));

		if (wholeSentence) {
			if (this.sentenceMentionCache.containsKey(mentionURI))
				return this.sentenceMentionCache.get(mentionURI); // grab from cache if possible;
			
			// search for sentence boundaries using a very simple heuristic
			String originalText = getFirstBufferedValue(Util.RELATION_NAME_RESOURCE_TEXT, resourceURI);
					//getOriginalText(resourceURI);
			if (originalText.isEmpty()) {
				if (log.isWarnEnabled())
					log.warn(String.format("empty original text, cannot find sentence boundaries for mention '%s'", mentionURI));
				return new KSMention(mentionURI);
			}
			List<Character> sentenceDelimiters = Arrays.asList('.', '!', '?');
			List<Character> skipChars = Arrays.asList(' ', '\n', '\t');
			while ((startIdx > 0) && (!sentenceDelimiters.contains(originalText.charAt(startIdx - 1))))
				startIdx--;
			while (skipChars.contains(originalText.charAt(startIdx)))
				startIdx++;

			while ((endIdx < originalText.length()) && (!sentenceDelimiters.contains(originalText.charAt(endIdx - 1))))
				endIdx++;
			
			this.sentenceMentionCache.putIfAbsent(mentionURI, new KSMention(resourceURI, startIdx, endIdx));
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
	 * Given the eventURI, picks the first mention and extracts the surrounding
	 * sentence from the original resource.
	 */
	public String retrieveSentencefromEvent(String eventURI) {
		List<String> results = retrieveSentencesfromEvent(eventURI);
		if (results.isEmpty())
			return "";
		else
			return results.get(0);
	}

	/**
	 * Given the eventURI, returns a list of all sentences mentioning this
	 * event.
	 */
	public List<String> retrieveSentencesfromEvent(String eventURI) {
		return retrievePhrasesFromEntity(eventURI, true);
	}
	// endregion

	// region handling mentions

	/**
	 * Retrieves the given property for the given mention within the standard
	 * timeout and returns it as list of Strings.
	 */
	public List<String> getMentionProperty(String mentionURI, String propertyURI) {
		return getMentionProperty(mentionURI, propertyURI, this.timeoutMsec);
	}

	/**
	 * Retrieves the given property for the given mention within the given
	 * timeout and returns it as list of Strings.
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
	 * Retrieves the given property for the given mention within the standard
	 * timeout and returns the first result as String.
	 */
	public String getUniqueMentionProperty(String mentionURI, String propertyURI) {
		return getUniqueMentionProperty(mentionURI, propertyURI, this.timeoutMsec);
	}

	/**
	 * Retrieves the given property for the given mention within the given
	 * timeout and returns the first result as String.
	 */
	public String getUniqueMentionProperty(String mentionURI, String propertyURI, long timeoutMillisec) {
		List<String> results = getMentionProperty(mentionURI, propertyURI, timeoutMillisec);
		if (results.size() > 0)
			return results.get(0);
		else
			return "";
	}

	private class EventMentionWorker implements Runnable {

		private Set<URI> uriSet;
		
		public EventMentionWorker(Set<URI> uriSet) {
			this.uriSet = uriSet;
		}
		
		@Override
		public void run() {
			Session session = knowledgeStore.newSession();
			try {
				Stream<Record> stream = session.retrieve(KS.RESOURCE).ids(uriSet).timeout((long) timeoutMsec).exec();
				List<Record> records = stream.toList();
				stream.close();
				
				for (Record r : records) {
					String key = r.getID().toString();
					List<String> stringValues = r.get(new URIImpl("http://dkm.fbk.eu/ontologies/knowledgestore#hasMention"), String.class);
					Set<KSMention> values = new HashSet<KSMention>();
					for (String s : stringValues)
						values.add(new KSMention(s));
					eventMentionCache.putIfAbsent(key, values);
				}
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("Cannot retrieve mentions from resources.");
				if (log.isDebugEnabled())
					log.debug("Cannot retrieve mentions from resources.", e);
			}
			session.close();
		}
		
	}
	
	/**
	 * Store all mentions of events mentioned in the given resourceURIs in the internal cache.
	 */
	public void retrieveAllEventMentions(Set<String> resourceURIs) {
		
		List<Set<URI>> queryURISets = new ArrayList<Set<URI>>();
		Set<URI> currentSet = new HashSet<URI>();
		int currentLength = 0;
		for (String resourceURI : resourceURIs) {
			if (currentLength + resourceURI.length() > 6000) {
				queryURISets.add(currentSet);
				currentSet = new HashSet<URI>();
				currentLength = 0;
			}
			currentSet.add(new URIImpl(resourceURI));
			currentLength += resourceURI.length();
		}
		queryURISets.add(currentSet);

		Session session = knowledgeStore.newSession();
		List<Future<?>> futures = new ArrayList<Future<?>>();
		for (Set<URI> uriSet : queryURISets) {
			EventMentionWorker w = new EventMentionWorker(uriSet);
			futures.add(this.submit(w));
		}
		
		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("thread execution somehow failed!");
				if (log.isDebugEnabled())
					log.debug("thread execution exception", e);
			} 
		}
		
		session.close();
	}
	
	/**
	 * Takes the given resourceURI and returns all mentions of this resource
	 * that link to an event.
	 */
	public Set<KSMention> getAllEventMentions(String resourceURI) {

		if (this.eventMentionCache.containsKey(resourceURI))
			return this.eventMentionCache.get(resourceURI);
		return new HashSet<KSMention>();
	}

	// endregion
}
