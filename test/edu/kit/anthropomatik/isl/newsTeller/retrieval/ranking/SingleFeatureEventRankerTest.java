package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking;

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
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class SingleFeatureEventRankerTest {

	private SingleFeatureEventRanker ranker;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	private static ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
	private static ConcurrentMap<String, Set<KSMention>> eventMentionCache = new ConcurrentHashMap<String, Set<KSMention>>();
	private static List<Keyword> keywords = new ArrayList<Keyword>();
	private static List<Keyword> twoKeywords = new ArrayList<Keyword>();
	private static UserModel userModel = new DummyUserModel();
	
	private static Set<NewsEvent> events = new HashSet<NewsEvent>();
	private static List<NewsEvent> expected = new ArrayList<NewsEvent>();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ConcurrentMap<String, Set<String>> eventMentionMap = new ConcurrentHashMap<String, Set<String>>();
		eventMentionMap.put("event-1", Sets.newHashSet("mention-1#char=4,7"));
		eventMentionMap.put("event-2", Sets.newHashSet("mention-2#char=4,7"));
		eventMentionMap.put("event-3", Sets.newHashSet("mention-1#char=4,7", "mention-2#char=4,7"));
		eventMentionMap.put("event-4", Sets.newHashSet("mention-1#char=4,7", "mention-4#char=4,7"));
		eventMentionMap.put("event-5", Sets.newHashSet("mention-4#char=4,7"));
		
		ConcurrentMap<String, Set<String>> eventResourceTextMap = new ConcurrentHashMap<String, Set<String>>();
		eventResourceTextMap.put("mention-1", Sets.newHashSet("One two three four."));
		eventResourceTextMap.put("mention-2", Sets.newHashSet("One keyword three four."));
		eventResourceTextMap.put("mention-4", Sets.newHashSet("One other keyword three four."));
		
		ConcurrentMap<String, Set<String>> eventResourceTitleMap = new ConcurrentHashMap<String, Set<String>>();
		eventResourceTitleMap.put("mention-1", Sets.newHashSet("Contains nothing of interest"));
		eventResourceTitleMap.put("mention-2", Sets.newHashSet("Contains a keyword"));
		
		
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), eventMentionMap);
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword search"), eventMentionMap);
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, eventResourceTextMap);
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_PROPERTY + Util.RESOURCE_PROPERTY_TITLE, eventResourceTitleMap);
		
		Keyword k = new Keyword("keyword search");
		Util.stemKeyword(k);
		keywords.add(k);
		twoKeywords.add(k);
		Keyword k2 = new Keyword("other");
		Util.stemKeyword(k2);
		twoKeywords.add(k2);
		
		expected.add(new NewsEvent("event-5"));
		expected.add(new NewsEvent("event-2"));
		expected.add(new NewsEvent("event-3"));
		expected.add(new NewsEvent("event-4"));
		expected.add(new NewsEvent("event-1"));
		
		events.addAll(expected);
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		ranker = (SingleFeatureEventRanker) context.getBean("rankerFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache);
	}
	
	@Test
	public void shouldReturnCorrectRanking() {
		List<NewsEvent> result = ranker.rankEvents(events, keywords, userModel);
		assertTrue(result.equals(expected));
	}

}
