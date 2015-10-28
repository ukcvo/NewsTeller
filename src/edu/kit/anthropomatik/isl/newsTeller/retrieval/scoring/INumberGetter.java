package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring;

import edu.kit.anthropomatik.isl.newsTeller.data.ConversationCycle;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Interface for ways to retrieve some sort of measurment/number to use for scoring purposes.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public interface INumberGetter {

	double getNumber(NewsEvent event, Keyword keyword, ConversationCycle historicalCycle);

}
