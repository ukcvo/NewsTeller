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

public class PropbankNeedsArgumentFeatureTest {

	private PropbankNeedsArgumentFeature feature;
	
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
		eventMentionMap.put("event-1", Sets.newHashSet("mention-1#char=123,456"));
		eventMentionMap.put("event-2", Sets.newHashSet("mention-2#char=123,456"));
		ConcurrentMap<String, Set<String>> mentionPropertyMap = new ConcurrentHashMap<String, Set<String>>();
		mentionPropertyMap.put("mention-1#char=123,456", Sets.newHashSet("http://www.newsreader-project.eu/propbank/tell.01"));
		mentionPropertyMap.put("mention-2#char=123,456", Sets.newHashSet("http://www.newsreader-project.eu/propbank/locate.01"));
		ConcurrentMap<String, Set<String>> mentionPOSMap = new ConcurrentHashMap<String, Set<String>>();
		mentionPOSMap.put("mention-1#char=123,456", Sets.newHashSet(Util.MENTION_PROPERTY_POS_VERB));
		mentionPOSMap.put("mention-2#char=123,456", Sets.newHashSet(Util.MENTION_PROPERTY_POS_VERB));
				
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), eventMentionMap);
		sparqlCache.put(Util.RELATION_NAME_MENTION_PROPERTY + Util.MENTION_PROPERTY_PROPBANK, mentionPropertyMap);
		sparqlCache.put(Util.RELATION_NAME_MENTION_PROPERTY + Util.MENTION_PROPERTY_POS, mentionPOSMap);
				
		Keyword k = new Keyword("keyword");
		Util.stemKeyword(k);
		keywords.add(k);
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		feature = (PropbankNeedsArgumentFeature) context.getBean("needsA2Feature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
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
