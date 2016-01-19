package edu.kit.anthropomatik.isl.newsTeller.knowledgeStore;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.logging.LogManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class KnowledgeStoreAdapterTest {

	private KnowledgeStoreAdapter ksAdapter;
	
	@BeforeClass
	public static void setUpBeforeClass() {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Before
	public void setUp() {
		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		ksAdapter.openConnection();
		
	}
	
	@After
	public void tearDown() {
		ksAdapter.closeConnection();
	}
	
	@Test
	public void shouldReturn10Events() {
		List<NewsEvent> events = ksAdapter.runSingleVariableEventQuery("SELECT ?s WHERE {?s rdf:type sem:Event} LIMIT 10", "s", 10000);
		assertTrue(events.size() == 10);
	}
	
	@Test
	public void shouldReturnEmptyListBecauseOfClosedConnection() {
		ksAdapter.closeConnection();
		List<NewsEvent> events = ksAdapter.runSingleVariableEventQuery("SELECT ?s WHERE {?s rdf:type sem:Event} LIMIT 10", "s", 10000);
		assertTrue(events.isEmpty());
	}
	
	@Test
	public void shouldReturnEmptyListBecauseOfNonMatchingEventVariable() {
		List<NewsEvent> events = ksAdapter.runSingleVariableEventQuery("SELECT ?s WHERE {?s rdf:type sem:Event} LIMIT 10", "t", 10000);
		assertTrue(events.isEmpty());
	}
	
	@Test
	public void shouldReturnEmptyListBecauseOfBrokenQuery() {
		List<NewsEvent> events = ksAdapter.runSingleVariableEventQuery("SELECT ?s HERE {?s rdf:type sem:Event} LIMIT 10", "s", 10000);
		assertTrue(events.isEmpty());
	}
	
	@Test
	public void shouldReturnOneElementedList() {
		List<Double> numbers = ksAdapter.runSingleVariableDoubleQuery("SELECT (count(?s) as ?n) WHERE {?s rdf:type sem:Event}", "n");
		assertTrue(numbers.size() == 1);
	}

	@Test
	public void shouldReturn632704() {
		double number = ksAdapter.runSingleVariableDoubleQuerySingleResult("SELECT (count(?s) as ?n) WHERE {?s rdf:type sem:Event}", "n");
		assertTrue(number == 632704);
	}
	
	@Test
	public void shouldReturnCorrectSentenceForSingleMentionEvent() {
		String expectedResult = "The stars accused of swearing before the watershed include Snoop Dogg, Madonna, Johnny Borrell (Razorlight) and Billie-Joe Armstrong (Green Day).";
		String retrievedSentence = ksAdapter.retrieveSentencefromEvent("http://en.wikinews.org/wiki/'Bad_language'_at_Live_8_concerts_trigger_complaints_to_the_BBC#ev41");
		assertTrue(expectedResult.equals(retrievedSentence));
	}
	
	@Test
	public void shouldReturnCorrectSentenceForMultipleMentionEvent() {
		List<String> expectedResults = 
				Arrays.asList(	"Instead, Kucinich is going to focus on his re-election bid to the United States House of Representatives because he is facing four other candidates in the Democratic primary for Ohio's 10th congressional district and has received criticism for spending more time on running for President than on the district which he has represented for the past 12 years.",
								"This was his second run for the presidency, his first was the 2004 presidential election.");
		String retrievedSentence = ksAdapter.retrieveSentencefromEvent("http://en.wikinews.org/wiki/Dennis_Kucinich_quits_U.S._Presidential_race#ev22");
		assertTrue(expectedResults.contains(retrievedSentence));
	}
	
	@Test
	public void shouldReturnAllSentencesForMultipleMentionEvent() {
		List<String> expectedResults = 
				Arrays.asList(	"Instead, Kucinich is going to focus on his re-election bid to the United States House of Representatives because he is facing four other candidates in the Democratic primary for Ohio's 10th congressional district and has received criticism for spending more time on running for President than on the district which he has represented for the past 12 years.",
								"This was his second run for the presidency, his first was the 2004 presidential election.");
		List<String> retrievedSentences = ksAdapter.retrieveSentencesfromEvent("http://en.wikinews.org/wiki/Dennis_Kucinich_quits_U.S._Presidential_race#ev22");
		assertTrue(expectedResults.equals(retrievedSentences));
	}
	
	@Test
	public void shouldReturnCorrectSentenceForMention() {
		String expectedResult = "This was his second run for the presidency, his first was the 2004 presidential election.";
		String retrievedSentence = ksAdapter.retrieveSentenceFromMention("http://en.wikinews.org/wiki/Dennis_Kucinich_quits_U.S._Presidential_race#char=1013,1016");
		assertTrue(expectedResult.equals(retrievedSentence));
	}
	
	@Test
	public void shouldReturnPOSVerb() {
		String pos = ksAdapter.getUniqueMentionProperty("http://en.wikinews.org/wiki/Mountaineers_'Climb_Up'_for_AIDS_funding#char=1238,1243", Util.MENTION_PROPERTY_POS);
		assertTrue(pos.equals(Util.MENTION_PROPERTY_POS_VERB));
	}
	
	@Test
	public void shouldReturnPOSNoun() {
		String pos = ksAdapter.getUniqueMentionProperty("http://en.wikinews.org/wiki/AT&amp;T_to_buy_BellSouth_for_$67_billion#char=2998,3008", Util.MENTION_PROPERTY_POS);
		assertTrue(pos.equals(Util.MENTION_PROPERTY_POS_NOUN));
	}
	
	@Test
	public void shouldReturnPropbankRaise02() {
		List<String> values = ksAdapter.getMentionProperty("http://en.wikinews.org/wiki/Mountaineers_'Climb_Up'_for_AIDS_funding#char=1238,1243", Util.MENTION_PROPERTY_PROPBANK);
		assertTrue(values.size() == 1 && values.get(0).endsWith("/raise.02"));
	}
	
	@Test
	public void shouldReturnPropbankList() {
		List<String> values = ksAdapter.getMentionProperty("http://en.wikinews.org/wiki/Mexican_president_defends_emigration#char=214,218", Util.MENTION_PROPERTY_PROPBANK);
		assertTrue(values.size() == 2);
	}
}
