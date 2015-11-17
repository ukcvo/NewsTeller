package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

/**
 * Data class holding performance information about one file.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class PerformanceMeasure {

	private int[] tp;
	private int[] fp;
	private int[] fn;
	private double[] precision;
	private double[] recall;
	private double[] fscore;
	
	public int[] getTp() {
		return tp;
	}
	public int[] getFp() {
		return fp;
	}
	public int[] getFn() {
		return fn;
	}
	public double[] getPrecision() {
		return precision;
	}
	public double[] getRecall() {
		return recall;
	}
	public double[] getFscore() {
		return fscore;
	}
	
	public PerformanceMeasure(int[] tp, int[] fp, int[] fn, double[] precision, double[] recall, double[] fscore) {
		this.tp = tp;
		this.fp = fp;
		this.fn = fn;
		this.precision = precision;
		this.recall = recall;
		this.fscore = fscore;
	}
}
