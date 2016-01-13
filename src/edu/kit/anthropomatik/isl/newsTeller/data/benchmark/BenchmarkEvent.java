package edu.kit.anthropomatik.isl.newsTeller.data.benchmark;

/**
 * Represents an event within the benchmark (consisting of fileName and eventURI).
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class BenchmarkEvent {

	private String fileName;
	
	private String eventURI;

	public String getFileName() {
		return fileName;
	}

	public String getEventURI() {
		return eventURI;
	}
	
	public BenchmarkEvent(String fileName, String eventURI) {
		this.fileName = fileName;
		this.eventURI = eventURI;
	}
	
	@Override
	public String toString() {
		return String.format("[%s|%s]", this.fileName, this.eventURI);
	}
	
	@Override
	public boolean equals(Object o) {
		return ((o != null) && o.toString().equals(this.toString()));
	}
	
	@Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
