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

public class POSFeatureTest {

	private POSFeature posFeature;
	
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
		posFeature = (POSFeature) context.getBean("posFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
	}

	@After
	public void tearDown() throws Exception {
		ksAdapter.closeConnection();
	}

	@Test
	public void shouldReturnOne() {
		double value = posFeature.getValue("http://en.wikinews.org/wiki/Mexican_president_defends_emigration#ev22", null);
		assertTrue(value == 1.0);
	}

	@Test
	public void shouldReturnZero() {
		double value = posFeature.getValue("http://en.wikinews.org/wiki/AT&amp;T_to_buy_BellSouth_for_$67_billion#ev79", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnThreeQuarters() {
		double value = posFeature.getValue("http://en.wikinews.org/wiki/Federer_wins_Pacific_Life_Open_in_Indian_Wells#ev25", null);
		assertTrue(value == 0.75);
	}
	
}
