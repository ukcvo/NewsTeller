package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.List;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;

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

	public KeywordInFullTextFeature(String queryFileName) {
		super(queryFileName);
	}

	@Override
	protected List<List<List<String>>> getLabels(String eventURI, List<Keyword> keywords) {
		List<List<List<String>>> result = new ArrayList<List<List<String>>>();
		
		for (Keyword keyword : keywords) {
			List<List<String>> list = new ArrayList<List<String>>();
			if (this.useOriginalString) {
				List<String> innerList = new ArrayList<String>();
				innerList.add(keyword.getWord());
				list.add(innerList);
			}
			if (this.useStem) {
				List<String> innerList = new ArrayList<String>();
				innerList.add(keyword.getStem());
				list.add(innerList);
			}	
			result.add(list);
		}
		
		return result;
	}

}
