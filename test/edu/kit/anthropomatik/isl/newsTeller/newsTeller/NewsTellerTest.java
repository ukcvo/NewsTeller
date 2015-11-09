package edu.kit.anthropomatik.isl.newsTeller.newsTeller;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.generation.SentencePicker;

public class NewsTellerTest {

	NewsTeller newsTellerScope0;
	NewsTeller newsTellerScope1;
	
	@BeforeClass
	public static void setUpBeforeClass() {
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
		newsTellerScope0 = (NewsTeller) context.getBean("newsTeller0");
		((AbstractApplicationContext) context).close();
		
		context = new FileSystemXmlApplicationContext("config/test.xml");
		newsTellerScope1 = (NewsTeller) context.getBean("newsTeller1");
		((AbstractApplicationContext) context).close();
	}

	//region Scope 0
	@Test
	public void shouldReturnDummySummary() {
		assertTrue(newsTellerScope0.getNews(null).equals("dummySummary"));
	}
	//endregion
	
	//region Scope 1
	@Test
	public void shouldReturnExtractedSentence() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(new Keyword("artificial intelligence"));
		String result = newsTellerScope1.getNews(keywords);
		assertTrue(!result.isEmpty() && !result.equals(SentencePicker.EMPTY_EVENT_RESPONSE));
	}
	
	@Test
	public void shouldReturnEmptyEventResponse() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(new Keyword("artificial brain"));
		String result = newsTellerScope1.getNews(keywords);
		assertTrue(result.equals(SentencePicker.EMPTY_EVENT_RESPONSE));
	}
	
	//endregion
}
