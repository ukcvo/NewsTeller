package edu.kit.anthropomatik.isl.newsTeller.benchmark;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.BenchmarkEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.IEventFilter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.ParallelEventFilter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.SequentialEventFilter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features.UsabilityFeature;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.SequentialEventRanker;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.search.EventSearcher;
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import eu.fbk.knowledgestore.KnowledgeStore;
import eu.fbk.knowledgestore.Session;
import eu.fbk.knowledgestore.client.Client;
import eu.fbk.knowledgestore.data.Record;
import eu.fbk.knowledgestore.data.Stream;
import eu.fbk.knowledgestore.vocabulary.KS;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.XRFFLoader;

/**
 * Tests regarding different runtime behaviors.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class RuntimeTester {

	private static Log log = LogFactory.getLog(RuntimeTester.class);

	private boolean doKSAccessTests;

	private boolean doSparqlSearchTests;

	private boolean doSparqlFeatureTests;

	private boolean doSequentialSearcherTest;

	private boolean doParallelSearcherTest;

	private boolean doParallelSparqlTest;

	private boolean doSequentialFilterTest;

	private boolean doParallelFilterTest;

	private boolean doFeatureTest;

	private boolean doDownloadTest;

	private boolean doClassifierTest;

	private boolean doBulkSparqlTest;
	
	private boolean doBulkMentionTest;
	
	private boolean doEndToEndTest;
	
	private KnowledgeStoreAdapter ksAdapter;

	private Map<String, String> sparqlSearchQueries;

	private Map<String, String> sparqlFeatureQueries;

	private Set<String> stemmedKeywords;

	private Set<String> eventURIs;

	private Map<List<Keyword>, Set<NewsEvent>> keywordsToEventsMap;

	private EventSearcher sequentialSearcher;

	private EventSearcher parallelSearcher;

	private SequentialEventFilter sequentialFilter;

	private ParallelEventFilter parallelFilter;

	private SequentialEventRanker sequentialRanker;
	
	private Set<List<Keyword>> keywords;

	private int numberOfRepetitions;

	private List<UsabilityFeature> features;

	private int maxNumberOfEvents;

	private Instances dataSet;

	private Classifier classifier;

	// region setters
	public void setDoKSAccessTests(boolean doKSAccessTests) {
		this.doKSAccessTests = doKSAccessTests;
	}

	public void setDoSparqlSearchTests(boolean doSparqlSearchTests) {
		this.doSparqlSearchTests = doSparqlSearchTests;
	}

	public void setDoSparqlFeatureTests(boolean doSparqlFeatureTests) {
		this.doSparqlFeatureTests = doSparqlFeatureTests;
	}

	public void setDoSequentialSearcherTest(boolean doSequentialSearcherTest) {
		this.doSequentialSearcherTest = doSequentialSearcherTest;
	}

	public void setDoParallelSearcherTest(boolean doParallelSearcherTest) {
		this.doParallelSearcherTest = doParallelSearcherTest;
	}

	public void setDoParallelSparqlTest(boolean doParallelSparqlTest) {
		this.doParallelSparqlTest = doParallelSparqlTest;
	}

	public void setDoSequentialFilterTest(boolean doSequentialFilterTest) {
		this.doSequentialFilterTest = doSequentialFilterTest;
	}

	public void setDoParallelFilterTest(boolean doParallelFilterTest) {
		this.doParallelFilterTest = doParallelFilterTest;
	}

	public void setDoFeatureTest(boolean doFeatureTest) {
		this.doFeatureTest = doFeatureTest;
	}

	public void setDoDownloadTest(boolean doDownloadTest) {
		this.doDownloadTest = doDownloadTest;
	}

	public void setDoClassifierTest(boolean doClassifierTest) {
		this.doClassifierTest = doClassifierTest;
	}

	public void setDoBulkSparqlTest(boolean doBulkSparqlTest) {
		this.doBulkSparqlTest = doBulkSparqlTest;
	}
	
	public void setDoBulkMentionTest(boolean doBulkMentionTest) {
		this.doBulkMentionTest = doBulkMentionTest;
	}
	
	public void setDoEndToEndTest(boolean doEndToEndTest) {
		this.doEndToEndTest = doEndToEndTest;
	}
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}

	public void setSparqlSearchQueries(Set<String> fileNames) {
		this.sparqlSearchQueries = new HashMap<String, String>();
		for (String s : fileNames) {
			sparqlSearchQueries.put(s, Util.readStringFromFile(s));
		}
	}

	public void setSparqlFeatureQueries(Set<String> fileNames) {
		this.sparqlFeatureQueries = new HashMap<String, String>();
		for (String s : fileNames) {
			sparqlFeatureQueries.put(s, Util.readStringFromFile(s));
		}
	}

	public void setStemmedKeywords(Set<String> stemmedKeywords) {
		this.stemmedKeywords = stemmedKeywords;
	}

	public void setSequentialSearcher(EventSearcher sequentialSearcher) {
		this.sequentialSearcher = sequentialSearcher;
	}

	public void setParallelSearcher(EventSearcher parallelSearcher) {
		this.parallelSearcher = parallelSearcher;
	}

	public void setSequentialFilter(SequentialEventFilter sequentialFilter) {
		this.sequentialFilter = sequentialFilter;
	}

	public void setParallelFilter(ParallelEventFilter parallelFilter) {
		this.parallelFilter = parallelFilter;
	}

	public void setSequentialRanker(SequentialEventRanker sequentialRanker) {
		this.sequentialRanker = sequentialRanker;
	}
	
	public void setNumberOfRepetitions(int numberOfRepetitions) {
		this.numberOfRepetitions = numberOfRepetitions;
	}

	public void setFeatures(List<UsabilityFeature> features) {
		this.features = features;
	}

	public void setMaxNumberOfEvents(int maxNumberOfEvents) {
		this.maxNumberOfEvents = maxNumberOfEvents;
	}
	
	// endregion

	public RuntimeTester(String configFileName, String dataSetFileName, String classifierFileName) {

		// handle benchmark files
		Map<String, List<Keyword>> benchmarkConfig = Util.readBenchmarkConfigFile(configFileName);
		this.keywords = new HashSet<List<Keyword>>();
		for (List<Keyword> queryKeywords : benchmarkConfig.values()) {
			this.keywords.add(queryKeywords);
		}

		this.eventURIs = new HashSet<String>();
		this.keywordsToEventsMap = new HashMap<List<Keyword>, Set<NewsEvent>>();
		Set<String> fileNames = benchmarkConfig.keySet();
		for (String f : fileNames) {
			Set<BenchmarkEvent> fileEvents = Util.readBenchmarkQueryFromFile(f).keySet();
			Set<NewsEvent> events = new HashSet<NewsEvent>();
			for (BenchmarkEvent e : fileEvents) {
				this.eventURIs.add(e.getEventURI());
				events.add(new NewsEvent(e.getEventURI()));
			}
			this.keywordsToEventsMap.put(benchmarkConfig.get(f), events);
		}

		// read data set
		try {
			XRFFLoader loader = new XRFFLoader();
			loader.setSource(new File(dataSetFileName));
			this.dataSet = loader.getDataSet();
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("Can't read data set");
			if (log.isDebugEnabled())
				log.debug("Can't read data set", e);
		}

		// read classifier
		try {
			Object[] input = SerializationHelper.readAll(classifierFileName);
			this.classifier = (Classifier) input[0];
		} catch (Exception e) {
			if (log.isFatalEnabled())
				log.fatal(String.format("Can't read classifier from file: '%s'", classifierFileName));
			if (log.isDebugEnabled())
				log.debug("can't read classifier from file", e);
		}
	}

	// region ksAccessTests
	private void ksAccessTests() {
		long averageClientOpenTime = 0;
		long averageClientClosingTime = 0;
		long averageSessionOpenTime = 0;
		long averageSessionClosingTime = 0;

		for (int i = 0; i < this.numberOfRepetitions; i++) {
			long t1 = System.currentTimeMillis();
			KnowledgeStore ks = Client.builder("http://knowledgestore2.fbk.eu/nwr/wikinews").compressionEnabled(true).maxConnections(2).validateServer(false).connectionTimeout(10000).build();
			long t2 = System.currentTimeMillis();
			averageClientOpenTime += (t2 - t1);
			t1 = System.currentTimeMillis();
			ks.close();
			t2 = System.currentTimeMillis();
			averageClientClosingTime += (t2 - t1);
		}
		averageClientOpenTime /= this.numberOfRepetitions;
		averageClientClosingTime /= this.numberOfRepetitions;

		KnowledgeStore ks = Client.builder("http://knowledgestore2.fbk.eu/nwr/wikinews").compressionEnabled(true).maxConnections(2).validateServer(false).connectionTimeout(10000).build();

		for (int i = 0; i < this.numberOfRepetitions; i++) {
			long t1 = System.currentTimeMillis();
			Session s = ks.newSession();
			long t2 = System.currentTimeMillis();
			averageSessionOpenTime += (t2 - t1);
			t1 = System.currentTimeMillis();
			s.close();
			t2 = System.currentTimeMillis();
			averageSessionClosingTime += (t2 - t1);
		}
		averageSessionOpenTime /= this.numberOfRepetitions;
		averageSessionClosingTime /= this.numberOfRepetitions;
		ks.close();

		if (log.isInfoEnabled()) {
			log.info(String.format("average client opening time: %d ms", averageClientOpenTime));
			log.info(String.format("average client closing time: %d ms", averageClientClosingTime));
			log.info(String.format("average session opening time: %d ms", averageSessionOpenTime));
			log.info(String.format("average session closing time: %d ms", averageSessionClosingTime));
		}

	}
	// endregion

	// region sparqlSearchTests
	private void sparqlSearchTests() {

		for (Map.Entry<String, String> entry : sparqlSearchQueries.entrySet()) {
			String fileName = entry.getKey();
			String query = entry.getValue();
			long averageQueryTime = 0;
			for (int i = 0; i < this.numberOfRepetitions; i++) {
				if (log.isInfoEnabled())
					log.info(i);
				for (String keyword : this.stemmedKeywords) {
					String modifiedQuery = query.replace(Util.PLACEHOLDER_KEYWORD, keyword);
					long t1 = System.currentTimeMillis();
					ksAdapter.runSingleVariableStringQuery(modifiedQuery, Util.VARIABLE_EVENT);
					long t2 = System.currentTimeMillis();
					averageQueryTime += (t2 - t1);
				}
			}
			averageQueryTime /= this.numberOfRepetitions;
			averageQueryTime /= this.stemmedKeywords.size();

			if (log.isInfoEnabled())
				log.info(String.format("%s: %d ms", fileName, averageQueryTime));
		}

	}
	// endregion

	// region sparqlFeatureTests
	private void sparqlFeatureTests() {

		for (Map.Entry<String, String> entry : sparqlFeatureQueries.entrySet()) {
			String fileName = entry.getKey();
			String query = entry.getValue();
			long averageQueryTime = 0;
			for (String eventURI : this.eventURIs) {
				String modifiedQuery = query.replace(Util.PLACEHOLDER_EVENT, eventURI);
				long t1 = System.currentTimeMillis();
				ksAdapter.runSingleVariableStringQuery(modifiedQuery, Util.VARIABLE_NUMBER, true);
				long t2 = System.currentTimeMillis();
				averageQueryTime += (t2 - t1);
			}
			averageQueryTime /= this.eventURIs.size();

			if (log.isInfoEnabled())
				log.info(String.format("%s: %d ms", fileName, averageQueryTime));
		}

	}
	// endregion

	// region evaluateFinder
	private void evaluateSearcher(EventSearcher searcher, String searcherName) {
		long averageFindingTime = 0;
		DummyUserModel um = new DummyUserModel();
		for (int i = 0; i < this.numberOfRepetitions; i++) {
			for (List<Keyword> queryKeywords : this.keywords) {
				long t1 = System.currentTimeMillis();
				searcher.findEvents(queryKeywords, um);
				long t2 = System.currentTimeMillis();
				averageFindingTime += (t2 - t1);
			}
		}
		averageFindingTime /= this.numberOfRepetitions;
		averageFindingTime /= this.keywords.size();
		if (log.isInfoEnabled())
			log.info(String.format("%s: %d ms", searcherName, averageFindingTime));
	}
	// endregion

	// region parallelSparqlTest
	private class ParallelSparqlTestWorker implements Runnable {

		private KnowledgeStore ks;

		private String query;

		private String name;

		public ParallelSparqlTestWorker(KnowledgeStore ks, String query, String name) {
			this.ks = ks;
			this.query = query;
			this.name = name;
		}

		public void run() {
			Session s = ks.newSession();
			try {
				long t1 = System.currentTimeMillis();
				s.sparql(query).timeout(10000L).execTuples();
				long t2 = System.currentTimeMillis();
				log.info(String.format("%s - %s: %d", s.toString(), this.name, (t2 - t1)));
			} catch (Exception e) {
				e.printStackTrace();
			}
			s.close();
		}

	}

	private void parallelSparqlTest() {
		KnowledgeStore ks = Client.builder("http://knowledgestore2.fbk.eu/nwr/wikinews").compressionEnabled(true).maxConnections(10).validateServer(false).connectionTimeout(10000).build();
		ExecutorService threadPool = Executors.newFixedThreadPool(10);
		List<Future<?>> futures = new ArrayList<Future<?>>();

		for (Map.Entry<String, String> entry : sparqlSearchQueries.entrySet()) {
			futures.add(threadPool.submit(new ParallelSparqlTestWorker(ks, entry.getValue().replace(Util.PLACEHOLDER_KEYWORD, "Real Madrid"), entry.getKey())));
		}
		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		threadPool.shutdown();

		for (Map.Entry<String, String> entry : sparqlSearchQueries.entrySet()) {
			(new ParallelSparqlTestWorker(ks, entry.getValue().replace(Util.PLACEHOLDER_KEYWORD, "Real Madrid"), entry.getKey() + " seq")).run();
		}
		ks.close();
	}
	// endregion

	// region FilterTest
	private void testFilter(IEventFilter filter, String filterName, int maxNumberOfEvents) {

		if (maxNumberOfEvents == 0)
			maxNumberOfEvents = this.eventURIs.size();
		long totalTime = 0;
		long averageTimePerEvent = 0;
		int i = 0;
		long t1 = System.currentTimeMillis();
		for (Map.Entry<List<Keyword>, Set<NewsEvent>> entry : this.keywordsToEventsMap.entrySet()) {
			filter.filterEvents(entry.getValue(), entry.getKey(), new DummyUserModel());
			i += entry.getValue().size();
			if (i >= maxNumberOfEvents)
				break;
		}
		long t2 = System.currentTimeMillis();

		totalTime = t2 - t1;

		averageTimePerEvent = totalTime / maxNumberOfEvents;

		if (log.isInfoEnabled())
			log.info(String.format("%s - total: %d ms, per event: %d ms", filterName, totalTime, averageTimePerEvent));

	}
	// endregion

	// region featureTest
	private void featureTest() {

		for (UsabilityFeature feature : this.features) {
			long avgQueryTime = 0;
			int i = 0;
			for (Map.Entry<List<Keyword>, Set<NewsEvent>> entry : this.keywordsToEventsMap.entrySet()) {

				List<Keyword> keywords = entry.getKey();
				Set<NewsEvent> events = entry.getValue();

				for (NewsEvent event : events) {
					String uri = event.getEventURI();
					long t1 = System.currentTimeMillis();
					feature.getValue(uri, keywords);
					long t2 = System.currentTimeMillis();
					avgQueryTime += (t2 - t1);
					i++;
				}
				if (i >= 1000)
					break;
			}
			avgQueryTime /= i;

			if (log.isInfoEnabled())
				log.info(String.format("%s: %d ms", feature.getName(), avgQueryTime));
		}
	}
	// endregion

	// region ksDownloadTest
	private void ksDownloadTest() {

		try {
			KnowledgeStore ks = Client.builder("http://knowledgestore2.fbk.eu/nwr/wikinews").compressionEnabled(true).maxConnections(2).validateServer(false).connectionTimeout(10000).build();
			Session session = ks.newSession();

			List<URI> urisToCheck = new ArrayList<URI>();
			int j = 0;
			for (String uri : this.eventURIs) {
				urisToCheck.add(new URIImpl(uri.substring(0, uri.indexOf("#"))));
				j++;
				if (j >= 100)
					break;
			}
			// standard way
			long standardTime = System.currentTimeMillis();
			List<String> standardResult = new ArrayList<String>();
			for (URI uri : urisToCheck) {
				String s = session.download(uri).timeout(10000L).exec().writeToString();
				standardResult.add(s);
			}
			standardTime = System.currentTimeMillis() - standardTime;
			if (log.isInfoEnabled())
				log.info(String.format("individual via download: %d ms", standardTime));

			// batch way
			long batchTime = System.currentTimeMillis();
			List<String> batchResult = new ArrayList<String>();
			Stream<Record> stream = session.retrieve(KS.RESOURCE).ids(urisToCheck).timeout(10000L).exec();
			List<Record> records = stream.toList();
			stream.close();

			for (Record r : records) {
				String s = r.toString();
				batchResult.add(s);
			}
			batchTime = System.currentTimeMillis() - batchTime;
			if (log.isInfoEnabled()) {
				log.info(String.format("batch via retrieve: %d ms", batchTime));
				log.info(batchResult.get(0));
			}

		} catch (Exception e) {
			if (log.isInfoEnabled())
				log.info("error", e);
		}

	}
	// endregion

	// region classifierTest
	private void classifierTest() {

		try {
			Instances filtered = new Instances(this.dataSet, 0);
			long totalTime = System.currentTimeMillis();
			for (Instance instance : this.dataSet) {
				double label = this.classifier.classifyInstance(instance);
				boolean isUsable = (label == dataSet.attribute(Util.ATTRIBUTE_USABLE).indexOfValue(Util.LABEL_TRUE));
				if (isUsable)
					filtered.add(instance);
			}
			totalTime = System.currentTimeMillis() - totalTime;
			long perInstance = totalTime / this.dataSet.numInstances();
			if (log.isInfoEnabled()) 
				log.info(String.format("filtering: total %d ms, per event %d ms", totalTime, perInstance));
				
			
		} catch (Exception e) {
			log.error("exception", e);
		}
	}
	// endregion

	// region bulkSparqlTest
	private void bulkSparqlTest() {
		String sequentialQuery = "SELECT ?entity WHERE { <*e*> propbank:A1 ?entity}";
		String bulkQuery = "SELECT ?event ?entity WHERE { VALUES ?event { *keys* } . ?event propbank:A1 ?entity}";
		
		long sequentialTime = System.currentTimeMillis();
		for (String uri : this.eventURIs) {
			String query = sequentialQuery.replace("*e*", uri);
			ksAdapter.runSingleVariableStringQuery(query, Util.VARIABLE_ENTITY);
		}
		sequentialTime = System.currentTimeMillis() - sequentialTime;
		if (log.isInfoEnabled())
			log.info(String.format("sequential sparql: total %d ms, per event %d ms", sequentialTime, sequentialTime / this.eventURIs.size()));
		
		long bulkTimeManual = System.currentTimeMillis();
		int maxLength = 6700;
		List<String> queries = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		for (String uri : this.eventURIs) {
			String s = String.format("<%s> ", uri);
			if (sb.length() + s.length() + bulkQuery.length() > maxLength) {
				queries.add(bulkQuery.replace("*keys*", sb.toString().trim()));
				sb = new StringBuilder();
			}
			sb.append(s);
		}
		queries.add(bulkQuery.replace("*keys*", sb.toString().trim()));
		
		for (String query : queries)
			ksAdapter.runSingleVariableStringQuery(query, Util.VARIABLE_ENTITY);
		bulkTimeManual = System.currentTimeMillis() - bulkTimeManual;
		
		if (log.isInfoEnabled())
			log.info(String.format("manual bulk sparql (%d queries): total %d ms, per event %d ms", queries.size(), bulkTimeManual, bulkTimeManual / this.eventURIs.size()));
		
		long bulkTimeKSA = System.currentTimeMillis();
		//ksAdapter.runKeyValueSparqlQuery(bulkQuery, "test", Util.VARIABLE_EVENT, Util.VARIABLE_ENTITY, eventURIs);
		bulkTimeKSA = System.currentTimeMillis() - bulkTimeKSA;
		
		if (log.isInfoEnabled())
			log.info(String.format("KSA bulk sparql: total %d ms, per event %d ms", bulkTimeKSA, bulkTimeKSA / this.eventURIs.size()));
		
	}
	// endregion
	
	// region bulkMentionTest
	private void bulkMentionTest() {
		
		long t = System.currentTimeMillis();
		//ksAdapter.runKeyValueSparqlQuery("SELECT ?event ?mention WHERE { VALUES ?event { *keys* } . ?event gaf:denotedBy ?mention}", "test", "event", "mention", this.eventURIs);
		if (log.isInfoEnabled())
			log.info(String.format("query time: %d ms", System.currentTimeMillis() - t));
		
		Set<String> mentionURIs = ksAdapter.getAllRelationValues("test");
		
		String propertyURI = Util.MENTION_PROPERTY_POS;
		
		/*
		long sequentialTime = System.currentTimeMillis();
		Map<String,List<String>> seqMap = new HashMap<String, List<String>>();
		for (String mentionURI : mentionURIs) {
			seqMap.put(mentionURI, ksAdapter.getMentionProperty(mentionURI, propertyURI));
		}
		sequentialTime = System.currentTimeMillis() - sequentialTime;
		if (log.isInfoEnabled())
			log.info(String.format("sequential property retrieval: %d ms", sequentialTime));
		*/
		long bulkTime = System.currentTimeMillis();
		
		try {
			List<Set<URI>> uris = new ArrayList<Set<URI>>();
			Set<URI> currentSet = new HashSet<URI>();
			int currentLength = 0;
			for (String mentionURI : mentionURIs) {
				if (currentLength + mentionURI.length() > 6000) {
					uris.add(currentSet);
					currentSet = new HashSet<URI>();
					currentLength = 0;
				}
				currentSet.add(new URIImpl(mentionURI));
				currentLength += mentionURI.length();
			}
			uris.add(currentSet);
				
			Map<String, List<String>> bulkMap = new HashMap<String, List<String>>();
			KnowledgeStore ks = Client.builder("http://knowledgestore2.fbk.eu/nwr/wikinews").compressionEnabled(true).maxConnections(2).validateServer(false).connectionTimeout(10000).build();
			Session session = ks.newSession();
			
			for (Set<URI> uriSet : uris) {
				Stream<Record> stream = session.retrieve(KS.MENTION).ids(uriSet).timeout(10000L).exec();
				List<Record> records = stream.toList();
				stream.close();
				
				for (Record r : records) {
					String key = r.getID().toString();
					List<String> values = r.get(new URIImpl(propertyURI), String.class);
					bulkMap.put(key, values);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		bulkTime = System.currentTimeMillis() - bulkTime;
		if (log.isInfoEnabled())
			log.info(String.format("bulk property retrieval: %d ms", bulkTime));
		
		long KSAtime = System.currentTimeMillis();
		//ksAdapter.runKeyValueMentionPropertyQuery(propertyURI, "bla", mentionURIs);
		KSAtime = System.currentTimeMillis() - KSAtime;
		if (log.isInfoEnabled())
			log.info(String.format("KSA property retrieval: %d ms", KSAtime));
	}
	// endregion
	
	// region endToEndTest
	public void endToEndTest() {
		
		long searchTime = 0;
		long relSearchTime = 0;
		long filterTime = 0;
		long relFilterTime = 0;
		long rankTime = 0;
		long relRankTime = 0;
		long overallTime = 0;
		long relOverallTime = 0;
		
		UserModel um = new DummyUserModel();
		
		if (log.isInfoEnabled())
			log.info("query;total;totalRel;search;searchRel;filter;filterRel;rank;rankRel");
		
		for (List<Keyword> query : this.keywords) {
			
			long total = 0;
			long search = System.currentTimeMillis();
			Set<NewsEvent> found = this.parallelSearcher.findEvents(query, um);
			search = System.currentTimeMillis() - search;
			searchTime += search;
			relSearchTime += search / found.size();
			total += search;
			
			long filter = System.currentTimeMillis();
			Set<NewsEvent> filtered = this.parallelFilter.filterEvents(found, query, um);
			filter = System.currentTimeMillis() - filter;
			filterTime += filter;
			relFilterTime += filter / found.size();
			total += filter;
			
			long rank = System.currentTimeMillis();
			this.sequentialRanker.rankEvents(filtered, query, um);
			rank = System.currentTimeMillis() - rank;
			rankTime += rank;
			relRankTime += rank / filtered.size();
			total += rank;
			
			overallTime += total;
			relOverallTime += total / found.size();
			
			if (log.isInfoEnabled())
				log.info(String.format("%s;%d;%d;%d;%d;%d;%d;%d;%d", 
						StringUtils.collectionToCommaDelimitedString(query), 
						total, total / found.size(), search, search / found.size(), filter, filter / found.size(), rank, rank / filtered.size()));
			
		}
		
		searchTime /= this.keywords.size();
		relSearchTime /= this.keywords.size();
		filterTime /= this.keywords.size();
		relFilterTime /= this.keywords.size();
		rankTime /= this.keywords.size();
		relRankTime /= this.keywords.size();
		overallTime /= this.keywords.size();
		relOverallTime /= this.keywords.size();
		
		if (log.isInfoEnabled()) {
			log.info(String.format("average;%d;%d;%d;%d;%d;%d;%d;%d\n\n", overallTime, relOverallTime, searchTime, relSearchTime, filterTime, relFilterTime, rankTime, relRankTime));
			log.info(String.format("overall time per query: %d (%d per event)", overallTime, relOverallTime));
			log.info(String.format("search time per query: %d (%d per event)", searchTime, relSearchTime));
			log.info(String.format("filter time per query: %d (%d per event)", filterTime, relFilterTime));
			log.info(String.format("rank time per query: %d (%d per event)", rankTime, relRankTime));
		}
	}
	// endregion
	
	public void run() {
		this.ksAdapter.openConnection();

		if (this.doKSAccessTests)
			ksAccessTests();
		if (this.doSparqlSearchTests)
			sparqlSearchTests();
		if (this.doSparqlFeatureTests)
			sparqlFeatureTests();
		if (this.doSequentialSearcherTest)
			evaluateSearcher(sequentialSearcher, "sequential finder");
		if (this.doParallelSearcherTest)
			evaluateSearcher(parallelSearcher, "parallel finder");
		if (this.doParallelSparqlTest)
			parallelSparqlTest();
		if (this.doSequentialFilterTest) {
			testFilter(sequentialFilter, "sequential filter", this.maxNumberOfEvents);
		}
		if (this.doParallelFilterTest) {
			testFilter(parallelFilter, "parallel filter", this.maxNumberOfEvents);
		}
		if (this.doFeatureTest)
			featureTest();
		if (this.doDownloadTest)
			ksDownloadTest();
		if (this.doClassifierTest)
			classifierTest();
		if (this.doBulkSparqlTest)
			bulkSparqlTest();
		if (this.doBulkMentionTest)
			bulkMentionTest();
		if(this.doEndToEndTest)
			endToEndTest();
		
		sequentialFilter.shutDown();
		parallelFilter.shutDown();
		this.ksAdapter.closeConnection();
	}

	public static void main(String[] args) {
		System.setProperty("java.util.logging.config.file", "./config/logging-benchmark.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(RuntimeTester.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/benchmarkRuntime.xml");
		RuntimeTester test = (RuntimeTester) context.getBean("runtimeTester");
		((AbstractApplicationContext) context).close();

		test.run();
	}
}
