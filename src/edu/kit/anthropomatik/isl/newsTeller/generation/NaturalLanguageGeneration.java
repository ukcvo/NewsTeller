package edu.kit.anthropomatik.isl.newsTeller.generation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features.WordNetVerbCountFeature;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import simplenlg.framework.CoordinatedPhraseElement;
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
	
	// SimpleNLG stuff, will be created in constructor
	private Lexicon lexicon;
	private NLGFactory nlgFactory;
	private Realiser realiser;
	
	private WordNetVerbCountFeature wordNetFeature;
	
	public void setWordNetFeature(WordNetVerbCountFeature wordNetFeature) {
		this.wordNetFeature = wordNetFeature;
	}
	
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
	
	// select the event label with the highest fraction of verb senses according to WordNet.
	private String selectVerb(List<String> eventLabels) {
		
		String result = null;
		double verbProbability = 0;
		
		for (String label : eventLabels) {
			double labelProbability = this.wordNetFeature.getLabelVerbFrequency(label);
			if (labelProbability > verbProbability) {
				result = label;
				verbProbability = labelProbability;
			}
		}
		
		return result;
	}
	
	@Override
	public String createSummary(NewsEvent event, List<Keyword> keywords) {
		
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
		CoordinatedPhraseElement subject = nlgFactory.createCoordinatedPhrase();
		VPPhraseSpec verb = nlgFactory.createVerbPhrase();
		CoordinatedPhraseElement object = nlgFactory.createCoordinatedPhrase();
		sentence.setSubject(subject);
		sentence.setObject(object);
		sentence.setVerb(verb);
		
		String verbString = selectVerb(constituents.get(LABEL));
		if (verbString != null) {
			// there is a verb --> construct a normal sentence
			
			for (String subjectString : constituents.get(A0)) {
				NPPhraseSpec subjectNP = nlgFactory.createNounPhrase(subjectString);
				subject.addCoordinate(subjectNP);
			}
			
			verb.setVerb(verbString);
			
			for (String objectString : constituents.get(A1)) {
				NPPhraseSpec objectNP = nlgFactory.createNounPhrase(objectString);
				subject.addCoordinate(objectNP);
			}
			
		} else {
			// there is nothing that can be interpreted as verb --> construct "there is" sentence.
			
			NPPhraseSpec subjectNP = nlgFactory.createNounPhrase("there");
			subject.addCoordinate(subjectNP);
			
			verb.setVerb("be");
			
			NPPhraseSpec objectNP = nlgFactory.createNounPhrase();
			objectNP.setDeterminer("a");
			objectNP.setNoun(constituents.get(LABEL).isEmpty() ? "nothing" : constituents.get(LABEL).get(0)); 
			object.addCoordinate(objectNP);
			
		}
		
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
