package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics;

import edu.kit.anthropomatik.isl.newsTeller.data.ConversationCycle;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Represents a single heuristic used for scoring.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class ScoringHeuristic {

	private ScoringFormula formula;
	
	private CoefficientDeterminer numberGetter;
	
	private String name;
	
	public void setFormula(ScoringFormula formula) {
		this.formula = formula;
	}

	public void setNumberGetter(CoefficientDeterminer numberGetter) {
		this.numberGetter = numberGetter;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	// get the score by finding a relevant number and applying a formula
	protected double getScore(NewsEvent event, Keyword keyword, ConversationCycle historicalCycle) {
		
		String eventURI = (event != null) ? event.getEventURI() : null;
		String keywordText = (keyword != null) ? keyword.getWord() : null;
		String oldEventURI = (historicalCycle != null) ? historicalCycle.getEventURI() : null;
		
		double number = numberGetter.getCoefficient(eventURI, keywordText, oldEventURI);
		double score = formula.apply(number);
		
		return score;
	}
}
