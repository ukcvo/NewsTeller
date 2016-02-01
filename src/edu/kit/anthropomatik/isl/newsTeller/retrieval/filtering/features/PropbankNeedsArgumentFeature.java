package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import edu.kit.anthropomatik.isl.newsTeller.util.propbank.PropbankArgument;
import edu.kit.anthropomatik.isl.newsTeller.util.propbank.PropbankFrames;
import edu.kit.anthropomatik.isl.newsTeller.util.propbank.PropbankRoleset;

public class PropbankNeedsArgumentFeature extends UsabilityFeature {

	private String propertyURI;

	private PropbankFrames propbank;

	private String argument;
	
	private boolean isLocation;
	
	public static final String ARGUMENT_A0 = "0";
	public static final String ARGUMENT_A1 = "1";
	public static final String ARGUMENT_A2 = "2";
	public static final String ARGUMENT_LOC = "m";
		
	public PropbankNeedsArgumentFeature(String queryFileName, String propertyURI, String argument, String propbankFolderName) {
		super(queryFileName);
		this.propertyURI = propertyURI;
		this.argument = argument;
		this.isLocation = argument.equalsIgnoreCase(ARGUMENT_LOC);
		this.propbank = PropbankFrames.getInstance(propbankFolderName, false);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		
		boolean eventNeedsArg = true;
		
		Set<String> mentionURIs = ksAdapter.getBufferedValues(Util.RELATION_NAME_EVENT_MENTION + sparqlQueryName, eventURI);

		for (String mentionURI : mentionURIs) {
			
			boolean mentionNeedsArg = false;
			
			String pos = 
					ksAdapter.getFirstBufferedValue(Util.RELATION_NAME_MENTION_PROPERTY + sparqlQueryName + Util.MENTION_PROPERTY_POS, mentionURI);

			String suffix;
			switch (pos) {
			case Util.MENTION_PROPERTY_POS_NOUN:
				suffix = PropbankFrames.SUFFIX_NOUN;
				break;
			case Util.MENTION_PROPERTY_POS_VERB:
				suffix = PropbankFrames.SUFFIX_VERB;
				break;
			default:
				suffix = ""; // should never happen, though...
				break;
			}
			
			Set<String> rolesetIds = ksAdapter.getBufferedValues(Util.RELATION_NAME_MENTION_PROPERTY + sparqlQueryName + propertyURI, mentionURI);

			for (String roleset : rolesetIds) {
				
				String id = roleset.substring(roleset.lastIndexOf("/") + 1);
				String word = id.substring(0, id.indexOf("."));

				PropbankRoleset r = propbank.getRoleset(word, suffix, id);

				if (r == null) // skip if roleset not existing
					continue;
				
				boolean rolesetNeedsArg = true;
				for (Set<PropbankArgument> argumentSet : r.getArgumentSets()) {
					boolean found = false;
					for (PropbankArgument arg : argumentSet) {
						if (arg.getN().equalsIgnoreCase(argument))
							found = this.isLocation ? arg.getF().equalsIgnoreCase("LOC") : true;
					}
					if (!found) {
						rolesetNeedsArg = false;
						break;
					}
				}
				
				mentionNeedsArg = (mentionNeedsArg || rolesetNeedsArg);
			}
			
			eventNeedsArg = (eventNeedsArg && mentionNeedsArg);
		}
		
		return eventNeedsArg ? 1.0 : 0.0;
	}

	@Override
	public void runBulkQueries(Set<String> eventURIs, List<Keyword> keywords) {
		ksAdapter.runKeyValueSparqlQuery(sparqlQuery, Util.RELATION_NAME_EVENT_MENTION + sparqlQuery, Util.VARIABLE_EVENT, 
				Util.VARIABLE_MENTION, eventURIs);
		Set<String> mentionURIs = ksAdapter.getAllRelationValues(Util.RELATION_NAME_EVENT_MENTION + sparqlQuery);
		Set<String> propertyURIs = new HashSet<String>();
		propertyURIs.add(Util.MENTION_PROPERTY_POS);
		propertyURIs.add(propertyURI);
		ksAdapter.runKeyValueMentionPropertyQuery(propertyURIs, Util.RELATION_NAME_MENTION_PROPERTY + sparqlQueryName, mentionURIs);
	}

}
