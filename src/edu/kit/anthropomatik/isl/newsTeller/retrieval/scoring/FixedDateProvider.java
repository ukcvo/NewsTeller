package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring;

import java.util.Date;

/**
 * Return a fixed date, used for testing purposes.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class FixedDateProvider implements IDateProvider {

	private Date date;
	
	public FixedDateProvider(Date date) {
		this.date = date;
	}
	
	public Date getDate() {
		return date;
	}

}
