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

public class ActorPositionFeatureTest {

	private KnowledgeStoreAdapter ksAdapter;

	private ActorPositionFeature feature;

	private static ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
	
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
		ConcurrentMap<String, Set<String>> eventActorMap = new ConcurrentHashMap<String, Set<String>>();
		eventActorMap.put("event-1", Sets.newHashSet("actor-1"));
		eventActorMap.put("event-2", Sets.newHashSet("actor-2"));
		eventActorMap.put("event-3", Sets.newHashSet("actor-3a","actor-3b"));
		ConcurrentMap<String, Set<String>> actorLabelMap = new ConcurrentHashMap<String, Set<String>>();
		actorLabelMap.put("actor-1", Sets.newHashSet("Alice"));
		actorLabelMap.put("actor-2", Sets.newHashSet("Bob"));
		actorLabelMap.put("actor-3a", Sets.newHashSet("John"));
		actorLabelMap.put("actor-3b", Sets.newHashSet("Jane"));
		ConcurrentMap<String, Set<String>> mentionLabelMap = new ConcurrentHashMap<String, Set<String>>();
		mentionLabelMap.put("mention-1#char=2,5", Sets.newHashSet("saw"));
		mentionLabelMap.put("mention-2#char=4,7", Sets.newHashSet("saw"));
		mentionLabelMap.put("mention-3#char=5,8", Sets.newHashSet("saw"));
		ConcurrentMap<String, Set<String>> resourceTextMap = new ConcurrentHashMap<String, Set<String>>();
		resourceTextMap.put("mention-1", Sets.newHashSet("I saw Alice yesterday."));
		resourceTextMap.put("mention-2", Sets.newHashSet("Bob saw me yesterday."));
		resourceTextMap.put("mention-3", Sets.newHashSet("John saw Jane yesterday."));
		
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), eventMentionMap);
		sparqlCache.put(Util.getRelationName("event", "actor", "keyword"), eventActorMap);
		sparqlCache.put(Util.getRelationName("entity", "entityPrefLabel", "keyword"), actorLabelMap);
		sparqlCache.put(Util.RELATION_NAME_MENTION_PROPERTY + Util.MENTION_PROPERTY_ANCHOR_OF, mentionLabelMap);
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, resourceTextMap);
		
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		feature = (ActorPositionFeature) context.getBean("actorPositionLeftFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, new ConcurrentHashMap<String, Set<KSMention>>());
	}

	@Test
	public void shouldReturnZero() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(new Keyword("keyword"));
		double value = feature.getValue("event-1", keywords);
		assertTrue(value == 0);
	}

	@Test
	public void shouldReturnOne() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(new Keyword("keyword"));
		double value = feature.getValue("event-2", keywords);
		assertTrue(value == 1.0);
	}

	@Test
	public void shouldReturnZeroPointFive() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(new Keyword("keyword"));
		double value = feature.getValue("event-3", keywords);
		assertTrue(value == 0.5);
	}
}
