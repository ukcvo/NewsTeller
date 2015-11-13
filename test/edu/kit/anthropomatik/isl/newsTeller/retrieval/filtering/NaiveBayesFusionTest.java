package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import static org.junit.Assert.*;

import java.util.logging.LogManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class NaiveBayesFusionTest {

	private NaiveBayesFusion bayes;
	
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
		bayes = (NaiveBayesFusion) context.getBean("bayesFusionTest");
		((AbstractApplicationContext) context).close();
	}

	@Test
	public void shouldReturnCorrectProbability() {
		NewsEvent e = new NewsEvent("http://en.wikinews.org/wiki/60th_anniversary_of_the_end_of_the_war_in_Asia_and_Pacific_commemorated#ev67");
		double expectedProbability = 0.114963579;
		double bayesProbability = bayes.getProbabilityOfEvent(e);
		assertTrue(Math.abs(bayesProbability-expectedProbability) < Util.EPSILON);
	}

}
