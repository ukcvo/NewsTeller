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

public class PrepPhraseFeatureTest {

	private PrepPhraseFeature feature;
	
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
		feature = (PrepPhraseFeature) context.getBean("prepPhraseFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
	}

	@After
	public void tearDown() throws Exception {
		ksAdapter.closeConnection();
	}

	@Test
	public void shouldReturnZeroBecauseNoActors() {
		double value = feature.getValue("http://en.wikinews.org/wiki/ESA_launches_largest_commercial_telecom_satellite#ev22", null);
		assertTrue(value == 0.0);
	}

	@Test
	public void shouldReturnZero() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Mexican_president_defends_emigration#ev18", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnOneQuarter() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Mexican_president_defends_emigration#ev19", null);
		assertTrue(Math.abs(value - 0.25) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnOneThird() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Amnesty_International_calls_for_Guantanamo_shutdown#ev29", null);
		assertTrue(Math.abs(value - (1.0/3)) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnOne() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Iceland_voters_reject_deal_to_repay_billions_to_UK,_Dutch#ev87", null);
		assertTrue(value == 1.0);
	}
}
