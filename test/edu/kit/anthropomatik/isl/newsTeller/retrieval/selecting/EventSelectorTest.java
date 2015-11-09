package edu.kit.anthropomatik.isl.newsTeller.retrieval.selecting;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.LogManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

public class EventSelectorTest {
	
	private EventSelector selector;
	
	private Set<NewsEvent> events;
	private NewsEvent eventToBeSelected;
	
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
		this.selector = new EventSelector();
		
		this.events = new HashSet<NewsEvent>();
		NewsEvent event1 = new NewsEvent("event-1");
		event1.setTotalRelevanceScore(0.2);
		NewsEvent event2 = new NewsEvent("event-2");
		event2.setTotalRelevanceScore(0.7);
		NewsEvent event3 = new NewsEvent("event-3");
		event3.setTotalRelevanceScore(0.3);
		NewsEvent event4 = new NewsEvent("event-4");
		event4.setTotalRelevanceScore(0.6);
		this.events.add(event1);
		this.events.add(event2);
		this.events.add(event3);
		this.events.add(event4);
		this.eventToBeSelected = event2;
	}

	@Test
	public void shouldReturnEvent2() {
		
		NewsEvent selectedEvent = this.selector.selectEvent(this.events);
		assertTrue(selectedEvent == this.eventToBeSelected);
	}

}
