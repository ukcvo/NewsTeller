package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.date;

import java.util.Date;

/**
 * Providing the reference date for event age calculation.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public interface IDateProvider {

	public Date getDate();
}
