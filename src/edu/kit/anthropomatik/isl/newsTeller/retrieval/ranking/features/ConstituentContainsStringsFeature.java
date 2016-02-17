package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import edu.stanford.nlp.simple.Sentence;

public class ConstituentContainsStringsFeature extends ContainsStringsFeature {

	private String valueName;
	
	private String entityName;
	
	private boolean needsKeywordIteration;
	
	public void setValueName(String valueName) {
		this.valueName = valueName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public void setNeedsKeywordIteration(boolean needsKeywordIteration) {
		this.needsKeywordIteration = needsKeywordIteration;
	}
	
	public ConstituentContainsStringsFeature(String stringFileName) {
		super(stringFileName);
	}

	@Override
	protected Set<List<String>> getText(String eventURI, List<Keyword> keywords, UserModel userModel) {
		Set<List<String>> result = new HashSet<List<String>>();
		
		if (this.needsKeywordIteration) {
			
			for (Keyword keyword : keywords) {
				Set<String> constituents = ksAdapter.getBufferedValues(Util.getRelationName("event", entityName, keyword.getWord()), eventURI);
				
				for (String entity : constituents) {
					Set<String> labels = ksAdapter.getBufferedValues(Util.getRelationName("entity", valueName, keyword.getWord()), entity);
					for (String label : labels) {
						List<String> labelTokens = (new Sentence(label)).words(); // TODO: might need buffering/optimization?
						result.add(labelTokens);
					}
				}
			}
			
		} else {
			String arbitraryKeyword = keywords.get(0).getWord();
			Set<String> constituents = ksAdapter.getBufferedValues(Util.getRelationName("event", entityName, arbitraryKeyword), eventURI);
			for (String entity : constituents) {
				Set<String> labels = ksAdapter.getBufferedValues(Util.getRelationName("entity", valueName, arbitraryKeyword), entity);
				for (String label : labels) {
					List<String> labelTokens = (new Sentence(label)).words(); // TODO: might need buffering/optimization?
					result.add(labelTokens);
				}
			}	
		}
		
		
		return result;
	}

}
