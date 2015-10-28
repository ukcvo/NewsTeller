package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics;

import java.math.BigDecimal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.udojava.evalex.Expression;

/**
 * Represents a formula used for transforming the retrieved number into a score between 0 and 1.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class ScoringFormula {

	private static Log log = LogFactory.getLog(ScoringFormula.class);
	
	private String formula;

	public ScoringFormula(String formula) {
		this.formula = formula;
	}
	
	public double apply(double x) {
		BigDecimal argument = BigDecimal.valueOf(x);
		Expression expression = new Expression(formula).with("x", argument);
		double result = expression.eval().doubleValue();

		if ((result < 0) || (result > 1)) {
			if (log.isErrorEnabled())
				log.error(String.format("invalid result value, forcing to zero: %f", result));
			result = 0;
		}
		
		return result;
	}
}
