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

public class EntityMentionSurroundingsFeatureTest {

	private EntityMentionSurroundingsFeature feature;
	
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
		
		ConcurrentMap<String, Set<String>> eventMentionMap = new ConcurrentHashMap<String, Set<String>>();
		eventMentionMap.put("event-1", Sets.newHashSet("mention-1#char=0,3"));
		eventMentionMap.put("event-2", Sets.newHashSet("mention-2#char=0,3"));
		ConcurrentMap<String, Set<String>> eventActorMap = new ConcurrentHashMap<String, Set<String>>();
		eventActorMap.put("event-1", Sets.newHashSet("actor-1a"));
		eventActorMap.put("event-2", Sets.newHashSet("actor-2a"));
		ConcurrentMap<String, Set<String>> entityMentionMap = new ConcurrentHashMap<String, Set<String>>();
		entityMentionMap.put("actor-1a", Sets.newHashSet("mention-1#char=17,21"));
		entityMentionMap.put("actor-2a", Sets.newHashSet("mention-2#char=19,23"));
		ConcurrentMap<String, Set<String>> resourceTextMap = new ConcurrentHashMap<String, Set<String>>();
		resourceTextMap.put("mention-1", Sets.newHashSet("One two three at five six seven."));
		resourceTextMap.put("mention-2", Sets.newHashSet("One two three four five six seven."));
		
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), eventMentionMap);
		sparqlCache.put(Util.getRelationName("event", "actor", "keyword"), eventActorMap);
		sparqlCache.put(Util.getRelationName("entity", "mention", "keyword"), entityMentionMap);
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, resourceTextMap);
		
		Keyword k = new Keyword("keyword");
		Util.stemKeyword(k);
		keywords.add(k);
	}
	
	@Before
	public void setUp() {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		feature = (EntityMentionSurroundingsFeature) context.getBean("locationPrepBeforeActorFeature");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache);
	}

	@Test
	public void shouldReturnOne() {
		double value = feature.getValue("event-1", keywords);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZero() {
		double value = feature.getValue("event-2", keywords);
		assertTrue(value == 0.0);
	}
	
	
	
}
