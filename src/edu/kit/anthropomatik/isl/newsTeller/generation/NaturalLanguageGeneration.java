package edu.kit.anthropomatik.isl.newsTeller.generation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import simplenlg.features.Feature;
import simplenlg.features.Tense;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.phrasespec.VPPhraseSpec;
import simplenlg.realiser.english.Realiser;

/**
 * Summary creation based on NLG.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class NaturalLanguageGeneration extends SummaryCreator {

	private static final Log log = LogFactory.getLog(NaturalLanguageGeneration.class);
	
	private static final String A0 = "A0";
	private static final String A1 = "A1";
	private static final String A2 = "A2";
	private static final String LABEL = "eventLabel";
	private static final String PLACE = "place";
	private static final String TIME = "time";
	
	private Lexicon lexicon;
	private NLGFactory nlgFactory;
	private Realiser realiser;
	
	// contains the SPARQL queries
	private Map<String,String> sparqlQueries;
	
	public NaturalLanguageGeneration(String configFileName) {
		this.lexicon = Lexicon.getDefaultLexicon();
		this.nlgFactory = new NLGFactory(this.lexicon);
		this.realiser = new Realiser(this.lexicon);
		
		this.sparqlQueries = Util.readNLGQueries(configFileName);
	}
	
	// get all the constituents attached to the event
	private Map<String,List<String>> analyzeEvent(NewsEvent event) {
		
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		
		for (Map.Entry<String, String> entry : this.sparqlQueries.entrySet()) {
			String constituentName = entry.getKey();
			String sparqlQuery = entry.getValue();
			
			List<String> results = this.ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, event.getEventURI()), 
					Util.VARIABLE_LABEL);
			
			result.put(constituentName, results);
		}
		
		return result;
	}
	
	@Override
	public String createSummary(NewsEvent event) {
		
//		String a1 = "Barack Obama";
//		String verb = "elect";
//		String a2 = "president";
//		
//		SPhraseSpec sentence = nlgFactory.createClause();
//		NPPhraseSpec object = nlgFactory.createNounPhrase(a1);
//		NPPhraseSpec indirectObject = nlgFactory.createNounPhrase(a2);
//		VPPhraseSpec vp = nlgFactory.createVerbPhrase(verb);
//		sentence.setObject(object);
//		sentence.setVerb(vp);
//		sentence.setIndirectObject(indirectObject);
//		sentence.setFeature(Feature.PASSIVE, true);
//		sentence.setFeature(Feature.TENSE, Tense.PAST);
//		
//		String result = realiser.realiseSentence(sentence);
//		System.out.println(result);
		
		Map<String, List<String>> constituents = analyzeEvent(event);
		
		SPhraseSpec sentence = nlgFactory.createClause();
		NPPhraseSpec object = nlgFactory.createNounPhrase();
		NPPhraseSpec subject = nlgFactory.createNounPhrase();
		VPPhraseSpec verb = nlgFactory.createVerbPhrase();
		sentence.setSubject(subject);
		sentence.setObject(object);
		sentence.setVerb(verb);
		
		subject.setNoun(constituents.get(A0).isEmpty() ? null : constituents.get(A0).get(0));
		object.setNoun(constituents.get(A1).isEmpty() ? null : constituents.get(A1).get(0));
		verb.setVerb(constituents.get(LABEL).isEmpty() ? null : constituents.get(LABEL));
		
		String result = "";
		
		try {
			realiser.realiseSentence(sentence);
		} catch (Exception e) {
			if (log.isWarnEnabled())
				log.warn("SimpleNLG failed, returning empty String");
			if (log.isDebugEnabled())
				log.debug("SimpleNLG exception: ", e);
		}
		
		return result;
	}

}
