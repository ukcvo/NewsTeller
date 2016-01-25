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

public class ConstituentWordDistanceFeatureTest {

	private ConstituentWordDistanceFeature avgFeature;
	private ConstituentWordDistanceFeature minFeature;
	private ConstituentWordDistanceFeature maxFeature;
	private ConstituentWordDistanceFeature normFeature;
	
	
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
		avgFeature = (ConstituentWordDistanceFeature) context.getBean("avgWordDistanceFeature");
		minFeature = (ConstituentWordDistanceFeature) context.getBean("minWordDistanceFeature");
		maxFeature = (ConstituentWordDistanceFeature) context.getBean("maxWordDistanceFeature");
		normFeature = (ConstituentWordDistanceFeature) context.getBean("avgNormalizedWordDistanceFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
	}

	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}

	@Test
	public void shouldReturnZeroAvg() {
		double value = avgFeature.getValue("http://en.wikinews.org/wiki/Council_of_Australian_Governments_agree_on_reduced_environmental_regulation#ev32", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnZeroMin() {
		double value = minFeature.getValue("http://en.wikinews.org/wiki/Council_of_Australian_Governments_agree_on_reduced_environmental_regulation#ev32", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnZeroMax() {
		double value = maxFeature.getValue("http://en.wikinews.org/wiki/Council_of_Australian_Governments_agree_on_reduced_environmental_regulation#ev32", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnZeroNorm() {
		double value = normFeature.getValue("http://en.wikinews.org/wiki/Council_of_Australian_Governments_agree_on_reduced_environmental_regulation#ev32", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnOneAvg() {
		double value = avgFeature.getValue("http://en.wikinews.org/wiki/Council_of_Australian_Governments_agree_on_reduced_environmental_regulation#ev48", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnOneMin() {
		double value = minFeature.getValue("http://en.wikinews.org/wiki/Council_of_Australian_Governments_agree_on_reduced_environmental_regulation#ev48", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnOneMax() {
		double value = maxFeature.getValue("http://en.wikinews.org/wiki/Council_of_Australian_Governments_agree_on_reduced_environmental_regulation#ev48", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZeroPointZeroFourNorm() {
		double value = normFeature.getValue("http://en.wikinews.org/wiki/Council_of_Australian_Governments_agree_on_reduced_environmental_regulation#ev48", null);
		assertTrue(Math.abs(value - 0.041666666666666664) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnTwoThirdsAvg() {
		double value = avgFeature.getValue("http://en.wikinews.org/wiki/EU_adopts_renewable_energy_measures#ev96", null);
		assertTrue(Math.abs(value - (2.0/3)) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroMinEU() {
		double value = minFeature.getValue("http://en.wikinews.org/wiki/EU_adopts_renewable_energy_measures#ev96", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnTwoMax() {
		double value = maxFeature.getValue("http://en.wikinews.org/wiki/EU_adopts_renewable_energy_measures#ev96", null);
		assertTrue(value == 2.0);
	}
	
	@Test
	public void shouldReturnZeroPointZeroSixNorm() {
		double value = normFeature.getValue("http://en.wikinews.org/wiki/EU_adopts_renewable_energy_measures#ev96", null);
		assertTrue(Math.abs(value - 0.06060606060606061) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnNegativeOneMin() {
		double value = minFeature.getValue("http://en.wikinews.org/wiki/Wikinews_interviews_Pa%c3%bal_M._Velazco_about_new_yellow-shouldered_bat_species#ev155", null);
		assertTrue(value == -1);
	}
}
