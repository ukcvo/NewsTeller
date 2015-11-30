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
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class KeywordInFullTextFeatureTest {

	private KnowledgeStoreAdapter ksAdapter;
	
	private KeywordInFullTextFeature onlyKeywordFeature;
	private KeywordInFullTextFeature onlyStemFeature;
	private KeywordInFullTextFeature keywordAndStemFeature;
	
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
		onlyKeywordFeature = (KeywordInFullTextFeature) context.getBean("keywordInTextFeature");
		onlyStemFeature = (KeywordInFullTextFeature) context.getBean("keywordStemInTextFeature");
		keywordAndStemFeature = (KeywordInFullTextFeature) context.getBean("keywordOrStemInTextFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
	}

	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}

	@Test
	public void shouldReturnZeroForKeyword() {
		Keyword k = new Keyword("Michael Jackson");
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		
		double result = this.onlyKeywordFeature.getValue("http://en.wikinews.org/wiki/Autopsy_reveals_that_Terri_Schiavo_was_in_a_persistent_vegetative_state#ev17", keywords);
		
		assertTrue(result == 0.0);
	}

	@Test
	public void shouldReturnOneForKeyword() {
		Keyword k = new Keyword("volcano");
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		
		double result = this.onlyKeywordFeature.getValue("http://en.wikinews.org/wiki/11,000_evacuated_in_Indonesia_as_Mount_Merapi_threatens_to_erupt#ev16", keywords);
		
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnOneForStem() {
		Keyword k = new Keyword("dance");
		Util.stemKeyword(k);
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		
		double result = this.onlyStemFeature.getValue("http://en.wikinews.org/wiki/BBC's_Strictly_Come_Dancing_to_be_broadcast_from_Blackpool_Tower#ev13", keywords);
		
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroForStem() {
		Keyword k = new Keyword("Michael Jackson");
		Util.stemKeyword(k);
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		
		double result = this.onlyStemFeature.getValue("http://en.wikinews.org/wiki/Autopsy_reveals_that_Terri_Schiavo_was_in_a_persistent_vegetative_state#ev17", keywords);
		
		assertTrue(result == 0.0);
	}
	
	@Test
	public void shouldReturnOneForBothBecauseOfKeyword() {
		Keyword k = new Keyword("comedy");
		Util.stemKeyword(k);
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		
		double result = this.keywordAndStemFeature.getValue("http://en.wikinews.org/wiki/US_TV:_Jay_Leno_bested_by_Conan_O'Brien_in_late_night_ratings#ev30", keywords);
		
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnOneForBothBecauseOfStem() {
		Keyword k = new Keyword("charity");
		Util.stemKeyword(k);
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		
		double result = this.keywordAndStemFeature.getValue("http://en.wikinews.org/wiki/Australian_Senate_Committee_recommends_formation_of_Charities_Commission#ev38_7", keywords);
		
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroForBoth() {
		Keyword k = new Keyword("power station");
		Util.stemKeyword(k);
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		
		double result = this.keywordAndStemFeature.getValue("http://en.wikinews.org/wiki/Nuclear_leaks_after_Japan_quake_are_worse_than_first_reported#ev3", keywords);
		
		assertTrue(result == 0.0);
	}
}
