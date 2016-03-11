package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features.UsabilityFeature;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

/**
 * Filter events in a sequential manner using a WEKA classifier.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SequentialEventFilter implements IEventFilter {

	private static Log log = LogFactory.getLog(SequentialEventFilter.class);
	
	private Classifier classifier;
	
	private Instances header;
	
	private List<UsabilityFeature> features;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	private String eventStatisticsQuery;
	
	private String eventConstituentsQuery;
	
	private String entityPropertiesQuery;
	
	private String entityMentionsQuery;
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public void setFeatures(List<UsabilityFeature> features) {
		this.features = features;
	}
	
	public SequentialEventFilter(String classifierFileName, String eventStatisticsQueryFileName, String eventConstituentsQueryFileName,
									String entityPropertiesQueryFileName, String entityMentionsQueryFileName) {
		try {
			Object[] input = SerializationHelper.readAll(classifierFileName);
			this.classifier = (Classifier) input[0];
			this.header = (Instances) input[1];
		} catch (Exception e) {
			if (log.isFatalEnabled())
				log.fatal(String.format("Can't read classifier from file: '%s'", classifierFileName));
			if (log.isDebugEnabled())
				log.debug("can't read classifier from file", e);
		}
		this.eventStatisticsQuery = Util.readStringFromFile(eventStatisticsQueryFileName);
		this.eventConstituentsQuery = Util.readStringFromFile(eventConstituentsQueryFileName);
		this.entityPropertiesQuery = Util.readStringFromFile(entityPropertiesQueryFileName);
		this.entityMentionsQuery = Util.readStringFromFile(entityMentionsQueryFileName);
	}
	
	public Set<NewsEvent> filterEvents(Set<NewsEvent> events, List<Keyword> userQuery, UserModel userModel) {
		
		ksAdapter.flushBuffer();
		
		Set<NewsEvent> result = new HashSet<NewsEvent>();
		
		long t = System.currentTimeMillis();
		Set<String> eventURIs = new HashSet<String>();
		for (NewsEvent e : events)
			eventURIs.add(e.getEventURI());
		
		List<Keyword> allKeywords = new ArrayList<Keyword>();
		allKeywords.addAll(userQuery);
		//allKeywords.addAll(userModel.getInterests());
		
		
		ksAdapter.runKeyValueMentionFromEventQuery(eventURIs, allKeywords);
		Set<String> mentionURIs = ksAdapter.getAllRelationValues(Util.getRelationName("event", "mention", allKeywords.get(0).getWord()));
		
		Set<String> mentionProperties = new HashSet<String>();
		for (UsabilityFeature feature : this.features) {
			mentionProperties.addAll(feature.getRequiredMentionProperties());
		}
		
		ksAdapter.runKeyValueMentionPropertyQuery(mentionProperties, Util.RELATION_NAME_MENTION_PROPERTY, mentionURIs);
		ksAdapter.runKeyValueResourceTextQuery(Util.resourceURIsFromMentionURIs(mentionURIs));
		
		ksAdapter.runKeyValueSparqlQuery(eventStatisticsQuery, eventURIs, allKeywords);
		ksAdapter.runKeyValueSparqlQuery(eventConstituentsQuery, eventURIs, allKeywords);

		Set<String> entities = ksAdapter.getAllRelationValues("event-entity-" + allKeywords.get(0).getWord());
		ksAdapter.runKeyValueSparqlQuery(entityPropertiesQuery, entities, allKeywords);
		ksAdapter.runKeyValueSparqlQuery(entityMentionsQuery, entities, allKeywords);
		
		t = System.currentTimeMillis() - t;
		if (log.isInfoEnabled())
			log.info(String.format("bulk queries: %d ms", t));
		t = System.currentTimeMillis();
		
		Map<UsabilityFeature, Long> featureRuntime = new HashMap<UsabilityFeature, Long>();
		for (UsabilityFeature f : this.features)
			featureRuntime.put(f, 0L);
		
		int idxOfPositiveClass = header.attribute(Util.ATTRIBUTE_USABLE).indexOfValue(Util.LABEL_TRUE);
		
		for (NewsEvent event : events) {
			double[] values = new double[features.size() + 1];
			
			for (int i = 0; i < features.size(); i++) {
				long tFeature = System.currentTimeMillis();
				UsabilityFeature f = features.get(i);
				values[i] = f.getValue(event.getEventURI(), userQuery);
				tFeature = System.currentTimeMillis() - tFeature;
				featureRuntime.put(f, featureRuntime.get(f) + tFeature);
				event.addUsabilityFeatureValue(f.getName(), values[i]);
			}
			
			Instance example = new DenseInstance(1.0, values);
			example.setDataset(header);
			
			boolean isUsable;
			try {
				double label = classifier.classifyInstance(example);
				isUsable = (label == idxOfPositiveClass);
				double probabilityOfUsable = classifier.distributionForInstance(example)[idxOfPositiveClass];
				event.setUsabilityProbability(probabilityOfUsable);
			} catch (Exception e) {
				if (log.isWarnEnabled())
					log.warn(String.format("Could not classify event, setting classification to false: %s", event.toVerboseString()));
				if (log.isDebugEnabled())
					log.debug("classification problem", e);
				isUsable = false;
			}
			
			if (isUsable)
				result.add(event);
			else
				ksAdapter.removeEvent(event.getEventURI());
		}
		
		t = System.currentTimeMillis() - t;
		if (log.isInfoEnabled())
			log.info(String.format("feature extraction & classification: %d ms", t));
		
//		for (UsabilityFeature f : features) {
//			if (log.isInfoEnabled())
//				log.info(String.format("%s: %d ms", f.getName(), featureRuntime.get(f)));
//		}
		
		return result;
	}

	public void shutDown() {
		// nothing to do
	}

}
