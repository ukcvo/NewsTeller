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

public class EntityMentionSurroundingsFeatureTest {

	private EntityMentionSurroundingsFeature previousFeature;
	private EntityMentionSurroundingsFeature nextFeature;
	
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
		previousFeature = (EntityMentionSurroundingsFeature) context.getBean("prepBeforeActorFeature");
		nextFeature = (EntityMentionSurroundingsFeature) context.getBean("prepAfterActorFeature");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
		
	}
	
	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}

	@Test
	public void shouldReturnOneForPrevious() {
		double value = previousFeature.getValue("http://en.wikinews.org/wiki/Ash-triggered_flight_disruptions_cost_airlines_$1.7_billion#ev19", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZeroForNext() {
		double value = nextFeature.getValue("http://en.wikinews.org/wiki/Ash-triggered_flight_disruptions_cost_airlines_$1.7_billion#ev19", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnOneForPreviousIran() {
		double value = previousFeature.getValue("http://en.wikinews.org/wiki/Iran_and_Britain_expel_diplomats_after_Iranian_presidential_election#ev36", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnOneThirdForNextIran() {
		double value = nextFeature.getValue("http://en.wikinews.org/wiki/Iran_and_Britain_expel_diplomats_after_Iranian_presidential_election#ev36", null);
		assertTrue(Math.abs(value - 0.3333333) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroForPrevious() {
		double value = previousFeature.getValue("http://en.wikinews.org/wiki/Archbishop_Levada_questioned_regarding_sex_abuse_cases_in_Portland#ev21", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnZeroPointFiveForNext() {
		double value = nextFeature.getValue("http://en.wikinews.org/wiki/Archbishop_Levada_questioned_regarding_sex_abuse_cases_in_Portland#ev21", null);
		assertTrue(value == 0.5);
	}
	
	@Test
	public void shouldReturnZeroForPreviousBomb() {
		double value = previousFeature.getValue("http://en.wikinews.org/wiki/Egyptians_conduct_roundup_of_bombing_suspects#ev52", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnZeroForPreviousKatia() {
		double value = previousFeature.getValue("http://en.wikinews.org/wiki/Icelandic_volcanic_eruption_prompts_evacuation,_flight_diversions#ev22", null);
		assertTrue(value == 0.0);
	}
	
}
