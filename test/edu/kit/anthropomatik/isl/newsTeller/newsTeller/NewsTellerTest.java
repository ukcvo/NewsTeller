package edu.kit.anthropomatik.isl.newsTeller.newsTeller;

import static org.junit.Assert.*;

import java.util.logging.LogManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class NewsTellerTest {

	NewsTeller newsTellerScope0;
	
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
		ApplicationContext context = new FileSystemXmlApplicationContext("config/Scope0_test.xml");
		newsTellerScope0 = (NewsTeller) context.getBean("newsTeller");
		((AbstractApplicationContext) context).close();
	}

	@Test
	public void shouldReturnDummySummary() {
		assertTrue(newsTellerScope0.getNews(null).equals("dummySummary"));
	}

}
