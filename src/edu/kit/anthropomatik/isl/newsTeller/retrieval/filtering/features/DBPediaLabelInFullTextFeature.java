package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Checks if entities are actually mentioned somewhere in the text with one of their DBPedia labels.
 * Either look at all entities or only at the ones matching the keyword (depending on sparql query).
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class DBPediaLabelInFullTextFeature extends FullTextFeature {

	private String directLabelQuery;
	
	private String inheritedLabelQuery;
	
	private boolean splitLabels;
	
	public void setSplitLabels(boolean splitLabels) {
		this.splitLabels = splitLabels;
	}
	
	public DBPediaLabelInFullTextFeature(String queryFileName, String directLabelQueryFileName, String inheritedLabelQueryFileName) {
		super(queryFileName);
		this.directLabelQuery = Util.readStringFromFile(directLabelQueryFileName);
		this.inheritedLabelQuery = Util.readStringFromFile(inheritedLabelQueryFileName);
	}
	
	@Override
	protected List<List<List<String>>> getLabels(String eventURI, List<Keyword> keywords) {
		List<List<List<String>>> result = new ArrayList<List<List<String>>>();
		
		for (Keyword keyword : keywords) {
			List<String> entities = ksAdapter.runSingleVariableStringQuery(
					sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI).replace(Util.PLACEHOLDER_KEYWORD, keyword.getStemmedRegex()),
					Util.VARIABLE_ENTITY);
			
			for (String entity : entities) {
				List<String> labels = ksAdapter.runSingleVariableStringQuery(
						directLabelQuery.replace(Util.PLACEHOLDER_ENTITY, entity).replace(Util.PLACEHOLDER_KEYWORD, keyword.getStemmedRegex()), 
						Util.VARIABLE_LABEL);
				if (labels.isEmpty() && !inheritedLabelQuery.isEmpty())	// no direct DBpedia labels --> look at parent concepts
					labels = ksAdapter.runSingleVariableStringQuery(
							inheritedLabelQuery.replace(Util.PLACEHOLDER_ENTITY, entity).replace(Util.PLACEHOLDER_KEYWORD, keyword.getStemmedRegex()),
							Util.VARIABLE_LABEL); 
				// if there is no dbpedia label, then labels will be empty!
				
				List<List<String>> list = new ArrayList<List<String>>();
				for (String label : labels) {
					if (this.splitLabels) {
						list.add(Arrays.asList(label.split(Util.SPLIT_REGEX)));
					}
					else {
						List<String> dummyList = new ArrayList<String>();
						dummyList.add(label);
						list.add(dummyList);
					}
				}
				
				result.add(list);
			}
		}
		
		return result;
	}

}
