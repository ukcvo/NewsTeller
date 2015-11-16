package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

/**
 * Represents a bin in which a value can fall. Including left border and excluding right border: [left,right)
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class ValueBin {

	private double leftBorder;
	
	private double rightBorder;
	
	private int label;
	
	public int getLabel() {
		return this.label;
	}
	
	public ValueBin(double leftBorder, double rightBorder, int label) {
		this.leftBorder = leftBorder;
		this.rightBorder = rightBorder;
		this.label = label;
	}
	
	public boolean contains(double value) {
		return ((leftBorder <= value) && (rightBorder > value));
	}
}
