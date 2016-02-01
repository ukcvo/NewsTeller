package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	
	private String labelQueryName;
	
	public GenitiveFeature(String queryFileName, String labelQueryFileName) {
		super(queryFileName);
		this.labelQuery = Util.readStringFromFile(labelQueryFileName);
		this.labelQueryName = Util.queryNameFromFileName(labelQueryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		
		double result = 0;
		
		Set<String> entities = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_CONSTITUENT + this.sparqlQueryName, eventURI);
		for (String entity : entities) {
			Set<String> labels = ksAdapter.getBufferedValues(Util.RELATION_NAME_CONSTITUENT_LABEL + this.sparqlQueryName + this.labelQueryName, entity);
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

	@Override
	public void runBulkQueries(Set<String> eventURIs, List<Keyword> keywords) {
		ksAdapter.runKeyValueSparqlQuery(sparqlQuery, Util.RELATION_NAME_EVENT_CONSTITUENT + sparqlQueryName, Util.VARIABLE_EVENT, Util.VARIABLE_ENTITY, eventURIs);
		Set<String> entities = ksAdapter.getAllRelationValues(Util.RELATION_NAME_EVENT_CONSTITUENT + sparqlQueryName);
		ksAdapter.runKeyValueSparqlQuery(labelQuery, Util.RELATION_NAME_CONSTITUENT_LABEL + sparqlQueryName + labelQueryName, Util.VARIABLE_ENTITY, Util.VARIABLE_LABEL, entities);
	}

	@Override
	public Set<String> getRequiredMentionProperties() {
		return new HashSet<String>();
	}

}
