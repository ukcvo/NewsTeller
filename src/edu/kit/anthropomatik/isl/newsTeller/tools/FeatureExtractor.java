package edu.kit.anthropomatik.isl.newsTeller.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import edu.kit.anthropomatik.isl.newsTeller.data.BenchmarkEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.GroundTruth;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features.UsabilityFeature;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.XRFFSaver;

/**
 * Extract features for all the benchmark events and store them in WEKA format for classifier training.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class FeatureExtractor {

	private static Log log = LogFactory.getLog(FeatureExtractor.class);
	
	private Map<String, List<Keyword>> benchmarkKeywords;	// file and corresponding keyword list
	
	private Map<BenchmarkEvent, GroundTruth> benchmark;	// event and corresponding labels / ratings
	
	private List<UsabilityFeature> features;
	
	private String outputFileName;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	private boolean doAddEventInformation;
	
	public void setFeatures(List<UsabilityFeature> features) {
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
	
	public FeatureExtractor(String configFileName) {
		this.benchmarkKeywords = Util.readBenchmarkConfigFile(configFileName);
		this.benchmark = new HashMap<BenchmarkEvent, GroundTruth>();

		for (String fileName : benchmarkKeywords.keySet()) {
			Map<BenchmarkEvent, GroundTruth> fileContent = Util.readBenchmarkQueryFromFile(fileName);
			this.benchmark.putAll(fileContent);
		}
	}

	private Instances createDataSetSkeleton() {
		FastVector attributes = new FastVector();
		
		for (UsabilityFeature feature : this.features) {
			// TODO: check for numeric vs. nominal & deal w/ it (FastVector labels = new FastVector(); Attribute attr= new Attribute(name,labels);)
			Attribute attr = new Attribute(feature.getName());
			attributes.addElement(attr);
		}
		
		FastVector classLabels = new FastVector();
		classLabels.addElement(Util.CLASS_LABEL_POSITIVE);
		classLabels.addElement(Util.CLASS_LABEL_NEGATIVE);
		attributes.addElement(new Attribute(Util.ATTRIBUTE_USABLE, classLabels));
		
		if (this.doAddEventInformation) {
			attributes.addElement(new Attribute(Util.ATTRIBUTE_URI, (FastVector) null));
			attributes.addElement(new Attribute(Util.ATTRIBUTE_FILE, (FastVector) null));
		}
		
		int numberOfExpectedExamples = this.benchmark.size();
		Instances dataSet = new Instances("usabilityTest", attributes, numberOfExpectedExamples);
		dataSet.setClass(dataSet.attribute(Util.ATTRIBUTE_USABLE));
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
	
	// takes care of extracting all features for one given event
	private class EventWorker implements Callable<Instance> {

		private String eventURI;
		private List<Keyword> keywords;
		private int usabilityIndex;
		private int uriIndex;
		private int fileIndex;
		
		public EventWorker(String eventURI, List<Keyword> keywords, int usabilityIndex, int uriIndex, int fileIndex) {
			this.eventURI = eventURI;
			this.keywords = keywords;
			this.usabilityIndex = usabilityIndex;
			this.uriIndex = uriIndex;
			this.fileIndex = fileIndex;
		}
		
		public Instance call() throws Exception {
			
			int numberOfAttributes = doAddEventInformation ? (features.size() + 3) : features.size();
			
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
			
			Instance example = new Instance(1.0, values);
			
			return example;
		}

		
	}
	
	public void run() {
		this.ksAdapter.openConnection();
		
		Instances dataSet = createDataSetSkeleton();
		
		ExecutorService threadPool = Executors.newFixedThreadPool(ksAdapter.getMaxNumberOfConnections());
		List<Future<Instance>> futures = new ArrayList<Future<Instance>>();
		
		for (Map.Entry<BenchmarkEvent, GroundTruth> entry : this.benchmark.entrySet()) {
			String eventURI = entry.getKey().getEventURI();
			List<Keyword> keywords = this.benchmarkKeywords.get(entry.getKey().getFileName());
			String fileName = entry.getKey().getFileName();
			String label = (entry.getValue().getUsabilityRating() == 1.0) ? Util.CLASS_LABEL_POSITIVE : Util.CLASS_LABEL_NEGATIVE;
			int usabilityIndex = dataSet.attribute(Util.ATTRIBUTE_USABLE).indexOfValue(label);
			int uriIndex = dataSet.attribute(Util.ATTRIBUTE_URI).addStringValue(eventURI);
			int fileIndex = dataSet.attribute(Util.ATTRIBUTE_FILE).addStringValue(fileName);
			EventWorker w = new EventWorker(eventURI, keywords, usabilityIndex, uriIndex, fileIndex);
			
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
				
		this.ksAdapter.closeConnection();
	}
	
	public static void main(String[] args) {
		System.setProperty("java.util.logging.config.file", "./config/logging.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(FeatureExtractor.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/default.xml");
		FeatureExtractor extractor = (FeatureExtractor) context.getBean("featureExtractor");
		((AbstractApplicationContext) context).close();
		
		extractor.run();
	}

}
