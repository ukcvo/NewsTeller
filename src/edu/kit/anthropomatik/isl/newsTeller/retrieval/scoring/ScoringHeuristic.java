package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring;

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

	protected ScoringFormula formula;
	
	protected INumberGetter numberGetter;
	
	public void setFormula(ScoringFormula formula) {
		this.formula = formula;
	}

	public void setNumberGetter(INumberGetter numberGetter) {
		this.numberGetter = numberGetter;
	}

	protected double getScore(NewsEvent event, Keyword keyword, ConversationCycle historicalCycle) {
		
		double number = numberGetter.getNumber(event, keyword, historicalCycle);
		double score = formula.apply(number);
		
		return score;
	}
}
