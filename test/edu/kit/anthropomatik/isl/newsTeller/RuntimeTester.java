package edu.kit.anthropomatik.isl.newsTeller;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
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
	
	private boolean doSparqlTests;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	private Set<String> sparqlKeywordQueries;
	
	private Set<String> sparqlEventQueries;
	
	private Set<String> keywords;
	
	private int numberOfRepetitions;
	
	public void setDoKSAccessTests(boolean doKSAccessTests) {
		this.doKSAccessTests = doKSAccessTests;
	}
	
	public void setDoSparqlTests(boolean doSparqlTests) {
		this.doSparqlTests = doSparqlTests;
	}
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public void setSparqlKeywordQueries(Set<String> fileNames) {
		this.sparqlKeywordQueries = new HashSet<String>();
		for (String s : fileNames) {
			sparqlKeywordQueries.add(Util.readStringFromFile(s));
		}
	}
	
	public void setSparqlEventQueries(Set<String> fileNames) {
		this.sparqlEventQueries = new HashSet<String>();
		for (String s : fileNames) {
			sparqlEventQueries.add(Util.readStringFromFile(s));
		}
	}
	
	public void setKeywords(Set<String> keywords) {
		this.keywords = keywords;
	}
	
	public void setNumberOfRepetitions(int numberOfRepetitions) {
		this.numberOfRepetitions = numberOfRepetitions;
	}
	
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
	
	private void sparqlTests() {
		
		ksAdapter.openConnection();
		for (String query : sparqlKeywordQueries) {
			long averageQueryTime = 0;
			for (int i = 0; i < this.numberOfRepetitions; i++) {
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
				log.info(averageQueryTime);
		}
		
		// TODO: same here!
//		for (String query : sparqlEventQueries) {
//			
//		}
		ksAdapter.closeConnection();
	}
	
	public void run() {
		if (this.doKSAccessTests)
			ksAccessTests();
		if (this.doSparqlTests)
			sparqlTests();
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
