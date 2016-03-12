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

public class ConstituentContainsStringsFeatureTest {

	private ConstituentContainsStringsFeature feature;
	private KnowledgeStoreAdapter ksAdapter;
	
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
		
		ConcurrentMap<String, Set<String>> eventEntityMap = new ConcurrentHashMap<String, Set<String>>();
		eventEntityMap.put("event-1", Sets.newHashSet("actor-1"));
		eventEntityMap.put("event-2", Sets.newHashSet("actor-1", "actor-2"));
		
		ConcurrentMap<String, Set<String>> entityLabelMap = new ConcurrentHashMap<String, Set<String>>();
		entityLabelMap.put("actor-1", Sets.newHashSet("This does not contain any quotation marks."));
		entityLabelMap.put("actor-2", Sets.newHashSet("This ` sentence \" however ' does '' so."));
		
		
		sparqlCache.put(Util.getRelationName("event", "entity", "keyword"), eventEntityMap);
		sparqlCache.put(Util.getRelationName("entity", "entityPrefLabel", "keyword"), entityLabelMap);
		
		
		Keyword k = new Keyword("keyword");
		Util.stemKeyword(k);
		keywords.add(k);
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		feature = (ConstituentContainsStringsFeature) context.getBean("constituentContainsFeature_quotation_ep");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache, new ConcurrentHashMap<String, ConcurrentMap<String,Set<KSMention>>>());
	}

	@Test
	public void shouldReturnZero() {
		double value = feature.getValue("event-1", keywords, userModel);
		assertTrue(value == 0.0);
	}

	@Test
	public void shouldReturnFour() {
		double value = feature.getValue("event-2", keywords, userModel);
		assertTrue(value == 4.0);
	}

}
