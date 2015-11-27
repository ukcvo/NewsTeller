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
		double value = feature.getValue("http://en.wikinews.org/wiki/Pakistani_scientist_says_government_knew_about_nuclear_shipment_to_North_Korea#ev49");
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZero() {
		double value = feature.getValue("http://en.wikinews.org/wiki/9_US_soldiers_killed_in_Iraq_bombing;_20_others_wounded#ev27");
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnAboutZeroPointFive() {
		double value = feature.getValue("http://en.wikinews.org/wiki/SpaceX_successfully_test_fires_Falcon_9_rocket_in_Texas#ev27_1");
		assertTrue(Math.abs(value - 0.5384615384615384) < Util.EPSILON);
	}

}
