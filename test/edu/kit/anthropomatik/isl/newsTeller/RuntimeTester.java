package edu.kit.anthropomatik.isl.newsTeller;

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
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.BenchmarkEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.IEventFilter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.ParallelBayesEventFilter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.finding.EventFinder;
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import eu.fbk.knowledgestore.KnowledgeStore;
import eu.fbk.knowledgestore.Session;
import eu.fbk.knowledgestore.client.Client;

/**
 * Tests regarding different runtime behaviors.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class RuntimeTester {

	private static Log log = LogFactory.getLog(RuntimeTester.class);
	
	private boolean doKSAccessTests;
	
	private boolean doSparqlFindingTests;
	
	private boolean doSparqlFeatureTests;
	
	private boolean doSequentialFinderTest;
	
	private boolean doParallelFinderTest;
	
	private boolean doParallelSparqlTest;
	
	private boolean doSequentialFilterTest;
	
	private boolean doParallelFilterTest;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	private Map<String, String> sparqlFindingQueries;
	
	private Map<String, String> sparqlFeatureQueries;
	
	private Set<String> stemmedKeywords;
	
	private Set<String> eventURIs;
	
	private Set<NewsEvent> events;
	
	private EventFinder sequentialFinder;
	
	private EventFinder parallelFinder;
	
	private IEventFilter sequentialFilter;
	
	private ParallelBayesEventFilter parallelFilter;
	
	private List<Integer> threadNumbers;
	
	private Set<List<Keyword>> keywords;
	
	private int numberOfRepetitions;
	
	//region setters
	public void setDoKSAccessTests(boolean doKSAccessTests) {
		this.doKSAccessTests = doKSAccessTests;
	}
		
	public void setDoSparqlFindingTests(boolean doSparqlFindingTests) {
		this.doSparqlFindingTests = doSparqlFindingTests;
	}
	
	public void setDoSparqlFeatureTests(boolean doSparqlFeatureTests) {
		this.doSparqlFeatureTests = doSparqlFeatureTests;
	}
	
	public void setDoSequentialFinderTest(boolean doSequentialFinderTest) {
		this.doSequentialFinderTest = doSequentialFinderTest;
	}
	
	public void setDoParallelFinderTest(boolean doParallelFinderTest) {
		this.doParallelFinderTest = doParallelFinderTest;
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
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public void setSparqlFindingQueries(Set<String> fileNames) {
		this.sparqlFindingQueries = new HashMap<String, String>();
		for (String s : fileNames) {
			sparqlFindingQueries.put(s, Util.readStringFromFile(s));
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
	
	public void setSequentialFinder(EventFinder sequentialFinder) {
		this.sequentialFinder = sequentialFinder;
	}
	
	public void setParallelFinder(EventFinder parallelFinder) {
		this.parallelFinder = parallelFinder;
	}
	
	public void setSequentialFilter(IEventFilter sequentialFilter) {
		this.sequentialFilter = sequentialFilter;
	}

	public void setParallelFilter(ParallelBayesEventFilter parallelFilter) {
		this.parallelFilter = parallelFilter;
	}
	
	public void setThreadNumbers(List<Integer> threadNumbers) {
		this.threadNumbers = threadNumbers;
	}
	
	public void setNumberOfRepetitions(int numberOfRepetitions) {
		this.numberOfRepetitions = numberOfRepetitions;
	}
	//endregion
	
	public RuntimeTester(String configFileName) {
		
		Map<String,List<Keyword>> benchmarkConfig = Util.readBenchmarkConfigFile(configFileName);
		this.keywords = new HashSet<List<Keyword>>();
		for (List<Keyword> queryKeywords : benchmarkConfig.values()) {
			this.keywords.add(queryKeywords);
		}
		
		this.eventURIs = new HashSet<String>();
		this.events = new HashSet<NewsEvent>();
		Set<String> fileNames = benchmarkConfig.keySet();
		for (String f : fileNames) {
			Set<BenchmarkEvent> fileEvents = Util.readBenchmarkQueryFromFile(f).keySet();
			for (BenchmarkEvent e : fileEvents) {
				this.eventURIs.add(e.getEventURI());
				this.events.add(new NewsEvent(e.getEventURI()));
			}
				
		}
	}
	
	//region ksAccessTests
	private void ksAccessTests() {
		long averageClientOpenTime = 0;
		long averageClientClosingTime = 0;
		long averageSessionOpenTime = 0;
		long averageSessionClosingTime = 0;
		
		for (int i = 0; i < this.numberOfRepetitions; i++) {
			long t1 = System.currentTimeMillis();
			KnowledgeStore ks = Client.builder("http://knowledgestore2.fbk.eu/nwr/wikinews").compressionEnabled(true).maxConnections(2)
									.validateServer(false).connectionTimeout(10000).build();
			long t2 = System.currentTimeMillis();
			averageClientOpenTime += (t2 - t1);
			t1 = System.currentTimeMillis();
			ks.close();
			t2 = System.currentTimeMillis();
			averageClientClosingTime += (t2 - t1);
		}
		averageClientOpenTime /= this.numberOfRepetitions;
		averageClientClosingTime /= this.numberOfRepetitions;
		
		KnowledgeStore ks = Client.builder("http://knowledgestore2.fbk.eu/nwr/wikinews").compressionEnabled(true).maxConnections(2)
				.validateServer(false).connectionTimeout(10000).build();
		
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
	//endregion
	
	//region sparqlFindingTests
	private void sparqlFindingTests() {
		
		for (Map.Entry<String, String> entry : sparqlFindingQueries.entrySet()) {
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
					averageQueryTime += (t2-t1);
				}
			}
			averageQueryTime /= this.numberOfRepetitions;
			averageQueryTime /= this.stemmedKeywords.size();
			
			if (log.isInfoEnabled())
				log.info(String.format("%s: %d ms", fileName, averageQueryTime));
		}

	}
	//endregion
	
	//region sparqlFeatureTests
	private void sparqlFeatureTests() {
		
		for(Map.Entry<String, String> entry : sparqlFeatureQueries.entrySet()) {
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
	//endregion
	
	//region evaluateFinder
	private void evaluateFinder(EventFinder finder, String finderName) {
		long averageFindingTime = 0;
		DummyUserModel um = new DummyUserModel();
		for (int i = 0; i < this.numberOfRepetitions; i++) {
			for (List<Keyword> queryKeywords : this.keywords) {
				long t1 = System.currentTimeMillis();
				finder.findEvents(queryKeywords, um);
				long t2 = System.currentTimeMillis();
				averageFindingTime += (t2 - t1);
			}
		}
		averageFindingTime /= this.numberOfRepetitions;
		averageFindingTime /= this.keywords.size();
		if (log.isInfoEnabled())
			log.info(String.format("%s: %d ms",finderName, averageFindingTime));
	}
	//endregion
	
	//region parallelSparqlTest
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
		KnowledgeStore ks = Client.builder("http://knowledgestore2.fbk.eu/nwr/wikinews").compressionEnabled(true).maxConnections(10)
				.validateServer(false).connectionTimeout(10000).build();
		ExecutorService threadPool = Executors.newFixedThreadPool(10);
		List<Future<?>> futures = new ArrayList<Future<?>>();
		
		for (Map.Entry<String, String> entry : sparqlFindingQueries.entrySet()) {
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
		
		for (Map.Entry<String, String> entry : sparqlFindingQueries.entrySet()) {
			(new ParallelSparqlTestWorker(ks, entry.getValue().replace(Util.PLACEHOLDER_KEYWORD, "Real Madrid"), entry.getKey() + " seq")).run();
		}
		ks.close();
	}
	//endregion
	
	//region bayesEventFilterTest
	private void testFilter(IEventFilter filter, String filterName) {
		long totalTime = 0;
		long averageTimePerEvent = 0;
		
		long t1 = System.currentTimeMillis();
		filter.filterEvents(this.events);	
		long t2 = System.currentTimeMillis();
		
		totalTime = t2 - t1;
		averageTimePerEvent = totalTime / events.size();
		
		if (log.isInfoEnabled())
			log.info(String.format("%s - total: %d ms, per event: %d ms", filterName, totalTime, averageTimePerEvent));
		
	}
	
	private void parallelFilterTest() {
		
		for (int nThreads : threadNumbers) {
			parallelFilter.shutDown();
			parallelFilter.setNThreads(nThreads);
			ksAdapter.closeConnection();
			ksAdapter.setMaxNumberOfConnections(nThreads);
			ksAdapter.openConnection();
			testFilter(parallelFilter, String.format("%d thread filter", nThreads));
		}
		
	}
	//endregion
	
	public void run() {
		this.ksAdapter.openConnection();
		
		if (this.doKSAccessTests)
			ksAccessTests();
		if (this.doSparqlFindingTests)
			sparqlFindingTests();
		if (this.doSparqlFeatureTests)
			sparqlFeatureTests();
		if (this.doSequentialFinderTest)
			evaluateFinder(sequentialFinder, "sequential finder");
		if (this.doParallelFinderTest)
			evaluateFinder(parallelFinder, "parallel finder");
		if (this.doParallelSparqlTest)
			parallelSparqlTest();
		if (this.doSequentialFilterTest) {
			testFilter(sequentialFilter, "sequential filter");
		}
		if (this.doParallelFilterTest) {
			parallelFilterTest();
		}
			
		
		sequentialFinder.shutDown();
		parallelFinder.shutDown();
		sequentialFilter.shutDown();
		parallelFilter.shutDown();
		this.ksAdapter.closeConnection();
	}
	
	public static void main(String[] args) {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(RuntimeTester.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		RuntimeTester test = (RuntimeTester) context.getBean("runtimeTester");
		((AbstractApplicationContext) context).close();
		
		test.run();
	}
}
