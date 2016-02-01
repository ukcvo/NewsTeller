package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.HashSet;
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

	private String a0QueryName;
	private String a1QueryName;
	private String a2QueryName;
	private String locQueryName;
	
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
		this.a0QueryName = Util.queryNameFromFileName(a0QueryFileName);
		this.a1QueryName = Util.queryNameFromFileName(a1QueryFileName);
		this.a2QueryName = Util.queryNameFromFileName(a2QueryFileName);
		this.locQueryName = Util.queryNameFromFileName(locQueryFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {

		Set<String> mentionURIs = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_MENTION + sparqlQueryName, eventURI);
		
		boolean hasA0 = Util.parseXMLDoubleFromSet(ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_NUMBER + a0QueryName, eventURI)) > 0;
		boolean hasA1 = Util.parseXMLDoubleFromSet(ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_NUMBER + a1QueryName, eventURI)) > 0;
		boolean hasA2 = Util.parseXMLDoubleFromSet(ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_NUMBER + a2QueryName, eventURI)) > 0;
		boolean hasLoc = Util.parseXMLDoubleFromSet(ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_NUMBER + locQueryName, eventURI)) > 0;
		
		double bestFraction = Double.POSITIVE_INFINITY;
		double averageFraction = 0;

		String propertyURI = this.propertyURI;

		for (String mentionURI : mentionURIs) {

			double bestMentionFraction = Double.POSITIVE_INFINITY;
			double averageMentionFraction = 0;

			String pos = 
					ksAdapter.getFirstBufferedValue(Util.RELATION_NAME_MENTION_PROPERTY + sparqlQueryName + Util.MENTION_PROPERTY_POS, mentionURI);

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

			Set<String> rolesetIds = ksAdapter.getBufferedValues(Util.RELATION_NAME_MENTION_PROPERTY + sparqlQueryName + propertyURI, mentionURI);

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
							ksAdapter.getBufferedValues(Util.RELATION_NAME_MENTION_PROPERTY + sparqlQueryName + Util.MENTION_PROPERTY_PROPBANK, mentionURI);
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
	public void runBulkQueries(Set<String> eventURIs, List<Keyword> keywords) {
		ksAdapter.runKeyValueSparqlQuery(sparqlQuery, Util.RELATION_NAME_EVENT_MENTION + sparqlQueryName, Util.VARIABLE_EVENT, Util.VARIABLE_MENTION, eventURIs);
		ksAdapter.runKeyValueSparqlQuery(a0Query, Util.RELATION_NAME_EVENT_NUMBER + a0QueryName, Util.VARIABLE_EVENT, Util.VARIABLE_NUMBER, eventURIs);
		ksAdapter.runKeyValueSparqlQuery(a1Query, Util.RELATION_NAME_EVENT_NUMBER + a1QueryName, Util.VARIABLE_EVENT, Util.VARIABLE_NUMBER, eventURIs);
		ksAdapter.runKeyValueSparqlQuery(a2Query, Util.RELATION_NAME_EVENT_NUMBER + a2QueryName, Util.VARIABLE_EVENT, Util.VARIABLE_NUMBER, eventURIs);
		ksAdapter.runKeyValueSparqlQuery(locQuery, Util.RELATION_NAME_EVENT_NUMBER + locQueryName, Util.VARIABLE_EVENT, Util.VARIABLE_NUMBER, eventURIs);
		Set<String> mentionURIs = ksAdapter.getAllRelationValues(Util.RELATION_NAME_EVENT_MENTION + sparqlQueryName);
		Set<String> propertyURIs = new HashSet<String>();
		propertyURIs.add(Util.MENTION_PROPERTY_POS);
		propertyURIs.add(propertyURI);
		propertyURIs.add(Util.MENTION_PROPERTY_PROPBANK);
		propertyURIs.add(Util.MENTION_PROPERTY_NOMBANK);
		ksAdapter.runKeyValueMentionPropertyQuery(propertyURIs, Util.RELATION_NAME_MENTION_PROPERTY + sparqlQueryName, mentionURIs);
	}

}
