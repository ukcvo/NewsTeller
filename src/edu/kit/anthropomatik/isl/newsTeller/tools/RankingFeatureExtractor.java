package edu.kit.anthropomatik.isl.newsTeller.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.util.StringUtils;

import com.google.common.collect.Sets;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.BenchmarkEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.GroundTruth;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features.RankingFeature;
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.XRFFSaver;

/**
 * Extracts features for the regression ranking problem.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class RankingFeatureExtractor {

	private static Log log = LogFactory.getLog(UsabilityFeatureExtractor.class);

	private Map<List<Keyword>, Map<BenchmarkEvent, GroundTruth>> benchmark; // mapping from keywords to events and corresponding groundTruth info

	private Set<String> eventURIs;

	private List<RankingFeature> features;

	private String outputFileName;

	private KnowledgeStoreAdapter ksAdapter;

	private boolean doAddEventInformation;
	
	private String eventStatisticsQuery;
	
	private String eventConstituentsQuery;
	
	private String entityPropertiesQuery;
	
	private ExecutorService threadPool;
	
	public void setFeatures(List<RankingFeature> features) {
		this.features = features;
	}

	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}

	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}

	public void setDoAddEventInformation(boolean doAddEventInformation) {
		this.doAddEventInformation = doAddEventInformation;
	}
	
	public void setNThreads (int nThreads) {
		this.threadPool = Executors.newFixedThreadPool(nThreads);
	}
	
	public RankingFeatureExtractor(String configFileName, String eventStatisticsQueryFileName, String eventConstituentsQueryFileName, 
									String entityPropertiesQueryFileName) {
		Map<String, List<Keyword>> benchmarkKeywords = Util.readBenchmarkConfigFile(configFileName);
		this.benchmark = new HashMap<List<Keyword>, Map<BenchmarkEvent, GroundTruth>>();
		this.eventURIs = new HashSet<String>();

		for (String fileName : benchmarkKeywords.keySet()) {
			Map<BenchmarkEvent, GroundTruth> fileContent = Util.readBenchmarkQueryFromFile(fileName);
			this.benchmark.put(benchmarkKeywords.get(fileName), fileContent);
			for (BenchmarkEvent e : fileContent.keySet())
				this.eventURIs.add(e.getEventURI());
		}
		
		this.eventStatisticsQuery = Util.readStringFromFile(eventStatisticsQueryFileName);
		this.eventConstituentsQuery = Util.readStringFromFile(eventConstituentsQueryFileName);
		this.entityPropertiesQuery = Util.readStringFromFile(entityPropertiesQueryFileName);
	}

	private Instances createDataSetSkeleton() {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();

		for (RankingFeature feature : this.features) {
			Attribute attr = new Attribute(feature.getName());
			attributes.add(attr);
		}

		attributes.add(new Attribute(Util.ATTRIBUTE_RELEVANCE));

		if (this.doAddEventInformation) {
			attributes.add(new Attribute(Util.ATTRIBUTE_URI, (ArrayList<String>) null));
			attributes.add(new Attribute(Util.ATTRIBUTE_FILE, (ArrayList<String>) null));
		}
		
		int numberOfExpectedExamples = this.benchmark.size();
		Instances dataSet = new Instances("rankingTest", attributes, numberOfExpectedExamples);
		dataSet.setClass(dataSet.attribute(Util.ATTRIBUTE_RELEVANCE));

		return dataSet;
	}

	private void writeDataSet(Instances dataSet) {
		try {
			XRFFSaver saver = new XRFFSaver();
			saver.setInstances(dataSet);
			saver.setFile(new File(outputFileName));
			saver.writeBatch();
		} catch (IOException e) {
			if (log.isErrorEnabled())
				log.error("Could not write extracted features to file");
			if (log.isDebugEnabled())
				log.debug("I/O exception", e);
		}
	}

	private class EventWorker implements Callable<Instance> {

		private String eventURI;
		private UserModel userModel;
		private List<Keyword> keywords;
		private double relevance;
		private int numberOfAttributes;
		private int uriIndex;
		private int fileIndex;
		
		public EventWorker(String eventURI, UserModel userModel, List<Keyword> keywords, double relevance, int numberOfAttributes, int uriIndex, int fileIndex) {
			this.eventURI = eventURI;
			this.userModel = userModel;
			this.keywords = keywords;
			this.relevance = relevance;
			this.numberOfAttributes = numberOfAttributes;
			this.uriIndex = uriIndex;
			this.fileIndex = fileIndex;
		}
		
		@Override
		public Instance call() throws Exception {
			double[] values = new double[numberOfAttributes];
			for (int i = 0; i < features.size(); i++) {
				RankingFeature f = features.get(i);
				values[i] = f.getValue(eventURI, keywords, userModel);
			}
			
			values[features.size()] = relevance;
			
			if (doAddEventInformation) {
				values[features.size() + 1] = uriIndex;
				values[features.size() + 2] = fileIndex;
			}

			Instance instance = new DenseInstance(1.0, values);
			return instance;
		}
		
	}
	
	public void run() {
		Instances dataSet = createDataSetSkeleton();
		this.ksAdapter.openConnection();

		for (Map.Entry<List<Keyword>, Map<BenchmarkEvent, GroundTruth>> entry : this.benchmark.entrySet()) {
			List<Keyword> keywords = entry.getKey();
			Map<BenchmarkEvent, GroundTruth> content = entry.getValue();
			if (log.isInfoEnabled())
				log.info(StringUtils.collectionToCommaDelimitedString(keywords));
			
			// run the queries
			ksAdapter.flushBuffer();
			List<Future<?>> futures = new ArrayList<Future<?>>();
			
			// task 1: get event mentions, based on this get resource texts and resource titles
			futures.add(ksAdapter.submit(new Runnable() {
				
				@Override
				public void run() {
					ksAdapter.runKeyValueMentionFromEventQuery(eventURIs, keywords);
					Set<String> resourceURIs = Util.resourceURIsFromMentionURIs(ksAdapter.getAllRelationValues(Util.getRelationName("event", "mention", keywords.get(0).getWord())));
					
					List<Future<?>> futures = new ArrayList<Future<?>>();
					futures.add(ksAdapter.submit(new Runnable() {
						
						@Override
						public void run() {
							ksAdapter.runKeyValueResourceTextQuery(resourceURIs);
						}
					}));
					futures.add(ksAdapter.submit(new Runnable() {
						
						@Override
						public void run() {
							ksAdapter.runKeyValueResourcePropertyQuery(Sets.newHashSet(Util.RESOURCE_PROPERTY_TIME, Util.RESOURCE_PROPERTY_TITLE) ,resourceURIs);
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
			
			// task 2: event statistics
			futures.add(ksAdapter.submit(new Runnable() {
				
				@Override
				public void run() {
					ksAdapter.runKeyValueSparqlQuery(eventStatisticsQuery, eventURIs, keywords);
				}
			}));
			
			// task 3: event entities & entity properties
			futures.add(ksAdapter.submit(new Runnable() {
				
				@Override
				public void run() {
					ksAdapter.runKeyValueSparqlQuery(eventConstituentsQuery, eventURIs, keywords);
					Set<String> entities = ksAdapter.getAllRelationValues(Util.getRelationName("event", "entity", keywords.get(0).getWord()));
					ksAdapter.runKeyValueSparqlQuery(entityPropertiesQuery, entities, keywords);
				}
			}));
			
			// wait until all are done
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
			if (log.isInfoEnabled())
				log.info("...queries done");
			
			List<Future<Instance>> instanceFutures = new ArrayList<Future<Instance>>();
			
			int numberOfAttributes = dataSet.numAttributes();
			
			// compute the features
			for (Map.Entry<BenchmarkEvent, GroundTruth> innerEntry : content.entrySet()) {

				BenchmarkEvent event = innerEntry.getKey();
				GroundTruth gt = innerEntry.getValue();
				String eventURI = event.getEventURI();
				String fileName = event.getFileName();
				double relevance = gt.getRegressionRelevanceValue();
				UserModel userModel = new DummyUserModel();
				int uriIndex = dataSet.attribute(Util.ATTRIBUTE_URI).addStringValue(eventURI);
				int fileIndex = dataSet.attribute(Util.ATTRIBUTE_FILE).addStringValue(fileName);
				
				EventWorker w = new EventWorker(eventURI, userModel, keywords, relevance, numberOfAttributes, uriIndex, fileIndex);
				instanceFutures.add(threadPool.submit(w));
			}
			
//			for (Map.Entry<BenchmarkEvent, GroundTruth> innerEntry : content.entrySet()) {
//
//				BenchmarkEvent event = innerEntry.getKey();
//				GroundTruth gt = innerEntry.getValue();
//				String eventURI = event.getEventURI();
//				String fileName = event.getFileName();
//				double relevance = gt.getRegressionRelevanceValue();
//				UserModel userModel = new DummyUserModel();
//
//				double[] values = new double[dataSet.numAttributes()];
//				for (int i = 0; i < this.features.size(); i++) {
//					RankingFeature f = features.get(i);
//					values[i] = f.getValue(eventURI, keywords, userModel);
//				}
//				values[this.features.size()] = relevance;
//				
//				if (this.doAddEventInformation) {
//					values[this.features.size() + 1] = dataSet.attribute(Util.ATTRIBUTE_URI).addStringValue(eventURI);
//					values[this.features.size() + 2] = dataSet.attribute(Util.ATTRIBUTE_FILE).addStringValue(fileName);
//				}
//
//				Instance instance = new DenseInstance(1.0, values);
//				dataSet.add(instance);
//			}
			
			// wait until all are done
			for (Future<Instance> f : instanceFutures) {
				try {
					dataSet.add(f.get());
				} catch (Exception e) {
					if (log.isErrorEnabled())
						log.error("thread execution somehow failed!");
					if (log.isDebugEnabled())
						log.debug("thread execution exception", e);
				}
			}
			if (log.isInfoEnabled())
				log.info("...features done");
		}

		if (log.isInfoEnabled())
			log.info("done");

		this.ksAdapter.closeConnection();
		this.threadPool.shutdown();
		writeDataSet(dataSet);
	}

	public static void main(String[] args) {
		System.setProperty("java.util.logging.config.file", "./config/logging.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(UsabilityFeatureExtractor.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (log.isInfoEnabled())
			log.info("ranking feature extractor started");
		
		ApplicationContext context = new FileSystemXmlApplicationContext("config/default.xml");
		RankingFeatureExtractor extractor = (RankingFeatureExtractor) context.getBean("rankingFeatureExtractor");
		((AbstractApplicationContext) context).close();

		if (log.isInfoEnabled())
			log.info("loaded everything, calling 'run' now");
		extractor.run();
	}

}
