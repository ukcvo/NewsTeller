package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring;

import edu.kit.anthropomatik.isl.newsTeller.data.ConversationCycle;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Calculates the "age" in days of a news event.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class DateDifferenceGetter implements INumberGetter {

	private IDateProvider dateProvider;
	
	private String query; 
	
	public double getNumber(NewsEvent event, Keyword keyword, ConversationCycle historicalCycle) {
		// TODO Auto-generated method stub
		return 0;
	}

}
