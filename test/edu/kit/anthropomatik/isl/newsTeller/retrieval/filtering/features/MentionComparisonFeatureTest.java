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

public class MentionComparisonFeatureTest {

	private MentionComparisonFeature textFeature;
	private MentionComparisonFeature distanceFeature;
	private MentionComparisonFeature sentenceFeature;
	private MentionComparisonFeature intersectionFeature;
	
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
		textFeature = (MentionComparisonFeature) context.getBean("mentionTextComparisonFeature");
		distanceFeature = (MentionComparisonFeature) context.getBean("mentionDistanceComparisonFeature");
		sentenceFeature = (MentionComparisonFeature) context.getBean("mentionSentenceComparisonFeature");
		intersectionFeature = (MentionComparisonFeature) context.getBean("mentionIntersectionComparisonFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
	}

	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}

	@Test
	public void shouldReturnZeroForText() {
		double value = textFeature.getValue("http://en.wikinews.org/wiki/US_Federal_Reserve_prepares_to_take_over_AIG#ev39", null);
		assertTrue(value == 0.0);
	}

	@Test
	public void shouldReturnZeroForDistance() {
		double value = distanceFeature.getValue("http://en.wikinews.org/wiki/US_Federal_Reserve_prepares_to_take_over_AIG#ev39", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnZeroForSentence() {
		double value = sentenceFeature.getValue("http://en.wikinews.org/wiki/US_Federal_Reserve_prepares_to_take_over_AIG#ev39", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnZeroPointEightFourForIntersection() {
		double value = intersectionFeature.getValue("http://en.wikinews.org/wiki/US_Federal_Reserve_prepares_to_take_over_AIG#ev39", null);
		assertTrue(Math.abs(value - 0.8484848484848485) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnOneForText() {
		double value = textFeature.getValue("http://en.wikinews.org/wiki/67th_Annual_Golden_Globe_Award_highlights#ev30", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturn62ForDistance() {
		double value = distanceFeature.getValue("http://en.wikinews.org/wiki/67th_Annual_Golden_Globe_Award_highlights#ev30", null);
		assertTrue(value == 62.0);
	}
	
	@Test
	public void shouldReturnZeroForSentenceHost() {
		double value = sentenceFeature.getValue("http://en.wikinews.org/wiki/67th_Annual_Golden_Globe_Award_highlights#ev30", null);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnZeroPointOneSevenForIntersection() {
		double value = intersectionFeature.getValue("http://en.wikinews.org/wiki/67th_Annual_Golden_Globe_Award_highlights#ev30", null);
		assertTrue(Math.abs(value - 0.17391304347826086) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnOneForTextCartoon() {
		double value = textFeature.getValue("http://en.wikinews.org/wiki/University_of_Illinois_student_newspaper_runs_six_Prophet_Mohammed_cartoons#ev33", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturn15ForDistance() {
		double value = distanceFeature.getValue("http://en.wikinews.org/wiki/University_of_Illinois_student_newspaper_runs_six_Prophet_Mohammed_cartoons#ev33", null);
		assertTrue(value == 15.0);
	}
	
	@Test
	public void shouldReturnOneForSentence() {
		double value = sentenceFeature.getValue("http://en.wikinews.org/wiki/University_of_Illinois_student_newspaper_runs_six_Prophet_Mohammed_cartoons#ev33", null);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnOneForIntersection() {
		double value = intersectionFeature.getValue("http://en.wikinews.org/wiki/University_of_Illinois_student_newspaper_runs_six_Prophet_Mohammed_cartoons#ev33", null);
		assertTrue(value == 1.0);
	}
}
