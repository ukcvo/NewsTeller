package edu.kit.anthropomatik.isl.newsTeller.knowledgeStore;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class KnowledgeStoreAdapterTester {

	private KnowledgeStoreAdapter ksAdapter;
	
	@Before
	public void init() {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/Scope0_test.xml");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
		
	}
	
	@After
	public void cleanup() {
		ksAdapter.closeConnection();
	}
	
	@Test
	public void shouldReturn10Events() {
		List<URI> events = ksAdapter.runSingleVariableURIQuery("SELECT ?s WHERE {?s rdf:type sem:Event} LIMIT 10", "s", 10000);
		assertTrue(events.size() == 10);
	}
	
	@Test
	public void shouldReturnEmptyListBecauseOfClosedConnection() {
		ksAdapter.closeConnection();
		List<URI> events = ksAdapter.runSingleVariableURIQuery("SELECT ?s WHERE {?s rdf:type sem:Event}", "s", 10000);
		assertTrue(events.isEmpty());
	}
	
	@Test
	public void shouldReturnEmptyListBecauseOfNonMatchingEventVariable() {
		List<URI> events = ksAdapter.runSingleVariableURIQuery("SELECT ?s WHERE {?s rdf:type sem:Event}", "t", 10000);
		assertTrue(events.isEmpty());
	}
	
	@Test
	public void shouldReturnEmptyListBecauseOfBrokenQuery() {
		List<URI> events = ksAdapter.runSingleVariableURIQuery("SELECT ?s HERE {?s rdf:type sem:Event}", "s", 10000);
		assertTrue(events.isEmpty());
	}

}
