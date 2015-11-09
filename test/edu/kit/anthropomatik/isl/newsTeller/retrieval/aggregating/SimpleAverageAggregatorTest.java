package edu.kit.anthropomatik.isl.newsTeller.retrieval.aggregating;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.kit.anthropomatik.isl.newsTeller.data.Scoring;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class SimpleAverageAggregatorTest {
	
	private List<Scoring> scorings; 
	
	private SimpleAverageAggregator aggregator;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Before
	public void setUp() throws Exception {
		
		this.aggregator = new SimpleAverageAggregator();
		
		this.scorings = new ArrayList<Scoring>();
		scorings.add(new Scoring("heuristic-1", 1));
		scorings.add(new Scoring("heuristic-2", 0));
		scorings.add(new Scoring("heuristic-3", 0.7));
		
	}

	@Test
	public void shouldDoAverageAggregation() {
		double aggregatedScore = this.aggregator.getTotalScore(scorings);
		
		assertTrue(aggregatedScore - (1.7/3) < Util.EPSILON);
	}

}
