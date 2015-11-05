package edu.kit.anthropomatik.isl.newsTeller.util;

import static org.junit.Assert.*;

import java.util.List;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.junit.Test;

public class UtilTest {

	private static Log log;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(UtilTest.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void shouldReturnEmptyString() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnEmptyString()");
		String query = Util.readStringFromFile("");
		assertTrue(query.isEmpty());
	}

	@Test
	public void shouldReturnEmptyList() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnEmptyList()");
		List<String> queries = Util.readStringsFromFolder("resources/SPARQL/test/non-existing-directory");
		assertTrue(queries.isEmpty());
	}
	
	@Test
	public void shouldReturnSPARQLQuery() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnSPARQLQuery()");
		String query = Util.readStringFromFile("resources/SPARQL/test/retrieveEntity.qry");
		assertTrue(!query.isEmpty());
	}
	
	@Test
	public void shouldReturnOneElementedList() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnOneElementedList()");
		List<String> queries = Util.readStringsFromFolder("resources/SPARQL/test");
		assertTrue(queries.size() == 1);
	}
}
