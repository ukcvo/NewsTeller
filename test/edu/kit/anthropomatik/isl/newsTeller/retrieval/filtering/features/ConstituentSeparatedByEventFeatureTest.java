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

public class ConstituentSeparatedByEventFeatureTest {

	private ConstituentSeparatedByEventFeature avgFeature;
	private ConstituentSeparatedByEventFeature maxFeature;
	
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
		avgFeature = (ConstituentSeparatedByEventFeature) context.getBean("avgConstituentSeparatedByEventFeature");
		maxFeature = (ConstituentSeparatedByEventFeature) context.getBean("maxConstituentSeparatedByEventFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
	}

	@After
	public void tearDown() throws Exception {
		ksAdapter.closeConnection();
	}

	@Test
	public void shouldReturnZeroPointFive() {
		double value = avgFeature.getValue("http://en.wikinews.org/wiki/Brookfield,_Wisconsin_man_charged_with_stealing_toilet_and_urinal_parts#ev25", null);
		assertTrue(value == 0.5);
	}
	
	@Test
	public void shouldReturnZero() {
		double value = avgFeature.getValue("http://en.wikinews.org/wiki/Brookfield,_Wisconsin_man_charged_with_stealing_toilet_and_urinal_parts#ev30", null);
		assertTrue(value == 0.0);
	}

	@Test
	public void shouldReturnOneMax() {
		double value = maxFeature.getValue("http://en.wikinews.org/wiki/Brookfield,_Wisconsin_man_charged_with_stealing_toilet_and_urinal_parts#ev25", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZeroMax() {
		double value = maxFeature.getValue("http://en.wikinews.org/wiki/Brookfield,_Wisconsin_man_charged_with_stealing_toilet_and_urinal_parts#ev30", null);
		assertTrue(value == 0.0);
	}
}
