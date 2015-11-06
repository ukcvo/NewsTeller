package edu.kit.anthropomatik.isl.newsTeller.retrieval.finding;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

public class EventFinderTest {

	private EventFinder finder;
	private UserModel userModel;
	
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
		finder = (EventFinder) context.getBean("finder");
		userModel = (UserModel) context.getBean("userModel");
		((AbstractApplicationContext) context).close();
	}

	@Test
	public void shouldReturn8Events() {
		List<Keyword> keywords = new ArrayList<Keyword>();
		keywords.add(new Keyword("artificial intelligence"));
		Set<NewsEvent> result = finder.findEvents(keywords, userModel);
		assertTrue(result.size() == 8);
	}

}
