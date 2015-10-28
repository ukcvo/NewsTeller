package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring;

import java.util.Calendar;
import java.util.Date;

/**
 * Uses the current date.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class CurrentDateProvider implements IDateProvider {

	private Calendar cal = Calendar.getInstance();
	
	public Date getDate() {
		return cal.getTime();
	}

}
