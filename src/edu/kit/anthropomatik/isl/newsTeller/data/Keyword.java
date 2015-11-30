package edu.kit.anthropomatik.isl.newsTeller.data;

/**
 * Represents a user keyword: word + weight
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class Keyword {

	private String word;
	
	private String stem;
	
	private String stemmedRegex;
	
	private double weight;

	public String getWord() {
		return word;
	}

	public double getWeight() {
		return weight;
	}
	
	public String getStemmedRegex() {
		return stemmedRegex;
	}

	public void setStemmedRegex(String stemmedRegex) {
		this.stemmedRegex = stemmedRegex;
	}

	public String getStem() {
		return stem;
	}

	public void setStem(String stem) {
		this.stem = stem;
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
		return String.format("[%s|%f]", word, weight);
	}
}
