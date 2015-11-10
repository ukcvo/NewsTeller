package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWordSet;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.dictionary.Dictionary;

/**
 * Uses WordNet to return the relative number of synsets for the event label that are tagged as verbs.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class WordNetVerbCountDeterminer extends CoefficientDeterminer {
	
	private static Log log = LogFactory.getLog(WordNetVerbCountDeterminer.class);
	
	private Dictionary dict = null;
	
	public WordNetVerbCountDeterminer(String queryFileName) {
		super(queryFileName);
		try {
			this.dict = Dictionary.getDefaultResourceInstance();
		} catch (JWNLException e) {
			if (log.isErrorEnabled())
				log.error("failed to open WordNet dictionary!");
			if (log.isDebugEnabled())
				log.debug("failed to open WordNet dictionary", e);
		}
	}

	private double getLabelVerbFrequency(String label) {
		try {
			IndexWordSet indexWords = dict.lookupAllIndexWords(label);
			int numberOfVerbSenses = indexWords.getSenseCount(POS.VERB);
			int totalNumberOfSenses = numberOfVerbSenses;
			totalNumberOfSenses += indexWords.getSenseCount(POS.ADJECTIVE);
			totalNumberOfSenses += indexWords.getSenseCount(POS.ADVERB);
			totalNumberOfSenses += indexWords.getSenseCount(POS.NOUN);
			
			double relativeNumberOfVerbSenses = 0;
			if (totalNumberOfSenses > 0)
				relativeNumberOfVerbSenses = (1.0 * numberOfVerbSenses) / totalNumberOfSenses;
							
			return relativeNumberOfVerbSenses;
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(String.format("cannot access WordNet, returning score 0: '%s'", label));
			if (log.isDebugEnabled())
				log.debug("exception accessing WordNet", e);
			return 0;
		}		
	}
	
	public double getCoefficient(String eventURI, String keyword, String historicalEventURI) {

		List<String> labels = executeQuery(eventURI, keyword, historicalEventURI, Util.VARIABLE_LABEL);
		
		if (labels.isEmpty()) {
			if (log.isWarnEnabled())
				log.warn("empty label set, returning 0");
			return 0;
		}
		
		double sum = 0;
		for (String label : labels) {
			sum += getLabelVerbFrequency(label);
		}
		
		return sum/labels.size();
	}

}
