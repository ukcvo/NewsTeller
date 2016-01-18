package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import edu.kit.anthropomatik.isl.newsTeller.util.propbank.PropbankArgument;
import edu.kit.anthropomatik.isl.newsTeller.util.propbank.PropbankFrames;
import edu.kit.anthropomatik.isl.newsTeller.util.propbank.PropbankRoleset;

public class PropbankArgumentFeature extends UsabilityFeature {

	private String countQuery;
	
	private String propertyURI;
	
	private PropbankFrames propbank;
	
	public PropbankArgumentFeature(String queryFileName, String countQueryFileName, String propertyURI, String propBankFolderName) {
		super(queryFileName);
		this.countQuery = Util.readStringFromFile(countQueryFileName);
		this.propertyURI = propertyURI;
		this.propbank = PropbankFrames.getInstance(propBankFolderName, false);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		
		double result = Double.POSITIVE_INFINITY;
		
		List<String> mentionURIs = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), 
				Util.VARIABLE_MENTION, false);
		
		double numberOfActors = ksAdapter.runSingleVariableDoubleQuerySingleResult(countQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_NUMBER);
		
		for (String mentionURI : mentionURIs) {
			
			double bestMentionFraction = Double.POSITIVE_INFINITY;
			
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
				
				PropbankRoleset r = propbank.getRoleset(pred, suffix, id);
				
				if (r == null)	// skip if roleset not existing
					continue;
				
				double bestRolesetFraction = Double.POSITIVE_INFINITY;
				
				for (Set<PropbankArgument> argumentSet : r.getArgumentSets()) {
					
					int numberOfExpectedActors = 0;
					for (PropbankArgument argument : argumentSet) {
						if (!argument.getN().isEmpty() && !argument.getN().equalsIgnoreCase("m"))
							numberOfExpectedActors++;
					}
					
					double fraction;
					if (numberOfExpectedActors == 0)
						fraction = Double.POSITIVE_INFINITY;
					else
						fraction = numberOfActors / numberOfExpectedActors;
					
					if (Math.abs(1 - fraction) < Math.abs(1 - bestRolesetFraction))
						bestRolesetFraction = fraction;
				}
				
				if (Math.abs(1 - bestRolesetFraction) < Math.abs(1 - bestMentionFraction))
					bestMentionFraction = bestRolesetFraction;
			}
			
			if (Math.abs(1 - bestMentionFraction) < Math.abs(1 - result))
				result = bestMentionFraction;
			
		}
		
		if (Double.isInfinite(result))
			result = 0;
		
		return result;
		
//		List<String> labels = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_LABEL);
//		
//		double result = 0;
//		for (String label : labels) {
//			if (!propBankMap.containsKey(label) || propBankMap.get(label).isEmpty()) {
//				result = 1;	// there's at least one label w/o propBank requirements (either unknown or doesn't need arguments)
//				break;		// so no requirements --> all requirements fulfilled --> return 1
//			}
//			Set<Set<String>> argumentPossibilities = propBankMap.get(label);
//			for (Set<String> expectedArguments : argumentPossibilities) {
//				if (expectedArguments.isEmpty()) {
//					result = 1;	// no requirements --> all requirements fulfilled --> return 1
//					break;
//				}
//				double fulfilledness = 0;
//				for (String arg : expectedArguments) {
//					double numberOfArgs = ksAdapter.runSingleVariableDoubleQuerySingleResult(
//							countQuery.replace(Util.PLACEHOLDER_EVENT, eventURI).replace(Util.PLACEHOLDER_LINK, arg), Util.VARIABLE_NUMBER);
//					if (numberOfArgs > 0)
//						fulfilledness++;
//				}
//				fulfilledness /= expectedArguments.size();
//				
//				result = Math.max(result, fulfilledness); // take the max: if one label has everything it needs, we're happy
//			}
//		}
//		
//		return result;
	}

}
