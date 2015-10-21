package edu.kit.anthropomatik.isl.newsTeller.newsTeller;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class NewsTellerTesterScope0 {

	NewsTeller newsTeller;
	
	@Before
	public void setUp() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/Scope0_test.xml");
		newsTeller = (NewsTeller) context.getBean("newsTeller");
		((AbstractApplicationContext) context).close();
	}

	@Test
	public void shouldReturnDummySummary() {
		assertTrue(newsTeller.getNews(null).equals("dummySummary"));
	}

}
