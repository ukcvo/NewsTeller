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

public class ConstituentsPerMentionFeatureTest {

	private ConstituentsPerMentionFeature avgFeature;
	private ConstituentsPerMentionFeature maxFeature;
	private ConstituentsPerMentionFeature minFeature;
	private ConstituentsPerMentionFeature zeroFeature;
	private ConstituentsPerMentionFeature nonzeroFeature;
	
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
		avgFeature = (ConstituentsPerMentionFeature) context.getBean("avgConstituentsPerMentionFeature");
		maxFeature = (ConstituentsPerMentionFeature) context.getBean("maxConstituentsPerMentionFeature");
		minFeature = (ConstituentsPerMentionFeature) context.getBean("minConstituentsPerMentionFeature");
		zeroFeature = (ConstituentsPerMentionFeature) context.getBean("zeroConstituentsPerMentionFeature");
		nonzeroFeature = (ConstituentsPerMentionFeature) context.getBean("nonzeroConstituentsPerMentionFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
	}

	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}
	
	@Test
	public void shouldReturnOneAvgSingleMention() {
		double value = avgFeature.getValue("http://en.wikinews.org/wiki/NSW_appeal_court_acquits_Jeffrey_Gilham_of_parents'_murders#ev49", null);
		assertTrue(value == 1.0);
	}

	@Test
	public void shouldReturnOneMaxSingleMention() {
		double value = maxFeature.getValue("http://en.wikinews.org/wiki/NSW_appeal_court_acquits_Jeffrey_Gilham_of_parents'_murders#ev49", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnOneMinSingleMention() {
		double value = minFeature.getValue("http://en.wikinews.org/wiki/NSW_appeal_court_acquits_Jeffrey_Gilham_of_parents'_murders#ev49", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZeroZeroSingleMention() {
		double value = zeroFeature.getValue("http://en.wikinews.org/wiki/NSW_appeal_court_acquits_Jeffrey_Gilham_of_parents'_murders#ev49", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnOneNonzeroSingleMention() {
		double value = nonzeroFeature.getValue("http://en.wikinews.org/wiki/NSW_appeal_court_acquits_Jeffrey_Gilham_of_parents'_murders#ev49", null);
		assertTrue(value == 1.0);
	}
	
	
	
	@Test
	public void shouldReturnZeroPointFiveAvg() {
		double value = avgFeature.getValue("http://en.wikinews.org/wiki/Thousands_to_celebrate_twenty_years_since_fall_of_Berlin_Wall#ev74", null);
		assertTrue(value == 0.5);
	}
	
	@Test
	public void shouldReturnOneMax() {
		double value = maxFeature.getValue("http://en.wikinews.org/wiki/Thousands_to_celebrate_twenty_years_since_fall_of_Berlin_Wall#ev74", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZeroMin() {
		double value = minFeature.getValue("http://en.wikinews.org/wiki/Thousands_to_celebrate_twenty_years_since_fall_of_Berlin_Wall#ev74", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnZeroPointFiveZero() {
		double value = zeroFeature.getValue("http://en.wikinews.org/wiki/Thousands_to_celebrate_twenty_years_since_fall_of_Berlin_Wall#ev74", null);
		assertTrue(value == 0.5);
	}
	
	@Test
	public void shouldReturnOneNonzero() {
		double value = nonzeroFeature.getValue("http://en.wikinews.org/wiki/Thousands_to_celebrate_twenty_years_since_fall_of_Berlin_Wall#ev74", null);
		assertTrue(value == 1.0);
	}
	
	
	
	@Test
	public void shouldReturnThreeQuartersAvg() {
		double value = avgFeature.getValue("http://en.wikinews.org/wiki/American_bandleader_Kevin_Eubanks_to_leave_'Tonight_Show'#ev22", null);
		assertTrue(value == 0.75);
	}
	
	@Test
	public void shouldReturnOneMaxLeno() {
		double value = maxFeature.getValue("http://en.wikinews.org/wiki/American_bandleader_Kevin_Eubanks_to_leave_'Tonight_Show'#ev22", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZeroPointFiveMin() {
		double value = minFeature.getValue("http://en.wikinews.org/wiki/American_bandleader_Kevin_Eubanks_to_leave_'Tonight_Show'#ev22", null);
		assertTrue(value == 0.5);
	}
	
	@Test
	public void shouldReturnZeroZero() {
		double value = zeroFeature.getValue("http://en.wikinews.org/wiki/American_bandleader_Kevin_Eubanks_to_leave_'Tonight_Show'#ev22", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnTwoNonzero() {
		double value = nonzeroFeature.getValue("http://en.wikinews.org/wiki/American_bandleader_Kevin_Eubanks_to_leave_'Tonight_Show'#ev22", null);
		assertTrue(value == 2.0);
	}
	
	
	@Test
	public void shouldReturnOneAvg() {
		double value = avgFeature.getValue("http://en.wikinews.org/wiki/US_Federal_Reserve_prepares_to_take_over_AIG#ev39", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnOneMaxLehman() {
		double value = maxFeature.getValue("http://en.wikinews.org/wiki/US_Federal_Reserve_prepares_to_take_over_AIG#ev39", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnOneMin() {
		double value = minFeature.getValue("http://en.wikinews.org/wiki/US_Federal_Reserve_prepares_to_take_over_AIG#ev39", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZeroZeroLehman() {
		double value = zeroFeature.getValue("http://en.wikinews.org/wiki/US_Federal_Reserve_prepares_to_take_over_AIG#ev39", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnTwoNonzeroLehman() {
		double value = nonzeroFeature.getValue("http://en.wikinews.org/wiki/US_Federal_Reserve_prepares_to_take_over_AIG#ev39", null);
		assertTrue(value == 2.0);
	}
}
