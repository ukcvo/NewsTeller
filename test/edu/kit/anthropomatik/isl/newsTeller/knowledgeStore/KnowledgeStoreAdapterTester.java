package edu.kit.anthropomatik.isl.newsTeller.knowledgeStore;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KnowledgeStoreAdapterTester {

	private KnowledgeStoreAdapter ksAdapter = new KnowledgeStoreAdapter();
	
	@Before
	public void init() {
		ksAdapter.openConnection("http://knowledgestore2.fbk.eu/nwr/wikinews", 10);
		
	}
	
	@After
	public void cleanup() {
		ksAdapter.closeConnection();
	}
	
	@Test
	public void shouldReturn10Events() {
		List<URI> events = ksAdapter.getEvents("SELECT ?s WHERE {?s rdf:type sem:Event} LIMIT 10", "s", 10000);
		assertTrue(events.size() == 10);
	}

}
