package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;

public class SparqlFeatureTest {

	private KnowledgeStoreAdapter ksAdapter;
	
	private SparqlFeature feature;
	
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
		feature = (SparqlFeature) context.getBean("hasKeywordAsLabelFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
	}

	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}
	
	@Test
	public void shouldReturnZero() {
		Keyword k = new Keyword("Hawking");
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		double result = feature.getValue("http://en.wikinews.org/wiki/14_US_soldiers_dead_after_helicopter_crash_in_Iraq#ev15", keywords);

		assertTrue(result == 0.0);
	}
	
	@Test
	public void shouldReturnOne() {
		Keyword k = new Keyword("Hawking");
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		double result = feature.getValue("http://en.wikinews.org/wiki/Stephen_Hawking_concludes_visit_to_Israel_and_Palestine#ev16", keywords);

		assertTrue(result == 1.0);
	}

}
