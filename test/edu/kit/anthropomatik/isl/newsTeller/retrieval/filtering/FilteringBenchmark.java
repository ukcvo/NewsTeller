package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.io.FileReader;
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
import org.jumpmind.symmetric.csv.CsvReader;
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

	private static final String FEATURE_MAP_FILENAME = "resources/benchmark/featureMap.csv";
	
	private boolean doWordNetAnalysis;

	private boolean doFeatureAnalysis;

	private boolean doCreateFeatureMap;
	
	private WordNetVerbCountDeterminer wordNetFeature;

	private Map<String, List<Keyword>> benchmarkFiles;

	private Map<String, Map<String, GroundTruth>> benchmark;

	private Map<String, Set<String>> positiveEventsMap;

	private Set<String> positiveEvents;

	private Map<String, Set<String>> negativeEventsMap;

	private Set<String> negativeEvents;

	private Set<String> allEvents;
	
	Map<String,Map<String,Integer>> featureMap;
	
	private KnowledgeStoreAdapter ksAdapter;

	private List<UsabilityFeature> features;

	//region setters
	public void setDoWordNetAnalysis(boolean doWordNetAnalysis) {
		this.doWordNetAnalysis = doWordNetAnalysis;
	}

	public void setDoFeatureAnalysis(boolean doFeatureAnalysis) {
		this.doFeatureAnalysis = doFeatureAnalysis;
	}

	public void setDoCreateFeatureMap(boolean doCreateFeatureMap) {
		this.doCreateFeatureMap = doCreateFeatureMap;
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
	//endregion
	
	public FilteringBenchmark(String configFileName) {
		this.benchmarkFiles = Util.readBenchmarkConfigFile(configFileName);
		this.benchmark = new HashMap<String, Map<String, GroundTruth>>();
		this.positiveEventsMap = new HashMap<String, Set<String>>();
		this.positiveEvents = new HashSet<String>();
		this.negativeEventsMap = new HashMap<String, Set<String>>();
		this.negativeEvents = new HashSet<String>();

		for (String fileName : benchmarkFiles.keySet()) {
			Map<String, GroundTruth> fileContent = Util.readBenchmarkQueryFromFile(fileName);
			this.benchmark.put(fileName, fileContent);
			this.positiveEventsMap.put(fileName, new HashSet<String>());
			this.negativeEventsMap.put(fileName, new HashSet<String>());
			for (Map.Entry<String, GroundTruth> entry : fileContent.entrySet()) {
				if (entry.getValue().getUsabilityRating() < Util.EPSILON) {
					negativeEvents.add(entry.getKey());
					negativeEventsMap.get(fileName).add(entry.getKey());
				} else {
					positiveEvents.add(entry.getKey());
					positiveEventsMap.get(fileName).add(entry.getKey());
				}

			}
		}
		
		this.allEvents = new HashSet<String>();
		this.allEvents.addAll(positiveEvents);
		this.allEvents.addAll(negativeEvents);
	}

	//region set up feature map
	// create feature map by querying the knowledge store and storing the result in a csv file
	private void createFeatureMap() {
		
		this.featureMap = new HashMap<String, Map<String,Integer>>();
				
		this.ksAdapter.openConnection();
		for (String eventURI : this.allEvents) {
			Map<String, Integer> featureValues = new HashMap<String, Integer>();
			
			for (UsabilityFeature f : features) {
				featureValues.put(f.getName(), f.getValue(eventURI));
			}
			
			this.featureMap.put(eventURI, featureValues);
		}
		this.ksAdapter.closeConnection();
		
		List<String> featureNames = new ArrayList<String>();
		for (UsabilityFeature f : features) {
			featureNames.add(f.getName());
		}
		
		// write to file
		try {
			CsvWriter w = new CsvWriter(new FileWriter(FEATURE_MAP_FILENAME, false), ';');
			w.write("eventURI");
			for (String s : featureNames)
				w.write(s);
			w.endRecord();
			
			for(Map.Entry<String, Map<String,Integer>> entry : featureMap.entrySet()) {
				w.write(entry.getKey());
				for (String s: featureNames)
					w.write(entry.getValue().get(s).toString());
				w.endRecord();
			}
			
			w.close();
		} catch (IOException e) {
			if(log.isErrorEnabled())
				log.error(String.format("cannot write file '%s'", FEATURE_MAP_FILENAME));
			if(log.isDebugEnabled())
				log.debug("csv write error", e);
		}
	}
	
	// read feature values from file
	private void readFeatureMap() {
		this.featureMap = new HashMap<String, Map<String,Integer>>();
		
		try {
			CsvReader r = new CsvReader(new FileReader(FEATURE_MAP_FILENAME), ';');
			
			r.readHeaders();
			List<String> featureNames = new ArrayList<String>();
			for (int i = 1; i < r.getHeaderCount(); i++)
				featureNames.add(r.getHeader(i));
			
			while(r.readRecord()) {
				String eventURI = r.get("eventURI");
				Map<String,Integer> featureValues = new HashMap<String, Integer>();
				for (String s : featureNames)
					featureValues.put(s, Integer.parseInt(r.get(s)));
				featureMap.put(eventURI, featureValues);
			}
			
			r.close();
			
		} catch (IOException e) {
			if(log.isFatalEnabled())
				log.fatal(String.format("cannot write file '%s'", FEATURE_MAP_FILENAME));
			if(log.isDebugEnabled())
				log.debug("csv write error", e);
		}
	}
	//endregion
	
	// region analyzeVerbNetFeature
	private void analyzeVerbNetFeature() {
		if (log.isTraceEnabled())
			log.trace("analyzeVerbNetFeature()");

		// collect labels
		Set<String> labels = new HashSet<String>();
		ksAdapter.openConnection();
		for (Map<String, GroundTruth> line : this.benchmark.values()) {
			for (String eventURI : line.keySet()) {
				labels.addAll(ksAdapter.runSingleVariableStringQuery(Util.readStringFromFile("resources/SPARQL/usability/areLabelsVerbs.qry").replace("*e*", eventURI), Util.VARIABLE_LABEL));
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
			bins[(int) (Math.min(val, 1.0 - Util.EPSILON) * 20)]++;

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
	// endregion

	// region analzyeFeatures
	private void analyzeFeatures() {
		
		double posProb = (1.0 * positiveEvents.size()) / (positiveEvents.size() + negativeEvents.size());
		double negProb = 1.0 - posProb;
				
		for (UsabilityFeature feature : this.features) {
			Map<Integer,Integer> posCounts = new HashMap<Integer, Integer>();

			for (String eventURI : positiveEvents) {
				int key = featureMap.get(eventURI).get(feature.getName());
				int currentCount = posCounts.containsKey(key) ? posCounts.get(key) : 0;
				posCounts.put(key, currentCount + 1);
			}
			
			Map<Integer,Integer> negCounts = new HashMap<Integer, Integer>();
			for (String eventURI : negativeEvents) {
				int key = featureMap.get(eventURI).get(feature.getName());
				int currentCount = negCounts.containsKey(key) ? negCounts.get(key) : 0;
				negCounts.put(key, currentCount + 1);
			}
			
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
						
			// entropy calculation
			double overallEntropy = 0;
			double posEntropy = 0;
			double negEntropy = 0;
			for (Map.Entry<Integer, DoubleTriple> entry : probabilityMap.entrySet()) {
				if (entry.getValue().first > 0)
					posEntropy -= entry.getValue().first * (Math.log(entry.getValue().first)/Math.log(2)); 
				if (entry.getValue().second > 0)
					negEntropy -= entry.getValue().second * (Math.log(entry.getValue().second)/Math.log(2));
				overallEntropy -= entry.getValue().third * (Math.log(entry.getValue().third)/Math.log(2));
			}
			double conditionalEntropy = posProb * posEntropy + negProb * negEntropy;
			
			// calculate Pearson correlation coefficient
			double averageLabel = posProb;
			double averageFeature = 0;
			for (Integer value : possibleValues) {
				int posCount = (posCounts.containsKey(value) ? posCounts.get(value) : 0);
				int negCount = (negCounts.containsKey(value) ? negCounts.get(value) : 0);
				averageFeature += (posCount + negCount)*value;
			}
			averageFeature /= (positiveEvents.size() + negativeEvents.size());

			double nominator = 0;
			double denominatorFeature = 0;
			double denominatorLabel = 0;
			
			for (String eventURI : positiveEvents) {
				int value = featureMap.get(eventURI).get(feature.getName());
				nominator += (value - averageFeature)*(1.0 - averageLabel);
				denominatorFeature += Math.pow((value - averageFeature), 2);
				denominatorLabel += Math.pow((1.0 - averageLabel),2);
			}
			for (String eventURI : negativeEvents) {
				int value = featureMap.get(eventURI).get(feature.getName());
				nominator += (value - averageFeature)*(0.0 - averageLabel);
				denominatorFeature += Math.pow((value - averageFeature), 2);
				denominatorLabel += Math.pow((0.0 - averageLabel),2);
			}
			
			denominatorFeature = Math.sqrt(denominatorFeature);
			denominatorLabel = Math.sqrt(denominatorLabel);
			
			double correlation = nominator / (denominatorFeature * denominatorLabel);
			
			// do MLE prediction
			int tp = 0;
			int fp = 0;
			int fn = 0;
			
			for (String eventURI : positiveEvents) {
				int value = featureMap.get(eventURI).get(feature.getName());
				DoubleTriple triple = probabilityMap.get(value);
				if(triple.first > triple.second)
					tp++;
				else
					fn++;
			}
			for (String eventURI : negativeEvents) {
				int value = featureMap.get(eventURI).get(feature.getName());
				DoubleTriple triple = probabilityMap.get(value);
				if(triple.first > triple.second)
					fp++;
				// don't count tn, as we don't need them 
			}

			double precision = (1.0 * tp) / (tp + fp);
			double recall = (1.0 * tp) / (tp + fn);
			double fscore = 2 * (precision * recall) / (precision + recall);
			
			if(log.isInfoEnabled()) {
				log.info(String.format("feature: %s", feature.getName()));
				log.info(String.format("entropy: %f, condEntropy: %f", overallEntropy, conditionalEntropy));
				log.info(String.format("normalized entropy: %f, normalized condEntropy: %f", overallEntropy/possibleValues.size(), conditionalEntropy/possibleValues.size()));
				log.info(String.format("correlation: %f", correlation));
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
	// endregion
	
	public void run() {
		if (this.doCreateFeatureMap)
			createFeatureMap();
		else
			readFeatureMap();
		
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
