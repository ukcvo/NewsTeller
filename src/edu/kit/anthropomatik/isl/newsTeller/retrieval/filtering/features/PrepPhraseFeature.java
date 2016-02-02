package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class PrepPhraseFeature extends UsabilityFeature {

	private List<String> prepositions;
	
	private boolean doSearchInsidePhrase;
	
	public void setDoSearchInsidePhrase(boolean doSearchInsidePhrase) {
		this.doSearchInsidePhrase = doSearchInsidePhrase;
	}
		
	public PrepPhraseFeature(String prepositionFileName) {
		super();
		this.prepositions = Util.readStringListFromFile(prepositionFileName);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		double result = 0;
		String arbitraryKeyword = keywords.get(0).getWord();
		
		Set<String> actors = ksAdapter.getBufferedValues(Util.getRelationName("event", "actor", arbitraryKeyword), eventURI);
		for (String actor : actors) {
			
			Set<String> labels = ksAdapter.getBufferedValues(Util.getRelationName("entity", "entityPrefLabel", arbitraryKeyword), actor);
			
			double actorResult = 0;
			for (String label : labels) {
				String[] tokens = label.split(Util.SPLIT_REGEX);
				if (this.doSearchInsidePhrase) {
					for (int i = 1; i < tokens.length; i++) { // look at all tokens
						if (prepositions.contains(tokens[i])) {
							actorResult++;
							break;
						}
					}
				} else if (prepositions.contains(tokens[0])) // look at first token
					actorResult++;
			}
			result += actorResult/labels.size();
		}
		if (!actors.isEmpty())
			result /= actors.size();
		
		return result;
	}
	
	@Override
	public Set<String> getRequiredMentionProperties() {
		return new HashSet<String>();
	}

}
