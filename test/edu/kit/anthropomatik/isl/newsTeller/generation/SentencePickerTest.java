package edu.kit.anthropomatik.isl.newsTeller.generation;

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
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class SentencePickerTest {

	private SummaryCreator sentencePicker;
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
		eventMentionMap.put("event-1", Sets.newHashSet("mention-1#char=3,5"));
		ConcurrentMap<String, Set<String>> resourceTextMap = new ConcurrentHashMap<String, Set<String>>();
		resourceTextMap.put("mention-1", Sets.newHashSet("Aa Bb cC."));
				
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), eventMentionMap);
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, resourceTextMap);
		
		Keyword k = new Keyword("keyword");
		Util.stemKeyword(k);
		keywords.add(k);
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		sentencePicker = (SummaryCreator) context.getBean("generator1");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.manuallyFillCaches(sparqlCache, eventMentionCache);
	}
	
	@Test
	public void shouldReturnEmptyEventResponseBecauseOfNullEvent() {
		String result = sentencePicker.summarizeEvent(null, keywords, new DummyUserModel());
		assertTrue(result.equals(Util.EMPTY_EVENT_RESPONSE));
	}

	@Test
	public void shouldReturnEmptyEventResponseBecauseOfNonexistentEvent() {
		String result = sentencePicker.summarizeEvent(new NewsEvent("http://en.wikinews.org/wiki/Non_existing_text#ev999"), keywords, new DummyUserModel());
		assertTrue(result.equals(Util.EMPTY_EVENT_RESPONSE));
	}
	
	@Test
	public void shouldReturnRegularResponse() {
		String result = sentencePicker.summarizeEvent(new NewsEvent("event-1"), keywords, new DummyUserModel());
		assertTrue(!result.isEmpty() && !result.equals(Util.EMPTY_EVENT_RESPONSE));
	}
}
