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

	private String entityType;
	
	private String directLabelType;
	
	private String indirectLabelType;
	
	private boolean splitLabels;
	
	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public void setDirectLabelType(String directLabelType) {
		this.directLabelType = directLabelType;
	}

	public void setIndirectLabelType(String indirectLabelType) {
		this.indirectLabelType = indirectLabelType;
	}

	public void setSplitLabels(boolean splitLabels) {
		this.splitLabels = splitLabels;
	}
	
	public DBPediaLabelInFullTextFeature() {
		super();
	}
	
	@Override
	protected List<List<List<String>>> getLabels(String eventURI, List<Keyword> keywords) {
		List<List<List<String>>> result = new ArrayList<List<List<String>>>();
		
		for (Keyword keyword : keywords) {
			Set<String> entities = 
					ksAdapter.getBufferedValues(Util.getRelationName("event", this.entityType, keyword.getWord()), eventURI);
			
			for (String entity : entities) {
				Set<String> labels = ksAdapter.getBufferedValues(Util.getRelationName("entity", directLabelType, keyword.getWord()), entity);
						
				if (labels.isEmpty() && !indirectLabelType.isEmpty())	// no direct DBpedia labels --> look at parent concepts
					labels = ksAdapter.getBufferedValues(Util.getRelationName("entity", indirectLabelType, keyword.getWord()), entity);
					
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
}
