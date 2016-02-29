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
import edu.kit.anthropomatik.isl.newsTeller.userModel.ActualUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import jersey.repackaged.com.google.common.collect.Lists;

public class KeywordsInSentenceFeatureTest {

private KnowledgeStoreAdapter ksAdapter;
	
	private KeywordsInSentenceFeature avgFeature;
	private KeywordsInSentenceFeature maxFeature;
	private KeywordsInSentenceFeature minFeature;
	private KeywordsInSentenceFeature titleFeature;
	private KeywordsInSentenceFeature containsFeature;
	private KeywordsInSentenceFeature stemFeature;
	private KeywordsInSentenceFeature splitFeature;
	private KeywordsInSentenceFeature interestFeature;
	
	private static ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
	private static ConcurrentMap<String, Set<KSMention>> eventMentionCache = new ConcurrentHashMap<String, Set<KSMention>>();
	private static List<Keyword> keywords = new ArrayList<Keyword>();
	private static List<Keyword> twoKeywords = new ArrayList<Keyword>();
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
		sparqlCache.put(Util.getRelationName("event", "mention", "key"), eventMentionMap);
		sparqlCache.put(Util.getRelationName("event", "mention", "keyed"), eventMentionMap);
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword search"), eventMentionMap);
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, eventResourceTextMap);
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_PROPERTY + Util.RESOURCE_PROPERTY_TITLE, eventResourceTitleMap);
		
		Keyword k = new Keyword("keyword");
		Util.stemKeyword(k);
		keywords.add(k);
		twoKeywords.add(k);
		Keyword k2 = new Keyword("other");
		Util.stemKeyword(k2);
		twoKeywords.add(k2);
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		avgFeature = (KeywordsInSentenceFeature) context.getBean("keywordInSentenceFeature00ffff");
		minFeature = (KeywordsInSentenceFeature) context.getBean("keywordInSentenceFeature11ffff");
		maxFeature = (KeywordsInSentenceFeature) context.getBean("keywordInSentenceFeature22ffff");
		titleFeature = (KeywordsInSentenceFeature) context.getBean("keywordInSentenceFeature00tfff");
		containsFeature = (KeywordsInSentenceFeature) context.getBean("keywordInSentenceFeature00ftff");
		stemFeature = (KeywordsInSentenceFeature) context.getBean("keywordInSentenceFeature00fftf");
		splitFeature = (KeywordsInSentenceFeature) context.getBean("keywordInSentenceFeature00ffft");
		interestFeature = (KeywordsInSentenceFeature) context.getBean("keywordInSentenceFeature00fffftrue");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache);
	}
	
	@Test
	public void shouldReturnZeroAvg() {
		double result = avgFeature.getValue("event-1", keywords, userModel);
		assertTrue(result == 0.0);
	}

	@Test
	public void shouldReturnOneAvg() {
		double result = avgFeature.getValue("event-2", keywords, userModel);
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroPointFiveAvg() {
		double result = avgFeature.getValue("event-3", keywords, userModel);
		assertTrue(result == 0.5);
	}
	
	@Test
	public void shouldReturnZeroPointTwoFiveAvg() {
		double result = avgFeature.getValue("event-3", twoKeywords, userModel);
		assertTrue(result == 0.25);
	}
	
	@Test
	public void shouldReturnZeroPointFiveAvgEvent4() {
		double result = avgFeature.getValue("event-4", twoKeywords, userModel);
		assertTrue(result == 0.5);
	}
	
	@Test
	public void shouldReturnZeroMin() {
		double result = minFeature.getValue("event-3", keywords, userModel);
		assertTrue(result == 0.0);
	}
	
	@Test
	public void shouldReturnZeroMinEvent3() {
		double result = minFeature.getValue("event-3", twoKeywords, userModel);
		assertTrue(result == 0.0);
	}
	
	@Test
	public void shouldReturnZeroMinEvent4() {
		double result = minFeature.getValue("event-4", twoKeywords, userModel);
		assertTrue(result == 0.0);
	}
	
	@Test
	public void shouldReturnOneMinEvent5() {
		double result = minFeature.getValue("event-5", twoKeywords, userModel);
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnOneMax() {
		double result = maxFeature.getValue("event-3", keywords, userModel);
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnOneMaxTwoKeywords() {
		double result = maxFeature.getValue("event-3", twoKeywords, userModel);
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroMax() {
		double result = maxFeature.getValue("event-1", keywords, userModel);
		assertTrue(result == 0.0);
	}
	
	@Test
	public void shouldReturnZeroTitle() {
		double result = titleFeature.getValue("event-1", keywords, userModel);
		assertTrue(result == 0.0);
	}

	@Test
	public void shouldReturnOneTitle() {
		double result = titleFeature.getValue("event-2", keywords, userModel);
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroPointFiveTitle() {
		double result = titleFeature.getValue("event-3", keywords, userModel);
		assertTrue(result == 0.5);
	}
	
	@Test
	public void shouldReturnZeroPointTwoFiveTitle() {
		double result = titleFeature.getValue("event-3", twoKeywords, userModel);
		assertTrue(result == 0.25);
	}

	@Test
	public void shouldReturnZeroContains() {
		Keyword k = new Keyword("key");
		Util.stemKeyword(k);
		double result = containsFeature.getValue("event-1", Lists.newArrayList(k), userModel);
		assertTrue(result == 0.0);
	}

	@Test
	public void shouldReturnOneContains() {
		Keyword k = new Keyword("key");
		Util.stemKeyword(k);
		double result = containsFeature.getValue("event-2", Lists.newArrayList(k), userModel);
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroPointFiveContains() {
		Keyword k = new Keyword("key");
		Util.stemKeyword(k);
		double result = containsFeature.getValue("event-3", Lists.newArrayList(k), userModel);
		assertTrue(result == 0.5);
	}
	
	@Test
	public void shouldReturnZeroPointTwoFiveContains() {
		Keyword k = new Keyword("key");
		Util.stemKeyword(k);
		Keyword k2 = new Keyword("nonexistent");
		Util.stemKeyword(k2);
		double result = containsFeature.getValue("event-3", Lists.newArrayList(k, k2), userModel);
		assertTrue(result == 0.25);
	}
	
	@Test
	public void shouldReturnZeroStem() {
		Keyword k = new Keyword("keyed");
		Util.stemKeyword(k);
		double result = stemFeature.getValue("event-1", Lists.newArrayList(k), userModel);
		assertTrue(result == 0.0);
	}

	@Test
	public void shouldReturnOneStem() {
		Keyword k = new Keyword("keyed");
		Util.stemKeyword(k);
		double result = stemFeature.getValue("event-2", Lists.newArrayList(k), userModel);
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroPointFiveStem() {
		Keyword k = new Keyword("keyed");
		Util.stemKeyword(k);
		double result = stemFeature.getValue("event-3", Lists.newArrayList(k), userModel);
		assertTrue(result == 0.5);
	}
	
	@Test
	public void shouldReturnZeroPointTwoFiveStem() {
		Keyword k = new Keyword("keyed");
		Util.stemKeyword(k);
		Keyword k2 = new Keyword("nonexistent");
		Util.stemKeyword(k2);
		double result = stemFeature.getValue("event-3", Lists.newArrayList(k, k2), userModel);
		assertTrue(result == 0.25);
	}
	
	@Test
	public void shouldReturnZeroSplit() {
		Keyword k = new Keyword("keyword search");
		Util.stemKeyword(k);
		double result = splitFeature.getValue("event-1", Lists.newArrayList(k), userModel);
		assertTrue(result == 0.0);
	}

	@Test
	public void shouldReturnZeroPointFiveSplit() {
		Keyword k = new Keyword("keyword search");
		Util.stemKeyword(k);
		double result = splitFeature.getValue("event-2", Lists.newArrayList(k), userModel);
		assertTrue(result == 0.5);
	}
	
	@Test
	public void shouldReturnZeroPointTwoFiveSplit() {
		Keyword k = new Keyword("keyword search");
		Util.stemKeyword(k);
		double result = splitFeature.getValue("event-3", Lists.newArrayList(k), userModel);
		assertTrue(result == 0.25);
	}
	
	@Test
	public void shouldReturnZeroPointOneTwoFiveSplit() {
		Keyword k = new Keyword("keyword search");
		Util.stemKeyword(k);
		Keyword k2 = new Keyword("nonexistent");
		Util.stemKeyword(k2);
		double result = splitFeature.getValue("event-3", Lists.newArrayList(k, k2), userModel);
		assertTrue(result == 0.125);
	}
	
	@Test
	public void shouldReturnZeroInterest() {
		double result = interestFeature.getValue("event-1", keywords, new ActualUserModel(keywords));
		assertTrue(result == 0.0);
	}

	@Test
	public void shouldReturnOneInterest() {
		double result = interestFeature.getValue("event-2", keywords, new ActualUserModel(keywords));
		assertTrue(result == 1.0);
	}
	
	@Test
	public void shouldReturnZeroPointFiveInterest() {
		double result = interestFeature.getValue("event-3", keywords, new ActualUserModel(keywords));
		assertTrue(result == 0.5);
	}
	
	@Test
	public void shouldReturnZeroPointTwoFiveInterest() {
		double result = interestFeature.getValue("event-3", keywords, new ActualUserModel(twoKeywords));
		assertTrue(result == 0.25);
	}
	
	@Test
	public void shouldReturnZeroPointFiveInterestEvent4() {
		double result = interestFeature.getValue("event-4", keywords, new ActualUserModel(twoKeywords));
		assertTrue(result == 0.5);
	}
	
	
}
