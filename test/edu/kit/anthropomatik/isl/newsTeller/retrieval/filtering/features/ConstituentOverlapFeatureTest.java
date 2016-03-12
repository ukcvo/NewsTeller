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

public class ConstituentOverlapFeatureTest {

	private ConstituentOverlapFeature feature;

	private KnowledgeStoreAdapter ksAdapter;

	private static ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
	
	private static ConcurrentHashMap<String, ConcurrentMap<String,Set<KSMention>>> entityMentionCache = new ConcurrentHashMap<String, ConcurrentMap<String,Set<KSMention>>>();
	
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
		eventMentionMap.put("event-1", Sets.newHashSet("mention-1#char=2,5"));
		eventMentionMap.put("event-2", Sets.newHashSet("mention-2#char=4,7"));
		eventMentionMap.put("event-3", Sets.newHashSet("mention-3#char=5,8"));
		ConcurrentMap<String, Set<String>> eventEntityMap = new ConcurrentHashMap<String, Set<String>>();
		eventEntityMap.put("event-1", Sets.newHashSet("actor-1"));
		eventEntityMap.put("event-2", Sets.newHashSet("actor-2"));
		eventEntityMap.put("event-3", Sets.newHashSet("actor-3a","actor-3b"));
//		ConcurrentMap<String, Set<String>> entityMentionMap = new ConcurrentHashMap<String, Set<String>>();
//		entityMentionMap.put("actor-1", Sets.newHashSet("mention-1#char=6,11"));
//		entityMentionMap.put("actor-2", Sets.newHashSet("mention-2#char=0,7"));
//		entityMentionMap.put("actor-3a", Sets.newHashSet("mention-3#char=0,13"));
//		entityMentionMap.put("actor-3b", Sets.newHashSet("mention-3#char=9,13"));
		ConcurrentMap<String, Set<String>> resourceTextMap = new ConcurrentHashMap<String, Set<String>>();
		resourceTextMap.put("mention-1", Sets.newHashSet("I saw Alice yesterday."));
		resourceTextMap.put("mention-2", Sets.newHashSet("Bob saw me yesterday."));
		resourceTextMap.put("mention-3", Sets.newHashSet("John saw Jane yesterday."));
		
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), eventMentionMap);
		sparqlCache.put(Util.getRelationName("event", "entity", "keyword"), eventEntityMap);
//		sparqlCache.put(Util.getRelationName("entity", "mention", "keyword"), entityMentionMap);
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, resourceTextMap);
		
		ConcurrentMap<String, Set<KSMention>> actor1Map = new ConcurrentHashMap<String, Set<KSMention>>();
		actor1Map.put("mention-1", Sets.newHashSet(new KSMention("mention-1#char=6,11")));
		entityMentionCache.put("actor-1", actor1Map);
		
		ConcurrentMap<String, Set<KSMention>> actor2Map = new ConcurrentHashMap<String, Set<KSMention>>();
		actor2Map.put("mention-2", Sets.newHashSet(new KSMention("mention-2#char=0,7")));
		entityMentionCache.put("actor-2", actor2Map);
		
		ConcurrentMap<String, Set<KSMention>> actor3aMap = new ConcurrentHashMap<String, Set<KSMention>>();
		actor3aMap.put("mention-3", Sets.newHashSet(new KSMention("mention-3#char=0,13")));
		entityMentionCache.put("actor-3a", actor3aMap);
		
		ConcurrentMap<String, Set<KSMention>> actor3bMap = new ConcurrentHashMap<String, Set<KSMention>>();
		actor3bMap.put("mention-3", Sets.newHashSet(new KSMention("mention-3#char=9,13")));
		entityMentionCache.put("actor-3b", actor3bMap);
		
		
		keywords.add(new Keyword("keyword"));
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		feature = (ConstituentOverlapFeature) context.getBean("overlapFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, new ConcurrentHashMap<String, Set<KSMention>>(), entityMentionCache);
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
	public void shouldReturnTwo() {
		double value = feature.getValue("event-3", keywords);
		assertTrue(value == 2.0);
	}
}
