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

public class SameDocumentFeatureTest {

	private SameDocumentFeature textFeature;
	private SameDocumentFeature sentenceFeature;
	private SameDocumentFeature normalizedFeature;
	private SameDocumentFeature minFeature;
	private SameDocumentFeature maxFeature;
	
	
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
		eventMentionMap.put("event-1", Sets.newHashSet("mention-1#char=5,13"));
		eventMentionMap.put("event-2", Sets.newHashSet("mention-1#char=14,16", "mention-3#char=5,13"));
		eventMentionMap.put("event-3", Sets.newHashSet("mention-2#char=38,41"));
		eventMentionMap.put("event-4", Sets.newHashSet("mention-2#char=0,4"));
		eventMentionMap.put("event-5", Sets.newHashSet("mention-4#char=0,4"));
		
		ConcurrentMap<String, Set<String>> resourceTextMap = new ConcurrentHashMap<String, Set<String>>();
		resourceTextMap.put("mention-1", Sets.newHashSet("This sentence is not about Barack Obama. The next one is not, either."));
		resourceTextMap.put("mention-2", Sets.newHashSet("Here, there is no useful information. But here we mention Obama!"));
		resourceTextMap.put("mention-3", Sets.newHashSet("This sentence is about Obama being mentioned by his last name."));
		resourceTextMap.put("mention-4", Sets.newHashSet("This is just another single-sentence-thing."));
		
		
		sparqlCache.put(Util.getRelationName("event", "mention", "Barack Obama"), eventMentionMap);
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, resourceTextMap);
		
		
		Keyword k = new Keyword("Barack Obama");
		Util.stemKeyword(k);
		keywords.add(k);
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		textFeature = (SameDocumentFeature) context.getBean("sameDocumentFeature0ff");
		sentenceFeature = (SameDocumentFeature) context.getBean("sameDocumentFeature0tf");
		normalizedFeature = (SameDocumentFeature) context.getBean("sameDocumentFeature0ft");
		minFeature = (SameDocumentFeature) context.getBean("sameDocumentFeature1ff");
		maxFeature = (SameDocumentFeature) context.getBean("sameDocumentFeature2ff");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache);
	}

	@Test
	public void shouldReturnTwoText() {
		double value = textFeature.getValue("event-1", keywords, userModel);
		assertTrue(value == 2.0);
	}

	@Test
	public void shouldReturnOnePointFiveText() {
		double value = textFeature.getValue("event-2", keywords, userModel);
		assertTrue(value == 1.5);
	}
	
	@Test
	public void shouldReturnOneText() {
		double value = textFeature.getValue("event-5", keywords, userModel);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnTwoSentence() {
		double value = sentenceFeature.getValue("event-1", keywords, userModel);
		assertTrue(value == 2.0);
	}

	@Test
	public void shouldReturnOnePointFiveSentence() {
		double value = sentenceFeature.getValue("event-2", keywords, userModel);
		assertTrue(value == 1.5);
	}
	
	@Test
	public void shouldReturnOneSentence() {
		double value = sentenceFeature.getValue("event-3", keywords, userModel);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZeroPointFourNormalized() {
		double value = normalizedFeature.getValue("event-1", keywords, userModel);
		assertTrue(value == 0.4);
	}

	@Test
	public void shouldReturnZeroPointThreeNormalized() {
		double value = normalizedFeature.getValue("event-2", keywords, userModel);
		assertTrue(Math.abs(value - 0.3) < Util.EPSILON);
	}
	
	@Test
	public void shouldReturnZeroPointTwoNormalized() {
		double value = normalizedFeature.getValue("event-5", keywords, userModel);
		assertTrue(value == 0.2);
	}
	
	@Test
	public void shouldReturnOneMin() {
		double value = minFeature.getValue("event-2", keywords, userModel);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnTwoMax() {
		double value = maxFeature.getValue("event-2", keywords, userModel);
		assertTrue(value == 2.0);
	}
}
