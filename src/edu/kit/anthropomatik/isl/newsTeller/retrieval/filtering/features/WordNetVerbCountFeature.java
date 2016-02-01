package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWordSet;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.dictionary.Dictionary;

/**
 * Special feature counting relative number of WordNet verb senses.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class WordNetVerbCountFeature extends UsabilityFeature {

	private static Log log = LogFactory.getLog(WordNetVerbCountFeature.class);

	private Dictionary dict = null;

	public WordNetVerbCountFeature(String queryFileName) {
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

	/**
	 * Looks up the given label in WordNet and counts the percentage of verb synsets for this label.
	 */
	public double getLabelVerbFrequency(String label) {

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

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		Set<String> labels = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_LABEL + sparqlQueryName, eventURI);

		if (labels.isEmpty()) {
			if (log.isWarnEnabled())
				log.warn("empty label set, returning 0");
			return 0;
		}

		double sum = 0;
		for (String label : labels) {
			sum += getLabelVerbFrequency(label);
		}

		return sum / labels.size();
	}

	@Override
	public void runBulkQueries(Set<String> eventURIs, List<Keyword> keywords) {
		ksAdapter.runKeyValueSparqlQuery(sparqlQuery, Util.RELATION_NAME_EVENT_LABEL + sparqlQueryName, Util.VARIABLE_EVENT, Util.VARIABLE_LABEL, 
				eventURIs);
	}

	@Override
	public Set<String> getRequiredMentionProperties() {
		return new HashSet<String>();
	}

}
