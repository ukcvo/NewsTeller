package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Checks if all entities are actually mentioned somewhere in the text with one of their DBPedia labels.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class DBPediaLabelInFullTextFeature extends UsabilityFeature {

	private String labelQuery;
	
	public DBPediaLabelInFullTextFeature(String queryFileName, String labelQueryFileName) {
		super(queryFileName);
		this.labelQuery = Util.readStringFromFile(labelQueryFileName);
	}

	// get all the labels of all the entities
	private List<List<String>> getEntityLabels(String eventURI) {
		List<List<String>> result = new ArrayList<List<String>>();
		
		List<String> entities = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_ENTITY);
		
		for (String entity : entities) {
			List<String> labels = ksAdapter.runSingleVariableStringQuery(labelQuery.replace(Util.PLACEHOLDER_ENTITY, entity), Util.VARIABLE_LABEL);
			result.add(labels);
		}
		
		return result;
	}
	
	// check if the given label appears in one of the texts
	private double checkLabel(String label, Set<String> originalTexts) {
		for (String text : originalTexts) {
			if (text.toLowerCase().contains(label.toLowerCase())) //ignoring case
				return 1.0;
		}
		return 0.0;
	}
	
	// check all entities and figure out whether one of their labels appears or not
	private List<Double> checkLabels(List<List<String>> entityLabels, Set<String> originalTexts) {
		List<Double> result = new ArrayList<Double>();
		
		for (List<String> labels : entityLabels) {
			
			double appeared = 0;
			for (String label : labels) {
				appeared = checkLabel(label, originalTexts);
				if (appeared > 0)
					break;
			}
			result.add(appeared);
		}
		
		return result;
	}
	
	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		List<List<String>> entityLabels = getEntityLabels(eventURI);
		Set<String> originalTexts = ksAdapter.retrieveOriginalTexts(eventURI);
		List<Double> appearances = checkLabels(entityLabels, originalTexts);
		double averageAppearance = Util.averageFromCollection(appearances);
		
		return averageAppearance;
	}

}
