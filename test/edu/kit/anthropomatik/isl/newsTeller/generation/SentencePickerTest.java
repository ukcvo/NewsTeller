package edu.kit.anthropomatik.isl.newsTeller.generation;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.logging.LogManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class SentencePickerTest {

	private SummaryCreator sentencePicker;
	private KnowledgeStoreAdapter ksAdapter;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		sentencePicker = (SummaryCreator) context.getBean("generator1");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
	}

	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}
	
	@Test
	public void shouldReturnEmptyEventResponseBecauseOfNullEvent() {
		String result = sentencePicker.summarizeEvent(null, null);
		assertTrue(result.equals(Util.EMPTY_EVENT_RESPONSE));
	}

	@Test
	public void shouldReturnEmptyEventResponseBecauseOfNonexistentEvent() {
		String result = sentencePicker.summarizeEvent(new NewsEvent("http://en.wikinews.org/wiki/Non_existing_text#ev999"), null);
		assertTrue(result.equals(Util.EMPTY_EVENT_RESPONSE));
	}
	
	@Test
	public void shouldReturnRegularResponse() {
		String result = sentencePicker.summarizeEvent(new NewsEvent("http://en.wikinews.org/wiki/Journals_tackle_scientific_fraud#ev34"), 
				Arrays.asList(new Keyword("science")));
		assertTrue(!result.isEmpty() && !result.equals(Util.EMPTY_EVENT_RESPONSE));
	}
}
