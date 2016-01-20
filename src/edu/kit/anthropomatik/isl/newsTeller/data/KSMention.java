package edu.kit.anthropomatik.isl.newsTeller.data;

/**
 * Represents a KnowledgeStore mention, to be used for intersection checking.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class KSMention {

	private String resourceURI;
	
	private int startIdx; // idx of first letter contained in the mention
	
	private int endIdx; // idx of first letter AFTER end of the mention
	
	public String getResourceURI() {
		return resourceURI;
	}

	public int getStartIdx() {
		return startIdx;
	}

	public int getEndIdx() {
		return endIdx;
	}

	public KSMention(String mentionURI) {
		this.resourceURI = mentionURI.substring(0, mentionURI.indexOf("#"));
		this.startIdx = Integer.parseInt(mentionURI.substring(mentionURI.indexOf("=")+1, mentionURI.indexOf(",", mentionURI.indexOf("="))));
		this.endIdx = Integer.parseInt(mentionURI.substring(mentionURI.indexOf(",", mentionURI.indexOf("="))+1));
	}
	
	public KSMention(String resourceURI, int startIdx, int endIdx) {
		this.resourceURI = resourceURI;
		this.startIdx = startIdx;
		this.endIdx = endIdx;
	}
	
	private boolean isIdxInRange(int idx) {
		return ((idx >= this.startIdx) && (idx <= this.endIdx));
	}
	
	private double getLength() {
		return  (this.endIdx - this.startIdx);
	}
	
	/**
	 * Decides whether this mention completely contains the other one.
	 */
	public boolean contains(KSMention other) {
		return (this.resourceURI.equals(other.resourceURI) && this.isIdxInRange(other.startIdx) && this.isIdxInRange(other.endIdx));
	}
	
	/**
	 * Computes whether the two mentions overlap and returns the fraction of the smaller mention that is overlapping with the larger mention.
	 */
	public double overlap(KSMention other) {
		double result = 0;
		
		if (this.resourceURI.equals(other.resourceURI)) {
			// can only overlap if from same source text
			
			if (this.contains(other)) {
				// other is completely contained in this
				result = 1;
			} else if (other.contains(this)) {
				// this is completely contained in other
				result = 1;
			} else if (this.isIdxInRange(other.startIdx)) {
				// other starts in this, but continues after this ends
				result = (this.endIdx - other.startIdx) / Math.min(other.getLength(), this.getLength());
			} else if (this.isIdxInRange(other.endIdx)) {
				// other ends in this, but starts before this
				result = (other.endIdx - this.startIdx) / Math.min(other.getLength(), this.getLength());
			}
			// otherwise: no overlap
		}
		
		return result;
	}
	
	@Override
	public String toString() {
		return String.format("[%s,%d,%d]", this.resourceURI, this.startIdx, this.endIdx);
	}
	
}
