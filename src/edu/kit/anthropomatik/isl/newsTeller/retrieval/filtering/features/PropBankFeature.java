package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class PropBankFeature extends UsabilityFeature {

	private String countQuery;
	
	private Map<String, Set<Set<String>>> propBankMap;
	
	public PropBankFeature(String queryFileName, String countQueryFileName, String propBankFolderName) {
		super(queryFileName);
		this.countQuery = Util.readStringFromFile(countQueryFileName);
		this.propBankMap = Util.parseAllPropBankFrames(propBankFolderName, false);
	}

	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		List<String> labels = ksAdapter.runSingleVariableStringQuery(sparqlQuery.replace(Util.PLACEHOLDER_EVENT, eventURI), Util.VARIABLE_LABEL);
		
		double result = 0;
		for (String label : labels) {
			if (!propBankMap.containsKey(label) || propBankMap.get(label).isEmpty()) {
				result = 1;	// there's at least one label w/o propBank requirements (either unknown or doesn't need arguments)
				break;		// so no requirements --> all requirements fulfilled --> return 1
			}
			Set<Set<String>> argumentPossibilities = propBankMap.get(label);
			for (Set<String> expectedArguments : argumentPossibilities) {
				if (expectedArguments.isEmpty()) {
					result = 1;	// no requirements --> all requirements fulfilled --> return 1
					break;
				}
				double fulfilledness = 0;
				for (String arg : expectedArguments) {
					double numberOfArgs = ksAdapter.runSingleVariableDoubleQuerySingleResult(
							countQuery.replace(Util.PLACEHOLDER_EVENT, eventURI).replace(Util.PLACEHOLDER_LINK, arg), Util.VARIABLE_NUMBER);
					if (numberOfArgs > 0)
						fulfilledness++;
				}
				fulfilledness /= expectedArguments.size();
				
				result = Math.max(result, fulfilledness); // take the max: if one label has everything it needs, we're happy
			}
		}
		
		return result;
	}

}
