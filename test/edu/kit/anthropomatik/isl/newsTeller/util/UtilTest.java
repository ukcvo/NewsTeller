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
		if(log.isInfoEnabled())
			log.info("shouldReturnEmptyString()");
		String query = Util.readStringFromFile("");
		assertTrue(query.isEmpty());
	}

	@Test
	public void shouldReturnEmptyList() {
		if(log.isInfoEnabled())
			log.info("shouldReturnEmptyList()");
		List<String> queries = Util.readStringsFromFolder("resources/SPARQL/non-existing-directory");
		assertTrue(queries.isEmpty());
	}
	
	@Test
	public void shouldReturnSPARQLQuery() {
		if(log.isInfoEnabled())
			log.info("shouldReturnSPARQLQuery()");
		String query = Util.readStringFromFile("resources/SPARQL/general/retrieveEntity.qry");
		assertTrue(!query.isEmpty());
	}
	
	@Test
	public void shouldReturnOneElementedList() {
		if(log.isInfoEnabled())
			log.info("shouldReturnOneElementedList()");
		List<String> queries = Util.readStringsFromFolder("resources/SPARQL/general");
		assertTrue(queries.size() == 1);
	}
}
