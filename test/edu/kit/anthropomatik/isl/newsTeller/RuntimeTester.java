package edu.kit.anthropomatik.isl.newsTeller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.BenchmarkEvent;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.FilteringBenchmark;
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
	
	private KnowledgeStoreAdapter ksAdapter;
	
	private Map<String, String> sparqlFindingQueries;
	
	private Map<String, String> sparqlFeatureQueries;
	
	private Set<String> keywords;
	
	private Set<String> eventURIs;
	
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
	
	public void setKeywords(Set<String> keywords) {
		this.keywords = keywords;
	}
	
	public void setEventURIFileName(String fileName) {
		this.eventURIs = new HashSet<String>();
		Set<String> fileNames = Util.readBenchmarkConfigFile(fileName).keySet();
		for (String f : fileNames) {
			Set<BenchmarkEvent> events = Util.readBenchmarkQueryFromFile(f).keySet();
			for (BenchmarkEvent e : events)
				eventURIs.add(e.getEventURI());
		}
	}
	
	public void setNumberOfRepetitions(int numberOfRepetitions) {
		this.numberOfRepetitions = numberOfRepetitions;
	}
	//endregion
	
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
			log.info(String.format("average client opening time: %d", averageClientOpenTime));
			log.info(String.format("average client closing time: %d", averageClientClosingTime));
			log.info(String.format("average session opening time: %d", averageSessionOpenTime));
			log.info(String.format("average session closing time: %d", averageSessionClosingTime));
		}
		
	}
	//endregion
	
	//region sparqlFindingTests
	private void sparqlFindingTests() {
		
		ksAdapter.openConnection();
		for (Map.Entry<String, String> entry : sparqlFindingQueries.entrySet()) {
			String fileName = entry.getKey();
			String query = entry.getValue();
			long averageQueryTime = 0;
			for (int i = 0; i < this.numberOfRepetitions; i++) {
				if (log.isInfoEnabled())
					log.info(i);
				for (String keyword : this.keywords) {
					String modifiedQuery = query.replace(Util.PLACEHOLDER_KEYWORD, keyword);
					long t1 = System.currentTimeMillis();
					ksAdapter.runSingleVariableStringQuery(modifiedQuery, Util.VARIABLE_EVENT);
					long t2 = System.currentTimeMillis();
					averageQueryTime += (t2-t1);
				}
			}
			averageQueryTime /= this.numberOfRepetitions;
			averageQueryTime /= this.keywords.size();
			
			if (log.isInfoEnabled())
				log.info(String.format("%s: %d", fileName, averageQueryTime));
		}

		ksAdapter.closeConnection();
	}
	//endregion
	
	//region sparqlFeatureTests
	private void sparqlFeatureTests() {
		ksAdapter.openConnection();
		
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
				log.info(String.format("%s: %d", fileName, averageQueryTime));
		}
		
		ksAdapter.closeConnection();
	}
	//endregion
	
	public void run() {
		if (this.doKSAccessTests)
			ksAccessTests();
		if (this.doSparqlFindingTests)
			sparqlFindingTests();
		if (this.doSparqlFeatureTests)
			sparqlFeatureTests();
	}
	
	public static void main(String[] args) {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(FilteringBenchmark.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		RuntimeTester test = (RuntimeTester) context.getBean("runtimeTester");
		((AbstractApplicationContext) context).close();
		
		test.run();
	}
}
