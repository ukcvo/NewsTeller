package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

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
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class KeywordEntityMatchingKeywordFeatureTest {

	private KeywordEntityMatchingKeywordFeature feature;
	
	private KnowledgeStoreAdapter ksAdapter;

	private static ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
	private static ConcurrentMap<String, Set<KSMention>> eventMentionCache = new ConcurrentHashMap<String, Set<KSMention>>();
	private static List<Keyword> keywords = new ArrayList<Keyword>();
	private static List<Keyword> twoKeywords = new ArrayList<Keyword>();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ConcurrentMap<String, Set<String>> eventKeyEntityMap = new ConcurrentHashMap<String, Set<String>>();
		eventKeyEntityMap.put("event-1", Sets.newHashSet("actor-1a"));
		eventKeyEntityMap.put("event-2", Sets.newHashSet("actor-2a"));
		ConcurrentMap<String, Set<String>> entityMatchLabelMap = new ConcurrentHashMap<String, Set<String>>();
		entityMatchLabelMap.put("actor-1a", Sets.newHashSet("Stephen Hawking"));
		entityMatchLabelMap.put("actor-2a", Sets.newHashSet("Hawk Helicopter"));
		
		sparqlCache.put(Util.getRelationName("event", "keywordEntity", "Hawking"), eventKeyEntityMap);
		sparqlCache.put(Util.getRelationName("entity", "matchingEntityLabel", "Hawking"), entityMatchLabelMap);
		
		Keyword k = new Keyword("Hawking");
		Util.stemKeyword(k);
		keywords.add(k);
		twoKeywords.add(k);
		
		Keyword k2 = new Keyword("Obama");
		Util.stemKeyword(k2);
		twoKeywords.add(k2);
	}

	@Before
	public void setUp() {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		feature = (KeywordEntityMatchingKeywordFeature) context.getBean("keywordEntityMatchingKeywordFeature");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache, new ConcurrentHashMap<String, ConcurrentMap<String,Set<KSMention>>>());
	}

	@Test
	public void shouldReturnOne() {
		double value = feature.getValue("event-1", keywords);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnOneMultipleKeywords() {
		double value = feature.getValue("event-1", twoKeywords);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZero() {
		double value = feature.getValue("event-2", keywords);
		assertTrue(value == 0.0);
	}	
	
	@Test
	public void shouldReturnZeroMultipleKeywords() {
		double value = feature.getValue("event-2", twoKeywords);
		assertTrue(value == 0.0);
	}
}
