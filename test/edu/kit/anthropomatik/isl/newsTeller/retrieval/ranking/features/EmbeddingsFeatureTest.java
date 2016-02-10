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
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class EmbeddingsFeatureTest {

	private static EmbeddingsFeature avgFeature;
	private static EmbeddingsFeature minFeature;
	private static EmbeddingsFeature maxFeature;
	private static EmbeddingsFeature geomFeature;
	private static EmbeddingsFeature titleFeature;
	
	private static KnowledgeStoreAdapter ksAdapter;
	
	private static ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
	private static ConcurrentMap<String, Set<KSMention>> eventMentionCache = new ConcurrentHashMap<String, Set<KSMention>>();
	private static List<Keyword> keywords = new ArrayList<Keyword>();
	private static UserModel userModel = new DummyUserModel();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ConcurrentMap<String, Set<String>> eventMentionMap = new ConcurrentHashMap<String, Set<String>>();
		eventMentionMap.put("event-1", Sets.newHashSet("mention-1#char=4,7"));
		eventMentionMap.put("event-2", Sets.newHashSet("mention-2#char=4,7"));
		eventMentionMap.put("event-3", Sets.newHashSet("mention-3#char=4,7"));
		eventMentionMap.put("event-4", Sets.newHashSet("mention-4#char=4,7"));
		eventMentionMap.put("event-5", Sets.newHashSet("mention-3#char=4,7", "mention-2#char=4,7"));
		
		
		ConcurrentMap<String, Set<String>> resourceTextMap = new ConcurrentHashMap<String, Set<String>>();
		resourceTextMap.put("mention-1", Sets.newHashSet("volcano"));
		resourceTextMap.put("mention-2", Sets.newHashSet("A volcano erupted in Malysia today."));
		resourceTextMap.put("mention-3", Sets.newHashSet("The chicken crossed the road."));
		resourceTextMap.put("mention-4", Sets.newHashSet(""));
		
		ConcurrentMap<String, Set<String>> resourceTitleMap = new ConcurrentHashMap<String, Set<String>>();
		resourceTitleMap.put("mention-1", Sets.newHashSet("Mount Merapi volcano erupts"));
		resourceTitleMap.put("mention-2", Sets.newHashSet("Most European flights cancelled"));
		
		
		sparqlCache.put(Util.getRelationName("event", "mention", "volcano"), eventMentionMap);
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, resourceTextMap);
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_PROPERTY + Util.RESOURCE_PROPERTY_TITLE, resourceTitleMap);
		
		Keyword k = new Keyword("volcano");
		Util.stemKeyword(k);
		keywords.add(k);
		
		ApplicationContext context = new FileSystemXmlApplicationContext("config/rankingEmbeddingsFeatures.xml");
		avgFeature = (EmbeddingsFeature) context.getBean("embeddingsFeature00f");
		minFeature = (EmbeddingsFeature) context.getBean("embeddingsFeature11f");
		maxFeature = (EmbeddingsFeature) context.getBean("embeddingsFeature22f");
		geomFeature = (EmbeddingsFeature) context.getBean("embeddingsFeature33f");
		titleFeature = (EmbeddingsFeature) context.getBean("embeddingsFeature00t");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache);
	}

	@Test
	public void shouldReturnOneAvg() {
		double value = avgFeature.getValue("event-1", keywords, userModel);
		assertTrue(value == 1.0);
	}

	@Test
	public void shouldReturnOneMin() {
		double value = minFeature.getValue("event-1", keywords, userModel);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnOneMax() {
		double value = maxFeature.getValue("event-1", keywords, userModel);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnOneGeom() {
		double value = geomFeature.getValue("event-1", keywords, userModel);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZeroPointFiveFiveAvg() {
		double value = avgFeature.getValue("event-2", keywords, userModel);
		assertTrue(Math.abs(value - 0.5500468348569636) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointTwoAvg() {
		double value = avgFeature.getValue("event-3", keywords, userModel);
		assertTrue(Math.abs(value - 0.20774005092950987) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroAvg() {
		double value = avgFeature.getValue("event-4", keywords, userModel);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnZeroPointThreeSevenAvg() {
		double value = avgFeature.getValue("event-5", keywords, userModel);
		assertTrue(Math.abs(value - 0.3788934428932367) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointTwoMin() {
		double value = minFeature.getValue("event-5", keywords, userModel);
		assertTrue(Math.abs(value - 0.20774005092950987) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointFiveFiveMax() {
		double value = maxFeature.getValue("event-5", keywords, userModel);
		assertTrue(Math.abs(value - 0.5500468348569636) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointThreeThreeGeom() {
		double value = geomFeature.getValue("event-5", keywords, userModel);
		assertTrue(Math.abs(value - 0.3380336632449515) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointEightNineTitle() {
		double value = titleFeature.getValue("event-1", keywords, userModel);
		assertTrue(Math.abs(value - 0.8953319644352975) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointTwoFiveTitle() {
		double value = titleFeature.getValue("event-2", keywords, userModel);
		assertTrue(value == 0.2549590756690914);
	}
}
