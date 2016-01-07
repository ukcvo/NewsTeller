package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import static org.junit.Assert.*;

import java.util.logging.LogManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class WordNetVerbCountFeatureTest {

	private WordNetVerbCountFeature feature;
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
		feature = (WordNetVerbCountFeature) context.getBean("wordNetFeature");
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
		double value = feature.getValue("http://en.wikinews.org/wiki/Pranab_and_Oli_discuss_Nepal_peace_talks#ev34", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZero() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Two_boys_dead_after_their_father_throws_them_off_hotel_balcony#ev33", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnAboutZeroPointFourFive() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Nick_Smith_responds_to_claims_he_is_New_Zealand's_worst_behaved_politician#ev33", null);
		assertTrue(Math.abs(value - 0.4444444444) < Util.EPSILON);
	}

}
