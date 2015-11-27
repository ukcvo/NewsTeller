package edu.kit.anthropomatik.isl.newsTeller.util;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.kit.anthropomatik.isl.newsTeller.data.BenchmarkEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.GroundTruth;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;

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
	public void shouldReturn77ElementBenchmarkMap() {
		Map<BenchmarkEvent, GroundTruth> map = Util.readBenchmarkQueryFromFile("resources/benchmark/queries/riot.csv");
		assertTrue(map.size() == 77);
	}
	
	@Test
	public void shouldReturnRating1() {
		Map<BenchmarkEvent, GroundTruth> map = Util.readBenchmarkQueryFromFile("resources/benchmark/queries/riot.csv");
		BenchmarkEvent e = new BenchmarkEvent("resources/benchmark/queries/riot.csv", 
				"http://en.wikinews.org/wiki/60th_anniversary_of_the_end_of_the_war_in_Asia_and_Pacific_commemorated#ev67");
		GroundTruth buf = map.get(e);
		assertTrue(buf.getUsabilityRating() - 1.0 < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnRank1() {
		Map<BenchmarkEvent, GroundTruth> map = Util.readBenchmarkQueryFromFile("resources/benchmark/queries/riot.csv");
		BenchmarkEvent e = new BenchmarkEvent("resources/benchmark/queries/riot.csv", 
				"http://en.wikinews.org/wiki/60th_anniversary_of_the_end_of_the_war_in_Asia_and_Pacific_commemorated#ev67");
		GroundTruth buf = map.get(e);
		
		assertTrue(buf.getRelevanceRank() == 1);
	}
	
	@Test
	public void shouldReturnEmptyConfigMap() {
		Map<String, List<Keyword>> map = Util.readBenchmarkConfigFile("resources/benchmark/nonexisting-file.csv");
		assertTrue(map.isEmpty());
	}
	
	@Test
	public void shouldReturn45ElementConfigMap() {
		Map<String, List<Keyword>> map = Util.readBenchmarkConfigFile("resources/benchmark/Scope 0.csv");
		assertTrue(map.size() == 45);
	}
	
	@Test
	public void shouldReturnAlbum() {
		Map<String, List<Keyword>> map = Util.readBenchmarkConfigFile("resources/benchmark/Scope 0.csv");
		
		assertTrue(map.get("resources/benchmark/queries/album.csv").get(0).getWord().equals("album"));
	}
	//endregion

	//region regarding XML
	@Test
	public void shouldReturnOnlyA0() {
		Set<Set<String>> result = Util.parsePropBankFrame(new File("resources/propbank-frames/race-v.xml"));
		Set<Set<String>> expected = new HashSet<Set<String>>();
		Set<String> helper = new HashSet<String>();
		helper.add("A0");
		expected.add(helper);
		assertTrue(result.equals(expected));
	}
	
	@Test
	public void shouldReturnA0AndA1() {
		Set<Set<String>> result = Util.parsePropBankFrame(new File("resources/propbank-frames/contradict-v.xml"));
		Set<Set<String>> expected = new HashSet<Set<String>>();
		Set<String> helper = new HashSet<String>();
		helper.add("A0");
		helper.add("A1");
		expected.add(helper);
		assertTrue(result.equals(expected));
	}
	
	// TODO: find example w/ multiple solutions
	
	@Test
	public void shouldParseAllFiles() {
		Map<String, Set<Set<String>>> result = Util.parseAllPropBankFrames("resources/propbank-frames", true);
		assertTrue(result.size() == 9702);
	}
	
	@Test
	public void shouldReadMap() {
		Map<String, Set<Set<String>>> result = Util.parseAllPropBankFrames("resources/propbank-frames", false);
		assertTrue(result.size() == 9702);
	}
	//endregion
}
