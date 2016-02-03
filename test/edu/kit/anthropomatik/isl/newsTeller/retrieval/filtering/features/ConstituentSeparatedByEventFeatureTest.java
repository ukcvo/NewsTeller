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

public class ConstituentSeparatedByEventFeatureTest {

	private ConstituentSeparatedByEventFeature maxFeature;
	
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
		eventMentionMap.put("event-1", Sets.newHashSet("mention-1#char=14,18"));
		eventMentionMap.put("event-2", Sets.newHashSet("mention-2#char=14,18"));
		ConcurrentMap<String, Set<String>> eventEntityMap = new ConcurrentHashMap<String, Set<String>>();
		eventEntityMap.put("event-1", Sets.newHashSet("actor-1a", "actor-1b"));
		eventEntityMap.put("event-2", Sets.newHashSet("actor-2a", "actor-2b"));
		ConcurrentMap<String, Set<String>> entityMentionMap = new ConcurrentHashMap<String, Set<String>>();
		entityMentionMap.put("actor-1a", Sets.newHashSet("mention-1#char=8,13"));
		entityMentionMap.put("actor-1b", Sets.newHashSet("mention-1#char=19,23"));
		entityMentionMap.put("actor-2a", Sets.newHashSet("mention-2#char=0,4"));
		entityMentionMap.put("actor-2b", Sets.newHashSet("mention-2#char=19,23"));
		ConcurrentMap<String, Set<String>> resourceTextMap = new ConcurrentHashMap<String, Set<String>>();
		resourceTextMap.put("mention-1", Sets.newHashSet("One two three four five six seven."));
		resourceTextMap.put("mention-2", Sets.newHashSet("One two three four five six seven."));
		
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), eventMentionMap);
		sparqlCache.put(Util.getRelationName("event", "entity", "keyword"), eventEntityMap);
		sparqlCache.put(Util.getRelationName("entity", "mention", "keyword"), entityMentionMap);
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, resourceTextMap);
		
		eventMentionCache.put("mention-1", Sets.newHashSet(new KSMention("mention-1#char=14,18"), new KSMention("mention-1#char=0,4")));
		eventMentionCache.put("mention-2", Sets.newHashSet(new KSMention("mention-2#char=14,18"), new KSMention("mention-2#char=8,13")));
		
		keywords.add(new Keyword("keyword"));
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		maxFeature = (ConstituentSeparatedByEventFeature) context.getBean("maxConstituentSeparatedByEventFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache);
	}

	@Test
	public void shouldReturnZeroMax() {
		double value = maxFeature.getValue("event-1", keywords);
		assertTrue(value == 0.0);
	}
	
	@Test
	public void shouldReturnOneMax() {
		double value = maxFeature.getValue("event-2", keywords);
		assertTrue(value == 1.0);
	}
}
