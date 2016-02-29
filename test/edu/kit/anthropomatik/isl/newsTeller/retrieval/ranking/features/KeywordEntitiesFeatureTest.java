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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.kit.anthropomatik.isl.newsTeller.data.KSMention;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.userModel.ActualUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class KeywordEntitiesFeatureTest {

	private KnowledgeStoreAdapter ksAdapter;
	
	private KeywordEntitiesFeature avgFeature;
	private KeywordEntitiesFeature minFeature;
	private KeywordEntitiesFeature maxFeature;
	private KeywordEntitiesFeature stemFeature;
	private KeywordEntitiesFeature normalizedFeature;
	private KeywordEntitiesFeature dbpediaFeature;
	private KeywordEntitiesFeature interestFeature;
	
	private static ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
	private static ConcurrentMap<String, Set<KSMention>> eventMentionCache = new ConcurrentHashMap<String, Set<KSMention>>();
	private static List<Keyword> keywords = new ArrayList<Keyword>();
	private static List<Keyword> twoKeywords = new ArrayList<Keyword>();
	private static UserModel userModel = new DummyUserModel();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ConcurrentMap<String, Set<String>> eventEntityMap = new ConcurrentHashMap<String, Set<String>>();
		eventEntityMap.put("event-1", Sets.newHashSet("entity-1", "entity-2"));
		eventEntityMap.put("event-2", Sets.newHashSet("entity-1", "entity-3"));
		
		ConcurrentMap<String, Set<String>> entityLabelMap = new ConcurrentHashMap<String, Set<String>>();
		entityLabelMap.put("entity-1", Sets.newHashSet("Barack Obama"));
		entityLabelMap.put("entity-2", Sets.newHashSet("Donald Trump"));
		entityLabelMap.put("entity-3", Sets.newHashSet("Keyword Man", "Superman"));
		
		sparqlCache.put(Util.getRelationName("event", "entity", "keyword"), eventEntityMap);
		sparqlCache.put(Util.getRelationName("event", "entity", "keyed"), eventEntityMap);
		sparqlCache.put(Util.getRelationName("entity", "entityPrefLabel", "keyword"), entityLabelMap);
		sparqlCache.put(Util.getRelationName("entity", "entityDbpediaLabel", "keyword"), entityLabelMap);
		sparqlCache.put(Util.getRelationName("entity", "entityPrefLabel", "keyed"), entityLabelMap);
		
		Keyword k = new Keyword("keyword");
		Util.stemKeyword(k);
		keywords.add(k);
		twoKeywords.add(k);
		Keyword k2 = new Keyword("other");
		Util.stemKeyword(k2);
		twoKeywords.add(k2);
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		avgFeature = (KeywordEntitiesFeature) context.getBean("keywordEntitiesFeature0ffp");
		minFeature = (KeywordEntitiesFeature) context.getBean("keywordEntitiesFeature1ffp");
		maxFeature = (KeywordEntitiesFeature) context.getBean("keywordEntitiesFeature2ffp");
		stemFeature = (KeywordEntitiesFeature) context.getBean("keywordEntitiesFeature0tfp");
		normalizedFeature = (KeywordEntitiesFeature) context.getBean("keywordEntitiesFeature0ftp");
		dbpediaFeature = (KeywordEntitiesFeature) context.getBean("keywordEntitiesFeature0ffd");
		interestFeature = (KeywordEntitiesFeature) context.getBean("keywordEntitiesFeature0ffptrue");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache);
	}
	
	@Test
	public void shouldReturnZeroAvg() {
		double result = avgFeature.getValue("event-1", keywords, userModel);
		assertTrue(result == 0.0);
	}

	@Test
	public void shouldReturnOneAvg() {
		double result = avgFeature.getValue("event-2", keywords, userModel);
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroPointFiveAvg() {
		double result = avgFeature.getValue("event-2", twoKeywords, userModel);
		assertTrue(result == 0.5);
	}
	
	@Test
	public void shouldReturnZeroMin() {
		double result = minFeature.getValue("event-1", keywords, userModel);
		assertTrue(result == 0.0);
	}

	@Test
	public void shouldReturnOneMin() {
		double result = minFeature.getValue("event-2", keywords, userModel);
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroMinTwoKeywords() {
		double result = minFeature.getValue("event-2", twoKeywords, userModel);
		assertTrue(result == 0.0);
	}
	
	@Test
	public void shouldReturnZeroMax() {
		double result = maxFeature.getValue("event-1", keywords, userModel);
		assertTrue(result == 0.0);
	}

	@Test
	public void shouldReturnOneMax() {
		double result = maxFeature.getValue("event-2", keywords, userModel);
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnOneMaxTwoKeywords() {
		double result = maxFeature.getValue("event-2", twoKeywords, userModel);
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroStem() {
		Keyword k = new Keyword("keyed");
		Util.stemKeyword(k);
		double result = stemFeature.getValue("event-1", Lists.newArrayList(k), userModel);
		assertTrue(result == 0.0);
	}

	@Test
	public void shouldReturnOneStem() {
		Keyword k = new Keyword("keyed");
		Util.stemKeyword(k);
		double result = stemFeature.getValue("event-2", Lists.newArrayList(k), userModel);
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroPointFiveStem() {
		Keyword k = new Keyword("keyed");
		Util.stemKeyword(k);
		Keyword k2 = new Keyword("other");
		Util.stemKeyword(k2);
		double result = stemFeature.getValue("event-2", Lists.newArrayList(k, k2), userModel);
		assertTrue(result == 0.5);
	}
	
	@Test
	public void shouldReturnZeroNormalized() {
		double result = normalizedFeature.getValue("event-1", keywords, userModel);
		assertTrue(result == 0.0);
	}

	@Test
	public void shouldReturnZeroPointFiveNormalized() {
		double result = normalizedFeature.getValue("event-2", keywords, userModel);
		assertTrue(result == 0.5);
	}
	
	@Test
	public void shouldReturnZeroPointTwoFiveNormalized() {
		double result = normalizedFeature.getValue("event-2", twoKeywords, userModel);
		assertTrue(result == 0.25);
	}
	
	@Test
	public void shouldReturnZeroDbpedia() {
		double result = dbpediaFeature.getValue("event-1", keywords, userModel);
		assertTrue(result == 0.0);
	}

	@Test
	public void shouldReturnOneDbpedia() {
		double result = dbpediaFeature.getValue("event-2", keywords, userModel);
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroPointFiveDbpedia() {
		double result = dbpediaFeature.getValue("event-2", twoKeywords, userModel);
		assertTrue(result == 0.5);
	}
	
	@Test
	public void shouldReturnZeroInterest() {
		double result = interestFeature.getValue("event-1", keywords, new ActualUserModel(keywords));
		assertTrue(result == 0.0);
	}

	@Test
	public void shouldReturnOneInterest() {
		double result = interestFeature.getValue("event-2", keywords, new ActualUserModel(keywords));
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroPointFiveInterest() {
		double result = interestFeature.getValue("event-2", keywords, new ActualUserModel(twoKeywords));
		assertTrue(result == 0.5);
	}
	
	
}
