package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

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
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class RecencyFeatureTest {

	private KnowledgeStoreAdapter ksAdapter;
	
	private RecencyFeature daysFeature;
	private RecencyFeature yearsFeature;
	
	private static ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
	private static ConcurrentMap<String, Set<KSMention>> eventMentionCache = new ConcurrentHashMap<String, Set<KSMention>>();
	private static List<Keyword> keywords = new ArrayList<Keyword>();
	private static UserModel userModel = new DummyUserModel();
	
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
		eventMentionMap.put("event-3", Sets.newHashSet("mention-1#char=4,7","mention-2#char=4,7"));
		
		ConcurrentMap<String, Set<String>> eventResourceTimeMap = new ConcurrentHashMap<String, Set<String>>();
		eventResourceTimeMap.put("mention-1", Sets.newHashSet("2015-12-31T00:00:00.000+02:00"));
		eventResourceTimeMap.put("mention-2", Sets.newHashSet("2005-05-14T02:00:00.000+02:00"));
		
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), eventMentionMap);
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_PROPERTY + Util.RESOURCE_PROPERTY_TIME, eventResourceTimeMap);
		
		Keyword k = new Keyword("keyword");
		Util.stemKeyword(k);
		keywords.add(k);
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		daysFeature = (RecencyFeature) context.getBean("recencyFeatureDays");
		yearsFeature = (RecencyFeature) context.getBean("recencyFeatureYears");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache);
	}
	
	@Test
	public void shouldReturnOneDay() {
		double result = daysFeature.getValue("event-1", keywords, userModel);
		assertTrue(result == 1.0);
	}

	@Test
	public void shouldReturn3883Days() {
		double result = daysFeature.getValue("event-2", keywords, userModel);
		assertTrue(result == 3883.0);
	}
	
	@Test
	public void shouldReturnOneDayDueToMin() {
		double result = daysFeature.getValue("event-3", keywords, userModel);
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroYears() {
		double result = yearsFeature.getValue("event-1", keywords, userModel);
		assertTrue(result == 0.0);
	}

	@Test
	public void shouldReturnTenYears() {
		double result = yearsFeature.getValue("event-2", keywords, userModel);
		assertTrue(result == 10.0);
	}
	
	@Test
	public void shouldReturnZeroYearsDueToMin() {
		double result = yearsFeature.getValue("event-3", keywords, userModel);
		assertTrue(result == 0.0);
	}
}
