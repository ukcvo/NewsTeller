package edu.kit.anthropomatik.isl.newsTeller.main;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class MainTester {

	@Test
	public void shouldReturnBeep() {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/Scope0_test.xml");
		Main m = (Main) context.getBean("main");
		((AbstractApplicationContext) context).close();
		
		assertTrue(m.getMsg().equals("beep"));
	}

}
