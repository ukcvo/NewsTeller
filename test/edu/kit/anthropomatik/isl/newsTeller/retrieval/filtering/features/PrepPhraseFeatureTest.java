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

public class PrepPhraseFeatureTest {

	private PrepPhraseFeature feature;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	private static ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
	private static ConcurrentMap<String, Set<KSMention>> eventMentionCache = new ConcurrentHashMap<String, Set<KSMention>>();
	private static List<Keyword> keywords = new ArrayList<Keyword>();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ConcurrentMap<String, Set<String>> eventActorMap = new ConcurrentHashMap<String, Set<String>>();
		eventActorMap.put("event-1", Sets.newHashSet("actor-1"));
		eventActorMap.put("event-2", Sets.newHashSet("actor-2"));
		eventActorMap.put("event-3", Sets.newHashSet("actor-3a", "actor-3b"));
		ConcurrentMap<String, Set<String>> entityLabelMap = new ConcurrentHashMap<String, Set<String>>();
		entityLabelMap.put("actor-1", Sets.newHashSet("Peter"));
		entityLabelMap.put("actor-2", Sets.newHashSet("for the greater good"));
		entityLabelMap.put("actor-3a", Sets.newHashSet("Peter"));
		entityLabelMap.put("actor-3b", Sets.newHashSet("for the greater good", "the greater good"));
		
		sparqlCache.put(Util.getRelationName("event", "actor", "keyword"), eventActorMap);
		sparqlCache.put(Util.getRelationName("entity", "entityPrefLabel", "keyword"), entityLabelMap);
		
		Keyword k = new Keyword("keyword");
		Util.stemKeyword(k);
		keywords.add(k);
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		feature = (PrepPhraseFeature) context.getBean("prepPhraseFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache, new ConcurrentHashMap<String, ConcurrentMap<String,Set<KSMention>>>());
	}

	@Test
	public void shouldReturnZeroBecauseNoActors() {
		double value = feature.getValue("event-0", keywords);
		assertTrue(value == 0.0);
	}

	@Test
	public void shouldReturnZero() {
		double value = feature.getValue("event-1", keywords);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnOne() {
		double value = feature.getValue("event-2", keywords);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZeroPointTwoFive() {
		double value = feature.getValue("event-3", keywords);
		assertTrue(value == 0.25);
	}
}
