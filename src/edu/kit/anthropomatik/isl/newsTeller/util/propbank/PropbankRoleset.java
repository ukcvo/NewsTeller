package edu.kit.anthropomatik.isl.newsTeller.util.propbank;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a roleset from propbank, consisting of a name and different sets of arguments.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class PropbankRoleset implements Serializable {

	private static final long serialVersionUID = 3364789599476843897L;

	private String name;
	
	private Set<Set<PropbankArgument>> argumentSets;
	
	public String getName() {
		return name;
	}
	
	public Set<Set<PropbankArgument>> getArgumentSets() {
		return argumentSets;
	}

	public void addArgumentSet(Set<PropbankArgument> arguments) {
		this.argumentSets.add(arguments);
	}
	
	public PropbankRoleset(String name) {
		this.name = name;
		this.argumentSets = new HashSet<Set<PropbankArgument>>();
	}

}
