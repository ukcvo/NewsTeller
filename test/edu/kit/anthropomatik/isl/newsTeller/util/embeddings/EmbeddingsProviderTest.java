package edu.kit.anthropomatik.isl.newsTeller.util.embeddings;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.google.common.collect.Sets;

import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class EmbeddingsProviderTest {

	private static EmbeddingsProvider glove;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/rankingEmbeddingsFeatures.xml");
		glove = (EmbeddingsProvider) context.getBean("embeddingsGlove");
		((AbstractApplicationContext) context).close();
	}

	@Test
	public void shouldReturnTrue() {
		assertTrue(glove.hasWord("volcano"));
	}
	
	@Test
	public void shouldReturnZeroPointFiveFour() {
		double similarity = EmbeddingsProvider.cosineSimilarity(glove.wordsToVector(Sets.newHashSet("erupt")), 
				glove.wordsToVector(Sets.newHashSet("volcano")));
		assertTrue(Math.abs(similarity - 0.541080) < Util.EPSILON);
	}
	
	
}
