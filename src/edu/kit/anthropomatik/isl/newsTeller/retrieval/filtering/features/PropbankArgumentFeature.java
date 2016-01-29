package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import edu.kit.anthropomatik.isl.newsTeller.util.propbank.PropbankArgument;
import edu.kit.anthropomatik.isl.newsTeller.util.propbank.PropbankFrames;
import edu.kit.anthropomatik.isl.newsTeller.util.propbank.PropbankRoleset;

public class PropbankArgumentFeature extends UsabilityFeature {

	private String a0Query;
	private String a1Query;
	private String a2Query;
	private String locQuery;

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

	public PropbankArgumentFeature(String queryFileName, String a0QueryFileName, String a1QueryFileName, String a2QueryFileName, String locQueryFileName, String propertyURI, String propbankFolderName) {
		super(queryFileName);
		this.a0Query = Util.readStringFromFile(a0QueryFileName);
		this.a1Query = Util.readStringFromFile(a1QueryFileName);
		this.a2Query = Util.readStringFromFile(a2QueryFileName);
		this.locQuery = Util.readStringFromFile(locQueryFileName);
		this.propertyURI = propertyURI;
		this.propbank = PropbankFrames.getInstance(propbankFolderName, false);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {

		List<String> mentionURIs = new ArrayList<String>();
		mentionURIs.addAll(ksAdapter.getBufferedValues(Util.RELATION_NAME_MENTION, eventURI));
		if (mentionURIs.isEmpty())
			mentionURIs = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_MENTION, false);

		boolean hasA0 = ksAdapter.runSingleVariableDoubleQuerySingleResult(a0Query.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_NUMBER) > 0;
		boolean hasA1 = ksAdapter.runSingleVariableDoubleQuerySingleResult(a1Query.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_NUMBER) > 0;
		boolean hasA2 = ksAdapter.runSingleVariableDoubleQuerySingleResult(a2Query.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_NUMBER) > 0;
		boolean hasLoc = ksAdapter.runSingleVariableDoubleQuerySingleResult(locQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_NUMBER) > 0;

		double bestFraction = Double.POSITIVE_INFINITY;
		double averageFraction = 0;

		String propertyURI = this.propertyURI;

		for (String mentionURI : mentionURIs) {

			double bestMentionFraction = Double.POSITIVE_INFINITY;
			double averageMentionFraction = 0;

			String pos = ksAdapter.getUniqueMentionProperty(mentionURI, Util.MENTION_PROPERTY_POS);

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

			List<String> rolesetIds = ksAdapter.getMentionProperty(mentionURI, propertyURI);

			if (suffix.equals(PropbankFrames.SUFFIX_NOUN) && this.doUseVerbAsFallback) {
				boolean useFallback = true;
				for (String roleset : rolesetIds) {
					if (propbank.containsFrame(roleset.substring(roleset.lastIndexOf("/") + 1, roleset.lastIndexOf(".")), suffix)) {
						useFallback = false;
						break;
					}
				}
				if (useFallback) {
					rolesetIds = ksAdapter.getMentionProperty(mentionURI, Util.MENTION_PROPERTY_PROPBANK);
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

}
