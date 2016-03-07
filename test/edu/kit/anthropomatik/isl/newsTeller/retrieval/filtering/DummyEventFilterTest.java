package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.LogManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

public class DummyEventFilterTest {

	private DummyEventFilter dummyFilter;
	
	private Set<NewsEvent> events;
	
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
		
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		dummyFilter = (DummyEventFilter) context.getBean("filter0");
		((AbstractApplicationContext) context).close();
		
		this.events = new HashSet<NewsEvent>();
		this.events.add(new NewsEvent("event-1"));
		this.events.add(new NewsEvent("event-2"));
	}

	@Test
	public void shouldReturnFullSet() {
		Set<NewsEvent> filteredEvents = this.dummyFilter.filterEvents(events, null, null);
		assertTrue(filteredEvents.equals(events));
	}

}
