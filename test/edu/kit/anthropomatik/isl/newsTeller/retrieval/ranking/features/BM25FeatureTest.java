package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.LogManager;

import org.junit.Before;
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

public class BM25FeatureTest {

	private BM25Feature sentenceFeature;
	private BM25Feature textFeature;
	
	private KnowledgeStoreAdapter ksAdapter;
	
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
		eventMentionMap.put("event-2", Sets.newHashSet("mention-2#char=4,7", "mention-3#char=4,7"));
		
		ConcurrentMap<String, Set<String>> resourceTextMap = new ConcurrentHashMap<String, Set<String>>();
		resourceTextMap.put("mention-1", Sets.newHashSet("This sentence is not about Barack Obama. The next one is not, either."));
		resourceTextMap.put("mention-2", Sets.newHashSet("Here, there is no useful information. But here we mention Obama!"));
		resourceTextMap.put("mention-3", Sets.newHashSet("This sentence is about Obama being mentioned by his last name."));
		
		sparqlCache.put(Util.getRelationName("event", "mention", "Barack Obama"), eventMentionMap);
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, resourceTextMap);
		
		
		Keyword k = new Keyword("Barack Obama");
		Util.stemKeyword(k);
		keywords.add(k);
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		sentenceFeature = (BM25Feature) context.getBean("BM25FeatureSentence1.5");
		textFeature = (BM25Feature) context.getBean("BM25FeatureText2.0");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache);
	}

	@Test
	public void shouldReturnOnePointFiveFourSentence() {
		double value = sentenceFeature.getValue("event-1", keywords, userModel);
		assertTrue(Math.abs(value - 3.1237994879375193) < Util.EPSILON);
	}

	@Test
	public void shouldReturnZeroSentence() {
		double value = sentenceFeature.getValue("event-2", keywords, userModel);
		assertTrue(value == 0.8421053994957518);
	}
	
	@Test
	public void shouldReturnOnePointSevenFiveText() {
		double value = textFeature.getValue("event-1", keywords, userModel);
		assertTrue(Math.abs(value - 2.6105264717760224) < Util.EPSILON);
	}

	@Test
	public void shouldReturnZeroText() {
		double value = textFeature.getValue("event-2", keywords, userModel);
		assertTrue(value == 0.0);
	}
}