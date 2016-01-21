package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.List;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Checks if any of the constituents is a genitive.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class GenitiveFeature extends UsabilityFeature {

	private String labelQuery;
	
	public GenitiveFeature(String queryFileName, String labelQueryFileName) {
		super(queryFileName);
		this.labelQuery = Util.readStringFromFile(labelQueryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		
		double result = 0;
		
		List<String> entities = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_ENTITY);
		for (String entity : entities) {
			List<String> labels = ksAdapter.runSingleVariableStringQuery(labelQuery.replace(Util.PLACEHOLDER_ENTITY, entity), Util.VARIABLE_LABEL);
			for (String label : labels) {
				if (label.endsWith("'s") || label.endsWith("s'")) {
					result++;
					break;
				}
			}
		}
		result /= entities.size();
		
		return result;
	}

}
