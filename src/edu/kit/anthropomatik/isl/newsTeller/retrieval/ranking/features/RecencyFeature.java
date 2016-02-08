package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.joda.time.Weeks;
import org.joda.time.Years;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class RecencyFeature extends RankingFeature {

	private static final int TIME_SPAN_DAYS = 0;
	private static final int TIME_SPAN_WEEKS = 1;
	private static final int TIME_SPAN_MONTHS = 2;
	private static final int TIME_SPAN_YEARS = 3;
	
	private int timeSpan;
	
	private final DateTime pointOfReference = DateTime.parse("2016-01-01");
	
	public void setTimeSpan(int timeSpan) {
		this.timeSpan = timeSpan;
	}
	
	@Override
	public double getValue(String eventURI, List<Keyword> keywords, UserModel userModel) {
		
		double result = Double.POSITIVE_INFINITY;
		
		Set<String> resourceURIs = Util.resourceURIsFromMentionURIs(
				ksAdapter.getBufferedValues(Util.getRelationName("event", "mention", keywords.get(0).getWord()), eventURI));
		
		for (String resourceURI : resourceURIs) {
			String timeString =  ksAdapter.getFirstBufferedValue(Util.RELATION_NAME_RESOURCE_PROPERTY + Util.RESOURCE_PROPERTY_TIME, resourceURI);
			DateTime time = DateTime.parse(timeString);
			
			double distance;
			switch (this.timeSpan) {
			case TIME_SPAN_DAYS:
				distance = Days.daysBetween(time, pointOfReference).getDays();
				break;
			case TIME_SPAN_WEEKS:
				distance = Weeks.weeksBetween(time, pointOfReference).getWeeks();
				break;
			case TIME_SPAN_MONTHS:
				distance = Months.monthsBetween(time, pointOfReference).getMonths();
				break;
			case TIME_SPAN_YEARS:
				distance = Years.yearsBetween(time, pointOfReference).getYears();
				break;
			default:
				distance = Double.POSITIVE_INFINITY;
				break;
			}
			
			result = Math.min(result, distance);
		}
		
		return result;
	}

}
