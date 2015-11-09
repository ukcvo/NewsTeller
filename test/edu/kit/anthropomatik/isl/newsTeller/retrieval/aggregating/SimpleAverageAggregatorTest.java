package edu.kit.anthropomatik.isl.newsTeller.retrieval.aggregating;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.Scoring;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class SimpleAverageAggregatorTest {
	
	private Set<NewsEvent> events; 
	
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
		
		this.events = new HashSet<NewsEvent>();
		List<Scoring> scoringSet1 = new ArrayList<Scoring>();
		scoringSet1.add(new Scoring("heuristic-1", 1));
		scoringSet1.add(new Scoring("heuristic-2", 0));
		scoringSet1.add(new Scoring("heuristic-3", 0.7));
		
		this.events.add(new NewsEvent("event-1", scoringSet1));
	}

	@Test
	public void shouldDoAverageAggregation() {
		this.aggregator.aggregateScores(this.events);
		assertTrue(((NewsEvent) this.events.toArray()[0]).getTotalRelevanceScore() - (1.7/3) < Util.EPSILON);
	}

}
