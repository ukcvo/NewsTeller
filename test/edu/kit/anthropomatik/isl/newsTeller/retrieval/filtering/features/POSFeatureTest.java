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

public class POSFeatureTest {

	private POSFeature posFeature;
	
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
		eventMentionMap.put("event-3", Sets.newHashSet("mention-3#char=123,456","mention-3#char=22,33"));
		ConcurrentMap<String, Set<String>> mentionPropertyMap = new ConcurrentHashMap<String, Set<String>>();
		mentionPropertyMap.put("mention-1#char=123,456", Sets.newHashSet(Util.MENTION_PROPERTY_POS_NOUN));
		mentionPropertyMap.put("mention-2#char=123,456", Sets.newHashSet(Util.MENTION_PROPERTY_POS_VERB));
		mentionPropertyMap.put("mention-3#char=123,456", Sets.newHashSet(Util.MENTION_PROPERTY_POS_NOUN));
		mentionPropertyMap.put("mention-3#char=22,33", Sets.newHashSet(Util.MENTION_PROPERTY_POS_VERB));
		
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), eventMentionMap);
		sparqlCache.put(Util.RELATION_NAME_MENTION_PROPERTY + Util.MENTION_PROPERTY_POS, mentionPropertyMap);
		
		Keyword k = new Keyword("keyword");
		Util.stemKeyword(k);
		keywords.add(k);
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		posFeature = (POSFeature) context.getBean("posFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache, new ConcurrentHashMap<String, ConcurrentMap<String,Set<KSMention>>>());
	}

	@Test
	public void shouldReturnZero() {
		double value = posFeature.getValue("event-1", keywords);
		assertTrue(value == 0.0);
	}

	@Test
	public void shouldReturnOne() {
		double value = posFeature.getValue("event-2", keywords);
		assertTrue(value == 1.0);
	}
	
	@Test
	public void shouldReturnZeroPointFive() {
		double value = posFeature.getValue("event-3", keywords);
		assertTrue(value == 0.5);
	}
	
}
