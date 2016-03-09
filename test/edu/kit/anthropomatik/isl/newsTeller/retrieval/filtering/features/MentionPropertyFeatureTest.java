package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
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

public class MentionPropertyFeatureTest {

	KnowledgeStoreAdapter ksAdapter;
	
	MentionPropertyFeature feature;
	
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
		eventMentionMap.put("event-1", Sets.newHashSet("mention-1#char=123,456"));
		eventMentionMap.put("event-2", Sets.newHashSet("mention-2#char=123,456"));
		eventMentionMap.put("event-3", Sets.newHashSet("mention-3#char=123,456"));
		ConcurrentMap<String, Set<String>> mentionPropertyMap = new ConcurrentHashMap<String, Set<String>>();
		mentionPropertyMap.put("mention-1#char=123,456", new HashSet<String>());
		mentionPropertyMap.put("mention-2#char=123,456", Sets.newHashSet("one"));
		mentionPropertyMap.put("mention-3#char=123,456", Sets.newHashSet("one", "two"));
		
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), eventMentionMap);
		sparqlCache.put(Util.RELATION_NAME_MENTION_PROPERTY + "http://dkm.fbk.eu/ontologies/newsreader#wordnetRef", mentionPropertyMap);
		
		Keyword k = new Keyword("keyword");
		Util.stemKeyword(k);
		keywords.add(k);
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		feature = (MentionPropertyFeature) context.getBean("wordnetRefFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache);
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
