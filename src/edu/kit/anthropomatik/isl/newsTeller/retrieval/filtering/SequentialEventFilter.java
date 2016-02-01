package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

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
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public void setFeatures(List<UsabilityFeature> features) {
		this.features = features;
	}
	
	public SequentialEventFilter(String classifierFileName) {
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
	}
	
	public Set<NewsEvent> filterEvents(Set<NewsEvent> events, List<Keyword> userQuery) {
		
		ksAdapter.flushBuffer();
		
		Set<NewsEvent> result = new HashSet<NewsEvent>();
		
		long t = System.currentTimeMillis();
		Set<String> eventURIs = new HashSet<String>();
		for (NewsEvent e : events)
			eventURIs.add(e.getEventURI());
		
		ksAdapter.runKeyValueMentionFromEventQuery(eventURIs);
		Set<String> mentionURIs = ksAdapter.getAllRelationValues(Util.RELATION_NAME_EVENT_MENTION);
		
		Set<String> mentionProperties = new HashSet<String>();
		for (UsabilityFeature feature : this.features) {
			mentionProperties.addAll(feature.getRequiredMentionProperties());
		}
		
		ksAdapter.runKeyValueMentionPropertyQuery(mentionProperties, Util.RELATION_NAME_MENTION_PROPERTY, mentionURIs);
		ksAdapter.runKeyValueResourceTextQuery(Util.resourceURIsFromMentionURIs(mentionURIs));
		
		for (UsabilityFeature feature : this.features) {
			long t1 = System.currentTimeMillis();
			feature.runBulkQueries(eventURIs, userQuery);
			t1 = System.currentTimeMillis() - t1;
//			if (log.isInfoEnabled())
//				log.info(String.format("%s: %d ms", feature.getName(), t1));
		}
		t = System.currentTimeMillis() - t;
		if (log.isInfoEnabled())
			log.info(String.format("bulk queries: %d ms", t));
		t = System.currentTimeMillis();
		
		Map<UsabilityFeature, Long> featureRuntime = new HashMap<UsabilityFeature, Long>();
		for (UsabilityFeature f : this.features)
			featureRuntime.put(f, 0L);
		
		for (NewsEvent event : events) {
			double[] values = new double[features.size() + 1];
			
			for (int i = 0; i < features.size(); i++) {
				long tFeature = System.currentTimeMillis();
				UsabilityFeature f = features.get(i);
				values[i] = f.getValue(event.getEventURI(), userQuery);
				tFeature = System.currentTimeMillis() - tFeature;
				featureRuntime.put(f, featureRuntime.get(f) + tFeature);
			}
			
			Instance example = new DenseInstance(1.0, values);
			example.setDataset(header);
			
			boolean isUsable;
			try {
				double label = classifier.classifyInstance(example);
				isUsable = (label == header.attribute(Util.ATTRIBUTE_USABLE).indexOfValue(Util.LABEL_TRUE));
			} catch (Exception e) {
				if (log.isWarnEnabled())
					log.warn(String.format("Could not classify event, setting classification to false: %s", event.toVerboseString()));
				if (log.isDebugEnabled())
					log.debug("classification problem", e);
				isUsable = false;
			}
			
			if (isUsable)
				result.add(event);
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
