package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring;

import edu.kit.anthropomatik.isl.newsTeller.data.ConversationCycle;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics.INumberGetter;

/**
 * Gets a number by executing a SPARQL query.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SPARQLNumberGetter implements INumberGetter {

	private KnowledgeStoreAdapter ksAdapter;
	
	private String query; // the SPARQL-query
	
	private boolean queryContainsEvent;
	private boolean queryContainsKeyword;
	private boolean queryContainsHistoricalCycle;
	
	public double getNumber(NewsEvent event, Keyword keyword, ConversationCycle historicalCycle) {
		// TODO implement
		return 0;
	}

}
