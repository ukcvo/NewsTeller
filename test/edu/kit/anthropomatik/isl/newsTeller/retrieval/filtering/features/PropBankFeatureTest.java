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

public class PropBankFeatureTest {

	private PropBankFeature feature;
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
		feature = (PropBankFeature) context.getBean("propBankFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
	}

	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}
	
	@Test
	public void shouldReturnZeroPointFive() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Pakistani_scientist_says_government_knew_about_nuclear_shipment_to_North_Korea#ev49");
		assertTrue(value == 0.5);
	}
	
	@Test
	public void shouldReturnOne() {
		double value = feature.getValue("http://en.wikinews.org/wiki/UN:_Ethiopian_GDP_grew_only_1.7%25_in_2009,_may_not_reach_anti-poverty_goals#ev24");
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnOneMultipleLables() {
		double value = feature.getValue("http://en.wikinews.org/wiki/NSW_appeal_court_acquits_Jeffrey_Gilham_of_parents'_murders#ev49");
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnOneForDance() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Brazil_wins_Confederations_Cup#ev30");
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZero() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Several_hundred_buried_in_mass_graves_in_Nigeria_following_clashes#ev27");
		assertTrue(value == 0.0);
	}
}
