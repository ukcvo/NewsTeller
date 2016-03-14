package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.LogManager;

import org.junit.After;
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

public class DBPediaLabelInFullTextFeatureTest {

	private DBPediaLabelInFullTextFeature feature;
	private DBPediaLabelInFullTextFeature keywordFeature;
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
		
		ConcurrentMap<String, Set<String>> eventMentionMap = new ConcurrentHashMap<String, Set<String>>();
		eventMentionMap.put("event-1", Sets.newHashSet("mention-1#char=14,18"));
		eventMentionMap.put("event-2", Sets.newHashSet("mention-2#char=14,18"));
		ConcurrentMap<String, Set<String>> eventEntityMap = new ConcurrentHashMap<String, Set<String>>();
		eventEntityMap.put("event-1", Sets.newHashSet("actor-1a", "actor-1b"));
		eventEntityMap.put("event-2", Sets.newHashSet("actor-2a", "actor-2b"));
		ConcurrentMap<String, Set<String>> eventKeyEntityMap = new ConcurrentHashMap<String, Set<String>>();
		eventKeyEntityMap.put("event-1", Sets.newHashSet("actor-1a"));
		eventKeyEntityMap.put("event-2", Sets.newHashSet("actor-2a"));
		ConcurrentMap<String, Set<String>> eventKeyEntityMap2 = new ConcurrentHashMap<String, Set<String>>();
		eventKeyEntityMap2.put("event-1", Sets.newHashSet("actor-1b"));
		ConcurrentMap<String, Set<String>> entityLabelMap = new ConcurrentHashMap<String, Set<String>>();
		entityLabelMap.put("actor-1a", Sets.newHashSet("One", "Eins"));
		entityLabelMap.put("actor-1b", Sets.newHashSet("2"));
		entityLabelMap.put("actor-2a", Sets.newHashSet("1"));
		entityLabelMap.put("actor-2b", Sets.newHashSet("2"));
		ConcurrentMap<String, Set<String>> resourceTextMap = new ConcurrentHashMap<String, Set<String>>();
		resourceTextMap.put("mention-1", Sets.newHashSet("One two three four five six seven."));
		resourceTextMap.put("mention-2", Sets.newHashSet("One two three four five six seven."));
		
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), eventMentionMap);
		sparqlCache.put(Util.getRelationName("event", "entity", "keyword"), eventEntityMap);
		sparqlCache.put(Util.getRelationName("event", "keywordEntity", "keyword"), eventKeyEntityMap);
		sparqlCache.put(Util.getRelationName("event", "keywordEntity", "other"), eventKeyEntityMap2);
		sparqlCache.put(Util.getRelationName("entity", "entityLabel", "keyword"), entityLabelMap);
		sparqlCache.put(Util.getRelationName("entity", "entityLabel", "other"), entityLabelMap);
		sparqlCache.put(Util.getRelationName("entity", "inheritedLabel", "keyword"), new ConcurrentHashMap<String, Set<String>>());
		sparqlCache.put(Util.getRelationName("entity", "inheritedLabel", "other"), new ConcurrentHashMap<String, Set<String>>());
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, resourceTextMap);
		
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
		feature = (DBPediaLabelInFullTextFeature) context.getBean("appearLabelsInTextFeature");
		keywordFeature = (DBPediaLabelInFullTextFeature) context.getBean("appearKeywordLabelsInSentenceFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache, new ConcurrentHashMap<String, ConcurrentMap<String,Set<KSMention>>>());
	}

	@After
	public void shutDown() {
		feature.shutDown();
		keywordFeature.shutDown();
	}
	
	@Test
	public void ShouldReturnZeroPointFive() {
		double value = feature.getValue("event-1", keywords);
		assertTrue(value == 0.5);
	}

	@Test
	public void ShouldReturnOneForKeyword() {
		double value = keywordFeature.getValue("event-1", keywords);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void ShouldReturnOneForMultipleKeywords() {
		double value = keywordFeature.getValue("event-1", twoKeywords);
		assertTrue(value == 1.0);
	}

	@Test
	public void ShouldReturnZero() {
		double value = feature.getValue("event-2", keywords);
		assertTrue(value == 0.0);
	}

	@Test
	public void ShouldReturnZeroForKeyword() {
		double value = keywordFeature.getValue("event-2", keywords);
		assertTrue(value == 0.0);
	}

}
