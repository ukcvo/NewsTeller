package edu.kit.anthropomatik.isl.newsTeller.retrieval.selecting;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

public class EventSelectorTest {

	private static Log log;
	
	private EventSelector selector;
	
	private List<NewsEvent> events;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(EventSelectorTest.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Before
	public void setUp() throws Exception {
		this.selector = new EventSelector();
		
		this.events = new ArrayList<NewsEvent>();
		NewsEvent event1 = new NewsEvent("event-1");
		event1.setTotalScore(0.2);
		NewsEvent event2 = new NewsEvent("event-2");
		event2.setTotalScore(0.7);
		NewsEvent event3 = new NewsEvent("event-3");
		event3.setTotalScore(0.3);
		NewsEvent event4 = new NewsEvent("event-4");
		event4.setTotalScore(0.7);
		this.events.add(event1);
		this.events.add(event2);
		this.events.add(event3);
		this.events.add(event4);
	}

	@Test
	public void shouldReturnEvent2() {
		if (log.isInfoEnabled())
			log.info("shouldReturnEvent2");
		
		NewsEvent selectedEvent = this.selector.selectEvent(this.events);
		assertTrue(selectedEvent == this.events.get(1));
	}

}