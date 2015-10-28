package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics;

import static org.junit.Assert.*;

import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ScoringFormulaTest {

	private static Log log;
	
	private ScoringFormula formula;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(ScoringFormulaTest.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Before
	public void setUp() throws Exception {
		this.formula = new ScoringFormula("2*x");
	}

	@Test
	public void shouldReturn1() {
		if (log.isInfoEnabled())
			log.info("shouldReturn1");
		assertTrue(this.formula.apply(0.5) == 1);
	}
	
	@Test
	public void shouldReturn0DueToOverflow() {
		if (log.isInfoEnabled())
			log.info("shouldReturn0DueToOverflow");
		assertTrue(this.formula.apply(0.7) == 0);
	}
	
	@Test
	public void shouldReturn0DueToUnderflow() {
		if (log.isInfoEnabled())
			log.info("shouldReturn0DueToUnderflow");
		assertTrue(this.formula.apply(-0.1) == 0);
	}

}
