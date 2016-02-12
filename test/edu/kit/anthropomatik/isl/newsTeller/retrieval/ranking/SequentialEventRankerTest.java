package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class SequentialEventRankerTest {

	private KnowledgeStoreAdapter ksAdapter;
	
	private Set<NewsEvent> input;
	private NewsEvent e1;
	private NewsEvent e2;
	private NewsEvent e3;
	private NewsEvent e4;
	private List<Keyword> keywords;
	
	private SequentialEventRanker ranker;
	
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
		ranker = (SequentialEventRanker) context.getBean("ranker3");
		ksAdapter = (KnowledgeStoreAdapter) context.getBean("ksAdapter");
		((AbstractApplicationContext) context).close();
		
		this.keywords = new ArrayList<Keyword>();
		Keyword k = new Keyword("NASA");
		Util.stemKeyword(k);
		keywords.add(k);
		this.e1 = new NewsEvent("http://en.wikinews.org/wiki/Astronomers_spot_two_supermassive_black_holes_orbiting_each_other#ev36");
		this.e2 = new NewsEvent("http://en.wikinews.org/wiki/Huygens_probe_lands_on_Saturn's_moon_Titan,_returns_pictures#ev43");
		this.e3 = new NewsEvent("http://en.wikinews.org/wiki/Ariane_5_rocket_launches_first_Automated_Transfer_Vehicle#ev53");
		this.e4 = new NewsEvent("http://en.wikinews.org/wiki/NASA_solar_sail_passes_first_major_test#ev35");
		this.input = Sets.newHashSet(e1, e2, e3, e4);
		ksAdapter.openConnection();
		Set<String> eventURIs = Sets.newHashSet("http://en.wikinews.org/wiki/Astronomers_spot_two_supermassive_black_holes_orbiting_each_other#ev36", 
				"http://en.wikinews.org/wiki/Huygens_probe_lands_on_Saturn's_moon_Titan,_returns_pictures#ev43",
				"http://en.wikinews.org/wiki/Ariane_5_rocket_launches_first_Automated_Transfer_Vehicle#ev53",
				"http://en.wikinews.org/wiki/NASA_solar_sail_passes_first_major_test#ev35");
		ksAdapter.runKeyValueMentionFromEventQuery(eventURIs, keywords);
		Set<String> resourceURIs = Util.resourceURIsFromMentionURIs(ksAdapter.getAllRelationValues(Util.getRelationName("event", "mention", "NASA")));
		ksAdapter.runKeyValueResourceTextQuery(resourceURIs);
	}
	
	@After
	public void shoutDown() {
		ksAdapter.closeConnection();
	}
	
	@Test
	public void shouldReturnCorrectOrderNoUM() {
		UserModel userModel = new DummyUserModel();
		List<NewsEvent> expected = Lists.newArrayList(e1, e2, e3, e4);
		List<NewsEvent> result = ranker.rankEvents(input, keywords, userModel);
		assertTrue(expected.equals(result));
	}

}
