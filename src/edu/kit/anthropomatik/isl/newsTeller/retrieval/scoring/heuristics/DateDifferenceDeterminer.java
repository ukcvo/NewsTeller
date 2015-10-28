package edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics;

import java.util.Date;
import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.date.IDateProvider;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Calculates the "age" in days of a news event.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class DateDifferenceDeterminer implements ICoefficientDeterminer {

	private IDateProvider dateProvider;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	private String query; 
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}

	public DateDifferenceDeterminer(String queryFileName) {
		this.query = Util.readStringFromFile(queryFileName);
	}
	
	@SuppressWarnings("unused")
	public double getCoefficient(String eventURI, String keyword, String historicalEventURI) {
		
		List<String> dateStrings = ksAdapter.runSingleVariableStringQuery(query, "date");
		Date currentDate = dateProvider.getDate();
		
		//TODO: actual computation (Scope 2)
		
		return 0;
	}

}
