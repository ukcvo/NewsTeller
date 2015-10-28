package edu.kit.anthropomatik.isl.newsTeller.knowledgeStore;

import static org.junit.Assert.*;

import java.util.List;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

public class KnowledgeStoreAdapterTest {

	private KnowledgeStoreAdapter ksAdapter;
	private static Log log;
	
	@BeforeClass
	public static void setUpBeforeClass() {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(KnowledgeStoreAdapterTest.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Before
	public void setUp() {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/Scope0_test.xml");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
		
	}
	
	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}
	
	@Test
	public void shouldReturn10Events() {
		if(log.isInfoEnabled())
			log.info("shouldReturn10Events()");
		List<NewsEvent> events = ksAdapter.runSingleVariableEventQuery("SELECT ?s WHERE {?s rdf:type sem:Event} LIMIT 10", "s", 10000);
		assertTrue(events.size() == 10);
	}
	
	@Test
	public void shouldReturnEmptyListBecauseOfClosedConnection() {
		if(log.isInfoEnabled())
			log.info("shouldReturnEmptyListBecauseOfClosedConnection()");
		ksAdapter.closeConnection();
		List<NewsEvent> events = ksAdapter.runSingleVariableEventQuery("SELECT ?s WHERE {?s rdf:type sem:Event} LIMIT 10", "s", 10000);
		assertTrue(events.isEmpty());
	}
	
	@Test
	public void shouldReturnEmptyListBecauseOfNonMatchingEventVariable() {
		if(log.isInfoEnabled())
			log.info("shouldReturnEmptyListBecauseOfNonMatchingEventVariable()");
		List<NewsEvent> events = ksAdapter.runSingleVariableEventQuery("SELECT ?s WHERE {?s rdf:type sem:Event} LIMIT 10", "t", 10000);
		assertTrue(events.isEmpty());
	}
	
	@Test
	public void shouldReturnEmptyListBecauseOfBrokenQuery() {
		if(log.isInfoEnabled())
			log.info("shouldReturnEmptyListBecauseOfBrokenQuery()");
		List<NewsEvent> events = ksAdapter.runSingleVariableEventQuery("SELECT ?s HERE {?s rdf:type sem:Event} LIMIT 10", "s", 10000);
		assertTrue(events.isEmpty());
	}
	
	@Test
	public void shouldReturnOneElementedList() {
		if(log.isInfoEnabled())
			log.info("shouldReturnOneElementedList()");
		List<Double> numbers = ksAdapter.runSingleVariableDoubleQuery("SELECT (count(?s) as ?n) WHERE {?s rdf:type sem:Event}", "n");
		assertTrue(numbers.size() == 1);
	}

	@Test
	public void shouldReturn624439() {
		if(log.isInfoEnabled())
			log.info("shouldReturn624439()");
		double number = ksAdapter.runSingleVariableDoubleQuerySingleResult("SELECT (count(?s) as ?n) WHERE {?s rdf:type sem:Event}", "n");
		assertTrue(number == 624439);
	}
	
}
