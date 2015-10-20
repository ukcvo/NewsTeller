package edu.kit.anthropomatik.isl.newsTeller.main;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class MainTester {

	private Main m;
	
	@Before
	public void init() {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/Scope0_test.xml");
		m = (Main) context.getBean("main");
		((AbstractApplicationContext) context).close();
	}
	
	@Test
	public void shouldReturnBeep() {
		assertTrue(m.getMsg().equals("beep"));
	}

}
