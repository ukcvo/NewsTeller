package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics;

/**
 * Interface for ways to retrieve some sort of measurment/characteristic number to use for scoring purposes.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public interface INumberGetter {

	//TODO: better name! (Scope 0)
	double getNumber(String eventURI, String keyword, String historicalEventURI);

}
