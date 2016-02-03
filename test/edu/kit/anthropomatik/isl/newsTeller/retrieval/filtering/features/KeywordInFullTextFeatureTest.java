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

public class KeywordInFullTextFeatureTest {

	private static ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache 
			= new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
	
	private KnowledgeStoreAdapter ksAdapter;
	
	private KeywordInFullTextFeature feature;
	
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
		ConcurrentMap<String, Set<String>> resourceTextMap = new ConcurrentHashMap<String, Set<String>>();
		resourceTextMap.putIfAbsent("mention-1", Sets.newHashSet("Michael Jordan lived in Los Angeles, where he met Jannet Jackson in 1982."));
		resourceTextMap.putIfAbsent("mention-2", Sets.newHashSet("We met Michael Jackson in Florida once."));
		
		sparqlCache.put(Util.getRelationName("event", "mention", "Michael Jackson"), eventMentionMap);
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, resourceTextMap);
		
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		feature = (KeywordInFullTextFeature) context.getBean("keywordInTextContainsFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, new ConcurrentHashMap<String, Set<KSMention>>());
	}

	@Test
	public void shouldReturnZero() {
		Keyword k = new Keyword("Michael Jackson");
		Util.stemKeyword(k);
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		
		double result = this.feature.getValue("event-1", keywords);
		
		assertTrue(result == 0.0);
	}

	@Test
	public void shouldReturnOne() {
		Keyword k = new Keyword("Michael Jackson");
		Util.stemKeyword(k);
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(k);
		
		double result = this.feature.getValue("event-2", keywords);
		
		assertTrue(result == 1.0);
	}
}
