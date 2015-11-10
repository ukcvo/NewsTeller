package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics;

import java.util.Date;
import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.date.IDateProvider;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Calculates the "age" in days of a news event.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class DateDifferenceDeterminer extends CoefficientDeterminer {

	private IDateProvider dateProvider;
	
	public DateDifferenceDeterminer(String queryFileName) {
		super(queryFileName);
	}
	
	@SuppressWarnings("unused")
	public double getCoefficient(String eventURI, String keyword, String historicalEventURI) {
		
		List<String> dateStrings = executeQuery(eventURI, keyword, historicalEventURI, Util.VARIABLE_LABEL);
		Date currentDate = dateProvider.getDate();
		
		//TODO: actual computation (Scope 2)
		
		return 0;
	}

}
