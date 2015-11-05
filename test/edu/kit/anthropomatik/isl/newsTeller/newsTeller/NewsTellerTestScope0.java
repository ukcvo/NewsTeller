package edu.kit.anthropomatik.isl.newsTeller.newsTeller;

import static org.junit.Assert.*;

import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class NewsTellerTestScope0 {

	NewsTeller newsTeller;
	
private static Log log;
	
	@BeforeClass
	public static void setUpBeforeClass() {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(NewsTellerTestScope0.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/Scope0_test.xml");
		newsTeller = (NewsTeller) context.getBean("newsTeller");
		((AbstractApplicationContext) context).close();
	}

	@Test
	public void shouldReturnDummySummary() {
		if(log.isTraceEnabled())
			log.trace("shouldReturnDummySummary()");
		assertTrue(newsTeller.getNews(null).equals("dummySummary"));
	}

}
