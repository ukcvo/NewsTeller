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

public class KeywordStemInFullTextFeatureTest {

	private KnowledgeStoreAdapter ksAdapter;
	
	private KeywordStemInFullTextFeature feature;
	
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
		feature = (KeywordStemInFullTextFeature) context.getBean("keywordStemInTextFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
	}

	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}

	@Test
	public void shouldReturnOne() {
		Keyword k = new Keyword("dance");
		k.setStem("danc");
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		
		double result = this.feature.getValue("http://en.wikinews.org/wiki/BBC's_Strictly_Come_Dancing_to_be_broadcast_from_Blackpool_Tower#ev13", keywords);
		
		assertTrue(result == 1.0);
	}

	@Test
	public void shouldReturnZero() {
		Keyword k = new Keyword("Michael Jackson");
		k.setStem("Michael Jackson");
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		
		double result = this.feature.getValue("http://en.wikinews.org/wiki/Autopsy_reveals_that_Terri_Schiavo_was_in_a_persistent_vegetative_state#ev17", keywords);
		
		assertTrue(result == 0.0);
	}
}
