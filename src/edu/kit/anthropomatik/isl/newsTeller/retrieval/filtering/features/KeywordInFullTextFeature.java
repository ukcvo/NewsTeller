package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.List;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Checks if the keywords appear anywhere in the orginal text.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class KeywordInFullTextFeature extends FullTextFeature {

	private boolean useOriginalString;
	
	private boolean useStem;
	
	//TODO: also check for parts of the label?
	
	public void setUseOriginalString(boolean useOriginalString) {
		this.useOriginalString = useOriginalString;
	}

	public void setUseStem(boolean useStem) {
		this.useStem = useStem;
	}

	public KeywordInFullTextFeature() {
		super();
	}

	@Override
	protected List<List<List<String>>> getLabels(String eventURI, List<Keyword> keywords) {
		List<List<List<String>>> result = new ArrayList<List<List<String>>>();
		
		for (Keyword keyword : keywords) {
			List<List<String>> list = new ArrayList<List<String>>();
			if (this.useOriginalString) {
				List<String> innerList = new ArrayList<String>();
				String word = this.doUseContainsInsteadOfRegex ? keyword.getWord() : Util.escapeText(keyword.getWord());
				innerList.add(word);
				list.add(innerList);
			}
			if (this.useStem) {
				List<String> innerList = new ArrayList<String>();
				String stem = this.doUseContainsInsteadOfRegex ? keyword.getStem() : Util.escapeText(keyword.getStem()) + Util.KEYWORD_REGEX_LETTERS_JAVA;
				innerList.add(stem);
				list.add(innerList);
			}	
			result.add(list);
		}
		
		return result;
	}

}
