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

public class MentionPropertyFeatureTest {

	KnowledgeStoreAdapter ksAdapter;
	
	MentionPropertyFeature feature;
	
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
		feature = (MentionPropertyFeature) context.getBean("propbankRefFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
	}

	@After
	public void tearDown() throws Exception {
		ksAdapter.closeConnection();
	}

	@Test
	public void shouldReturnTwo() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Mexican_president_defends_emigration#ev22", null);
		assertTrue(value == 2.0);
	}

	@Test
	public void shouldReturnOne() {
		double value = feature.getValue("http://en.wikinews.org/wiki/AT&amp;T_to_buy_BellSouth_for_$67_billion#ev79", null);
		assertTrue(value == 1.0);
	}
	
}
