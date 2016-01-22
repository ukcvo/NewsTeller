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

public class PropbankArgumentFeatureTest {

	private PropbankArgumentFeature feature;
	private PropbankArgumentFeature avgFeature;
	private PropbankArgumentFeature smartFeature;
	private PropbankArgumentFeature fallbackFeature;
	
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
		feature = (PropbankArgumentFeature) context.getBean("propbankArgumentFeature");
		avgFeature = (PropbankArgumentFeature) context.getBean("propbankAvgArgumentFeature");
		smartFeature = (PropbankArgumentFeature) context.getBean("smartPropbankArgumentFeature");
		fallbackFeature = (PropbankArgumentFeature) context.getBean("nombankFallbackArgumentFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
	}

	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}
	
	@Test
	public void shouldReturnOneThird() {
		double value = feature.getValue("http://en.wikinews.org/wiki/NSW_appeal_court_acquits_Jeffrey_Gilham_of_parents'_murders#ev49", null);
		assertTrue(Math.abs(value - (1.0/3)) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointFive() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Brazil_wins_Confederations_Cup#ev28", null);
		assertTrue(value == 0.5);
	}
	
	@Test
	public void shouldReturnOneThirdESA() {
		double value = feature.getValue("http://en.wikinews.org/wiki/ESA_launches_largest_commercial_telecom_satellite#ev22", null);
		assertTrue(Math.abs(value - (1.0/3)) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnAvgOneThird() {
		double value = avgFeature.getValue("http://en.wikinews.org/wiki/NSW_appeal_court_acquits_Jeffrey_Gilham_of_parents'_murders#ev49", null);
		assertTrue(Math.abs(value - (1.0/3)) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnAvgZeroPointThreeEight() {
		double value = avgFeature.getValue("http://en.wikinews.org/wiki/Brazil_wins_Confederations_Cup#ev28", null);
		assertTrue(Math.abs(value - 0.3833333333333333) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnAvgZeroPointZeroEight() {
		double value = avgFeature.getValue("http://en.wikinews.org/wiki/ESA_launches_largest_commercial_telecom_satellite#ev22", null);
		assertTrue(Math.abs(value - 0.0833333333333333) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnSmartOneThirdVerb() {
		double value = smartFeature.getValue("http://en.wikinews.org/wiki/ESA_launches_largest_commercial_telecom_satellite#ev22", null);
		assertTrue(Math.abs(value - (1.0/3)) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnSmartZeroNounNoEntry() {
		double value = smartFeature.getValue("http://en.wikinews.org/wiki/Polar_bear_Knut's_death_linked_to_encephalitis#ev28", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnSmartZeroNounNoEntry2() {
		double value = smartFeature.getValue("http://en.wikinews.org/wiki/Council_of_Australian_Governments_agree_on_reduced_environmental_regulation#ev48", null);
		assertTrue(value == 0.0);
	}
	
	
	@Test
	public void shouldReturnSmartZeroPointFiveVerb() {
		double value = smartFeature.getValue("http://en.wikinews.org/wiki/Brazil_wins_Confederations_Cup#ev28", null);
		assertTrue(value == 0.5);
	}
	
	@Test
	public void shouldReturnSmartOneThirdNoun() {
		double value = smartFeature.getValue("http://en.wikinews.org/wiki/Earthquake_reported_near_Rome's_coast,_no_damage_caused#ev10", null);
		assertTrue(Math.abs(value - (1.0/3)) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnSmartZeroNounNoArguments() {
		double value = smartFeature.getValue("http://en.wikinews.org/wiki/Icelandic_volcanic_eruption_prompts_evacuation,_flight_diversions#ev20", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnFallbackOneThirdNoun() {
		double value = fallbackFeature.getValue("http://en.wikinews.org/wiki/Council_of_Australian_Governments_agree_on_reduced_environmental_regulation#ev48", null);
		assertTrue(Math.abs(value - (1.0/3)) < Util.EPSILON);
	}
}
