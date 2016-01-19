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
	
	private boolean doComputeAverageInsteadOfMax;
	
	private boolean doAutomaticallyChooseProperty;
	
	private boolean doUseVerbAsFallback;
	
	public void setDoComputeAverageInsteadOfMax(boolean doComputeAverageInsteadOfMax) {
		this.doComputeAverageInsteadOfMax = doComputeAverageInsteadOfMax;
	}
	
	public void setDoAutomaticallyChooseProperty(boolean doAutomaticallyChooseProperty) {
		this.doAutomaticallyChooseProperty = doAutomaticallyChooseProperty;
	}
	
	public void setDoUseVerbAsFallback(boolean doUseVerbAsFallback) {
		this.doUseVerbAsFallback = doUseVerbAsFallback;
	}
	
	public PropbankArgumentFeature(String queryFileName, String countQueryFileName, String propertyURI, String propBankFolderName) {
		super(queryFileName);
		this.countQuery = Util.readStringFromFile(countQueryFileName);
		this.propertyURI = propertyURI;
		this.propbank = PropbankFrames.getInstance(propBankFolderName, false);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		
		List<String> mentionURIs = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), 
				Util.VARIABLE_MENTION, false);
		
		double numberOfActors = ksAdapter.runSingleVariableDoubleQuerySingleResult(countQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_NUMBER);
		
		double bestFraction = Double.POSITIVE_INFINITY;
		double averageFraction = 0;
		
		for (String mentionURI : mentionURIs) {
			
			double bestMentionFraction = Double.POSITIVE_INFINITY;
			double averageMentionFraction = 0;
			
			String pos = ksAdapter.getUniqueMentionProperty(mentionURI, Util.MENTION_PROPERTY_POS);
			
			String suffix;
			switch (pos) {
			case Util.MENTION_PROPERTY_POS_NOUN:
				suffix = PropbankFrames.SUFFIX_NOUN;
				if (this.doAutomaticallyChooseProperty)
					this.propertyURI = Util.MENTION_PROPERTY_NOMBANK;
				break;
			case Util.MENTION_PROPERTY_POS_VERB:
				suffix = PropbankFrames.SUFFIX_VERB;
				if (this.doAutomaticallyChooseProperty)
					this.propertyURI = Util.MENTION_PROPERTY_PROPBANK;
				break;
			default:
				suffix = "";	// should never happen, though...
				break;
			}
			
			List<String> rolesetIds = ksAdapter.getMentionProperty(mentionURI, propertyURI);
			
			if (suffix.equals(PropbankFrames.SUFFIX_NOUN) && this.doUseVerbAsFallback) {
				boolean useFallback = false;
				for (String roleset : rolesetIds) {
					if (!propbank.containsFrame(roleset.substring(roleset.lastIndexOf("/") + 1, roleset.lastIndexOf(".")), suffix)) {
						useFallback = true;
					}
				}
				if (useFallback) {
					rolesetIds = ksAdapter.getMentionProperty(mentionURI, Util.MENTION_PROPERTY_PROPBANK);
					suffix = PropbankFrames.SUFFIX_VERB;
				}
					
			}
			
//			if (!propbank.containsFrame(pred, suffix) && suffix.equals(PropbankFrames.SUFFIX_NOUN) && this.doUseVerbAsFallback) {
//				suffix = PropbankFrames.SUFFIX_VERB;
//			}
				
			
			for (String roleset : rolesetIds) {
				String id = roleset.substring(roleset.lastIndexOf("/") + 1);
				String word = id.substring(0, id.indexOf("."));
				
				PropbankRoleset r = propbank.getRoleset(word, suffix, id);
				
				if (r == null)	// skip if roleset not existing
					continue;
				
				double bestRolesetFraction = Double.POSITIVE_INFINITY;
				double averageRolesetFraction = 0;
				
				for (Set<PropbankArgument> argumentSet : r.getArgumentSets()) {
					
					int numberOfExpectedActors = 0;
					for (PropbankArgument argument : argumentSet) {
						if (!argument.getN().isEmpty() && !argument.getN().equalsIgnoreCase("m"))
							numberOfExpectedActors++;
					}
					
					double fraction;
					if (numberOfExpectedActors == 0) {
						if (this.doComputeAverageInsteadOfMax)
							fraction = (numberOfActors == 0) ? 1.0 : 0.0;
						else
							fraction = (numberOfActors == 0) ? 1.0 : Double.POSITIVE_INFINITY;
					}
					else
						fraction = numberOfActors / numberOfExpectedActors;
					
					if (Math.abs(1 - fraction) < Math.abs(1 - bestRolesetFraction))
						bestRolesetFraction = fraction;
					averageRolesetFraction += fraction;
				}
				averageRolesetFraction /= r.getArgumentSets().size();
				
				if (Math.abs(1 - bestRolesetFraction) < Math.abs(1 - bestMentionFraction))
					bestMentionFraction = bestRolesetFraction;
				averageMentionFraction += averageRolesetFraction;
			}
			averageMentionFraction /= rolesetIds.size();
			
			if (Math.abs(1 - bestMentionFraction) < Math.abs(1 - bestFraction))
				bestFraction = bestMentionFraction;
			averageFraction += averageMentionFraction;
		}
		averageFraction /= mentionURIs.size();
		
		if (this.doComputeAverageInsteadOfMax)
			return averageFraction;
		else
			return Double.isInfinite(bestFraction) ? 0.0 : bestFraction;
		
	}

}
