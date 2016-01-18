package edu.kit.anthropomatik.isl.newsTeller.util.propbank;

import static org.junit.Assert.*;

import java.util.logging.LogManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PropbankFramesTest {

	private PropbankFrames propbank;
	
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
		this.propbank = PropbankFrames.getInstance("resources/propbank-frames", false);
	}

	@Test
	public void shouldReturnTrue() {
		assertTrue(this.propbank.containsFrame("abandon", PropbankFrames.SUFFIX_VERB) == true);
	}
	
	@Test
	public void shouldReturnFalse() {
		assertTrue(this.propbank.containsFrame("abandon", PropbankFrames.SUFFIX_NOUN) == false);
	}

	@Test
	public void shouldReturnNullBecauseOfBrokenSuffix() {
		assertNull(this.propbank.getRoleset("abandon", PropbankFrames.SUFFIX_NOUN, "abandon.01"));
	}
	
	@Test
	public void shouldReturnNullBecauseOfBrokenId() {
		assertNull(this.propbank.getRoleset("abandon", PropbankFrames.SUFFIX_VERB, "abandon.99"));
	}
	
	@Test
	public void shouldReturnCorrectRoleset() {
		PropbankRoleset roleset = this.propbank.getRoleset("abandon", PropbankFrames.SUFFIX_VERB, "abandon.01");
		assertTrue(roleset != null && roleset.getName().equals("abandon.01") && roleset.getArgumentSets().size() == 3);
	}
	
}
