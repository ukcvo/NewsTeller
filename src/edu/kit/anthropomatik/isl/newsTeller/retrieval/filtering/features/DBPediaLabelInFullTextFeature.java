package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
	
	private String directLabelQueryName;
	
	private String inheritedLabelQuery;
	
	private String inheritedLabelQueryName;
	
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
			Set<String> entities = 
					ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_CONSTITUENT + this.sparqlQueryName + keyword.getWord(), eventURI);
			
			for (String entity : entities) {
				Set<String> labels = ksAdapter.getBufferedValues(Util.RELATION_NAME_CONSTITUENT_LABEL + this.sparqlQueryName 
						+ this.directLabelQueryName + keyword.getWord(), entity);
						
				if (labels.isEmpty() && !inheritedLabelQuery.isEmpty())	// no direct DBpedia labels --> look at parent concepts
					labels = ksAdapter.getBufferedValues(Util.RELATION_NAME_CONSTITUENT_LABEL + this.sparqlQueryName + this.inheritedLabelQueryName 
							+ keyword.getWord(), entity);
					
				// if there is no dbpedia label, then labels will be empty!
				
				List<List<String>> list = new ArrayList<List<String>>();
				for (String label : labels) {
					
					if (this.splitLabels) {
						String[] labelParts = label.split(Util.SPLIT_REGEX);
						if (!this.doUseContainsInsteadOfRegex) {
							for (int i = 0; i < labelParts.length; i++)
								labelParts[i] = Util.escapeText(labelParts[i]);
						}
						list.add(Arrays.asList(labelParts));
					}
					else {
						List<String> dummyList = new ArrayList<String>();
						String escapedLabel = this.doUseContainsInsteadOfRegex ? label : Util.escapeText(label);
						dummyList.add(escapedLabel);
						list.add(dummyList);
					}
				}
				
				result.add(list);
			}
		}
		
		return result;
	}

	@Override
	public void runBulkQueries(Set<String> eventURIs, List<Keyword> keywords) {
		for (Keyword keyword : keywords) {
			ksAdapter.runKeyValueQuery(sparqlQuery.replace(Util.PLACEHOLDER_KEYWORD, keyword.getStemmedRegex()), 
					Util.RELATION_NAME_EVENT_CONSTITUENT + this.sparqlQueryName + keyword.getWord(), Util.VARIABLE_EVENT, Util.VARIABLE_ENTITY, eventURIs);
			Set<String> entities = ksAdapter.getAllRelationValues(Util.RELATION_NAME_EVENT_CONSTITUENT + this.sparqlQueryName + keyword.getWord());
			ksAdapter.runKeyValueQuery(directLabelQuery.replace(Util.PLACEHOLDER_KEYWORD, keyword.getStemmedRegex()), 
					Util.RELATION_NAME_CONSTITUENT_LABEL + this.sparqlQueryName + this.directLabelQueryName + keyword.getWord(), Util.VARIABLE_ENTITY, Util.VARIABLE_LABEL, entities);
			ksAdapter.runKeyValueQuery(inheritedLabelQuery.replace(Util.PLACEHOLDER_KEYWORD, keyword.getStemmedRegex()), 
					Util.RELATION_NAME_CONSTITUENT_LABEL + this.sparqlQueryName + this.inheritedLabelQueryName + keyword.getWord(), Util.VARIABLE_ENTITY, Util.VARIABLE_LABEL, entities);
		}
	}

}
