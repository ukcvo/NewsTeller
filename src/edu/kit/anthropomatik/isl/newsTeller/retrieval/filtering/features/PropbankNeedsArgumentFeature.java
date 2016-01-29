package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
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
		
		List<String> mentionURIs = new ArrayList<String>();
		mentionURIs.addAll(ksAdapter.getBufferedValues(Util.RELATION_NAME_MENTION, eventURI));
		if (mentionURIs.isEmpty())
			mentionURIs = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_MENTION, false);

		for (String mentionURI : mentionURIs) {
			
			boolean mentionNeedsArg = false;
			
			String pos = ksAdapter.getUniqueMentionProperty(mentionURI, Util.MENTION_PROPERTY_POS);

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
			
			List<String> rolesetIds = ksAdapter.getMentionProperty(mentionURI, propertyURI);

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

}
