package edu.kit.anthropomatik.isl.newsTeller.util;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
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
	
	@Test
	public void shouldReturnEmptyMap() {
		if(log.isTraceEnabled())
			log.trace("shouldReturn82ElementMap()");
		Map<String, Double> map = Util.readBenchmarkQueryFromFile("resources/benchmark/queries/nonexisting-file.csv");
		assertTrue(map.isEmpty());
	}
	
	@Test
	public void shouldReturn82ElementMap() {
		if(log.isTraceEnabled())
			log.trace("shouldReturn82ElementMap()");
		Map<String, Double> map = Util.readBenchmarkQueryFromFile("resources/benchmark/queries/riot.csv");
		assertTrue(map.size() == 82);
	}
	
	@Test
	public void shouldReturnRating1() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnRating1()");
		Map<String, Double> map = Util.readBenchmarkQueryFromFile("resources/benchmark/queries/riot.csv");
		
		assertTrue(map.get("http://en.wikinews.org/wiki/60th_anniversary_of_the_end_of_the_war_in_Asia_and_Pacific_commemorated#ev67") - 1.0 < Util.EPSILON);
	}
}
