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

public class SparqlFeatureTest {

	private KnowledgeStoreAdapter ksAdapter;
	
	private SparqlFeature a1Feature;
	private SparqlFeature keyEntFeature;
	
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
		
		ConcurrentMap<String, Set<String>> eventA1Map = new ConcurrentHashMap<String, Set<String>>();
		eventA1Map.put("event-1", Sets.newHashSet("0"));
		eventA1Map.put("event-2", Sets.newHashSet("1"));
		
		ConcurrentMap<String, Set<String>> eventKeyEntMap = new ConcurrentHashMap<String, Set<String>>();
		eventKeyEntMap.put("event-1", Sets.newHashSet("0"));
		eventKeyEntMap.put("event-2", Sets.newHashSet("1"));
		ConcurrentMap<String, Set<String>> eventKeyEntMap2 = new ConcurrentHashMap<String, Set<String>>();
		eventKeyEntMap2.put("event-1", Sets.newHashSet("0"));
		eventKeyEntMap2.put("event-2", Sets.newHashSet("0"));
		
		
		sparqlCache.put(Util.getRelationName("event", "a1", "keyword"), eventA1Map);
		sparqlCache.put(Util.getRelationName("event", "numberOfKeywordEntities", "keyword"), eventKeyEntMap);
		sparqlCache.put(Util.getRelationName("event", "numberOfKeywordEntities", "other"), eventKeyEntMap2);
		
		
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
		a1Feature = (SparqlFeature) context.getBean("a1Feature");
		keyEntFeature = (SparqlFeature) context.getBean("hasDBpediaEntitiesFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache, new ConcurrentHashMap<String, ConcurrentMap<String,Set<KSMention>>>());
	}
	
	@Test
	public void shouldReturnZeroA1() {
		double result = a1Feature.getValue("event-1", keywords);
		assertTrue(result == 0.0);
	}
	
	@Test
	public void shouldReturnOneA1() {
		double result = a1Feature.getValue("event-2", keywords);
		assertTrue(result == 1.0);
	}

	@Test
	public void shouldReturnZeroKeyEnt() {
		double result = keyEntFeature.getValue("event-1", keywords);
		assertTrue(result == 0.0);
	}
	
	@Test
	public void shouldReturnOneKeyEnt() {
		double result = keyEntFeature.getValue("event-2", keywords);
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroKeyEntMultipleKeywords() {
		double result = keyEntFeature.getValue("event-1", twoKeywords);
		assertTrue(result == 0.0);
	}
	
	@Test
	public void shouldReturnOneKeyEntMultipleKeywords() {
		double result = keyEntFeature.getValue("event-2", twoKeywords);
		assertTrue(result == 1.0);
	}
}
