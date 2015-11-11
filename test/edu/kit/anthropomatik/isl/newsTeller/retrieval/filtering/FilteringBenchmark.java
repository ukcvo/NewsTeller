package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jumpmind.symmetric.csv.CsvWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.GroundTruth;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics.WordNetVerbCountDeterminer;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class FilteringBenchmark {

	private class DoubleTriple {
		public double first;
		public double second;
		public double third;
		
		public DoubleTriple(double first, double second, double third) {
			this.first = first;
			this.second = second;
			this.third = third;
		}
		
		@Override
		public String toString() {
			return String.format("%f;%f;%f", first, second, third);
		}
	}
	
	private static Log log;
	
	private boolean doWordNetAnalysis;
	
	private boolean doFeatureAnalysis;
	
	private WordNetVerbCountDeterminer wordNetFeature;
	
	private Map<String,List<Keyword>> benchmarkFiles;
	
	private Map<String,Map<String,GroundTruth>> benchmark;
	
	private Map<String,Set<String>> positiveEventsMap;
	
	private Set<String> positiveEvents;
	
	private Map<String,Set<String>> negativeEventsMap;
	
	private Set<String> negativeEvents;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	private List<UsabilityFeature> features;
	
	public void setDoWordNetAnalysis(boolean doWordNetAnalysis) {
		this.doWordNetAnalysis = doWordNetAnalysis;
	}
	
	public void setDoFeatureAnalysis(boolean doFeatureAnalysis) {
		this.doFeatureAnalysis = doFeatureAnalysis;
	}
	
	public void setWordNetFeature(WordNetVerbCountDeterminer wordNetFeature) {
		this.wordNetFeature = wordNetFeature;
	}
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public void setFeatures(List<UsabilityFeature> features) {
		this.features = features;
	}
	
	public FilteringBenchmark(String configFileName) {
		this.benchmarkFiles = Util.readBenchmarkConfigFile(configFileName);
		this.benchmark = new HashMap<String, Map<String,GroundTruth>>();
		this.positiveEventsMap = new HashMap<String, Set<String>>();
		this.positiveEvents = new HashSet<String>();
		this.negativeEventsMap = new HashMap<String, Set<String>>();
		this.negativeEvents = new HashSet<String>();
		
		for (String fileName : benchmarkFiles.keySet()) {
			Map<String,GroundTruth> fileContent = Util.readBenchmarkQueryFromFile(fileName);
			this.benchmark.put(fileName, fileContent);
			for (Map.Entry<String, GroundTruth> entry : fileContent.entrySet()) {
				if (entry.getValue().getUsabilityRating() < Util.EPSILON) {
					negativeEvents.add(entry.getKey());
				} else {
					positiveEvents.add(entry.getKey());
				}
					
			}
		}
	}
	
	//region analyzeVerbNetFeature
	private void analyzeVerbNetFeature() {
		if(log.isTraceEnabled())
			log.trace("analyzeVerbNetFeature()");
		
		// collect labels
		Set<String> labels = new HashSet<String>();
		ksAdapter.openConnection();
		for (Map<String,GroundTruth> line : this.benchmark.values()) {
			for (String eventURI : line.keySet()) {
				labels.addAll(ksAdapter.runSingleVariableStringQuery(
						Util.readStringFromFile("resources/SPARQL/usability/areLabelsVerbs.qry").replace("*e*", eventURI), Util.VARIABLE_LABEL));
			}
		}
		ksAdapter.closeConnection();
		
		// collect values
		List<Double> values = new ArrayList<Double>();
		for (String label : labels) {
			values.add(wordNetFeature.getLabelVerbFrequency(label));
		}
		
		// compute mean
		double meanSum = 0;
		for (Double val : values)
			meanSum += val;
		double mean = meanSum / values.size();
		
		// compute variance
		double varSum = 0;
		for (Double val : values)
			varSum += Math.pow((mean - val), 2);
		double variance = varSum / values.size();
		
		// compute bins
		int[] bins = new int[20];
		for (Double val : values)
			bins[(int)(Math.min(val, 1.0 - Util.EPSILON)*20)]++;
		
		// get counts for 0.0 and 1.0
		int numberOfZeroes = 0;
		int numberOfOnes = 0;
		for (Double val : values) {
			if ((1.0 - val) < Util.EPSILON)
				numberOfOnes++;
			if (val < Util.EPSILON)
				numberOfZeroes++;
		}
		
		// output everything
		if (log.isInfoEnabled()) {
			log.info(String.format("mean: %f, variance %f", mean, variance));
			log.info(String.format("total: %d, zeroes: %d, ones: %d", values.size(), numberOfZeroes, numberOfOnes));
			for (int i = 0; i < bins.length; i++)
				log.info(String.format("bin %d (%f - %f): %d", i, (i / 20.0), ((i + 1) / 20.0), bins[i]));
		}
	}
	//endregion
	
	//region analzyeFeatures
	private void analyzeFeatures() {
		
		double posProb = (1.0 * positiveEvents.size()) / (positiveEvents.size() + negativeEvents.size());
		double negProb = 1.0 - posProb;
		
		for (UsabilityFeature feature : this.features) {
			Map<Integer,Integer> posCounts = new HashMap<Integer, Integer>();
			ksAdapter.openConnection();
			for (String eventURI : positiveEvents) {
				int key = feature.getValue(eventURI);
				int currentCount = posCounts.containsKey(key) ? posCounts.get(key) : 0;
				posCounts.put(key, currentCount + 1);
			}
			
			Map<Integer,Integer> negCounts = new HashMap<Integer, Integer>();
			for (String eventURI : negativeEvents) {
				int key = feature.getValue(eventURI);
				int currentCount = negCounts.containsKey(key) ? negCounts.get(key) : 0;
				negCounts.put(key, currentCount + 1);
			}
			ksAdapter.closeConnection();
			Set<Integer> possibleValues = new HashSet<Integer>();
			possibleValues.addAll(posCounts.keySet());
			possibleValues.addAll(negCounts.keySet());
			
			Map<Integer,DoubleTriple> probabilityMap = new HashMap<Integer, DoubleTriple>();
			for (Integer value : possibleValues) {
				int posCount = (posCounts.containsKey(value) ? posCounts.get(value) : 0);
				double posProbability = (posCount + 0.0) / (positiveEvents.size() + 0.0); //TODO: laplace smoothing? https://en.wikipedia.org/wiki/Additive_smoothing
				int negCount = (negCounts.containsKey(value) ? negCounts.get(value) : 0);
				double negProbability = (negCount + 0.0) / (negativeEvents.size() + 0.0);
				double overallProbabiliy = (1.0 * (negCount + posCount)) / (positiveEvents.size() + negativeEvents.size());
				probabilityMap.put(value, new DoubleTriple(posProbability, negProbability, overallProbabiliy));
			}
			
			double overallEntropy = 0;
			double posEntropy = 0;
			double negEntropy = 0;
			for(Map.Entry<Integer, DoubleTriple> entry : probabilityMap.entrySet()) {
				if (entry.getValue().first > 0)
					posEntropy -= entry.getValue().first * (Math.log(entry.getValue().first)/Math.log(2)); 
				if (entry.getValue().second > 0)
					negEntropy -= entry.getValue().second * (Math.log(entry.getValue().second)/Math.log(2));
				overallEntropy -= entry.getValue().third * (Math.log(entry.getValue().third)/Math.log(2));
			}
			double conditionalEntropy = posProb * posEntropy + negProb * negEntropy;
			
			// do MLE prediction
			int tp = 0;
			int fp = 0;
			int tn = 0;
			int fn = 0;
			ksAdapter.openConnection();
			for (String eventURI : positiveEvents) {
				int value = feature.getValue(eventURI);
				DoubleTriple triple = probabilityMap.get(value);
				if(triple.first > triple.second)
					tp++;
				else
					fn++;
			}
			for (String eventURI : negativeEvents) {
				int value = feature.getValue(eventURI);
				DoubleTriple triple = probabilityMap.get(value);
				if(triple.first > triple.second)
					fp++;
				else
					tn++;
			}
			ksAdapter.closeConnection();
			double precision = (1.0 * tp) / (tp + fp);
			double recall = (1.0 * tp) / (tp + fn);
			double fscore = 2 * (precision * recall) / (precision + recall);
			
			if(log.isInfoEnabled()) {
				log.info(String.format("feature: %s", feature.getName()));
				log.info(String.format("entropy: %f, condEntropy: %f", overallEntropy, conditionalEntropy));
				log.info(String.format("precision: %f, recall: %f, fscore: %f", precision, recall, fscore));
			}
			
			String fileName = String.format("csv-out/%s.csv", feature.getName());
			try {
				CsvWriter w = new CsvWriter(new FileWriter(fileName, false), ';');
				w.write("value");
				w.write("posProb");
				w.write("negProb");
				w.write("overallProb");
				w.endRecord();
				
				for(Map.Entry<Integer, DoubleTriple> entry : probabilityMap.entrySet()) {
					w.write(entry.getKey().toString());
					w.write(Double.toString(entry.getValue().first));
					w.write(Double.toString(entry.getValue().second));
					w.write(Double.toString(entry.getValue().third));
					w.endRecord();
				}
				w.close();
			} catch (IOException e) {
				if(log.isErrorEnabled())
					log.error(String.format("cannot write file '%s'", fileName));
				if(log.isDebugEnabled())
					log.debug("csv write error", e);
			}
			
		}
	}
	//endregion
	
	public void run() {
		if (this.doWordNetAnalysis)
			analyzeVerbNetFeature();
		if (this.doFeatureAnalysis)
			analyzeFeatures();
	}
	
	public static void main(String[] args) {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(FilteringBenchmark.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		FilteringBenchmark benchmark = (FilteringBenchmark) context.getBean("filteringBenchmark");
		((AbstractApplicationContext) context).close();

		benchmark.run();
	}

}