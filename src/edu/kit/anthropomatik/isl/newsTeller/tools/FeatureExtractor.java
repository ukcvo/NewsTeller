package edu.kit.anthropomatik.isl.newsTeller.tools;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	
	public void run() {
		this.ksAdapter.openConnection();
		
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
		
		int numberOfExpectedExamples = this.benchmark.size();
		Instances dataSet = new Instances("usabilityTest", attributes, numberOfExpectedExamples);
		
		for (Map.Entry<BenchmarkEvent, GroundTruth> entry : this.benchmark.entrySet()) {
			if (log.isInfoEnabled())
				log.info(entry.getKey().toString());
			
			double[] values = new double[dataSet.numAttributes()];
			
			for (int i = 0; i < this.features.size(); i++) {
				UsabilityFeature f = this.features.get(i);
				values[i] = f.getValue(entry.getKey().getEventURI());
			}
			
			String label = (entry.getValue().getUsabilityRating() == 1.0) ? "true" : "false";
			values[values.length-1] = dataSet.attribute("usable").indexOfValue(label);
			
			Instance example = new Instance(1.0, values);
			dataSet.add(example);
		}
		
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
