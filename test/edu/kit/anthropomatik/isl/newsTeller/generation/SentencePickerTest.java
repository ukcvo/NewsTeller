package edu.kit.anthropomatik.isl.newsTeller.generation;

import static org.junit.Assert.*;

import java.util.logging.LogManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

public class SentencePickerTest {

	private SummaryCreator sentencePicker;
	
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
		ApplicationContext context = new FileSystemXmlApplicationContext("config/Scope1_test.xml");
		sentencePicker = (SummaryCreator) context.getBean("generator");
		((AbstractApplicationContext) context).close();
	}

	@Test
	public void shouldReturnEmptyEventResponseBecauseOfNullEvent() {
		String result = sentencePicker.summarizeEvent(null);
		assertTrue(result.equals(SentencePicker.EMPTY_EVENT_RESPONSE));
	}

	@Test
	public void shouldReturnEmptyEventResponseBecauseOfNonexistentEvent() {
		String result = sentencePicker.summarizeEvent(new NewsEvent("http://en.wikinews.org/wiki/Non_existing_text#ev999"));
		System.out.println("XXX:" + result);
		assertTrue(result.equals(SentencePicker.EMPTY_EVENT_RESPONSE));
	}
	
	@Test
	public void shouldReturnRegularResponse() {
		String result = sentencePicker.summarizeEvent(new NewsEvent("http://en.wikinews.org/wiki/Journals_tackle_scientific_fraud#ev34"));
		assertTrue(!result.isEmpty() && !result.equals(SentencePicker.EMPTY_EVENT_RESPONSE));
	}
}
