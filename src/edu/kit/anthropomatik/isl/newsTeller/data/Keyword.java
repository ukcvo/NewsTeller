package edu.kit.anthropomatik.isl.newsTeller.data;

/**
 * Represents a user keyword: word + weight
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class Keyword {

	private String word;
	
	private double weight;

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	public Keyword() {
		this.word = "";
		this.weight = 1.0;
	}
	
	public Keyword(String word) {
		this.word = word;
		this.weight = 1.0;
	}
	
	public Keyword(String word, double weight) {
		this.word = word;
		this.weight = weight;
	}
	
	@Override
	public String toString() {
		return String.format("<%s|%f>", word, weight);
	}
}
