package edu.kit.anthropomatik.isl.newsTeller.util;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;

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

	// region reading strings
	@Test
	public void shouldReturnEmptyString() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnEmptyString()");
		String query = Util.readStringFromFile("");
		assertTrue(query.isEmpty());
	}

	@Test
	public void shouldReturnSPARQLQuery() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnSPARQLQuery()");
		String query = Util.readStringFromFile("resources/SPARQL/test/retrieveEntity.qry");
		assertTrue(!query.isEmpty());
	}
	
	@Test
	public void shouldReturnEmptyListFromFolder() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnEmptyListFromFolder()");
		List<String> queries = Util.readStringsFromFolder("resources/SPARQL/test/non-existing-directory");
		assertTrue(queries.isEmpty());
	}
	
	@Test
	public void shouldReturnOneElementedListFromFolder() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnOneElementedListFromFolder()");
		List<String> queries = Util.readStringsFromFolder("resources/SPARQL/test");
		assertTrue(queries.size() == 1);
	}
	
	@Test
	public void shouldReturnOneElementedListFromConfigFile() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnOneElementedListFromConfigFile()");
		List<String> queries = Util.readQueriesFromConfigFile("config/SPARQL/test/configTest.txt");
		assertTrue(queries.size() == 1);
	}
	
	@Test
	public void shouldReturnEmptyListFromConfigFileBecauseConfigNonExistent() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnEmptyListFromConfigFileBecauseConfigNonExistent()");
		List<String> queries = Util.readQueriesFromConfigFile("config/SPARQL/test/non-existing-config-file.txt");
		assertTrue(queries.isEmpty());
	}
	
	@Test
	public void shouldReturnEmptyListFromConfigFileBecauseConfigEmpty() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnEmptyListFromConfigFileBecauseConfigEmpty()");
		List<String> queries = Util.readQueriesFromConfigFile("config/SPARQL/test/configEmpty.txt");
		assertTrue(queries.isEmpty());
	}
	
	@Test
	public void shouldReturnEmptyListFromConfigFileBecauseFileNonExistent() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnEmptyListFromConfigFileBecauseFileNonExistent()");
		List<String> queries = Util.readQueriesFromConfigFile("config/SPARQL/test/configWrongFile.txt");
		assertTrue(queries.isEmpty());
	}
	//endregion
	
	//region reading CSV
	@Test
	public void shouldReturnEmptyBenchmarkMap() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnEmptyBenchmarkMap()");
		Map<String, Double> map = Util.readBenchmarkQueryFromFile("resources/benchmark/queries/nonexisting-file.csv");
		assertTrue(map.isEmpty());
	}
	
	@Test
	public void shouldReturn82ElementBenchmarkMap() {
		if(log.isTraceEnabled())
			log.trace("shouldReturn82ElementBenchmarkMap()");
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
	
	@Test
	public void shouldReturnEmptyConfigMap() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnEmptyConfigMap()");
		Map<String, List<Keyword>> map = Util.readBenchmarkConfigFile("resources/benchmark/nonexisting-file.csv");
		assertTrue(map.isEmpty());
	}
	
	@Test
	public void shouldReturn45ElementConfigMap() {
		if(log.isTraceEnabled())
			log.trace("shouldReturn45ElementConfigMap()");
		Map<String, List<Keyword>> map = Util.readBenchmarkConfigFile("resources/benchmark/Scope 0.csv");
		assertTrue(map.size() == 45);
	}
	
	@Test
	public void shouldReturnAlbum() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnAlbum()");
		Map<String, List<Keyword>> map = Util.readBenchmarkConfigFile("resources/benchmark/Scope 0.csv");
		
		assertTrue(map.get("resources/benchmark/queries/album.csv").get(0).getWord().equals("album"));
	}
	//endregion
}
