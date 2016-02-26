package edu.kit.anthropomatik.isl.newsTeller.data.benchmark;

import java.util.List;
import java.util.Map;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;

/**
 * Encapsulates all information about a user from the benchmark.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class BenchmarkUser {

	private String id;
	
	private List<Keyword> interests;
	
	private Map<List<Keyword>, Map<BenchmarkEvent, GroundTruth>> queries;

	public String getId() {
		return id;
	}

	public List<Keyword> getInterests() {
		return interests;
	}

	public Map<List<Keyword>, Map<BenchmarkEvent, GroundTruth>> getQueries() {
		return queries;
	}
	
	public BenchmarkUser(String id, List<Keyword> interests, Map<List<Keyword>, Map<BenchmarkEvent, GroundTruth>> queries) {
		this.id = id;
		this.interests = interests;
		this.queries = queries;
	}
}
