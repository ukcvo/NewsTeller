package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.List;

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
		
		List<String> mentionURIs = new ArrayList<String>();
		mentionURIs.addAll(ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_MENTION, eventURI));
		if (mentionURIs.isEmpty())
			mentionURIs = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), 
				Util.VARIABLE_MENTION, false);
		
		for (String mentionURI : mentionURIs) {
			
			double mentionResult = 0;
			
			List<String> rolesetIds = ksAdapter.getMentionProperty(mentionURI, propertyURI);
			String pos = ksAdapter.getUniqueMentionProperty(mentionURI, Util.MENTION_PROPERTY_POS);
			String pred = ksAdapter.getUniqueMentionProperty(mentionURI, Util.MENTION_PROPERTY_PRED);
			
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

}
