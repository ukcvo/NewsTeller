package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.GroundTruth;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics.WordNetVerbCountDeterminer;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class FilteringBenchmark {

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
		Util.writeFeatureMapToFile(featureMap, featureNames, FEATURE_MAP_FILENAME);
	}
	
	// read feature values from file
	private void readFeatureMap() {
		this.featureMap = Util.readFeatureMapFromFile(FEATURE_MAP_FILENAME);		
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
		
		double overallPositiveProbability = (1.0 * positiveEvents.size()) / (positiveEvents.size() + negativeEvents.size());
		double overallNegativeProbability = 1.0 - overallPositiveProbability;
				
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
			
			Map<Integer,Map<String,Double>> probabilityMap = new HashMap<Integer, Map<String,Double>>();
			for (Integer value : possibleValues) {
				Map<String,Double> valueMap = new HashMap<String, Double>();
				
				int posCount = (posCounts.containsKey(value) ? posCounts.get(value) : 0);
				double posProbability = (posCount + 0.0) / (positiveEvents.size() + 0.0); //TODO: laplace smoothing? https://en.wikipedia.org/wiki/Additive_smoothing
				valueMap.put(Util.COLUMN_NAME_POSITIVE_PROBABILITY, posProbability);
				
				int negCount = (negCounts.containsKey(value) ? negCounts.get(value) : 0);
				double negProbability = (negCount + 0.0) / (negativeEvents.size() + 0.0);
				valueMap.put(Util.COLUMN_NAME_NEGATIVE_PROBABILITY, negProbability);
				
				double overallProbabiliy = (1.0 * (negCount + posCount)) / (positiveEvents.size() + negativeEvents.size());
				valueMap.put(Util.COLUMN_NAME_OVERALL_PROBABILITY, overallProbabiliy);
				
				probabilityMap.put(value, valueMap);
			}
						
			// entropy calculation
			double overallEntropy = 0;
			double posEntropy = 0;
			double negEntropy = 0;
			for (Map.Entry<Integer, Map<String,Double>> entry : probabilityMap.entrySet()) {
				double posProbability = entry.getValue().get(Util.COLUMN_NAME_POSITIVE_PROBABILITY);
				double negProbability = entry.getValue().get(Util.COLUMN_NAME_NEGATIVE_PROBABILITY);
				double overallProbablity = entry.getValue().get(Util.COLUMN_NAME_OVERALL_PROBABILITY);
				
				if (posProbability > 0)
					posEntropy -= posProbability * (Math.log(posProbability)/Math.log(2)); 
				if (negProbability > 0)
					negEntropy -= negProbability * (Math.log(negProbability)/Math.log(2));
				overallEntropy -= overallProbablity * (Math.log(overallProbablity)/Math.log(2));
			}
			double conditionalEntropy = overallPositiveProbability * posEntropy + overallNegativeProbability * negEntropy;
			
			// calculate Pearson correlation coefficient
			double averageLabel = overallPositiveProbability;
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
				Map<String,Double> valueMap = probabilityMap.get(value);
				if (valueMap.get(Util.COLUMN_NAME_POSITIVE_PROBABILITY) > valueMap.get(Util.COLUMN_NAME_NEGATIVE_PROBABILITY))
					tp++;
				else
					fn++;
			}
			for (String eventURI : negativeEvents) {
				int value = featureMap.get(eventURI).get(feature.getName());
				Map<String,Double> valueMap = probabilityMap.get(value);
				if (valueMap.get(Util.COLUMN_NAME_POSITIVE_PROBABILITY) > valueMap.get(Util.COLUMN_NAME_NEGATIVE_PROBABILITY))
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
			Util.writeProbabilityMapToFile(probabilityMap, fileName);
			
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
