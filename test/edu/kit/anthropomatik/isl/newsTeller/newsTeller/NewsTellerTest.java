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
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class NewsTellerTest {

	private NewsTeller newsTellerScope0;
	private NewsTeller newsTellerScope2;
	
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
		newsTellerScope0 = (NewsTeller) context.getBean("newsTeller0");
		newsTellerScope2 = (NewsTeller) context.getBean("newsTeller2");
		((AbstractApplicationContext) context).close();
	}

	//region Scope 0
	@Test
	public void shouldReturnDummySummary() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(new Keyword("artificial intelligence"));
		assertTrue(newsTellerScope0.getNews(keywords, new DummyUserModel()).equals("dummySummary"));
	}
	//endregion
	
	//region Scope 1
	@Test
	public void shouldReturnExtractedSentence() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(new Keyword("artificial intelligence"));
		String result = newsTellerScope2.getNews(keywords, new DummyUserModel());
		assertTrue(!result.isEmpty() && !result.equals(Util.EMPTY_EVENT_RESPONSE));
	}
	
	@Test
	public void shouldReturnEmptyEventResponse() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(new Keyword("artificial brain"));
		String result = newsTellerScope2.getNews(keywords, new DummyUserModel());
		assertTrue(result.equals(Util.EMPTY_EVENT_RESPONSE));
	}
	
	//endregion
}
