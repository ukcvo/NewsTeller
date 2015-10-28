package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics;

/**
 * Represents a formula used for transforming the retrieved number into a score between 0 and 1.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class ScoringFormula {

	private String formula;

	public ScoringFormula(String formula) {
		this.formula = formula;
	}
	
	public double apply(double x) {
		//TODO: implement
		return 0;
	}
}
