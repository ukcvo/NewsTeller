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

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.BenchmarkEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.GroundTruth;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.UsabilityRatingReason;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features.UsabilityFeature;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.XRFFLoader;
import weka.core.converters.XRFFSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Extract features for all the benchmark events and store them in WEKA format for classifier training.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class UsabilityFeatureExtractor {

	private static Log log = LogFactory.getLog(UsabilityFeatureExtractor.class);
	
	private Map<String, List<Keyword>> benchmarkKeywords;	// file and corresponding keyword list
	
	private Map<BenchmarkEvent, GroundTruth> benchmark;	// event and corresponding labels / ratings
	
	private List<UsabilityFeature> features;
	
	private String inputFileName;
	
	private String outputFileName;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	private boolean doAddEventInformation;
	
	private boolean doAddReasonInformation;
	
	private boolean doKeepOnlyListedReasons;
	
	private boolean doFiltering;
	
	private Set<UsabilityRatingReason> reasonsToKeep;
	
	public void setFeatures(List<UsabilityFeature> features) {
		this.features = features;
	}
	
	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}
	
	public void setInputFileName(String inputFileName) {
		this.inputFileName = inputFileName;
	}
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public void setDoAddEventInformation(boolean doAddEventInformation) {
		this.doAddEventInformation = doAddEventInformation;
	}
	
	public void setDoAddReasonInformation(boolean doAddReasonInformation) {
		this.doAddReasonInformation = doAddReasonInformation;
	}
	
	public void setDoKeepOnlyListedReasons(boolean doKeepOnlyListedReasons) {
		this.doKeepOnlyListedReasons = doKeepOnlyListedReasons;
	}
	
	public void setDoFiltering(boolean doFiltering) {
		this.doFiltering = doFiltering;
	}
	
	public void setReasonsToKeep(Set<Integer> reasonsToKeep) {
		this.reasonsToKeep = new HashSet<UsabilityRatingReason>();
		for (Integer i : reasonsToKeep)
			this.reasonsToKeep.add(UsabilityRatingReason.fromInteger(i));
	}
	
	public UsabilityFeatureExtractor(String configFileName) {
		this.benchmarkKeywords = Util.readBenchmarkConfigFile(configFileName);
		this.benchmark = new HashMap<BenchmarkEvent, GroundTruth>();

		for (String fileName : benchmarkKeywords.keySet()) {
			Map<BenchmarkEvent, GroundTruth> fileContent = Util.readBenchmarkQueryFromFile(fileName);
			this.benchmark.putAll(fileContent);
		}
	}

	private Instances createDataSetSkeleton() {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		
		for (UsabilityFeature feature : this.features) {
			Attribute attr = new Attribute(feature.getName());
			attributes.add(attr);
		}
		
		ArrayList<String> booleanLabels = new ArrayList<String>();
		booleanLabels.add(Util.LABEL_TRUE);
		booleanLabels.add(Util.LABEL_FALSE);
		attributes.add(new Attribute(Util.ATTRIBUTE_USABLE, booleanLabels));
		
		if (this.doAddEventInformation) {
			attributes.add(new Attribute(Util.ATTRIBUTE_URI, (ArrayList<String>) null));
			attributes.add(new Attribute(Util.ATTRIBUTE_FILE, (ArrayList<String>) null));
		}
		
		if (this.doAddReasonInformation) {
			for (UsabilityRatingReason r : UsabilityRatingReason.values()) {
				attributes.add(new Attribute(Util.ATTRIBUTE_REASON + r.toString(), booleanLabels));
			}
		}
		
		int numberOfExpectedExamples = this.benchmark.size();
		Instances dataSet = new Instances("usabilityTest", attributes, numberOfExpectedExamples);
		dataSet.setClass(dataSet.attribute(Util.ATTRIBUTE_USABLE));
		return dataSet;
	}
	
	private Instances loadDataSet(String fileName) {
		Instances result = null;
		try {
			XRFFLoader loader = new XRFFLoader();
			loader.setFile(new File(inputFileName));
			result = loader.getDataSet();
		} catch (IOException e) {
			if (log.isErrorEnabled())
				log.error("Could not load features from file");
			if (log.isDebugEnabled())
				log.debug("I/O exception", e);
		}
		return result;
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
	
	// takes care of extracting all features for one given event
	private class EventWorker implements Callable<Instance> {

		private String eventURI;
		private List<Keyword> keywords;
		private int usabilityIndex;
		private int uriIndex;
		private int fileIndex;
		private int[] reasonIndices;
		
		public EventWorker(String eventURI, List<Keyword> keywords, int usabilityIndex, int uriIndex, int fileIndex, int[] reasonIndices) {
			this.eventURI = eventURI;
			this.keywords = keywords;
			this.usabilityIndex = usabilityIndex;
			this.uriIndex = uriIndex;
			this.fileIndex = fileIndex;
			this.reasonIndices = reasonIndices;
		}
		
		public Instance call() throws Exception {
			
			int numberOfAttributes = features.size() + 1;
			if (doAddEventInformation)
				numberOfAttributes += 2;
			if (doAddReasonInformation)
				numberOfAttributes += reasonIndices.length;
			
			double[] values = new double[numberOfAttributes];
			
			for (int i = 0; i < features.size(); i++) {
				UsabilityFeature f = features.get(i);
				values[i] = f.getValue(this.eventURI, this.keywords);
			}
			
			values[features.size()] = this.usabilityIndex;
			
			if (doAddEventInformation) {
				values[features.size() + 1] = this.uriIndex;
				values[features.size() + 2] = this.fileIndex;
			}
			
			if (doAddReasonInformation) {
				int offset = doAddEventInformation ? 3 : 1;
				for (int i = 0; i < reasonIndices.length; i++)
					values[features.size() + offset + i] = reasonIndices[i];
			}
			
			Instance example = new DenseInstance(1.0, values);
			
			return example;
		}

		
	}

	// region createFromScratch
	private void createFromScratch() {
		Instances dataSet = createDataSetSkeleton();
		
		ExecutorService threadPool = Executors.newFixedThreadPool(ksAdapter.getMaxNumberOfConnections());
		List<Future<Instance>> futures = new ArrayList<Future<Instance>>();
		
		for (Map.Entry<BenchmarkEvent, GroundTruth> entry : this.benchmark.entrySet()) {
			BenchmarkEvent event = entry.getKey();
			GroundTruth gt = entry.getValue();
			
			if (this.doKeepOnlyListedReasons) {
				boolean abort = true;
				for (UsabilityRatingReason r : this.reasonsToKeep) {
					if (gt.getReasons().contains(r)) {
						abort = false;
						break;
					}
				}
				if (abort)
					continue;	// skip this entry if it doesn't have any of the required reasons
			}
			
			String eventURI = event.getEventURI();
			List<Keyword> keywords = this.benchmarkKeywords.get(entry.getKey().getFileName());
			String fileName = event.getFileName();
			String label = (gt.getUsabilityRating() == 1.0) ? Util.LABEL_TRUE : Util.LABEL_FALSE;
			
			int usabilityIndex = dataSet.attribute(Util.ATTRIBUTE_USABLE).indexOfValue(label);
			int uriIndex = this.doAddEventInformation ? dataSet.attribute(Util.ATTRIBUTE_URI).addStringValue(eventURI) : 0;
			int fileIndex = this.doAddEventInformation ? dataSet.attribute(Util.ATTRIBUTE_FILE).addStringValue(fileName) : 0;
			
			UsabilityRatingReason[] reasons = UsabilityRatingReason.values();
			int[] reasonIndices = new int[reasons.length];
			if (this.doAddReasonInformation) {
				for (int i = 0; i < reasons.length; i++) {
					boolean isReasonActive = gt.getReasons().contains(reasons[i]);
					int reasonIdx = dataSet.attribute(Util.ATTRIBUTE_REASON + reasons[i].toString()).indexOfValue(Boolean.toString(isReasonActive));	
					reasonIndices[i] = reasonIdx;
				}
			}
			
			EventWorker w = new EventWorker(eventURI, keywords, usabilityIndex, uriIndex, fileIndex, reasonIndices);
			
			futures.add(threadPool.submit(w));
		}
		if (log.isInfoEnabled())
			log.info("submitted all events");
		
		for (Future<Instance> f : futures) {
			try {
				dataSet.add(f.get());
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("thread execution somehow failed!");
				if (log.isDebugEnabled())
					log.debug("thread execution exception", e);
			}
		}
		
		threadPool.shutdown();
		
		if (log.isInfoEnabled())
			log.info("collected all results");
		
		writeDataSet(dataSet);
	}
	// endregion
	
	// region filterExistingDataSet
	private void filterExistingDataSet() {
		
		try {
			Instances dataSet = loadDataSet(this.inputFileName);
			
			// first filter instances
			if (this.doKeepOnlyListedReasons) {
				Instances filtered = new Instances(dataSet, 0);
				for (Instance instance : dataSet) {
					boolean shouldKeep = false;
					for (UsabilityRatingReason r : this.reasonsToKeep) {
						if (instance.value(dataSet.attribute(Util.ATTRIBUTE_REASON + r.toString())) 
								== dataSet.classAttribute().indexOfValue(Util.LABEL_TRUE)) {
							shouldKeep = true;
							break;
						}
					}
					if (shouldKeep)
						filtered.add(instance);
				}
				dataSet = filtered;
			}
			
			// then filter attributes
			List<Integer> attributesToKeep = new ArrayList<Integer>();
			
			for (UsabilityFeature f : this.features) {
				attributesToKeep.add(dataSet.attribute(f.getName()).index() + 1);
			}
			attributesToKeep.add(dataSet.classIndex() + 1); // always keep class obviously
			
			if (this.doAddEventInformation) {
				attributesToKeep.add(dataSet.attribute(Util.ATTRIBUTE_FILE).index() + 1);
				attributesToKeep.add(dataSet.attribute(Util.ATTRIBUTE_URI).index() + 1);
			}
			
			if (this.doAddReasonInformation) {
				for (UsabilityRatingReason r : UsabilityRatingReason.values())
					attributesToKeep.add(dataSet.attribute(Util.ATTRIBUTE_REASON + r.toString()).index() + 1);
			}
			
			Remove removeFilter = new Remove();
			removeFilter.setAttributeIndices(StringUtils.collectionToCommaDelimitedString(attributesToKeep));
			removeFilter.setInvertSelection(true);
			removeFilter.setInputFormat(dataSet);
			dataSet = Filter.useFilter(dataSet, removeFilter);
			
			writeDataSet(dataSet);
			
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("filtering somehow failed!");
			if (log.isDebugEnabled())
				log.debug("filtering exception", e);
		}
		
	}
	// endregion
	
	public void run() {
		this.ksAdapter.openConnection();
		
		if (this.doFiltering)
			filterExistingDataSet();
		else
			createFromScratch();
				
		this.ksAdapter.closeConnection();
	}
	
	public static void main(String[] args) {
		System.setProperty("java.util.logging.config.file", "./config/logging.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(UsabilityFeatureExtractor.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/default.xml");
		UsabilityFeatureExtractor extractor = (UsabilityFeatureExtractor) context.getBean("usabilityFeatureExtractor");
		((AbstractApplicationContext) context).close();
		
		extractor.run();
	}

}
