package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import edu.kit.anthropomatik.isl.newsTeller.util.propbank.PropbankFrames;

/**
 * Checks whether the propbank roleset referenced in the mention is actually existing.
 */
public class PropbankExistenceFeature extends UsabilityFeature {

	private String propertyURI;
	
	private PropbankFrames propbank;
	
	public PropbankExistenceFeature(String queryFileName, String propertyURI, String propbankFolderName) {
		super(queryFileName);

		this.propertyURI = propertyURI;
		this.propbank = PropbankFrames.getInstance(propbankFolderName, false);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {

		double result = 0;
		
		Set<String> mentionURIs = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_MENTION + sparqlQueryName, eventURI);
		
		for (String mentionURI : mentionURIs) {
			
			double mentionResult = 0;
			
			List<String> rolesetIds = ksAdapter.getMentionProperty(mentionURI, propertyURI);
			String pos = ksAdapter.getFirstBufferedValue(Util.RELATION_NAME_MENTION_PROPERTY + sparqlQueryName + Util.MENTION_PROPERTY_POS, mentionURI);
			String pred = ksAdapter.getFirstBufferedValue(Util.RELATION_NAME_MENTION_PROPERTY + sparqlQueryName + Util.MENTION_PROPERTY_PRED, mentionURI);
			
			String suffix;
			switch (pos) {
			case Util.MENTION_PROPERTY_POS_NOUN:
				suffix = PropbankFrames.SUFFIX_NOUN;
				break;
			case Util.MENTION_PROPERTY_POS_VERB:
				suffix = PropbankFrames.SUFFIX_VERB;
				break;
			default:
				suffix = "";	// should never happen, though...
				break;
			}
			
			for (String roleset : rolesetIds) {
				String id = roleset.substring(roleset.lastIndexOf("/") + 1);
				if (propbank.containsRoleset(pred, suffix, id))
					mentionResult++;
			}
			result += mentionResult / rolesetIds.size();
		}
		
		result = result / mentionURIs.size();
		
		return result;
	}

	@Override
	public void runBulkQueries(Set<String> eventURIs, List<Keyword> keywords) {
		ksAdapter.runKeyValueSparqlQuery(sparqlQuery, Util.RELATION_NAME_EVENT_MENTION + sparqlQueryName, Util.VARIABLE_EVENT, 
				Util.VARIABLE_MENTION, eventURIs);
		Set<String> mentionURIs = ksAdapter.getAllRelationValues(Util.RELATION_NAME_EVENT_MENTION + sparqlQueryName);
		Set<String> propertyURIs = new HashSet<String>();
		propertyURIs.add(Util.MENTION_PROPERTY_POS);
		propertyURIs.add(Util.MENTION_PROPERTY_PRED);
		ksAdapter.runKeyValueMentionPropertyQuery(propertyURIs, Util.RELATION_NAME_MENTION_PROPERTY + sparqlQueryName, mentionURIs);
	}

}
