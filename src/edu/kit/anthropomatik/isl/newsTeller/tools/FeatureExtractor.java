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
	
	public void setFeatures(List<UsabilityFeature> features) {
		this.features = features;
	}
	
	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
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
		classLabels.addElement("true");
		classLabels.addElement("false");
		attributes.addElement(new Attribute("usable", classLabels));
		
		attributes.addElement(new Attribute("eventURI", (FastVector) null));
		
		int numberOfExpectedExamples = this.benchmark.size();
		Instances dataSet = new Instances("usabilityTest", attributes, numberOfExpectedExamples);
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
		private int usabilityIndex;
		private int stringIndex;
		
		public EventWorker(String eventURI, int usabilityIndex, int stringIndex) {
			this.eventURI = eventURI;
			this.usabilityIndex = usabilityIndex;
			this.stringIndex = stringIndex;
		}
		
		public Instance call() throws Exception {
			
			double[] values = new double[features.size() + 2];
			
			for (int i = 0; i < features.size(); i++) {
				UsabilityFeature f = features.get(i);
				values[i] = f.getValue(this.eventURI);
			}
			
			values[values.length - 2] = this.usabilityIndex;
			values[values.length - 1] = this.stringIndex;
			
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
			String label = (entry.getValue().getUsabilityRating() == 1.0) ? "true" : "false";
			int usabilityIndex = dataSet.attribute("usable").indexOfValue(label);
			int stringIndex = dataSet.attribute("eventURI").addStringValue(eventURI);
			EventWorker w = new EventWorker(eventURI, usabilityIndex, stringIndex);
			
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
		FeatureExtractor test = (FeatureExtractor) context.getBean("featureExtractor");
		((AbstractApplicationContext) context).close();
		
		test.run();
	}

}