package edu.kit.anthropomatik.isl.newsTeller.knowledgeStore;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.LogManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.kit.anthropomatik.isl.newsTeller.data.KSMention;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
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
		ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
		ConcurrentMap<String, Set<String>> mentionMap = new ConcurrentHashMap<String, Set<String>>();
		mentionMap.put("http://en.wikinews.org/wiki/'Bad_language'_at_Live_8_concerts_trigger_complaints_to_the_BBC#ev41", 
				Sets.newHashSet("http://en.wikinews.org/wiki/'Bad_language'_at_Live_8_concerts_trigger_complaints_to_the_BBC#char=841,848"));
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), mentionMap);
		ksAdapter.manuallyFillCaches(sparqlCache, new ConcurrentHashMap<String, Set<KSMention>>());
		ksAdapter.runKeyValueResourceTextQuery(Sets.newHashSet("http://en.wikinews.org/wiki/'Bad_language'_at_Live_8_concerts_trigger_complaints_to_the_BBC"));
		String retrievedSentence = ksAdapter.retrieveSentencefromEvent("http://en.wikinews.org/wiki/'Bad_language'_at_Live_8_concerts_trigger_complaints_to_the_BBC#ev41", "keyword");
		assertTrue(expectedResult.equals(retrievedSentence));
	}
	
	@Test
	public void shouldReturnCorrectSentenceForMultipleMentionEvent() {
		List<String> expectedResults = 
				Arrays.asList(	"Instead, Kucinich is going to focus on his re-election bid to the United States House of Representatives because he is facing four other candidates in the Democratic primary for Ohio's 10th congressional district and has received criticism for spending more time on running for President than on the district which he has represented for the past 12 years.",
								"This was his second run for the presidency, his first was the 2004 presidential election.");
		ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
		ConcurrentMap<String, Set<String>> mentionMap = new ConcurrentHashMap<String, Set<String>>();
		mentionMap.put("http://en.wikinews.org/wiki/Dennis_Kucinich_quits_U.S._Presidential_race#ev22", 
				Sets.newHashSet("http://en.wikinews.org/wiki/Dennis_Kucinich_quits_U.S._Presidential_race#char=829,836", "http://en.wikinews.org/wiki/Dennis_Kucinich_quits_U.S._Presidential_race#char=1013,1016"));
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), mentionMap);
		ksAdapter.manuallyFillCaches(sparqlCache, new ConcurrentHashMap<String, Set<KSMention>>());
		ksAdapter.runKeyValueResourceTextQuery(Sets.newHashSet("http://en.wikinews.org/wiki/Dennis_Kucinich_quits_U.S._Presidential_race"));
		String retrievedSentence = ksAdapter.retrieveSentencefromEvent("http://en.wikinews.org/wiki/Dennis_Kucinich_quits_U.S._Presidential_race#ev22", "keyword");
		assertTrue(expectedResults.contains(retrievedSentence));
	}
	
	@Test
	public void shouldReturnAllSentencesForMultipleMentionEvent() {
		Set<String> expectedResults = 
				Sets.newHashSet( "This was his second run for the presidency, his first was the 2004 presidential election.",
								"Instead, Kucinich is going to focus on his re-election bid to the United States House of Representatives because he is facing four other candidates in the Democratic primary for Ohio's 10th congressional district and has received criticism for spending more time on running for President than on the district which he has represented for the past 12 years."
								);
		ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
		ConcurrentMap<String, Set<String>> mentionMap = new ConcurrentHashMap<String, Set<String>>();
		mentionMap.put("http://en.wikinews.org/wiki/Dennis_Kucinich_quits_U.S._Presidential_race#ev22", 
				Sets.newHashSet("http://en.wikinews.org/wiki/Dennis_Kucinich_quits_U.S._Presidential_race#char=829,836", "http://en.wikinews.org/wiki/Dennis_Kucinich_quits_U.S._Presidential_race#char=1013,1016"));
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), mentionMap);
		ksAdapter.manuallyFillCaches(sparqlCache, new ConcurrentHashMap<String, Set<KSMention>>());
		ksAdapter.runKeyValueResourceTextQuery(Sets.newHashSet("http://en.wikinews.org/wiki/Dennis_Kucinich_quits_U.S._Presidential_race"));
		Set<String> retrievedSentences = new HashSet<String>(ksAdapter.retrieveSentencesFromEvent("http://en.wikinews.org/wiki/Dennis_Kucinich_quits_U.S._Presidential_race#ev22", "keyword"));
		assertTrue(expectedResults.equals(retrievedSentences));
	}
	
	@Test
	public void shouldReturnCorrectSentenceForMention() {
		String expectedResult = "This was his second run for the presidency, his first was the 2004 presidential election.";
		ksAdapter.runKeyValueResourceTextQuery(Sets.newHashSet("http://en.wikinews.org/wiki/Dennis_Kucinich_quits_U.S._Presidential_race"));
		String retrievedSentence = ksAdapter.retrieveSentenceFromMention("http://en.wikinews.org/wiki/Dennis_Kucinich_quits_U.S._Presidential_race#char=1013,1016");
		assertTrue(expectedResult.equals(retrievedSentence));
	}
	
	@Test
	public void shouldReturnCorrectSentenceForMentionSpecialChars() {
		String expectedResult = "The European Union presidency says that air traffic over Europe could return to about 50 percent of its normal level on Monday, if weather forecasts confirm that skies over the continent are clearing of volcanic ash.";
		ksAdapter.runKeyValueResourceTextQuery(Sets.newHashSet("http://en.wikinews.org/wiki/Half_of_Europe's_flights_could_take_off_Monday,_EU_says"));
		String retrievedSentence = ksAdapter.retrieveSentenceFromMention("http://en.wikinews.org/wiki/Half_of_Europe's_flights_could_take_off_Monday,_EU_says#char=372,380");
		assertTrue(expectedResult.equals(retrievedSentence));
	}
	
	@Test
	public void shouldReturnCorrectPhraseFromMention() {
		String expectedResult = "videos uploaded by their users";
		ksAdapter.runKeyValueResourceTextQuery(Sets.newHashSet("http://en.wikinews.org/wiki/Movie_'The_Assassination_of_Jesse_James'_leaked_on_the_internet"));
		String retrievedSentence = ksAdapter.retrievePhraseFromMention("http://en.wikinews.org/wiki/Movie_'The_Assassination_of_Jesse_James'_leaked_on_the_internet#char=352,382");
		assertTrue(expectedResult.equals(retrievedSentence));
	}
	
	@Test
	public void shouldReturnCorrectPhraseFromEntity() {
		String expectedResult = "videos uploaded by their users";
		ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
		ConcurrentMap<String, Set<String>> mentionMap = new ConcurrentHashMap<String, Set<String>>();
		mentionMap.put("http://www.newsreader-project.eu/data/wikinews/non-entities/videos+uploaded+by+their+users", 
				Sets.newHashSet("http://en.wikinews.org/wiki/Movie_'The_Assassination_of_Jesse_James'_leaked_on_the_internet#char=352,382"));
		sparqlCache.put(Util.getRelationName("entity", "mention", "upload"), mentionMap);
		ksAdapter.manuallyFillCaches(sparqlCache, new ConcurrentHashMap<String, Set<KSMention>>());
		ksAdapter.runKeyValueResourceTextQuery(Sets.newHashSet("http://en.wikinews.org/wiki/Movie_'The_Assassination_of_Jesse_James'_leaked_on_the_internet"));
		List<String> retrievedSentences = ksAdapter.retrievePhrasesFromEntity("http://www.newsreader-project.eu/data/wikinews/non-entities/videos+uploaded+by+their+users", "upload");
		assertTrue(retrievedSentences.size() == 1 && retrievedSentences.contains(expectedResult));
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
	
	@Test
	public void shouldReturnAllEventMentions() {
		List<String> mentionURIs = Util.readStringListFromFile("resources/test/eventList.txt");
		Set<KSMention> expected = new HashSet<KSMention>();
		for (String uri : mentionURIs)
			expected.add(new KSMention(uri));
		ksAdapter.retrieveAllEventMentions(Sets.newHashSet("http://en.wikinews.org/wiki/Brazil_wins_Confederations_Cup"));
		Set<KSMention> retrieved = ksAdapter.getAllEventMentions("http://en.wikinews.org/wiki/Brazil_wins_Confederations_Cup");
		
		assertTrue(retrieved.equals(expected));
	}
	
	@Test
	public void shouldFillBufferAndRetrieveResult() {
		Set<String> uris = new HashSet<String>();
		uris.add("http://en.wikinews.org/wiki/Mexican_president_defends_emigration#ev18");
		uris.add("http://en.wikinews.org/wiki/Mexican_president_defends_emigration#ev19");
		uris.add("http://en.wikinews.org/wiki/Mexican_president_defends_emigration#ev20");
		List<Keyword> keywords = new ArrayList<Keyword>();
		Keyword k = new Keyword("keyword");
		Util.stemKeyword(k);
		keywords.add(k);
		ksAdapter.runKeyValueSparqlQuery("SELECT ?event ?entity WHERE { VALUES ?event { *keys* } . ?event sem:hasActor|sem:hasPlace ?entity}", 
				uris, keywords);
		Set<String> expected = new HashSet<String>();
		expected.add("http://dbpedia.org/resource/Vicente_Fox");
		expected.add("http://dbpedia.org/resource/Fox_Broadcasting_Company");
		Set<String> result = ksAdapter.getBufferedValues("event-entity-keyword", "http://en.wikinews.org/wiki/Mexican_president_defends_emigration#ev18");
		assertTrue(expected.equals(result));
	}
	
	@Test
	public void shouldFillBufferAndReturnEmptySetDueToRelationName() {
		Set<String> uris = new HashSet<String>();
		uris.add("http://en.wikinews.org/wiki/Mexican_president_defends_emigration#ev18");
		uris.add("http://en.wikinews.org/wiki/Mexican_president_defends_emigration#ev19");
		uris.add("http://en.wikinews.org/wiki/Mexican_president_defends_emigration#ev20");
		List<Keyword> keywords = new ArrayList<Keyword>();
		Keyword k = new Keyword("test");
		Util.stemKeyword(k);
		keywords.add(k);
		ksAdapter.runKeyValueSparqlQuery("SELECT ?event ?entity WHERE { VALUES ?event { *keys* } . ?event sem:hasActor|sem:hasPlace ?entity}", uris, keywords);
		Set<String> result = ksAdapter.getBufferedValues("other-relation", "http://en.wikinews.org/wiki/Mexican_president_defends_emigration#ev18");
		assertTrue(result.isEmpty());
	}
	
	@Test
	public void shouldFillBufferAndReturnEmptySetDueToKey() {
		Set<String> uris = new HashSet<String>();
		uris.add("http://en.wikinews.org/wiki/Mexican_president_defends_emigration#ev18");
		uris.add("http://en.wikinews.org/wiki/Mexican_president_defends_emigration#ev19");
		uris.add("http://en.wikinews.org/wiki/Mexican_president_defends_emigration#ev20");
		List<Keyword> keywords = new ArrayList<Keyword>();
		Keyword k = new Keyword("test");
		Util.stemKeyword(k);
		keywords.add(k);
		ksAdapter.runKeyValueSparqlQuery("SELECT ?event ?entity WHERE { VALUES ?event { *keys* } . ?event sem:hasActor|sem:hasPlace ?entity}", uris, keywords);
		Set<String> result = ksAdapter.getBufferedValues("event-entity-test", "http://en.wikinews.org/wiki/Mexican_president_defends_emigration#ev17");
		assertTrue(result.isEmpty());
	}
	
	@Test
	public void shouldReturnCorrectResourceTitle() {
		String expected = "Mexican president defends emigration";
		ksAdapter.runKeyValueResourcePropertyQuery(Sets.newHashSet(Util.RESOURCE_PROPERTY_TITLE), Sets.newHashSet("http://en.wikinews.org/wiki/Mexican_president_defends_emigration"));
		String result = ksAdapter.getFirstBufferedValue(Util.RELATION_NAME_RESOURCE_PROPERTY + Util.RESOURCE_PROPERTY_TITLE, "http://en.wikinews.org/wiki/Mexican_president_defends_emigration");
		assertTrue(result.equals(expected));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void shouldReturnAllSentenceTokens() {
		Set<ArrayList<String>> expectedResult = Sets.newHashSet(Lists.newArrayList("Expected", "result", "number", "one", "."), 
															Lists.newArrayList("ExpectedResultNumberTwo", "."));
		ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
		ConcurrentMap<String, Set<String>> mentionMap = new ConcurrentHashMap<String, Set<String>>();
		mentionMap.put("event-1", Sets.newHashSet("mention-1#char=1,2"));
		mentionMap.put("event-2", Sets.newHashSet("mention-2#char=1,2"));
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), mentionMap);
		ConcurrentMap<String, Set<String>> resourceMap = new ConcurrentHashMap<String, Set<String>>();
		resourceMap.put("mention-1", Sets.newHashSet("Expected result number one. Irrelevant Sentence."));
		resourceMap.put("mention-2", Sets.newHashSet("ExpectedResultNumberTwo. Another irrelevant Sentence."));
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, resourceMap);
		
		ksAdapter.manuallyFillCaches(sparqlCache, new ConcurrentHashMap<String, Set<KSMention>>());
		Set<List<String>> result = ksAdapter.getAllQuerySentenceTokens("keyword");
		assertTrue(expectedResult.equals(result));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void shouldReturnAllTextTokens() {
		Set<ArrayList<String>> expectedResult = Sets.newHashSet(Lists.newArrayList("Expected", "result", "number", "one", ".", "Irrelevant", "Sentence", "."),
															Lists.newArrayList("ExpectedResultNumberTwo", ".", "Another", "irrelevant", "Sentence", "."));
		ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
		ConcurrentMap<String, Set<String>> mentionMap = new ConcurrentHashMap<String, Set<String>>();
		mentionMap.put("event-1", Sets.newHashSet("mention-1#char=1,2"));
		mentionMap.put("event-2", Sets.newHashSet("mention-2#char=1,2"));
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), mentionMap);
		ConcurrentMap<String, Set<String>> resourceMap = new ConcurrentHashMap<String, Set<String>>();
		resourceMap.put("mention-1", Sets.newHashSet("Expected result number one. Irrelevant Sentence."));
		resourceMap.put("mention-2", Sets.newHashSet("ExpectedResultNumberTwo. Another irrelevant Sentence."));
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, resourceMap);
		
		ksAdapter.manuallyFillCaches(sparqlCache, new ConcurrentHashMap<String, Set<KSMention>>());
		Set<List<String>> result = ksAdapter.getAllQueryTextTokens("keyword");
		assertTrue(expectedResult.equals(result));
	}
	
	@Test
	public void shouldReturnEventSentenceTokens() {
		List<String> dummy = Lists.newArrayList("Expected", "result", "number", "one", ".");
		Set<List<String>> expectedResult = new HashSet<List<String>>();
		expectedResult.add(dummy);
		ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
		ConcurrentMap<String, Set<String>> mentionMap = new ConcurrentHashMap<String, Set<String>>();
		mentionMap.put("event-1", Sets.newHashSet("mention-1#char=1,2"));
		mentionMap.put("event-2", Sets.newHashSet("mention-2#char=1,2"));
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), mentionMap);
		ConcurrentMap<String, Set<String>> resourceMap = new ConcurrentHashMap<String, Set<String>>();
		resourceMap.put("mention-1", Sets.newHashSet("Expected result number one. Irrelevant Sentence."));
		resourceMap.put("mention-2", Sets.newHashSet("ExpectedResultNumberTwo. Another irrelevant Sentence."));
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, resourceMap);
		
		ksAdapter.manuallyFillCaches(sparqlCache, new ConcurrentHashMap<String, Set<KSMention>>());
		Set<List<String>> result = ksAdapter.retrieveSentenceTokensFromEvent("event-1", "keyword");
		assertTrue(expectedResult.equals(result));
	}
	
	@Test
	public void shouldReturnEventTextTokens() {
		List<String> dummy = Lists.newArrayList("Expected", "result", "number", "one", ".", "Irrelevant", "Sentence", ".");
		Set<List<String>> expectedResult = new HashSet<List<String>>();
		expectedResult.add(dummy);
		ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
		ConcurrentMap<String, Set<String>> mentionMap = new ConcurrentHashMap<String, Set<String>>();
		mentionMap.put("event-1", Sets.newHashSet("mention-1#char=1,2"));
		mentionMap.put("event-2", Sets.newHashSet("mention-2#char=1,2"));
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), mentionMap);
		ConcurrentMap<String, Set<String>> resourceMap = new ConcurrentHashMap<String, Set<String>>();
		resourceMap.put("mention-1", Sets.newHashSet("Expected result number one. Irrelevant Sentence."));
		resourceMap.put("mention-2", Sets.newHashSet("ExpectedResultNumberTwo. Another irrelevant Sentence."));
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, resourceMap);
		
		ksAdapter.manuallyFillCaches(sparqlCache, new ConcurrentHashMap<String, Set<KSMention>>());
		Set<List<String>> result = ksAdapter.retrieveOriginalTextTokens("event-1", "keyword");
		assertTrue(expectedResult.equals(result));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void shouldReturnAllTitleTokens() {
		Set<ArrayList<String>> expectedResult = Sets.newHashSet(Lists.newArrayList("Expected", "result", "number", "one", "."), 
															Lists.newArrayList("ExpectedResultNumberTwo", "."));
		ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
		ConcurrentMap<String, Set<String>> mentionMap = new ConcurrentHashMap<String, Set<String>>();
		mentionMap.put("event-1", Sets.newHashSet("mention-1#char=1,2", "mention-2#char=1,2"));
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), mentionMap);
		ConcurrentMap<String, Set<String>> resourceMap = new ConcurrentHashMap<String, Set<String>>();
		resourceMap.put("mention-1", Sets.newHashSet("Expected result number one."));
		resourceMap.put("mention-2", Sets.newHashSet("ExpectedResultNumberTwo."));
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_PROPERTY + Util.RESOURCE_PROPERTY_TITLE, resourceMap);
		
		ksAdapter.manuallyFillCaches(sparqlCache, new ConcurrentHashMap<String, Set<KSMention>>());
		Set<List<String>> result = ksAdapter.retrieveTitleTokensFromEvent("event-1", "keyword");
		assertTrue(expectedResult.equals(result));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void shouldReturnAllTextURIs() {
		List<HashSet<String>> expectedResult = Lists.newArrayList(Sets.newHashSet("mention-1"),	Sets.newHashSet("mention-1", "mention-2"));
		ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
		ConcurrentMap<String, Set<String>> mentionMap = new ConcurrentHashMap<String, Set<String>>();
		mentionMap.put("event-1", Sets.newHashSet("mention-1#char=1,2"));
		mentionMap.put("event-2", Sets.newHashSet("mention-1#char=1,2", "mention-2#char=1,2"));
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), mentionMap);
		
		ksAdapter.manuallyFillCaches(sparqlCache, new ConcurrentHashMap<String, Set<KSMention>>());
		List<Set<String>> result = ksAdapter.getAllQueryResourceURIs("keyword");
		assertTrue(expectedResult.equals(result));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void shouldReturnAllSentences() {
		List<HashSet<String>> expectedResult = Lists.newArrayList(Sets.newHashSet("Expected result number one."), 
																Sets.newHashSet("ExpectedResultNumberTwo.", "Expected result number one."));
		ConcurrentMap<String, ConcurrentMap<String, Set<String>>> sparqlCache = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
		ConcurrentMap<String, Set<String>> mentionMap = new ConcurrentHashMap<String, Set<String>>();
		mentionMap.put("event-1", Sets.newHashSet("mention-1#char=1,2"));
		mentionMap.put("event-2", Sets.newHashSet("mention-1#char=1,2", "mention-2#char=1,2"));
		sparqlCache.put(Util.getRelationName("event", "mention", "keyword"), mentionMap);
		ConcurrentMap<String, Set<String>> resourceMap = new ConcurrentHashMap<String, Set<String>>();
		resourceMap.put("mention-1", Sets.newHashSet("Expected result number one. Irrelevant Sentence."));
		resourceMap.put("mention-2", Sets.newHashSet("ExpectedResultNumberTwo. Another irrelevant Sentence."));
		sparqlCache.put(Util.RELATION_NAME_RESOURCE_TEXT, resourceMap);
		
		ksAdapter.manuallyFillCaches(sparqlCache, new ConcurrentHashMap<String, Set<KSMention>>());
		List<Set<String>> result = ksAdapter.getAllQuerySentences("keyword");
		assertTrue(expectedResult.equals(result));
	}
}
