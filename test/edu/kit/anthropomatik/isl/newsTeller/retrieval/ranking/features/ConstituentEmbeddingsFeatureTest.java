package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.LogManager;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.google.common.collect.Sets;

import edu.kit.anthropomatik.isl.newsTeller.data.KSMention;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.userModel.ActualUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class ConstituentEmbeddingsFeatureTest {

	private static ConstituentEmbeddingsFeature avgFeature;
	private static ConstituentEmbeddingsFeature interestFeature;
	
	private static KnowledgeStoreAdapter ksAdapter;
	
	private static ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
	private static ConcurrentMap<String, Set<KSMention>> eventMentionCache = new ConcurrentHashMap<String, Set<KSMention>>();
	private static List<Keyword> keywords = new ArrayList<Keyword>();
	private static UserModel dummyUserModel = new DummyUserModel();
	private static UserModel userModel;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ConcurrentMap<String, Set<String>> eventEntityMap = new ConcurrentHashMap<String, Set<String>>();
		eventEntityMap.put("event-1", Sets.newHashSet("entity-1"));
		eventEntityMap.put("event-2", Sets.newHashSet("entity-2"));
		eventEntityMap.put("event-3", Sets.newHashSet("entity-3"));
		eventEntityMap.put("event-4", Sets.newHashSet("entity-4"));
		eventEntityMap.put("event-5", Sets.newHashSet("entity-2", "entity-3"));
		
		
		ConcurrentMap<String, Set<String>> enitityLabelMap = new ConcurrentHashMap<String, Set<String>>();
		enitityLabelMap.put("entity-1", Sets.newHashSet("volcano"));
		enitityLabelMap.put("entity-2", Sets.newHashSet("A volcano erupted in Malysia today."));
		enitityLabelMap.put("entity-3", Sets.newHashSet("The chicken crossed the road."));
		enitityLabelMap.put("entity-4", Sets.newHashSet(""));
		
		sparqlCache.put(Util.getRelationName("event", "entity", "volcano"), eventEntityMap);
		sparqlCache.put(Util.getRelationName("entity", "entityPrefLabel", "volcano"), enitityLabelMap);
		
		Keyword k = new Keyword("volcano");
		Util.stemKeyword(k);
		keywords.add(k);
		
		userModel = new ActualUserModel(keywords);
		
		ApplicationContext context = new FileSystemXmlApplicationContext("config/testEmbeddings.xml");
		avgFeature = (ConstituentEmbeddingsFeature) context.getBean("constituentEmbeddingsFeatureAvg");
		interestFeature = (ConstituentEmbeddingsFeature) context.getBean("constituentEmbeddingsFeatureAvgUM");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache);
	}

	@Test
	public void shouldReturnOneAvg() {
		double value = avgFeature.getValue("event-1", keywords, dummyUserModel);
		assertTrue(value == 1.0);
	}

	@Test
	public void shouldReturnZeroPointSixEightAvg() {
		double value = avgFeature.getValue("event-2", keywords, dummyUserModel);
		assertTrue(Math.abs(value - 0.6880536740415572) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointTwoAvg() {
		double value = avgFeature.getValue("event-3", keywords, dummyUserModel);
		assertTrue(Math.abs(value - 0.20774005092950987) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroAvg() {
		double value = avgFeature.getValue("event-4", keywords, dummyUserModel);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnZeroPointFourFourAvg() {
		double value = avgFeature.getValue("event-5", keywords, dummyUserModel);
		assertTrue(Math.abs(value - 0.4478968624855335) < Util.EPSILON);
	}

	@Test
	public void shouldReturnOneAvgUM() {
		double value = interestFeature.getValue("event-1", keywords, userModel);
		assertTrue(value == 1.0);
	}

	@Test
	public void shouldReturnZeroPointSixEightAvgUM() {
		double value = interestFeature.getValue("event-2", keywords, userModel);
		assertTrue(Math.abs(value - 0.6880536740415572) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointTwoAvgUM() {
		double value = interestFeature.getValue("event-3", keywords, userModel);
		assertTrue(Math.abs(value - 0.20774005092950987) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroAvgUM() {
		double value = interestFeature.getValue("event-4", keywords, userModel);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnZeroPointFourFourAvgUM() {
		double value = interestFeature.getValue("event-5", keywords, userModel);
		assertTrue(Math.abs(value - 0.4478968624855335) < Util.EPSILON);
	}
}
