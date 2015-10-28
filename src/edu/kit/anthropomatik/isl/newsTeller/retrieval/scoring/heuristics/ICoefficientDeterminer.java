package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics;

/**
 * Interface for ways to retrieve some sort of coefficient/characteristic number to use for scoring purposes.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public interface ICoefficientDeterminer {

	/**
	 * Determine a coefficient based on the parameters.
	 */
	double getCoefficient(String eventURI, String keyword, String historicalEventURI);

}
