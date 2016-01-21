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

public class GenitiveFeatureTest {

	private GenitiveFeature feature;
	
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
		feature = (GenitiveFeature) context.getBean("genitiveFeature");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
		
	}
	
	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}

	@Test
	public void shouldReturnZero() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Ash-triggered_flight_disruptions_cost_airlines_$1.7_billion#ev19", null);
		assertTrue(value == 0.0);
	}

	@Test
	public void shouldReturnOne() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Australia_will_help_in_East_Timor_if_requested:_Downer#ev46", null);
		assertTrue(value == 1.0);
	}
	
}
