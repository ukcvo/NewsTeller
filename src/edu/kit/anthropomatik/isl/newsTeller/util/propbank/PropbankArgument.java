package edu.kit.anthropomatik.isl.newsTeller.util.propbank;

import java.io.Serializable;

/**
 * Represents a propbank argument, consisting of n (the argument number) and f (the argument function).
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class PropbankArgument implements Serializable {

	private static final long serialVersionUID = 3744737457216302308L;
	
	private String n;
	private String f;
	
	public String getN() {
		return this.n;
	}
	
	public String getF() {
		return this.f;
	}
	
	public PropbankArgument(String n, String f) {
		this.n = n;
		this.f = f;
	}
	
	@Override
	public String toString() {
		return String.format("[%s,%s]", n, f);
	}
	
}
