package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features.RankingFeature;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

public class SingleFeatureEventRanker implements IEventRanker {

	private RankingFeature feature;
	
	public void setFeature(RankingFeature feature) {
		this.feature = feature;
	}
	
	@Override
	public List<NewsEvent> rankEvents(Set<NewsEvent> events, List<Keyword> userQuery, UserModel userModel) {

		List<NewsEvent> result = new ArrayList<NewsEvent>();
		
		for (NewsEvent event : events) {
			event.setExpectedRelevance(feature.getValue(event.getEventURI(), userQuery, userModel));
			result.add(event);
		}
		
		Collections.sort(result, new Comparator<NewsEvent>() {

			@Override
			public int compare(NewsEvent o1, NewsEvent o2) {
				return (-1) * Double.compare(o1.getExpectedRelevance(), o2.getExpectedRelevance());
			}
		});
		
		return result;
	}


}
