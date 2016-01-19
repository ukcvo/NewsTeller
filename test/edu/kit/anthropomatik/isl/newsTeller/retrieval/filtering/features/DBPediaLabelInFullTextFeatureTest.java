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

public class DBPediaLabelInFullTextFeatureTest {

	private DBPediaLabelInFullTextFeature feature;
	private DBPediaLabelInFullTextFeature keywordFeature;
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
		feature = (DBPediaLabelInFullTextFeature) context.getBean("appearLabelsInTextFeature");
		keywordFeature = (DBPediaLabelInFullTextFeature) context.getBean("appearKeywordLabelsInTextFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
	}

	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}

	@Test
	public void ShouldReturnZero() {
		Keyword k = new Keyword("Michael Jackson");
		Util.stemKeyword(k);
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		
		double value = feature.getValue("http://en.wikinews.org/wiki/'Buried'_video_surfaces_of_police_making_mass_arrests_during_the_Republican_National_Convention#ev62", keywords);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void ShouldReturnOne() {
		Keyword k = new Keyword("Flying disc");
		Util.stemKeyword(k);
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		
		double value = feature.getValue("http://en.wikinews.org/wiki/Walter_Frederick_Morrison,_inventor_of_frisbee,_dies_at_age_90#ev30", keywords);
		assertTrue(value == 1.0);
	}

	@Test
	public void ShouldReturnZeroPointFour() {
		Keyword k = new Keyword("comedy");
		Util.stemKeyword(k);
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		
		double value = feature.getValue("http://en.wikinews.org/wiki/US_nuclear_security_director_asked_to_resign#ev10", keywords);
		assertTrue(Math.abs(value - 0.4) < Util.EPSILON);
	}
	
	@Test
	public void ShouldReturnZeroForKeyword() {
		Keyword k = new Keyword("actor");
		Util.stemKeyword(k);
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		
		double value = keywordFeature.getValue("http://en.wikinews.org/wiki/US_nuclear_security_director_asked_to_resign#ev10", keywords);
		assertTrue(value == 0.0);
	}
	
}
