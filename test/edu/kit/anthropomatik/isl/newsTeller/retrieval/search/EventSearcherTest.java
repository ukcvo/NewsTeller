package edu.kit.anthropomatik.isl.newsTeller.retrieval.search;

import static org.junit.Assert.*;

import java.util.ArrayList;
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
import edu.kit.anthropomatik.isl.newsTeller.retrieval.search.EventSearcher;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

public class EventSearcherTest {

	private EventSearcher searcher;
	private UserModel userModel;
	private KnowledgeStoreAdapter ksAdapter;
	
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
		searcher = (EventSearcher) context.getBean("searcher");
		userModel = (UserModel) context.getBean("userModel0");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
	}
	
	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}

	@Test
	public void shouldReturn93Events() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(new Keyword("artificial intelligence"));
		Set<NewsEvent> result = searcher.findEvents(keywords, userModel);
		assertTrue(result.size() == 93);
	}

	@Test
	public void shouldReturn211Events() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(new Keyword("erupt"));
		Set<NewsEvent> result = searcher.findEvents(keywords, userModel);
		assertTrue(result.size() == 211);
	}
	
	@Test
	public void shouldReturn52Events() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(new Keyword("Rafael Nadal"));
		Set<NewsEvent> result = searcher.findEvents(keywords, userModel);
		assertTrue(result.size() == 52);
	}
	
	@Test
	public void shouldReturn50Events() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(new Keyword("Roger Federer"));
		Set<NewsEvent> result = searcher.findEvents(keywords, userModel);
		assertTrue(result.size() == 50);
	}
	
	@Test
	public void shouldReturn95EventsForTwoKeywordQuery() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(new Keyword("Rafael Nadal"));
		keywords.add(new Keyword("Roger Federer"));
		Set<NewsEvent> result = searcher.findEvents(keywords, userModel);
		assertTrue(result.size() == 95);
	}
}
