package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWordSet;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.dictionary.Dictionary;

public class WordNetVerbCountFeature extends UsabilityFeature {

	private static Log log = LogFactory.getLog(WordNetVerbCountFeature.class);
	
	private Dictionary dict = null;
		
	public WordNetVerbCountFeature(String queryFileName, String probabilityFileName) {
		super(queryFileName, probabilityFileName);
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
		
		if (label.length() > 20) {
			if (log.isWarnEnabled())
				log.warn(String.format("label with more than 20 characters; ignoring and returning 0: '%s'", label));
			return 0;
		}
		
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
	
	private double getRelativeCount(String eventURI) {

		List<String> labels = this.ksAdapter.runSingleVariableStringQuery(
				sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_LABEL);
		
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
	
	@Override
	public int getValue(String eventURI) {
		double relativeCount = getRelativeCount(eventURI);
		
		/*
		if (relativeCount < Util.EPSILON)
			return 0;	// zero
		else if (relativeCount < 0.5)
			return 1;	// not zero, but less than 0.5
		else if ((1.0 - relativeCount) < Util.EPSILON)
			return 2;	// more than 0.5 but not one
		else
			return 3;	// one
		*/
		if (relativeCount < Util.EPSILON)
			return 10;
		else if ((1.0 - relativeCount) < Util.EPSILON)
			return 11;
		else
			return (int) (relativeCount * 10);
	}

}
