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
	private int[] tn;
	private int[] fn;
	private double[] precision;
	private double[] recall;
	private double[] fscore;
	private double[] balancedAccuracy;
	
	public int getTp(int i) {
		return tp[i];
	}
	public int getFp(int i) {
		return fp[i];
	}
	public int getTn(int i) {
		return tn[i];
	}
	public int getFn(int i) {
		return fn[i];
	}
	public double getPrecision(int i) {
		return precision[i];
	}
	public double getRecall(int i) {
		return recall[i];
	}
	public double getFscore(int i) {
		return fscore[i];
	}
	public double getBalacedAccuracy(int i) {
		return balancedAccuracy[i];
	}
	
	public PerformanceMeasure(int[] tp, int[] fp, int[] tn, int[] fn, double[] precision, double[] recall, double[] fscore, double[] balancedAccuracy) {
		this.tp = tp;
		this.fp = fp;
		this.tn = tn;
		this.fn = fn;
		this.precision = precision;
		this.recall = recall;
		this.fscore = fscore;
		this.balancedAccuracy = balancedAccuracy;
	}
}
