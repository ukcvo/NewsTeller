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

public class PropbankNeedsArgumentFeatureTest {

	private PropbankNeedsArgumentFeature feature;
	private PropbankNeedsArgumentFeature locFeature;
	
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
		feature = (PropbankNeedsArgumentFeature) context.getBean("needsA1Feature");
		locFeature = (PropbankNeedsArgumentFeature) context.getBean("needsLocFeature");
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
		double value = feature.getValue("http://en.wikinews.org/wiki/Brookfield,_Wisconsin_man_charged_with_stealing_toilet_and_urinal_parts#ev30", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZero() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Brazil_wins_Confederations_Cup#ev28", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnOneLoc() {
		double value = locFeature.getValue("http://en.wikinews.org/wiki/Brookfield,_Wisconsin_man_charged_with_stealing_toilet_and_urinal_parts#ev30", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZeroLoc() {
		double value = locFeature.getValue("http://en.wikinews.org/wiki/Brazil_wins_Confederations_Cup#ev28", null);
		assertTrue(value == 0.0);
	}
}
