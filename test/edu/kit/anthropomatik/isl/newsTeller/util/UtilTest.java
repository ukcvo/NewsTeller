package edu.kit.anthropomatik.isl.newsTeller.util;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.BenchmarkEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.BenchmarkUser;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.GroundTruth;

public class UtilTest {
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//region reading strings
	@Test
	public void shouldReturnEmptyString() {
		String query = Util.readStringFromFile("");
		assertTrue(query.isEmpty());
	}

	@Test
	public void shouldReturnSPARQLQuery() {
		String query = Util.readStringFromFile("resources/SPARQL/test/retrieveEntity.qry");
		assertTrue(!query.isEmpty());
	}
	
	@Test
	public void shouldReturnEmptyListFromFolder() {
		List<String> queries = Util.readStringsFromFolder("resources/SPARQL/test/non-existing-directory");
		assertTrue(queries.isEmpty());
	}
	
	@Test
	public void shouldReturnOneElementedListFromFolder() {
		List<String> queries = Util.readStringsFromFolder("resources/SPARQL/test");
		assertTrue(queries.size() == 1);
	}
	
	@Test
	public void shouldReturnOneElementedListFromConfigFile() {
		List<String> queries = Util.readQueriesFromConfigFile("config/SPARQL/test/configTest.txt");
		assertTrue(queries.size() == 1);
	}
	
	@Test
	public void shouldReturnEmptyListFromConfigFileBecauseConfigNonExistent() {
		List<String> queries = Util.readQueriesFromConfigFile("config/SPARQL/test/non-existing-config-file.txt");
		assertTrue(queries.isEmpty());
	}
	
	@Test
	public void shouldReturnEmptyListFromConfigFileBecauseConfigEmpty() {
		List<String> queries = Util.readQueriesFromConfigFile("config/SPARQL/test/configEmpty.txt");
		assertTrue(queries.isEmpty());
	}
	
	@Test
	public void shouldReturnEmptyListFromConfigFileBecauseFileNonExistent() {
		List<String> queries = Util.readQueriesFromConfigFile("config/SPARQL/test/configWrongFile.txt");
		assertTrue(queries.isEmpty());
	}
	//endregion
	
	//region reading CSV
	@Test
	public void shouldReturnEmptyBenchmarkMap() {
		Map<BenchmarkEvent, GroundTruth> map = Util.readBenchmarkQueryFromFile("resources/benchmark/queries/nonexisting-file.csv");
		assertTrue(map.isEmpty());
	}
	
	@Test
	public void shouldReturn93ElementBenchmarkMap() {
		Map<BenchmarkEvent, GroundTruth> map = Util.readBenchmarkQueryFromFile("resources/benchmark/queries/artificial intelligence.csv");
		assertTrue(map.size() == 93);
	}
	
	@Test
	public void shouldReturnRating1() {
		Map<BenchmarkEvent, GroundTruth> map = Util.readBenchmarkQueryFromFile("resources/benchmark/queries/artificial intelligence.csv");
		BenchmarkEvent e = new BenchmarkEvent("resources/benchmark/queries/artificial intelligence.csv", 
				"http://en.wikinews.org/wiki/Computer_professionals_celebrate_10th_birthday_of_A.L.I.C.E.#ev32");
		GroundTruth buf = map.get(e);
		assertTrue(buf.getUsabilityRating() - 1.0 < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnRank1() {
		Map<BenchmarkEvent, GroundTruth> map = Util.readBenchmarkQueryFromFile("resources/benchmark/queries/artificial intelligence.csv");
		BenchmarkEvent e = new BenchmarkEvent("resources/benchmark/queries/artificial intelligence.csv", 
				"http://en.wikinews.org/wiki/Computer_professionals_celebrate_10th_birthday_of_A.L.I.C.E.#ev32");
		GroundTruth buf = map.get(e);
		
		assertTrue(buf.getRelevanceRank() == 1);
	}
	
	@Test
	public void shouldReturnEmptyConfigMap() {
		Map<String, List<Keyword>> map = Util.readBenchmarkConfigFile("resources/benchmark/nonexisting-file.csv");
		assertTrue(map.isEmpty());
	}
	
	@Test
	public void shouldReturn46ElementConfigMap() {
		Map<String, List<Keyword>> map = Util.readBenchmarkConfigFile("resources/benchmark/Scope 0.csv");
		assertTrue(map.size() == 46);
	}
	
	@Test
	public void shouldReturnArtificialIntelligence() {
		Map<String, List<Keyword>> map = Util.readBenchmarkConfigFile("resources/benchmark/Scope 0.csv");
		
		assertTrue(map.get("resources/benchmark/queries/artificial intelligence.csv").get(0).getWord().equals("artificial intelligence"));
	}
	
	@Test
	public void shouldReturnEmptyNLGMapNoConfig() {
		Map<String, String> map = Util.readNLGQueries("config/SPARQL/nonexisting-file.csv");
		assertTrue(map.isEmpty());
	}
	
	@Test
	public void shouldReturnEmptyNLGMapEmptyConfig() {
		Map<String, String> map = Util.readNLGQueries("config/SPARQL/test/NLGConfigEmpty.csv");
		assertTrue(map.isEmpty());
	}
	
	@Test
	public void shouldReturnEmptyNLGMapBrokenConfig() {
		Map<String, String> map = Util.readNLGQueries("config/SPARQL/test/NLGConfigWrongFile.csv");
		assertTrue(map.isEmpty());
	}
	
	@Test
	public void shouldReturnCorrectNLGMap() {
		Map<String, String> map = Util.readNLGQueries("config/SPARQL/test/NLGConfigTest.csv");
		assertTrue(map.size() == 1);
	}
	
	@Test
	public void shouldReturnCorrectCompleteBenchmarkOld() {
		List<BenchmarkUser> benchmark = Util.readCompleteUserBenchmark("resources/benchmark/onlyOld.csv");
		assertTrue((benchmark.size() == 1) && (benchmark.get(0).getQueries().size() == 64) && (benchmark.get(0).getInterests().isEmpty()));
	}
	
	@Test
	public void shouldReturnCorrectCompleteBenchmarkUM() {
		List<BenchmarkUser> benchmark = Util.readCompleteUserBenchmark("resources/benchmark/onlyUM.csv");
		assertTrue((benchmark.size() == 11) && (benchmark.get(0).getQueries().size() == 10) && (benchmark.get(0).getInterests().size() == 6));
	}
	//endregion

}
