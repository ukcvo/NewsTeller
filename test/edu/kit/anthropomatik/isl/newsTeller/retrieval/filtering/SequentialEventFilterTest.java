package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class SequentialEventFilterTest {

	private SequentialEventFilter filter;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	private Set<NewsEvent> events;
	private List<Keyword> keywords;
	private NewsEvent target;
	
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
		filter = (SequentialEventFilter) context.getBean("filter2a");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		
		this.keywords = new ArrayList<Keyword>();
		Keyword k = new Keyword("belief");
		Util.stemKeyword(k);
		keywords.add(k);
		this.events = new HashSet<NewsEvent>();
		this.target = new NewsEvent("http://en.wikinews.org/wiki/Brazil_wins_Confederations_Cup#ev22");
		events.add(target);
		events.add(new NewsEvent("http://en.wikinews.org/wiki/Brazil_wins_Confederations_Cup#ev23"));
		
		ksAdapter.openConnection();
	}

	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}
	
	@Test
	public void shouldReturnOneElementedResult() {
		Set<NewsEvent> result = filter.filterEvents(events, keywords);
		assertTrue(result.size() == 1 && result.contains(target));
	}
}
