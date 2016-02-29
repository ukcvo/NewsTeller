package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Sets;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features.RankingFeature;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

public class SequentialEventRanker implements IEventRanker {

	private static Log log = LogFactory.getLog(SequentialEventRanker.class);
	
	private Classifier regressor;
	
	private Instances header;
	
	private List<RankingFeature> features;
	
	private String eventConstituentsQuery;
	
	private String entityPropertiesQuery;
	
	private String eventStatisticsQuery;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	public void setFeatures(List<RankingFeature> features) {
		this.features = features;
	}

	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}

	public SequentialEventRanker(String regressorFileName, String eventConstituentsQueryFileName, 
									String entityPropertiesQueryFileName, String eventStatisticsQueryFileName) {
		try {
			Object[] input = SerializationHelper.readAll(regressorFileName);
			this.regressor = (Classifier) input[0];
			this.header = (Instances) input[1];
		} catch (Exception e) {
			if (log.isFatalEnabled())
				log.fatal(String.format("Can't read classifier from file: '%s'", regressorFileName));
			if (log.isDebugEnabled())
				log.debug("can't read classifier from file", e);
		}
		
		this.eventConstituentsQuery = Util.readStringFromFile(eventConstituentsQueryFileName);
		this.entityPropertiesQuery = Util.readStringFromFile(entityPropertiesQueryFileName);
		this.eventStatisticsQuery = Util.readStringFromFile(eventStatisticsQueryFileName);
	}
	
	@Override
	public List<NewsEvent> rankEvents(Set<NewsEvent> events, List<Keyword> userQuery, UserModel userModel) {

		List<NewsEvent> result = new ArrayList<NewsEvent>();
		
		// run the necessary queries
		Set<String> eventURIs = new HashSet<String>();
		for (NewsEvent e : events)
			eventURIs.add(e.getEventURI());
		
		List<Keyword> allKeywords = new ArrayList<Keyword>();
		allKeywords.addAll(userQuery);
		allKeywords.addAll(userModel.getInterests());
		
		ksAdapter.runKeyValueSparqlQuery(eventConstituentsQuery, eventURIs, allKeywords);
		ksAdapter.runKeyValueSparqlQuery(eventStatisticsQuery, eventURIs, allKeywords);
		Set<String> entities = ksAdapter.getAllRelationValues(Util.getRelationName("event", "entity", allKeywords.get(0).getWord()));
		ksAdapter.runKeyValueSparqlQuery(entityPropertiesQuery, entities, allKeywords);
		Set<String> resourceURIs = Util.resourceURIsFromMentionURIs(ksAdapter.getAllRelationValues(Util.getRelationName("event", "mention", allKeywords.get(0).getWord())));
		ksAdapter.runKeyValueResourcePropertyQuery(Sets.newHashSet(Util.RESOURCE_PROPERTY_TIME, Util.RESOURCE_PROPERTY_TITLE) ,resourceURIs);
		
		
		for (NewsEvent event : events) {
			double[] values = new double[features.size() + 1];
			
			for (int i = 0; i < features.size(); i++) {
				RankingFeature f = features.get(i);
				values[i] = f.getValue(event.getEventURI(), userQuery, userModel);
				event.addRelevanceFeatureValue(f.getName(), values[i]);
			}
			
			Instance example = new DenseInstance(1.0, values);
			example.setDataset(header);
			
			try {
				double expectedRelevance = regressor.classifyInstance(example);
				event.setExpectedRelevance(expectedRelevance);
			} catch (Exception e) {
				if (log.isWarnEnabled())
					log.warn(String.format("Could not perform regression on event, setting value to -1: %s", event.toVerboseString()));
				if (log.isDebugEnabled())
					log.debug("regression problem", e);
				event.setExpectedRelevance(-1);
			}
			
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
