package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

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

public class ParallelEventFilter implements IEventFilter {

private static Log log = LogFactory.getLog(SequentialEventFilter.class);
	
	private Classifier classifier;
	
	private Instances header;
	
	private List<UsabilityFeature> features;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	public void setFeatures(List<UsabilityFeature> features) {
		this.features = features;
	}
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public ParallelEventFilter(String classifierFileName) {
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
	
	private class EventWorker implements Runnable {

		private NewsEvent event;
		
		private List<Keyword> userQuery;
		
		private ConcurrentMap<NewsEvent, Instance> map;
		
		public EventWorker(NewsEvent event, List<Keyword> userQuery, ConcurrentMap<NewsEvent, Instance> map) {
			this.event = event;
			this.userQuery = userQuery;
			this.map = map;
		}
		
		public void run() {
			double[] values = new double[features.size() + 1];
			
			for (int i = 0; i < features.size(); i++) {
				UsabilityFeature f = features.get(i);
				values[i] = f.getValue(event.getEventURI(), userQuery);
			}
			
			Instance example = new DenseInstance(1.0, values);
			example.setDataset(header);
			
			map.putIfAbsent(event, example);
		}
		
	}
	
	private class BulkQueryWorker implements Runnable {
		
		private UsabilityFeature feature;
		private Set<String> eventURIs;
		private List<Keyword> keywords;
		
		public BulkQueryWorker(UsabilityFeature feature, Set<String> eventURIs, List<Keyword> keywords) {
			this.feature = feature;
			this.eventURIs = eventURIs;
			this.keywords = keywords;
		}

		@Override
		public void run() {
			feature.runBulkQueries(eventURIs, keywords);
		}
	}
	
	public Set<NewsEvent> filterEvents(Set<NewsEvent> events, List<Keyword> userQuery) {
		
		ksAdapter.flushBuffer();
		
		Set<NewsEvent> result = new HashSet<NewsEvent>();
		
		Set<String> eventURIs = new HashSet<String>();
		for (NewsEvent e : events)
			eventURIs.add(e.getEventURI());
		
		long t = System.currentTimeMillis();
		// sequential bulk retrieval
//		for (UsabilityFeature feature : this.features)
//			feature.runBulkQueries(eventURIs, userQuery);
		List<Future<?>> futures = new ArrayList<Future<?>>();
		for (UsabilityFeature feature : this.features) {
			BulkQueryWorker w = new BulkQueryWorker(feature, eventURIs, userQuery);
			futures.add(ksAdapter.submit(w));
		}
		
		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("thread execution somehow failed!");
				if (log.isDebugEnabled())
					log.debug("thread execution exception", e);
			}
		}
		
		t = System.currentTimeMillis() - t;
		if (log.isInfoEnabled())
			log.info(String.format("bulk retrieval: %d ms", t));
		t = System.currentTimeMillis();
		
		// parallel feature extraction (parallel on events)
		futures = new ArrayList<Future<?>>();
		ConcurrentMap<NewsEvent, Instance> resultMap = new ConcurrentHashMap<NewsEvent, Instance>();
		
		// parallel feature extraction
		for (NewsEvent e : events) {
			EventWorker w = new EventWorker(e, userQuery, resultMap);
			futures.add(ksAdapter.submit(w));
		}
		
		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("thread execution somehow failed!");
				if (log.isDebugEnabled())
					log.debug("thread execution exception", e);
			}
		}
		
		t = System.currentTimeMillis() - t;
		if (log.isInfoEnabled())
			log.info(String.format("feature extraction: %d ms", t));
		t = System.currentTimeMillis();
		
		// sequential classification
		for (NewsEvent event : events) {
			boolean isUsable;
			try {
				double label = classifier.classifyInstance(resultMap.get(event));
				isUsable = (label == header.attribute(Util.ATTRIBUTE_USABLE).indexOfValue(Util.LABEL_TRUE));
			} catch (Exception e) {
				if (log.isWarnEnabled())
					log.warn(String.format("Could not classify event, setting classification to false: %s", event.toVerboseString()));
				isUsable = false;
			}
			
			if (isUsable)
				result.add(event);
		}
		
		t = System.currentTimeMillis() - t;
		if (log.isInfoEnabled())
			log.info(String.format("classification: %d ms", t));
		
		
		return result;
	}

	public void shutDown() {
		// nothing to do
	}

}
