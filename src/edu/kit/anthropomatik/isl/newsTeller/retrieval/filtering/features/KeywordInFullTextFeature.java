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
	protected List<List<String>> getLabels(String eventURI, List<Keyword> keywords) {
		List<List<String>> result = new ArrayList<List<String>>();
		
		for (Keyword keyword : keywords) {
			List<String> list = new ArrayList<String>();
			if (this.useOriginalString)
				list.add(keyword.getWord());
			if (this.useStem)
				list.add(keyword.getStem());
			result.add(list);
		}
		
		return result;
	}

}
