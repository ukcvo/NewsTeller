package edu.kit.anthropomatik.isl.newsTeller.util.embeddings;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import com.google.common.collect.Sets;

import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class EmbeddingsProviderTest {

	private static EmbeddingsProvider glove;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		glove = new EmbeddingsProvider("resources/wordvectors/glove.6B.50d.txt");
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
