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

public class KeywordEntityMatchingKeywordFeatureTest {

	private KeywordEntityMatchingKeywordFeature feature;
	private KeywordEntityMatchingKeywordFeature descriptionFeature;

	private KnowledgeStoreAdapter ksAdapter;

	@BeforeClass
	public static void setUpBeforeClass() {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Before
	public void setUp() {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		feature = (KeywordEntityMatchingKeywordFeature) context.getBean("keywordEntityMatchingKeywordFeature");
		descriptionFeature = (KeywordEntityMatchingKeywordFeature) context.getBean("keywordEntityDescriptionMatchingKeywordFeature");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();

	}

	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}

	@Test
	public void shouldReturnZeroNoEntity() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		Keyword k = new Keyword("erupt");
		Util.stemKeyword(k);
		keywords.add(k);
		double value = feature.getValue("http://en.wikinews.org/wiki/Ash-triggered_flight_disruptions_cost_airlines_$1.7_billion#ev19", keywords);
		assertTrue(value == 0.0);
	}

	@Test
	public void shouldReturnOne() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		Keyword k = new Keyword("artificial intelligence");
		Util.stemKeyword(k);
		keywords.add(k);
		double value = feature.getValue("http://en.wikinews.org/wiki/Possible_bodies,_wreckage_from_Air_France_Flight_447_found#ev45", keywords);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnOneForDescription() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		Keyword k = new Keyword("artificial intelligence");
		Util.stemKeyword(k);
		keywords.add(k);
		double value = descriptionFeature.getValue("http://en.wikinews.org/wiki/Possible_bodies,_wreckage_from_Air_France_Flight_447_found#ev45", keywords);
		assertTrue(value == 1.0);
	}

	@Test
	public void shouldReturnZeroNoMatch() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		Keyword k = new Keyword("Hawking");
		Util.stemKeyword(k);
		keywords.add(k);
		double value = feature.getValue("http://en.wikinews.org/wiki/Celtics_lose,_extend_streak#ev20", keywords);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnZeroRome() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		Keyword k = new Keyword("Rome");
		Util.stemKeyword(k);
		keywords.add(k);
		double value = feature.getValue("http://en.wikinews.org/wiki/Gunman_shoots_doctor,_then_kills_mother_and_self_at_Maryland_hospital#ev63", keywords);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnOneKatrina() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		Keyword k = new Keyword("hurricane Katrina");
		Util.stemKeyword(k);
		keywords.add(k);
		double value = feature.getValue("http://en.wikinews.org/wiki/E-Inquiries_-_Bring_Public_Inquiries_to_the_Net#ev58_1", keywords);
		assertTrue(value == 1.0);
	}	
	
	
}
