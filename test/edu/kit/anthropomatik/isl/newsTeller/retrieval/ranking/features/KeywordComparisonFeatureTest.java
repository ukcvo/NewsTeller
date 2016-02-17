package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import static org.junit.Assert.*;

import java.util.List;
import java.util.logging.LogManager;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.google.common.collect.Lists;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class KeywordComparisonFeatureTest {

	private static KeywordComparisonFeature avgFeature;
	private static KeywordComparisonFeature minFeature;
	private static KeywordComparisonFeature maxFeature;
	private static KeywordComparisonFeature geomFeature;
	
	private static List<Keyword> twoSimilarKeywords = Lists.newArrayList(new Keyword("eruption"), new Keyword("volcano"));
	private static List<Keyword> threeSimilarKeywords = Lists.newArrayList(new Keyword("volcano"), new Keyword("eruption"), new Keyword("lava"));
	private static List<Keyword> threeKeywordsOneOutlier = Lists.newArrayList(new Keyword("eruption"), new Keyword("volcano"), new Keyword("keyboard"));
	
	private static UserModel userModel = new DummyUserModel();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ApplicationContext context = new FileSystemXmlApplicationContext("config/testEmbeddings.xml");
		avgFeature = (KeywordComparisonFeature) context.getBean("keywordComparisonFeatureAvg");
		minFeature = (KeywordComparisonFeature) context.getBean("keywordComparisonFeatureMin");
		maxFeature = (KeywordComparisonFeature) context.getBean("keywordComparisonFeatureMax");
		geomFeature = (KeywordComparisonFeature) context.getBean("keywordComparisonFeatureGeom");
		((AbstractApplicationContext) context).close();
	}

	@Test
	public void shouldReturnZeroPointEightOneAvg() {
		double value = avgFeature.getValue("event-1", twoSimilarKeywords, userModel);
		assertTrue(Math.abs(value - 0.8189652009443932) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointSevenThreeAvg() {
		double value = avgFeature.getValue("event-1", threeSimilarKeywords, userModel);
		assertTrue(Math.abs(value - 0.735441) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointTwoTwoAvg() {
		double value = avgFeature.getValue("event-1", threeKeywordsOneOutlier, userModel);
		assertTrue(Math.abs(value - 0.22723395897158813) < Util.EPSILON);
	}

	@Test
	public void shouldReturnZeroPointEightOneMin() {
		double value = minFeature.getValue("event-1", twoSimilarKeywords, userModel);
		assertTrue(Math.abs(value - 0.8189652009443932) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointSixSixMin() {
		double value = minFeature.getValue("event-1", threeSimilarKeywords, userModel);
		assertTrue(Math.abs(value - 0.6694550195788995) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnMinusZeroPointOneThreeMin() {
		double value = minFeature.getValue("event-1", threeKeywordsOneOutlier, userModel);
		assertTrue(Math.abs(value + 0.13308640415965944) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointEightOneMaxTwo() {
		double value = maxFeature.getValue("event-1", twoSimilarKeywords, userModel);
		assertTrue(Math.abs(value - 0.8189652009443932) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointEightOneMaxThree() {
		double value = maxFeature.getValue("event-1", threeSimilarKeywords, userModel);
		assertTrue(Math.abs(value - 0.8189652009443932) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointEightOneMaxOutlier() {
		double value = maxFeature.getValue("event-1", threeKeywordsOneOutlier, userModel);
		assertTrue(Math.abs(value - 0.8189652009443932) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointEightOneGeom() {
		double value = geomFeature.getValue("event-1", twoSimilarKeywords, userModel);
		assertTrue(Math.abs(value - 0.8189652009443932) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointSevenThreeGeom() {
		double value = geomFeature.getValue("event-1", threeSimilarKeywords, userModel);
		System.out.println("2 " + value);
		assertTrue(Math.abs(value - 0.7328540520770158) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointZeroSevenGeom() {
		double value = geomFeature.getValue("event-1", threeKeywordsOneOutlier, userModel);
		System.out.println("3 " + value);
		assertTrue(Math.abs(value - 0.07692811562709764) < Util.EPSILON);
	}
}
