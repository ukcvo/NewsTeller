package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import edu.kit.anthropomatik.isl.newsTeller.util.propbank.PropbankArgument;
import edu.kit.anthropomatik.isl.newsTeller.util.propbank.PropbankFrames;
import edu.kit.anthropomatik.isl.newsTeller.util.propbank.PropbankRoleset;
import jersey.repackaged.com.google.common.collect.Sets;

public class PropbankArgumentFeature extends UsabilityFeature {

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

	public PropbankArgumentFeature(String propertyURI, String propbankFolderName) {
		super();
		this.propertyURI = propertyURI;
		this.propbank = PropbankFrames.getInstance(propbankFolderName, false);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {

		String arbitraryKeyword = keywords.get(0).getWord();
		
		Set<String> mentionURIs = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_MENTION, eventURI);
		
		boolean hasA0 = Util.parseXMLDouble(ksAdapter.getFirstBufferedValue(Util.getRelationName("event", "a0", arbitraryKeyword), eventURI)) > 0;
		boolean hasA1 = Util.parseXMLDouble(ksAdapter.getFirstBufferedValue(Util.getRelationName("event", "a1", arbitraryKeyword), eventURI)) > 0;
		boolean hasA2 = Util.parseXMLDouble(ksAdapter.getFirstBufferedValue(Util.getRelationName("event", "a2", arbitraryKeyword), eventURI)) > 0;
		boolean hasLoc = Util.parseXMLDouble(ksAdapter.getFirstBufferedValue(Util.getRelationName("event", "place", arbitraryKeyword), eventURI)) > 0;
		
		double bestFraction = Double.POSITIVE_INFINITY;
		double averageFraction = 0;

		String propertyURI = this.propertyURI;

		for (String mentionURI : mentionURIs) {

			double bestMentionFraction = Double.POSITIVE_INFINITY;
			double averageMentionFraction = 0;

			String pos = ksAdapter.getFirstBufferedValue(Util.RELATION_NAME_MENTION_PROPERTY + Util.MENTION_PROPERTY_POS, mentionURI);

			String suffix;
			switch (pos) {
			case Util.MENTION_PROPERTY_POS_NOUN:
				suffix = PropbankFrames.SUFFIX_NOUN;
				if (this.doAutomaticallyChooseProperty)
					propertyURI = Util.MENTION_PROPERTY_NOMBANK;
				break;
			case Util.MENTION_PROPERTY_POS_VERB:
				suffix = PropbankFrames.SUFFIX_VERB;
				if (this.doAutomaticallyChooseProperty)
					propertyURI = Util.MENTION_PROPERTY_PROPBANK;
				break;
			default:
				suffix = ""; // should never happen, though...
				break;
			}

			Set<String> rolesetIds = ksAdapter.getBufferedValues(Util.RELATION_NAME_MENTION_PROPERTY  + propertyURI, mentionURI);

			if (suffix.equals(PropbankFrames.SUFFIX_NOUN) && this.doUseVerbAsFallback) {
				boolean useFallback = true;
				for (String roleset : rolesetIds) {
					if (propbank.containsFrame(roleset.substring(roleset.lastIndexOf("/") + 1, roleset.lastIndexOf(".")), suffix)) {
						useFallback = false;
						break;
					}
				}
				if (useFallback) {
					rolesetIds = 
							ksAdapter.getBufferedValues(Util.RELATION_NAME_MENTION_PROPERTY + Util.MENTION_PROPERTY_PROPBANK, mentionURI);
					suffix = PropbankFrames.SUFFIX_VERB;
				}

			}

			for (String roleset : rolesetIds) {
				String id = roleset.substring(roleset.lastIndexOf("/") + 1);
				String word = id.substring(0, id.indexOf("."));

				PropbankRoleset r = propbank.getRoleset(word, suffix, id);

				if (r == null) // skip if roleset not existing
					continue;

				double bestRolesetFraction = Double.POSITIVE_INFINITY;
				double averageRolesetFraction = 0;

				for (Set<PropbankArgument> argumentSet : r.getArgumentSets()) {

					double fulfilled = 0;
					double needed = 0;

					for (PropbankArgument argument : argumentSet) {
						if (argument.getN().equals("0")) {
							needed++;
							if (hasA0)
								fulfilled++;
						} else if (argument.getN().equals("1")) {
							needed++;
							if (hasA1)
								fulfilled++;
						} else if (argument.getN().equals("2")) {
							needed++;
							if (hasA2)
								fulfilled++;
						} else if (argument.getN().equals("m") && argument.getF().equalsIgnoreCase("loc")) {
							needed++;
							if (hasLoc)
								fulfilled++;
						}
					}

					double fraction;
					if (needed == 0) {
						if (this.doComputeAverageInsteadOfMax)
							fraction = (fulfilled == 0) ? 1.0 : 0.0;
						else
							fraction = (fulfilled == 0) ? 1.0 : Double.POSITIVE_INFINITY;
					} else
						fraction = fulfilled / needed;

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

	@Override
	public Set<String> getRequiredMentionProperties() {
		return Sets.newHashSet(Util.MENTION_PROPERTY_POS, Util.MENTION_PROPERTY_PROPBANK, Util.MENTION_PROPERTY_NOMBANK, propertyURI);
	}

}
