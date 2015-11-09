package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.Scoring;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.aggregating.SimpleAverageAggregator;

public class EventRankerTest {

	private EventRanker ranker;
	
	private Set<NewsEvent> events;
	
	private NewsEvent event1;
	private NewsEvent event2;
	private NewsEvent event3;
	private NewsEvent event4;
	private NewsEvent event5;
	
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
		this.ranker = new EventRanker();
		this.ranker.setScoreAggregator(new SimpleAverageAggregator());
		
		Scoring scoring1 = new Scoring("h1", 0.0);
		Scoring scoring2 = new Scoring("h2", 1.0);
		Scoring scoring3 = new Scoring("h3", 0.5);
		Scoring scoring4 = new Scoring("h4", 0.75);
		Scoring scoring5 = new Scoring("h5", 0.1);
		
		event1 = new NewsEvent("event1");
		event1.addRelevanceScoring(scoring1);
		event2 = new NewsEvent("event2");
		event2.addRelevanceScoring(scoring2);
		event3 = new NewsEvent("event3");
		event3.addRelevanceScoring(scoring3);
		event4 = new NewsEvent("event4");
		event4.addRelevanceScoring(scoring4);
		event5 = new NewsEvent("event5");
		event5.addRelevanceScoring(scoring5);
		
		this.events = new HashSet<NewsEvent>();
		this.events.add(event1);
		this.events.add(event2);
		this.events.add(event3);
		this.events.add(event4);
		this.events.add(event5);
	}

	@Test
	public void shouldReturnListOfSize5() {
		List<NewsEvent> rankedList = ranker.getRankedEvents(events);
		assertTrue(rankedList.size() == 5);
	}
	
	@Test
	public void shouldReturnEventsInCorrectOrder() {
		List<NewsEvent> rankedList = ranker.getRankedEvents(events);
		assertTrue(rankedList.get(0).equals(event2) && rankedList.get(1).equals(event4) && rankedList.get(2).equals(event3) 
						&& rankedList.get(3).equals(event5) && rankedList.get(4).equals(event1));
	}

}
