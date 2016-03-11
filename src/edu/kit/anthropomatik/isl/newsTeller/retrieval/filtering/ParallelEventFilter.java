package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

public class ParallelEventFilter implements IEventFilter {

	private static Log log = LogFactory.getLog(SequentialEventFilter.class);
	
	private Classifier classifier;
	
	private Instances header;
	
	private List<UsabilityFeature> features;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	private ExecutorService threadPool;
	
	private String eventStatisticsQuery;
	
	private String eventStatisticsKeywordQuery;
		
	private String eventConstituentsQuery;
	
	private String eventConstituentsKeywordQuery;
	
	private String entityPropertiesQuery;
	
	private String entityPropertiesKeywordQuery;
		
	private String entityMentionsQuery;
	
	public void setFeatures(List<UsabilityFeature> features) {
		this.features = features;
	}
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public void setNumThreads(int numThreads) {
		this.threadPool = Executors.newFixedThreadPool(numThreads);
	}
	
	public ParallelEventFilter(String classifierFileName, String eventStatisticsQueryFileName, String eventStatisticsKeywordQueryFileName,
								String eventConstituentsQueryFileName, String eventConstituentsKeywordQueryFileName,
								String entityPropertiesQueryFileName, String entityPropertiesKeywordQueryFileName, String entityMentionsQueryFileName) {
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
		this.eventStatisticsKeywordQuery = Util.readStringFromFile(eventStatisticsKeywordQueryFileName);
		this.eventConstituentsQuery = Util.readStringFromFile(eventConstituentsQueryFileName);
		this.eventConstituentsKeywordQuery = Util.readStringFromFile(eventConstituentsKeywordQueryFileName);
		this.entityPropertiesQuery = Util.readStringFromFile(entityPropertiesQueryFileName);
		this.entityPropertiesKeywordQuery = Util.readStringFromFile(entityPropertiesKeywordQueryFileName);
		this.entityMentionsQuery = Util.readStringFromFile(entityMentionsQueryFileName);
	}
	
	private class FeatureWorker implements Callable<Double> {

		private UsabilityFeature feature;
		private String eventURI;
		private List<Keyword> keywords;
		
		public FeatureWorker(UsabilityFeature feature, String eventURI, List<Keyword> keywords) {
			this.feature = feature;
			this.eventURI = eventURI;
			this.keywords = keywords;
		}
		
		@Override
		public Double call() {
			return feature.getValue(eventURI, keywords);
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
			
			List<Future<Double>> futures = new ArrayList<Future<Double>>();
			for (int i = 0; i < features.size(); i++) {
				FeatureWorker w = new FeatureWorker(features.get(i), event.getEventURI(), userQuery);
				futures.add(threadPool.submit(w));
			}
			
			for (int i = 0; i < futures.size(); i++) {
				try {
					values[i] = (double) futures.get(i).get();
					event.addUsabilityFeatureValue(features.get(i).getName(), values[i]);
				} catch (Exception e) {
					if (log.isErrorEnabled())
						log.error("thread execution somehow failed!");
					if (log.isDebugEnabled())
						log.debug("thread execution exception", e);
				} 
			}
			
			Instance example = new DenseInstance(1.0, values);
			example.setDataset(header);
			
			this.map.putIfAbsent(event, example);
		}
		
	}
	
	public Set<NewsEvent> filterEvents(Set<NewsEvent> events, List<Keyword> userQuery, UserModel userModel) {
		
		ksAdapter.flushBuffer();
		
		Set<NewsEvent> result = new HashSet<NewsEvent>();
		
		long t = System.currentTimeMillis();
		final Set<String> eventURIs = new HashSet<String>();
		for (NewsEvent e : events)
			eventURIs.add(e.getEventURI());
		
		List<Future<?>> futures = new ArrayList<Future<?>>();
		
		// task 1: get all mentions and based on that both the resources and the mention properties
		futures.add(ksAdapter.submit(new Runnable() {
			
			@Override
			public void run() {
				ksAdapter.runKeyValueMentionFromEventQuery(eventURIs, userQuery);
				final Set<String> mentionURIs = ksAdapter.getAllRelationValues(Util.getRelationName("event", "mention", userQuery.get(0).getWord()));
				
				List<Future<?>> futures = new ArrayList<Future<?>>();
				
				futures.add(ksAdapter.submit(new Runnable() {
					
					@Override
					public void run() {
						Set<String> mentionProperties = new HashSet<String>();
						for (UsabilityFeature feature : features) {
							mentionProperties.addAll(feature.getRequiredMentionProperties());
						}
						ksAdapter.runKeyValueMentionPropertyQuery(mentionProperties, Util.RELATION_NAME_MENTION_PROPERTY, mentionURIs);
					}
				}));
				
				futures.add(ksAdapter.submit(new Runnable() {
					
					@Override
					public void run() {
						ksAdapter.runKeyValueResourceTextQuery(Util.resourceURIsFromMentionURIs(mentionURIs));
					}
				}));
				
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
			}
		}));
		
		// task 2: get the event statistics
		futures.add(ksAdapter.submit(new Runnable() {
			
			@Override
			public void run() {
				ksAdapter.runKeyValueSparqlQuery(eventStatisticsQuery, eventURIs, userQuery);
			}
		}));
		futures.add(ksAdapter.submit(new Runnable() {
	
			@Override
			public void run() {
				ksAdapter.runKeyValueSparqlQuery(eventStatisticsKeywordQuery, eventURIs, userQuery);
			}
		}));

		// task 3: get the event constituents and based on that the entity properties and entity mentions
		futures.add(ksAdapter.submit(new Runnable() {
			
			@Override
			public void run() {
				ksAdapter.runKeyValueSparqlQuery(eventConstituentsKeywordQuery, eventURIs, userQuery);
			}
		}));
		futures.add(ksAdapter.submit(new Runnable() {
			
			@Override
			public void run() {
				ksAdapter.runKeyValueSparqlQuery(eventConstituentsQuery, eventURIs, userQuery);
				final Set<String> entities = ksAdapter.getAllRelationValues(Util.getRelationName("event", "entity", userQuery.get(0).getWord()));
				
				List<Future<?>> futures = new ArrayList<Future<?>>();
				
				futures.add(ksAdapter.submit(new Runnable() {
					
					@Override
					public void run() {
						ksAdapter.runKeyValueSparqlQuery(entityPropertiesQuery, entities, userQuery);
					}
				}));
				
				futures.add(ksAdapter.submit(new Runnable() {
					
					@Override
					public void run() {
						ksAdapter.runKeyValueSparqlQuery(entityPropertiesKeywordQuery, entities, userQuery);
					}
				}));
				
				futures.add(ksAdapter.submit(new Runnable() {
					
					@Override
					public void run() {
						ksAdapter.runKeyValueSparqlQuery(entityMentionsQuery, entities, userQuery);
					}
				}));
				
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
			}
		}));
		
		// wait until everything is done
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
//		if (log.isInfoEnabled())
//			log.info(String.format("bulk retrieval: %d ms", t));
		t = System.currentTimeMillis();
		
		
		// parallel feature extraction (parallel on events)
		futures.clear();
		ConcurrentMap<NewsEvent, Instance> resultMap = new ConcurrentHashMap<NewsEvent, Instance>();
		
		// parallel feature extraction
		for (NewsEvent e : events) {
			EventWorker w = new EventWorker(e, userQuery, resultMap);
			futures.add(threadPool.submit(w));//ksAdapter.submit(w));
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
//		if (log.isInfoEnabled())
//			log.info(String.format("feature extraction: %d ms", t));
		t = System.currentTimeMillis();
		
		// sequential classification
		int idxOfPositiveClass = header.attribute(Util.ATTRIBUTE_USABLE).indexOfValue(Util.LABEL_TRUE);
		for (NewsEvent event : events) {
			boolean isUsable;
			try {
				double label = classifier.classifyInstance(resultMap.get(event));
				isUsable = (label == idxOfPositiveClass);
				double probabilityOfUsable = classifier.distributionForInstance(resultMap.get(event))[idxOfPositiveClass];
				event.setUsabilityProbability(probabilityOfUsable);
			} catch (Exception e) {
				if (log.isWarnEnabled())
					log.warn(String.format("Could not classify event, setting classification to false: %s", event.toVerboseString()));
				isUsable = false;
			}
			
			if (isUsable)
				result.add(event);
			else
				ksAdapter.removeEvent(event.getEventURI());
		}
		
		t = System.currentTimeMillis() - t;
//		if (log.isInfoEnabled())
//			log.info(String.format("classification: %d ms", t));
		
		
		return result;
	}

	public void shutDown() {
		this.threadPool.shutdown();
	}

}
