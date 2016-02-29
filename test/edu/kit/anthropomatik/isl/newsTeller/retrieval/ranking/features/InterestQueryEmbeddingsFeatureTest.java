package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.ActualUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class InterestQueryEmbeddingsFeatureTest {

	private static InterestQueryEmbeddingsFeature avgFeature;
	private static InterestQueryEmbeddingsFeature minFeature;
	private static InterestQueryEmbeddingsFeature maxFeature;
	
	private static List<Keyword> relatedKeyword = new ArrayList<Keyword>();
	private static List<Keyword> unrelatedKeyword = new ArrayList<Keyword>();
	private static List<Keyword> partiallyRelatedKeyword = new ArrayList<Keyword>();
	private static List<Keyword> twoKeywords = new ArrayList<Keyword>();
	private static UserModel userModel;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Keyword k = new Keyword("volcano");
		Util.stemKeyword(k);
		relatedKeyword.add(k);
		twoKeywords.add(k);
		
		List<Keyword> interests = new ArrayList<Keyword>();
		Keyword k2 = new Keyword("eruption");
		Util.stemKeyword(k2);
		interests.add(k2);
		
		Keyword k3 = new Keyword("lava");
		Util.stemKeyword(k3);
		interests.add(k3);
		
		Keyword k4 = new Keyword("keyboard");
		Util.stemKeyword(k4);
		unrelatedKeyword.add(k4);
		twoKeywords.add(k4);
		
		Keyword k5 = new Keyword("riot");
		Util.stemKeyword(k5);
		partiallyRelatedKeyword.add(k5);
		
		userModel = new ActualUserModel(interests);
		
		ApplicationContext context = new FileSystemXmlApplicationContext("config/testEmbeddings.xml");
		avgFeature = (InterestQueryEmbeddingsFeature) context.getBean("InterestQueryEmbeddingsFeatureAvg");
		minFeature = (InterestQueryEmbeddingsFeature) context.getBean("InterestQueryEmbeddingsFeatureMin");
		maxFeature = (InterestQueryEmbeddingsFeature) context.getBean("InterestQueryEmbeddingsFeatureMax");
		((AbstractApplicationContext) context).close();
	}

	@Test
	public void shouldReturnZeroPointSevenSixAvg() {
		double value = avgFeature.getValue("dummy", relatedKeyword, userModel);
		assertTrue(Math.abs(value - 0.7684339861521329) < Util.EPSILON);
	}

	@Test
	public void shouldReturnAboutZeroAvg() {
		double value = avgFeature.getValue("dummy", unrelatedKeyword, userModel);
		assertTrue(Math.abs(value + 0.012149214881243549) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointTwoNineAvg() {
		double value = avgFeature.getValue("dummy", partiallyRelatedKeyword, userModel);
		assertTrue(Math.abs(value - 0.2926759943188467) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointThreeSevenAvg() {
		double value = avgFeature.getValue("dummy", twoKeywords, userModel);
		assertTrue(Math.abs(value - 0.37814238563544467) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointSevenOneMin() {
		double value = minFeature.getValue("dummy", relatedKeyword, userModel);
		assertTrue(Math.abs(value - 0.7179027713598726) < Util.EPSILON);
	}

	@Test
	public void shouldReturnMinusZeroPointOneThreeMin() {
		double value = minFeature.getValue("dummy", unrelatedKeyword, userModel);
		assertTrue(Math.abs(value + 0.13308640415965944) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointTwoSixMin() {
		double value = minFeature.getValue("dummy", partiallyRelatedKeyword, userModel);
		assertTrue(Math.abs(value - 0.2630196785472441) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnMinusZeroPointOneThreeMinTwoKeywords() {
		double value = minFeature.getValue("dummy", twoKeywords, userModel);
		assertTrue(Math.abs(value + 0.13308640415965944) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointEightOneMax() {
		double value = maxFeature.getValue("dummy", relatedKeyword, userModel);
		assertTrue(Math.abs(value - 0.8189652009443932) < Util.EPSILON);
	}

	@Test
	public void shouldReturnZeroPointOneMax() {
		double value = maxFeature.getValue("dummy", unrelatedKeyword, userModel);
		assertTrue(Math.abs(value - 0.10878797439717235) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointThreeTwoMax() {
		double value = maxFeature.getValue("dummy", partiallyRelatedKeyword, userModel);
		assertTrue(Math.abs(value - 0.322332310090449277) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointEightOneMaxTwoKeywords() {
		double value = maxFeature.getValue("dummy", twoKeywords, userModel);
		assertTrue(Math.abs(value - 0.8189652009443932) < Util.EPSILON);
	}
	
	
}
