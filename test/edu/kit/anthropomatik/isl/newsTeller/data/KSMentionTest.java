package edu.kit.anthropomatik.isl.newsTeller.data;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class KSMentionTest {

	private KSMention mentionA;
	private KSMention mentionB;
	private KSMention mentionC;
	private KSMention mentionD;
	private KSMention mentionE;
	private KSMention mentionF;
	private KSMention mentionG;
	private KSMention mentionH;	
	private KSMention mentionI;	
	
	@Before
	public void setUp() throws Exception {
		mentionA = new KSMention("http://en.wikinews.org/wiki/text1#char=50,100");
		mentionB = new KSMention("http://en.wikinews.org/wiki/text1#char=20,25");
		mentionC = new KSMention("http://en.wikinews.org/wiki/text1#char=110,120");
		mentionD = new KSMention("http://en.wikinews.org/wiki/text1#char=70,80");
		mentionE = new KSMention("http://en.wikinews.org/wiki/text1#char=40,60");
		mentionF = new KSMention("http://en.wikinews.org/wiki/text1#char=90,130");
		mentionG = new KSMention("http://en.wikinews.org/wiki/text2#char=60,70");
		mentionH = new KSMention("http://en.wikinews.org/wiki/text1#char=99,104");
		mentionI = new KSMention("http://en.wikinews.org/wiki/text1#char=40,110");
	}

	@Test
	public void shouldReturnZeroForB() {
		double overlapOne = mentionA.overlap(mentionB);
		double overlapTwo = mentionB.overlap(mentionA);
		assertTrue((overlapOne == overlapTwo) && (overlapOne == 0.0));
	}

	@Test
	public void shouldReturnZeroForC() {
		double overlapOne = mentionA.overlap(mentionC);
		double overlapTwo = mentionC.overlap(mentionA);
		assertTrue((overlapOne == overlapTwo) && (overlapOne == 0.0));
	}
	
	@Test
	public void shouldReturnOneForD() {
		double overlapOne = mentionA.overlap(mentionD);
		double overlapTwo = mentionD.overlap(mentionA);
		assertTrue((overlapOne == overlapTwo) && (overlapOne == 1.0));
	}
	
	@Test
	public void shouldReturnZeroPointFiveForE() {
		double overlapOne = mentionA.overlap(mentionE);
		double overlapTwo = mentionE.overlap(mentionA);
		assertTrue((overlapOne == overlapTwo) && (overlapOne == 0.5));
	}
	
	@Test
	public void shouldReturnOneQuarterForF() {
		double overlapOne = mentionA.overlap(mentionF);
		double overlapTwo = mentionF.overlap(mentionA);
		assertTrue((overlapOne == overlapTwo) && (overlapOne == 0.25));
	}
	
	@Test
	public void shouldReturnZeroForG() {
		double overlapOne = mentionA.overlap(mentionG);
		double overlapTwo = mentionG.overlap(mentionA);
		assertTrue((overlapOne == overlapTwo) && (overlapOne == 0.0));
	}
	
	@Test
	public void shouldReturnZeroPointTwoForH() {
		double overlapOne = mentionA.overlap(mentionH);
		double overlapTwo = mentionH.overlap(mentionA);
		assertTrue((overlapOne == overlapTwo) && (overlapOne == 0.2));
	}
	
	@Test
	public void shouldReturnOneForI() {
		double overlapOne = mentionA.overlap(mentionI);
		double overlapTwo = mentionI.overlap(mentionA);
		assertTrue((overlapOne == overlapTwo) && (overlapOne == 1.0));
	}
	
	@Test
	public void shouldReturnTrueForContains() {
		assertTrue(mentionA.contains(mentionD));
	}
	
	@Test
	public void shouldReturnFalseForContains() {
		assertFalse(mentionD.contains(mentionA));
	}
	
	@Test
	public void shouldReturnTrueForSameText() {
		assertTrue(mentionA.hasSameResourceURI(mentionB));
	}
	
	@Test
	public void shouldReturnFalseForSameText() {
		assertFalse(mentionA.hasSameResourceURI(mentionG));
	}
	
	@Test
	public void shouldReturnDistanceZero() {
		assertTrue(mentionA.distanceTo(mentionE) == 0);
	}
	
	@Test
	public void shouldReturnDistanceEleven() {
		assertTrue(mentionA.distanceTo(mentionC) == 11);
	}
	
	@Test
	public void shouldReturnGreater() {
		assertTrue(mentionA.compareTo(mentionB) > 0);
	}
	
	@Test
	public void shouldReturnLess() {
		assertTrue(mentionA.compareTo(mentionC) < 0);
	}
	
	@Test
	public void shouldReturnEqual() {
		assertTrue(mentionA.compareTo(mentionD) == 0);
	}
}
