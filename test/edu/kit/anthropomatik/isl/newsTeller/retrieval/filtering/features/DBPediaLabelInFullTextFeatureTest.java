package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import static org.junit.Assert.*;

import java.util.logging.LogManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;

public class DBPediaLabelInFullTextFeatureTest {

	private DBPediaLabelInFullTextFeature feature;
	private KnowledgeStoreAdapter ksAdapter;
	
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
		feature = (DBPediaLabelInFullTextFeature) context.getBean("appearLabelsInTextFeature");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
	}

	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}

	@Test
	public void ShouldReturnZero() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Autopsy_reveals_that_Terri_Schiavo_was_in_a_persistent_vegetative_state#ev17");
		assertTrue(value == 0.0);
	}
	
	@Test
	public void ShouldReturnOne() {
		double value = feature.getValue("http://en.wikinews.org/wiki/Walter_Frederick_Morrison,_inventor_of_frisbee,_dies_at_age_90#ev18_3");
		assertTrue(value == 1.0);
	}

}
