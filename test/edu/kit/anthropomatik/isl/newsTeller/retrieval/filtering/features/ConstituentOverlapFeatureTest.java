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

public class ConstituentOverlapFeatureTest {

	private ConstituentOverlapFeature feature;

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
		feature = (ConstituentOverlapFeature) context.getBean("overlapFeature");
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
		double value = feature.getValue("http://en.wikinews.org/wiki/Council_of_Australian_Governments_agree_on_reduced_environmental_regulation#ev48", null);
		assertTrue(value == 0.0);
	}

	@Test
	public void shouldReturnOne() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Air_Berlin_to_code-share_with_American_Airlines_and_Finnair_by_November#ev19", null);
		assertTrue(value == 1.0);
	}

	@Test
	public void shouldReturnTwo() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Air_Berlin_to_code-share_with_American_Airlines_and_Finnair_by_November#ev18", null);
		assertTrue(value == 2.0);
	}

	@Test
	public void shouldReturnZeroForFacebook() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Facebook_blocked_in_Bangladesh#ev8", null);
		assertTrue(value == 0.0);
	}

	@Test
	public void shouldReturnZeroForMerkel() {
		double value = feature.getValue("http://en.wikinews.org/wiki/EU_adopts_renewable_energy_measures#ev96", null);
		assertTrue(value == 0.0);
	}

	@Test
	public void shouldReturnOneForRiot() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Calm_returns_to_Salt,_Jordan_after_riots_over_police_shooting;_35_arrested#ev13", null);
		assertTrue(value == 1.0);
	}
}
