package edu.kit.anthropomatik.isl.newsTeller.knowledgeStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.KSMention;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
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

	private static final int MAXIMUM_QUERY_LENGTH = 6000;

	private String serverURL;

	private int timeoutMsec;

	private int maxNumberOfConnections;

	private int maximumQueryLength = MAXIMUM_QUERY_LENGTH;

	private boolean useStanford;

	private boolean isConnectionOpen = false;

	private KnowledgeStore knowledgeStore;

	private String getMentionFromEventTemplate;

	private String getEventFromMentionTemplate;

	private String getMentionFromEntityTemplate;

	private ConcurrentMap<String, Set<KSMention>> eventMentionCache;

	private ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache; // relationship-id --> key --> values

	private ConcurrentMap<String, KSMention> sentenceMentionCache;

	private ConcurrentMap<String, ConcurrentMap<String, Set<KSMention>>> entityMentionCache;
	
	private ConcurrentMap<String, List<Sentence>> stanfordSentenceCache;

	private ConcurrentMap<String, List<String>> documentTokenCache;

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

	public void setUseStanford(boolean useStanford) {
		this.useStanford = useStanford;
	}

	public void setMaximumQueryLength(int maximumQueryLength) {
		this.maximumQueryLength = maximumQueryLength;
	}

	public boolean isConnectionOpen() {
		return isConnectionOpen;
	}

	public KnowledgeStoreAdapter(String getMentionFromEventFileName, String getEventFromMentionFileName, String getMentionFromEntityFileName) {
		this.getMentionFromEventTemplate = Util.readStringFromFile(getMentionFromEventFileName);
		this.getEventFromMentionTemplate = Util.readStringFromFile(getEventFromMentionFileName);
		this.getMentionFromEntityTemplate = Util.readStringFromFile(getMentionFromEntityFileName);
		this.eventMentionCache = new ConcurrentHashMap<String, Set<KSMention>>();
		this.sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
		this.sentenceMentionCache = new ConcurrentHashMap<String, KSMention>();
		this.entityMentionCache = new ConcurrentHashMap<String, ConcurrentMap<String,Set<KSMention>>>();
		this.stanfordSentenceCache = new ConcurrentHashMap<String, List<Sentence>>();
		this.documentTokenCache = new ConcurrentHashMap<String, List<String>>();
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
			int extendedTimeout = (int) (1.2 * timeoutMsec);
			this.knowledgeStore = Client.builder(serverURL).compressionEnabled(true).maxConnections(maxNumberOfConnections).validateServer(false).connectionTimeout(extendedTimeout).build();
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
		this.entityMentionCache.clear();
		this.stanfordSentenceCache.clear();
		this.documentTokenCache.clear();
	}

	/**
	 * Submit the given task to the internal threadpool.
	 */
	public Future<?> submit(Runnable task) {
		return this.threadPool.submit(task);
	}

	public <T> Future<T> submit(Callable<T> task) {
		return this.threadPool.submit(task);
	}
	
	/**
	 * Used for unit tests to provide some cached values without running costly
	 * bulk-queries. Copies the given contents into the local cache.
	 */
	public void manuallyFillCaches(ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache, ConcurrentMap<String, Set<KSMention>> eventMentionCache,
									ConcurrentMap<String, ConcurrentMap<String, Set<KSMention>>> entityMentionCache) {
		this.sparqlCache.putAll(sparqlCache);
		this.eventMentionCache.putAll(eventMentionCache);
		this.entityMentionCache.putAll(entityMentionCache);
	}

	/**
	 * Removes the given eventURI from the internal buffers.
	 */
	public void removeEvent(String eventURI) {
		for (ConcurrentMap<String, Set<String>> map : this.sparqlCache.values())
			map.remove(eventURI);
	}

	// region filling the buffer

	// region sparql
	// takes care of the individual bulk queries
	private class KeyValueWorker implements Runnable {

		private String query;
		private String keyVariable;
		private List<String> valueVariables;
		private ConcurrentMap<String, ConcurrentMap<String, Set<String>>> relationMaps;
		private Keyword keyword;

		public KeyValueWorker(String query, String keyVariable, List<String> valueVariables, Keyword keyword, ConcurrentMap<String, ConcurrentMap<String, Set<String>>> relationMaps) {
			this.query = query;
			this.keyVariable = keyVariable;
			this.valueVariables = valueVariables;
			this.keyword = keyword;
			this.relationMaps = relationMaps;
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
					String key = tuple.getValue(keyVariable).toString();
					for (String valueVariable : valueVariables) {
						if (Thread.interrupted())
							throw new InterruptedException("thread was killed");
						
						ConcurrentMap<String, Set<String>> relationMap = relationMaps.get(Util.getRelationName(keyVariable, valueVariable, keyword.getWord()));
						Set<String> values = relationMap.containsKey(key) ? relationMap.get(key) : new HashSet<String>();

						if (tuple.hasBinding(valueVariable)) { // ignore the
																// variable if
																// there is no
																// binding -
																// i.e. store an
																// empty set
							String value = tuple.getValue(valueVariable).toString();
							if (value.startsWith("\""))
								value = value.substring(1, value.lastIndexOf('"'));
							values.add(value);
						}

						relationMap.put(key, values);
					}
				}
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error(String.format("Query execution failed. Query: '%s'", query));
				if (log.isDebugEnabled())
					log.debug("Query execution exception", e);
			}
		}

	}

	/**
	 * Store all mentions of the given events in the internal map.
	 */
	public void runKeyValueMentionFromEventQuery(Set<String> eventURIs, List<Keyword> keywords) {
		runKeyValueSparqlQuery(getMentionFromEventTemplate, eventURIs, keywords);
	}

	/**
	 * Runs a key-value SPARQL query. Inserts the keyValues into the template
	 * (for each keyword), fires the query and stores the resulting key-value
	 * pairs in the internal cache.
	 */
	public void runKeyValueSparqlQuery(String sparqlQueryTemplate, Set<String> keyValues, List<Keyword> keywords) {
		// region sanity check
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
		// endregion

		// automatically extract the variables from the query header
		String queryHeader = sparqlQueryTemplate.substring(0, sparqlQueryTemplate.indexOf("WHERE"));
		Pattern variablePattern = Pattern.compile("([a-z]|SELECT)\\s\\?(\\w*)\\s");
		Pattern variablePatternAS = Pattern.compile("AS\\s\\?(\\w*)\\)");
		Matcher matcher = variablePattern.matcher(queryHeader);
		Matcher matcherAS = variablePatternAS.matcher(queryHeader);
		String keyVariable = null;
		List<String> valueVariables = new ArrayList<String>();
		if (matcher.find()) {
			do {
				String variable = matcher.group(2);
				if (keyVariable == null)
					keyVariable = variable;
				else
					valueVariables.add(variable);
			} while (matcher.find(matcher.start(2)));
		}

		while (matcherAS.find())
			valueVariables.add(matcherAS.group(1));

		boolean doOnce = !sparqlQueryTemplate.contains(Util.PLACEHOLDER_KEYWORD);
		boolean first = true;
		// TODO: parallelize for multiple keywords if necessary
		for (Keyword keyword : keywords) {

			if (doOnce && !first) {
				// if we only need to run the queries once: just copy results of
				// first run for all other keywords
				ConcurrentMap<String, ConcurrentMap<String, Set<String>>> relationMaps = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
				for (String valueVariable : valueVariables) {
					String oldRelationName = Util.getRelationName(keyVariable, valueVariable, keywords.get(0).getWord());
					String newRelationName = Util.getRelationName(keyVariable, valueVariable, keyword.getWord());
					relationMaps.putIfAbsent(newRelationName, this.sparqlCache.get(oldRelationName));
				}
				this.sparqlCache.putAll(relationMaps);
			} else {
				String queryWithKeyword = sparqlQueryTemplate.replace(Util.PLACEHOLDER_KEYWORD, keyword.getStemmedRegex());

				ConcurrentMap<String, ConcurrentMap<String, Set<String>>> relationMaps = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
				for (String valueVariable : valueVariables)
					relationMaps.putIfAbsent(Util.getRelationName(keyVariable, valueVariable, keyword.getWord()), new ConcurrentHashMap<String, Set<String>>());

				List<String> queries = new ArrayList<String>();
				StringBuilder sb = new StringBuilder();
				for (String uri : keyValues) {
					String s = String.format("<%s> ", uri);
					if (sb.length() + s.length() + queryWithKeyword.length() > this.maximumQueryLength) {
						queries.add(queryWithKeyword.replace(Util.PLACEHOLDER_KEYS, sb.toString().trim()));
						sb = new StringBuilder();
					}
					sb.append(s);
				}
				queries.add(queryWithKeyword.replace(Util.PLACEHOLDER_KEYS, sb.toString().trim()));

				List<Future<?>> futures = new ArrayList<Future<?>>();

				for (String query : queries) {
					KeyValueWorker w = new KeyValueWorker(query, keyVariable, valueVariables, keyword, relationMaps);
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
						f.cancel(true);
					}
				}

				this.sparqlCache.putAll(relationMaps);

				first = false;
			}
		}
	}
	// endregion
	
	// region entity-mention query
	private class EntityMentionWorker implements Runnable {

		private String query;
		private Set<String> resourceURIs;
		private ConcurrentMap<String, ConcurrentMap<String, Set<KSMention>>> resultMap;
		
		public EntityMentionWorker(String query, Set<String> resourceURIs, ConcurrentMap<String, ConcurrentMap<String, Set<KSMention>>> resultMap) {
			this.query = query;
			this.resourceURIs = resourceURIs;
			this.resultMap = resultMap;
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
					if (Thread.interrupted())
						throw new InterruptedException("thread was killed");
					
					String entityURI = tuple.getValue("entity").toString();
					ConcurrentMap<String, Set<KSMention>> entityMap = resultMap.get(entityURI);

					if (tuple.hasBinding("mention")) {
						String mentionURI = tuple.getValue("mention").toString();
						String resourceURI = Util.resourceURIFromMentionURI(mentionURI);
						if (!resourceURIs.contains(resourceURI))
							continue; // ignore this mention - it's from an irrelevant text
						Set<KSMention> ksMentions = entityMap.containsKey(resourceURI) ? entityMap.get(resourceURI) : new HashSet<KSMention>();
						ksMentions.add(new KSMention(mentionURI));
						entityMap.put(resourceURI, ksMentions);
						resultMap.put(entityURI, entityMap);
					}
				}
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error(String.format("Query execution failed. Query: '%s'", query));
				if (log.isDebugEnabled())
					log.debug("Query execution exception", e);
			}
		}
	}
	
	public void runKeyValueEntityMentionQuery(Set<String> entityURIs, Set<String> resourceURIs) {
		// region sanity check
		if (!isConnectionOpen) {
			if (log.isWarnEnabled())
				log.warn("Trying to access KnowledgeStore without having an open connection. Request ignored.");
			return;
		}
		if (resourceURIs.isEmpty()) {
			if (log.isWarnEnabled())
				log.warn("Empty set of resourceURIs. Request ignored.");
			return;
		}
		if (entityURIs.isEmpty()) {
			if (log.isWarnEnabled())
				log.warn("Empty set of keyValues. Request ignored.");
			return;
		}
		// endregion
	
		List<String> queries = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		for (String uri : entityURIs) {
			String s = String.format("<%s> ", uri);
			if (sb.length() + s.length() + this.getMentionFromEntityTemplate.length() > this.maximumQueryLength) {
				queries.add(this.getMentionFromEntityTemplate.replace(Util.PLACEHOLDER_KEYS, sb.toString().trim()));
				sb = new StringBuilder();
			}
			sb.append(s);
		}
		queries.add(this.getMentionFromEntityTemplate.replace(Util.PLACEHOLDER_KEYS, sb.toString().trim()));

		ConcurrentMap<String, ConcurrentMap<String, Set<KSMention>>> resultMap = new ConcurrentHashMap<String, ConcurrentMap<String,Set<KSMention>>>();
		for (String entityURI : entityURIs)
			resultMap.putIfAbsent(entityURI, new ConcurrentHashMap<String, Set<KSMention>>());
		
		List<Future<?>> futures = new ArrayList<Future<?>>();
		
		for (String query : queries) {
			EntityMentionWorker w = new EntityMentionWorker(query, resourceURIs, resultMap);
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
				f.cancel(true);
			}
		}
		
		// now filter them right away
		for (String entityURI : resultMap.keySet()) { // iterate over entities
			ConcurrentMap<String, Set<KSMention>> entityMap = resultMap.get(entityURI);
			for (String resourceURI : entityMap.keySet()) { // iterate over resources
				Set<KSMention> oldMentions = entityMap.get(resourceURI);
				Set<KSMention> newMentions = new HashSet<KSMention>();
				
				for (KSMention candidate : oldMentions) {
					boolean keep = true;
					for (KSMention other : oldMentions) {
						if (other.contains(candidate) && !other.equals(candidate)) {
							keep = false;
							break;
						}
					}
					if (keep)
						newMentions.add(candidate);
				}
				entityMap.put(resourceURI, newMentions);
			}
		}

		this.entityMentionCache.putAll(resultMap);
	}
	// endregion

	// region property query in general
	private class PropertyWorker implements Runnable {

		private Set<URI> uriSet;
		private Set<String> propertyURIs;
		private ConcurrentMap<String, ConcurrentMap<String, Set<String>>> propertyMap;
		private URI resourceType;

		public PropertyWorker(Set<URI> uriSet, Set<String> propertyURIs, ConcurrentMap<String, ConcurrentMap<String, Set<String>>> propertyMap, URI resourceType) {
			this.uriSet = uriSet;
			this.propertyURIs = propertyURIs;
			this.propertyMap = propertyMap;
			this.resourceType = resourceType;
		}

		@Override
		public void run() {
			try {
				Session session = knowledgeStore.newSession();
				Stream<Record> stream = session.retrieve(this.resourceType).ids(uriSet).timeout(10000L).exec();
				List<Record> records = stream.toList();
				stream.close();

				for (Record r : records) {
					String key = r.getID().toString();
					for (String propertyURI : propertyURIs) {
						if (Thread.interrupted())
							throw new InterruptedException("thread was killed");
						
						Set<String> values = new HashSet<String>(r.get(new URIImpl(propertyURI), String.class));
						propertyMap.get(propertyURI).put(key, values);
					}

				}
				session.close();
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error(String.format("Mention property access failed. Property: '%s'", StringUtils.collectionToCommaDelimitedString(propertyURIs)));
				if (log.isDebugEnabled())
					log.debug("Mention property access exception", e);
			}
		}

	}

	// internal worker for any kind of property query (both resource and mention
	// layer)
	private void runKeyValuePropertyQuery(Set<String> propertyURIs, String relationName, Set<String> resourceURIs, URI resourceType) {
		if (!isConnectionOpen) {
			if (log.isWarnEnabled())
				log.warn("Trying to access KnowledgeStore without having an open connection. Request ignored.");
			return;
		}
		if (resourceURIs.isEmpty()) {
			if (log.isWarnEnabled())
				log.warn("Empty set of mentionURIs. Request ignored.");
			return;
		}

		ConcurrentMap<String, ConcurrentMap<String, Set<String>>> propertyMap = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();

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
		for (String mentionURI : resourceURIs) {
			if (currentLength + mentionURI.length() > this.maximumQueryLength) {
				queryURISets.add(currentSet);
				currentSet = new HashSet<URI>();
				currentLength = 0;
			}
			currentSet.add(new URIImpl(mentionURI));
			currentLength += mentionURI.length();
		}
		queryURISets.add(currentSet);

		List<Future<?>> futures = new ArrayList<Future<?>>();

		for (Set<URI> uriSet : queryURISets) {
			PropertyWorker w = new PropertyWorker(uriSet, propertyURIs, propertyMap, resourceType);
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
				f.cancel(true);
			}
		}

		for (String propertyURI : propertyURIs) {
			this.sparqlCache.put(relationName + propertyURI, propertyMap.get(propertyURI));
		}

	}
	// endregion

	// region mention property
	/**
	 * Runs a key-value mentionProperty query for a set of properties. Stores
	 * the resulting mentionURI-propertyValue pairs in the internal cache.
	 */
	public void runKeyValueMentionPropertyQuery(Set<String> propertyURIs, String relationName, Set<String> mentionURIs) {
		runKeyValuePropertyQuery(propertyURIs, relationName, mentionURIs, KS.MENTION);
	}
	// endregion

	// region resource titles
	public void runKeyValueResourcePropertyQuery(Set<String> propertyURIs, Set<String> resourceURIs) {
		runKeyValuePropertyQuery(propertyURIs, Util.RELATION_NAME_RESOURCE_PROPERTY, resourceURIs, KS.RESOURCE);
	}
	// endregion

	// region resource text

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
				f.cancel(true);
			}
		}

		this.sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, relationMap);
	}
	// endregion

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
			if (log.isTraceEnabled())
				log.trace(String.format("relation '%s' does not contain key '%s'. Returning empty set.", relationName, key));
			return new HashSet<String>();
		}
		return relationMap.get(key);
	}

	/**
	 * Retrieves the first value from the internal buffer. Returns empty String
	 * in case of empty list.
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

	/**
	 * Returns all titles of the resources in which the given event was
	 * mentioned.
	 */
	public Set<String> getResourceTitlesFromEvent(String eventURI, String dummyKeyword) {
		Set<String> mentionURIs = getBufferedValues(Util.getRelationName("event", "mention", dummyKeyword), eventURI);
		Set<String> resourceURIs = Util.resourceURIsFromMentionURIs(mentionURIs);
		Set<String> titles = new HashSet<String>();

		for (String resourceURI : resourceURIs)
			titles.addAll(getBufferedValues(Util.RELATION_NAME_RESOURCE_PROPERTY + Util.RESOURCE_PROPERTY_TITLE, resourceURI));

		return titles;
	}
	
	public Set<KSMention> getEntityMentions(String entityURI, String resourceURI) {
		
		Set<KSMention> result = new HashSet<KSMention>();
		ConcurrentMap<String, Set<KSMention>> eventMap = this.entityMentionCache.get(entityURI);
		if (eventMap == null)
			return result;
		if (eventMap.containsKey(resourceURI))
			result = eventMap.get(resourceURI);
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

	/**
	 * Returns a list of all news stories in which the given event is mentioned.
	 */
	public Set<String> retrieveOriginalTexts(String eventURI, String dummyKeyword) {
		Set<String> originalTexts = new HashSet<String>();

		Set<String> mentionURIs = getBufferedValues(Util.getRelationName("event", "mention", dummyKeyword), eventURI);

		for (String mentionURI : mentionURIs) {
			String resourceURI = Util.resourceURIFromMentionURI(mentionURI);
			String originalText = getFirstBufferedValue(Util.RELATION_NAME_RESOURCE_TEXT, resourceURI);

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
		if (originalText.isEmpty())
			return "";

		return originalText.substring(mention.getStartIdx(), mention.getEndIdx());
	}

	/**
	 * Returns the phrases of the mentions of the given entity.
	 */
	public List<String> retrievePhrasesFromEntity(String entityURI, String dummyKeyword) {
		return retrievePhrasesFromEntity(entityURI, false, dummyKeyword);
	}

	/**
	 * Returns the phrases of the mentions of the given entity. If wholeSentence
	 * is set, returns whole sentences.
	 */
	public List<String> retrievePhrasesFromEntity(String entityURI, boolean wholeSentence, String dummyKeyword) {
		List<String> result = new ArrayList<String>();

		Set<String> mentions = getBufferedValues(Util.getRelationName("event", "mention", dummyKeyword), entityURI);
		if (mentions.isEmpty())
			mentions = getBufferedValues(Util.getRelationName("entity", "mention", dummyKeyword), entityURI);

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
				return this.sentenceMentionCache.get(mentionURI); // grab from
																	// cache if
																	// possible;

			// search for sentence boundaries using a very simple heuristic
			String originalText = getFirstBufferedValue(Util.RELATION_NAME_RESOURCE_TEXT, resourceURI);
			// getOriginalText(resourceURI);
			if (originalText.isEmpty()) {
				if (log.isWarnEnabled())
					log.warn(String.format("empty original text, cannot find sentence boundaries for mention '%s'", mentionURI));
				return new KSMention(mentionURI);
			}
			List<Character> sentenceDelimiters = Arrays.asList('.', '!', '?', '|');
			List<Character> skipChars = Arrays.asList(' ', '\n', '\t', '\"', ']', '\'', '|');
			while ((startIdx > 0) && (!sentenceDelimiters.contains(originalText.charAt(startIdx - 1))))
				startIdx--;
			while (skipChars.contains(originalText.charAt(startIdx)))
				startIdx++;

			while ((endIdx < originalText.length()) && (!sentenceDelimiters.contains(originalText.charAt(endIdx - 1))))
				endIdx++;

			if (this.useStanford) {
				String guessedSentence = originalText.substring(startIdx, endIdx);
				Document doc = new Document(originalText);
				List<Sentence> sentences;
				if (this.stanfordSentenceCache.containsKey(resourceURI))
					sentences = this.stanfordSentenceCache.get(resourceURI);
				else {
					sentences = doc.sentences();
					this.stanfordSentenceCache.putIfAbsent(resourceURI, sentences);
				}
				for (Sentence sent : sentences) {
					if (sent.text().toLowerCase().contains(guessedSentence.toLowerCase())) {
						startIdx = originalText.indexOf(sent.text());
						endIdx = startIdx + sent.text().length();
						break;
					}
				}
			}

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
	public String retrieveSentencefromEvent(String eventURI, String dummyKeyword) {
		List<String> results = retrieveSentencesFromEvent(eventURI, dummyKeyword);
		if (results.isEmpty())
			return "";
		else
			return results.get(0);
	}

	/**
	 * Given the eventURI, returns a list of all sentences mentioning this
	 * event.
	 */
	public List<String> retrieveSentencesFromEvent(String eventURI, String dummyKeyword) {
		return retrievePhrasesFromEntity(eventURI, true, dummyKeyword);
	}

	/**
	 * Gets ALL sentences retrieved in the current query. Important: assumes
	 * that all caches get regularly flushed!!!
	 */
	public Set<List<String>> getAllQuerySentenceTokens(String dummyKeyword) {
		Set<List<String>> result = new HashSet<List<String>>();

		Set<String> allEvents = sparqlCache.get(Util.getRelationName("event", "mention", dummyKeyword)).keySet();

		for (String eventURI : allEvents)
			result.addAll(retrieveSentenceTokensFromEvent(eventURI, dummyKeyword));

		return result;
	}

	/**
	 * Gets ALL texts retrieved in the current query. Important: assumes that
	 * all caches get regularly flushed!!!
	 */
	public Set<List<String>> getAllQueryTextTokens(String dummyKeyword) {
		Set<List<String>> result = new HashSet<List<String>>();

		Set<String> allEvents = sparqlCache.get(Util.getRelationName("event", "mention", dummyKeyword)).keySet();

		for (String eventURI : allEvents)
			result.addAll(retrieveOriginalTextTokens(eventURI, dummyKeyword));

		return result;
	}

	/**
	 * Returns a list, where for each event of the current query a set of all
	 * resourceURIs in which this event is mentioned is given.
	 */
	public List<Set<String>> getAllQueryResourceURIs(String dummyKeyword) {

		List<Set<String>> result = new ArrayList<Set<String>>();

		Set<String> allEvents = sparqlCache.get(Util.getRelationName("event", "mention", dummyKeyword)).keySet();
		for (String eventURI : allEvents)
			result.add(Util.resourceURIsFromMentionURIs(getBufferedValues(Util.getRelationName("event", "mention", dummyKeyword), eventURI)));

		return result;
	}

	/**
	 * Returns a list, where for each event of the current query a list of all
	 * mentioning sentences is given.
	 */
	public List<Set<String>> getAllQuerySentences(String dummyKeyword) {
		List<Set<String>> result = new ArrayList<Set<String>>();

		Set<String> allEvents = sparqlCache.get(Util.getRelationName("event", "mention", dummyKeyword)).keySet();
		for (String eventURI : allEvents)
			result.add(new HashSet<String>(retrieveSentencesFromEvent(eventURI, dummyKeyword)));

		return result;
	}

	public Set<List<String>> retrieveSentenceTokensFromEvent(String eventURI, String dummyKeyword) {

		Set<List<String>> result = new HashSet<List<String>>();

		Set<String> mentionURIs = getBufferedValues(Util.getRelationName("event", "mention", dummyKeyword), eventURI);
		for (String mentionURI : mentionURIs) {
			KSMention sentenceMention = retrieveKSMentionFromMentionURI(mentionURI, true);
			if (this.documentTokenCache.containsKey(sentenceMention.toString()))
				result.add(this.documentTokenCache.get(sentenceMention.toString()));
			else { // construct manually. TODO: move into bulk?
				String sentence = this.retrieveSentenceFromMention(mentionURI);
				if (sentence == null || sentence.isEmpty())
					continue;
				List<String> sentenceTokens = (new Sentence(sentence)).words();
				result.add(sentenceTokens);
				this.documentTokenCache.putIfAbsent(sentenceMention.toString(), sentenceTokens);
			}

		}

		return result;
	}

	public Set<List<String>> retrieveOriginalTextTokens(String eventURI, String dummyKeyword) {

		Set<List<String>> result = new HashSet<List<String>>();

		Set<String> mentionURIs = getBufferedValues(Util.getRelationName("event", "mention", dummyKeyword), eventURI);
		Set<String> resourceURIs = Util.resourceURIsFromMentionURIs(mentionURIs);
		for (String resourceURI : resourceURIs) {
			if (this.documentTokenCache.containsKey(resourceURI))
				result.add(this.documentTokenCache.get(resourceURI));
			else { // construct manually. TODO: move into bulk?
				String resourceText = getFirstBufferedValue(Util.RELATION_NAME_RESOURCE_TEXT, resourceURI);
				Document document = new Document(resourceText);
				List<String> textTokens = new ArrayList<String>();
				for (Sentence sentence : document.sentences())
					textTokens.addAll(sentence.words());
				result.add(textTokens);
				this.documentTokenCache.putIfAbsent(resourceURI, textTokens);
			}

		}

		return result;
	}

	public Set<List<String>> retrieveTitleTokensFromEvent(String eventURI, String dummyKeyword) {
		Set<List<String>> result = new HashSet<List<String>>();

		Set<String> titles = getResourceTitlesFromEvent(eventURI, dummyKeyword);

		for (String titleString : titles) {
			if (this.documentTokenCache.containsKey(titleString))
				result.add(this.documentTokenCache.get(titleString));
			else {
				Sentence sentence = new Sentence(titleString);
				List<String> titleTokens = sentence.words();
				result.add(titleTokens);
				this.documentTokenCache.putIfAbsent(titleString, titleTokens);
			}
		}

		return result;
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
			List<Keyword> keywords = new ArrayList<Keyword>();
			Keyword k = new Keyword("keyword");
			Util.stemKeyword(k);
			keywords.add(k);

			try {
				Session session = knowledgeStore.newSession();
				Stream<Record> stream = session.retrieve(KS.RESOURCE).ids(uriSet).timeout((long) timeoutMsec).exec();
				List<Record> records = stream.toList();
				stream.close();
				session.close();

				for (Record r : records) {
					if (Thread.interrupted())
						throw new InterruptedException("thread was killed");
					
					String key = r.getID().toString();
					Set<String> stringValues = new HashSet<String>(r.get(new URIImpl("http://dkm.fbk.eu/ontologies/knowledgestore#hasMention"), String.class));
					runKeyValueSparqlQuery(getEventFromMentionTemplate, stringValues, keywords);

					Set<KSMention> values = new HashSet<KSMention>();
					for (String s : stringValues) {
						if (!getBufferedValues(Util.getRelationName("mention", "event", "keyword"), s).isEmpty())
							values.add(new KSMention(s));
					}
					eventMentionCache.putIfAbsent(key, values);
				}
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("Cannot retrieve mentions from resources.");
				if (log.isDebugEnabled())
					log.debug("Cannot retrieve mentions from resources.", e);
			}
		}
	}

	/**
	 * Store all mentions of events mentioned in the given resourceURIs in the
	 * internal cache.
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
				f.cancel(true);
			}
		}
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
