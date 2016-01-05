package edu.kit.anthropomatik.isl.newsTeller.generation;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
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

	private Lexicon lexicon;
	private NLGFactory nlgFactory;
	private Realiser realiser;
	
	public NaturalLanguageGeneration() {
		this.lexicon = Lexicon.getDefaultLexicon();
		this.nlgFactory = new NLGFactory(this.lexicon);
		this.realiser = new Realiser(this.lexicon);
	}
	
	@Override
	public String createSummary(NewsEvent event) {
		
		String a1 = "Barack Obama";
		String verb = "elect";
		String a2 = "president";
		
		SPhraseSpec sentence = nlgFactory.createClause();
		NPPhraseSpec object = nlgFactory.createNounPhrase(a1);
		NPPhraseSpec indirectObject = nlgFactory.createNounPhrase(a2);
		VPPhraseSpec vp = nlgFactory.createVerbPhrase(verb);
		sentence.setObject(object);
		sentence.setVerb(vp);
		sentence.setIndirectObject(indirectObject);
		sentence.setFeature(Feature.PASSIVE, true);
		sentence.setFeature(Feature.TENSE, Tense.PAST);
		
		String result = realiser.realiseSentence(sentence);
		System.out.println(result);
		
		return "";
	}

}
