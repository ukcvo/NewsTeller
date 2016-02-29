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
import edu.kit.anthropomatik.isl.newsTeller.userModel.ActualUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class SparqlCountFeatureTest {

	private KnowledgeStoreAdapter ksAdapter;
	
	private SparqlCountFeature feature;
	private SparqlCountFeature avgFeature;
	private SparqlCountFeature minFeature;
	private SparqlCountFeature maxFeature;
	private SparqlCountFeature interestFeature;
	
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
		eventEntityMap.put("event-1", Sets.newHashSet("0"));
		eventEntityMap.put("event-2", Sets.newHashSet("1"));
		
		ConcurrentMap<String, Set<String>> eventKeyEntityMap = new ConcurrentHashMap<String, Set<String>>();
		eventKeyEntityMap.put("event-1", Sets.newHashSet("0"));
		eventKeyEntityMap.put("event-2", Sets.newHashSet("1"));
		
		ConcurrentMap<String, Set<String>> eventKey2EntityMap = new ConcurrentHashMap<String, Set<String>>();
		eventKey2EntityMap.put("event-1", Sets.newHashSet("0"));
		eventKey2EntityMap.put("event-2", Sets.newHashSet("0"));
		
		sparqlCache.put(Util.getRelationName("event", "numberOfEntities", "keyword"), eventEntityMap);
		sparqlCache.put(Util.getRelationName("event", "numberOfKeywordEntities", "keyword"), eventKeyEntityMap);
		sparqlCache.put(Util.getRelationName("event", "numberOfKeywordEntities", "other"), eventKey2EntityMap);
		
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
		feature = (SparqlCountFeature) context.getBean("numberOfEntitiesFeature");
		avgFeature = (SparqlCountFeature) context.getBean("numberOfKeywordEntitiesFeatureAvg");
		minFeature = (SparqlCountFeature) context.getBean("numberOfKeywordEntitiesFeatureMin");
		maxFeature = (SparqlCountFeature) context.getBean("numberOfKeywordEntitiesFeatureMax");
		interestFeature = (SparqlCountFeature) context.getBean("numberOfKeywordEntitiesFeatureAvgtrue");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache);
	}
	
	@Test
	public void shouldReturnZero() {
		double result = feature.getValue("event-1", keywords, userModel);
		assertTrue(result == 0.0);
	}
	
	@Test
	public void shouldReturnOne() {
		double result = feature.getValue("event-2", keywords, userModel);
		assertTrue(result == 1.0);
	}

	@Test
	public void shouldReturnZeroAvg() {
		double result = avgFeature.getValue("event-1", twoKeywords, userModel);
		assertTrue(result == 0.0);
	}
	
	@Test
	public void shouldReturnZeroPointFiveAvg() {
		double result = avgFeature.getValue("event-2", twoKeywords, userModel);
		assertTrue(result == 0.5);
	}
	
	@Test
	public void shouldReturnZeroMin() {
		double result = minFeature.getValue("event-1", twoKeywords, userModel);
		assertTrue(result == 0.0);
	}
	
	@Test
	public void shouldReturnZeroMinEvent2() {
		double result = minFeature.getValue("event-2", twoKeywords, userModel);
		assertTrue(result == 0.0);
	}
	
	@Test
	public void shouldReturnZeroMax() {
		double result = maxFeature.getValue("event-1", twoKeywords, userModel);
		assertTrue(result == 0.0);
	}
	
	@Test
	public void shouldReturnOneMax() {
		double result = maxFeature.getValue("event-2", twoKeywords, userModel);
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroInterest() {
		double result = interestFeature.getValue("event-1", new ArrayList<Keyword>(), new ActualUserModel(twoKeywords));
		assertTrue(result == 0.0);
	}
	
	@Test
	public void shouldReturnZeroPointFiveInterest() {
		double result = interestFeature.getValue("event-2", new ArrayList<Keyword>(), new ActualUserModel(twoKeywords));
		assertTrue(result == 0.5);
	}
	
	
}
