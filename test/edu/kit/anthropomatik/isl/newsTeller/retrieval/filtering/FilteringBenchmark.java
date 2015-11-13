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
import edu.kit.anthropomatik.isl.newsTeller.retrieval.BenchmarkEvent;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.GroundTruth;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class FilteringBenchmark {

	private static Log log;

	private static final String FEATURE_MAP_FILENAME = "resources/benchmark/featureMap.csv";

	private boolean doWordNetAnalysis;

	private boolean doFeatureAnalysis;

	private boolean doCreateFeatureMap;

	private Map<String, List<Keyword>> benchmarkKeywords;

	private Map<BenchmarkEvent, GroundTruth> benchmark;

	private Set<BenchmarkEvent> positiveEvents;

	private Set<BenchmarkEvent> negativeEvents;

	private Set<BenchmarkEvent> allEvents;

	Map<BenchmarkEvent, Map<String, Integer>> featureMap;

	private KnowledgeStoreAdapter ksAdapter;

	private WordNetVerbCountFeature wordNetFeature;

	private List<UsabilityFeature> features;

	// region setters
	public void setDoWordNetAnalysis(boolean doWordNetAnalysis) {
		this.doWordNetAnalysis = doWordNetAnalysis;
	}

	public void setDoFeatureAnalysis(boolean doFeatureAnalysis) {
		this.doFeatureAnalysis = doFeatureAnalysis;
	}

	public void setDoCreateFeatureMap(boolean doCreateFeatureMap) {
		this.doCreateFeatureMap = doCreateFeatureMap;
	}

	public void setWordNetFeature(WordNetVerbCountFeature wordNetFeature) {
		this.wordNetFeature = wordNetFeature;
	}

	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}

	public void setFeatures(List<UsabilityFeature> features) {
		this.features = features;
	}
	// endregion

	public FilteringBenchmark(String configFileName) {
		this.benchmarkKeywords = Util.readBenchmarkConfigFile(configFileName);
		this.benchmark = new HashMap<BenchmarkEvent, GroundTruth>();
		this.positiveEvents = new HashSet<BenchmarkEvent>();
		this.negativeEvents = new HashSet<BenchmarkEvent>();

		for (String fileName : benchmarkKeywords.keySet()) {
			Map<BenchmarkEvent, GroundTruth> fileContent = Util.readBenchmarkQueryFromFile(fileName);
			this.benchmark.putAll(fileContent);
			for (Map.Entry<BenchmarkEvent, GroundTruth> entry : fileContent.entrySet()) {
				if (entry.getValue().getUsabilityRating() < Util.EPSILON) {
					negativeEvents.add(entry.getKey());
				} else {
					positiveEvents.add(entry.getKey());
				}

			}
		}

		this.allEvents = new HashSet<BenchmarkEvent>();
		this.allEvents.addAll(positiveEvents);
		this.allEvents.addAll(negativeEvents);
	}

	// region set up feature map
	// create feature map by querying the knowledge store and storing the result in a csv file
	private void createFeatureMap() {

		this.featureMap = new HashMap<BenchmarkEvent, Map<String, Integer>>();

		this.ksAdapter.openConnection();
		for (BenchmarkEvent event : this.allEvents) {
			Map<String, Integer> featureValues = new HashMap<String, Integer>();

			for (UsabilityFeature f : features) {
				featureValues.put(f.getName(), f.getValue(event.getEventURI()));
			}

			this.featureMap.put(event, featureValues);
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
	// endregion

	// region analyzeVerbNetFeature
	private void analyzeVerbNetFeature() {
		if (log.isTraceEnabled())
			log.trace("analyzeVerbNetFeature()");

		// collect labels
		Set<String> labels = new HashSet<String>();
		ksAdapter.openConnection();
		for (BenchmarkEvent event : this.benchmark.keySet()) {
			labels.addAll(ksAdapter.runSingleVariableStringQuery(
					Util.readStringFromFile("resources/SPARQL/usability/areLabelsVerbs.qry").replace("*e*", event.getEventURI()), 
					Util.VARIABLE_LABEL));
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
			
			// count #occurences for each feature value - separate for positive and negative training examples
			Map<Integer, Integer> positiveValueCounts = countValues(feature, positiveEvents);
			Map<Integer, Integer> negativeValueCounts = countValues(feature, negativeEvents);
			
			// collect set of all possible feature values observed in the training set
			Set<Integer> possibleValues = new HashSet<Integer>();
			possibleValues.addAll(positiveValueCounts.keySet());
			possibleValues.addAll(negativeValueCounts.keySet());

			// calculate the probability map based on the counts 
			// (contains overall probability and conditional probability for positive and negative examples)
			Map<Integer, Map<String, Double>> probabilityMap = createProbabilityMap(positiveValueCounts, negativeValueCounts, possibleValues);

			// calculate entropy
			double overallEntropy = calculateEntropy(probabilityMap, Util.COLUMN_NAME_OVERALL_PROBABILITY);
			double posEntropy = calculateEntropy(probabilityMap, Util.COLUMN_NAME_POSITIVE_PROBABILITY);
			double negEntropy = calculateEntropy(probabilityMap, Util.COLUMN_NAME_NEGATIVE_PROBABILITY);
			double conditionalEntropy = overallPositiveProbability * posEntropy + overallNegativeProbability * negEntropy;

			// calculate Pearson correlation coefficient
			double averageLabel = overallPositiveProbability;
			double averageFeature = calculateAverageFeatureValue(positiveValueCounts, negativeValueCounts, possibleValues);
			double correlation = calculateCorrelation(feature, averageLabel, averageFeature);

			// output metrics
			if (log.isInfoEnabled()) {
				log.info(String.format("feature: %s", feature.getName()));
				log.info(String.format("entropy: %f, condEntropy: %f", overallEntropy, conditionalEntropy));
				log.info(String.format("normalized entropy: %f, normalized condEntropy: %f", overallEntropy / possibleValues.size(), conditionalEntropy / possibleValues.size()));
				log.info(String.format("correlation: %f", correlation));
			}

			// do MLE prediction (does output internally)
			doMLEPrediction(feature, probabilityMap);
						
			// write probability map to file
			String fileName = String.format("csv-out/%s.csv", feature.getName());
			Util.writeProbabilityMapToFile(probabilityMap, fileName);
		}
	}

	//region helper methods
	// evaluates a single feature as MLE predictor; calculates precision, recall and f-score
	private void doMLEPrediction(UsabilityFeature feature, Map<Integer, Map<String, Double>> probabilityMap) {
		
		int tp = 0;
		int fp = 0;
		int fn = 0;

		for (BenchmarkEvent event : positiveEvents) {
			int value = featureMap.get(event).get(feature.getName());
			Map<String, Double> valueMap = probabilityMap.get(value);
			if (valueMap.get(Util.COLUMN_NAME_POSITIVE_PROBABILITY) > valueMap.get(Util.COLUMN_NAME_NEGATIVE_PROBABILITY))
				tp++;
			else
				fn++;
		}
		for (BenchmarkEvent event : negativeEvents) {
			int value = featureMap.get(event).get(feature.getName());
			Map<String, Double> valueMap = probabilityMap.get(value);
			if (valueMap.get(Util.COLUMN_NAME_POSITIVE_PROBABILITY) > valueMap.get(Util.COLUMN_NAME_NEGATIVE_PROBABILITY))
				fp++;
			// don't count tn, as we don't need them
		}

		double precision = (1.0 * tp) / (tp + fp);
		double recall = (1.0 * tp) / (tp + fn);
		double fscore = 2 * (precision * recall) / (precision + recall);
		if (log.isInfoEnabled())
			log.info(String.format("precision: %f, recall: %f, fscore: %f", precision, recall, fscore));
	}

	// calculates the correlation between feature and target
	private double calculateCorrelation(UsabilityFeature feature, double averageLabel, double averageFeature) {
		double nominator = 0;
		double denominatorFeature = 0;
		double denominatorLabel = 0;

		for (BenchmarkEvent event : positiveEvents) {
			int value = featureMap.get(event).get(feature.getName());
			nominator += (value - averageFeature) * (1.0 - averageLabel);
			denominatorFeature += Math.pow((value - averageFeature), 2);
			denominatorLabel += Math.pow((1.0 - averageLabel), 2);
		}
		for (BenchmarkEvent event : negativeEvents) {
			int value = featureMap.get(event).get(feature.getName());
			nominator += (value - averageFeature) * (0.0 - averageLabel);
			denominatorFeature += Math.pow((value - averageFeature), 2);
			denominatorLabel += Math.pow((0.0 - averageLabel), 2);
		}

		denominatorFeature = Math.sqrt(denominatorFeature);
		denominatorLabel = Math.sqrt(denominatorLabel);

		double correlation = nominator / (denominatorFeature * denominatorLabel);
		return correlation;
	}

	// calculates the average value of a feature, given the counts
	private double calculateAverageFeatureValue(Map<Integer, Integer> positiveValueCounts, Map<Integer, Integer> negativeValueCounts, Set<Integer> possibleValues) {
		double averageValue = 0;
		for (Integer value : possibleValues) {
			int posCount = (positiveValueCounts.containsKey(value) ? positiveValueCounts.get(value) : 0);
			int negCount = (negativeValueCounts.containsKey(value) ? negativeValueCounts.get(value) : 0);
			averageValue += (posCount + negCount) * value;
		}
		averageValue /= (positiveEvents.size() + negativeEvents.size());
		return averageValue;
	}

	// calculates the entropy for the given probability measure and the given probabilityMap
	private double calculateEntropy(Map<Integer, Map<String, Double>> probabilityMap, String probabilityName) {
		
		double entropy = 0;
		for (Map.Entry<Integer, Map<String, Double>> entry : probabilityMap.entrySet()) {
			double probability = entry.getValue().get(probabilityName);
			if (probability > 0)
				entropy -= probability * (Math.log(probability) / Math.log(2));
		}
		return entropy;
	}
	
	// counts the number of occurrences for the different feature values; returns a map "value --> count"
	private Map<Integer, Integer> countValues(UsabilityFeature feature, Set<BenchmarkEvent> events) {
		Map<Integer, Integer> posCounts = new HashMap<Integer, Integer>();
		for (BenchmarkEvent event : events) {
			int key = featureMap.get(event).get(feature.getName());
			int currentCount = posCounts.containsKey(key) ? posCounts.get(key) : 0;
			posCounts.put(key, currentCount + 1);
		}
		return posCounts;
	}

	// given the positive and negative counts, create a probability map "value --> (probabilityName --> probability)"
	private Map<Integer, Map<String, Double>> createProbabilityMap(Map<Integer, Integer> posCounts, Map<Integer, Integer> negCounts, 
																	Set<Integer> possibleValues) {
		
		Map<Integer, Map<String, Double>> probabilityMap = new HashMap<Integer, Map<String, Double>>();
		
		for (Integer value : possibleValues) {
			Map<String, Double> valueMap = new HashMap<String, Double>();

			int posCount = (posCounts.containsKey(value) ? posCounts.get(value) : 0);
			double posProbability = (posCount + 1.0) / (positiveEvents.size() + possibleValues.size());
			valueMap.put(Util.COLUMN_NAME_POSITIVE_PROBABILITY, posProbability);

			int negCount = (negCounts.containsKey(value) ? negCounts.get(value) : 0);
			double negProbability = (negCount + 1.0) / (negativeEvents.size() + possibleValues.size());
			valueMap.put(Util.COLUMN_NAME_NEGATIVE_PROBABILITY, negProbability);

			double overallProbabiliy = (1.0 * (negCount + posCount + 1.0)) / (positiveEvents.size() + negativeEvents.size() + possibleValues.size());
			valueMap.put(Util.COLUMN_NAME_OVERALL_PROBABILITY, overallProbabiliy);

			probabilityMap.put(value, valueMap);
		}
		
		return probabilityMap;
	}
	// endregion
	// endregion

	/**
	 * Runs the benchmark, depending on the boolean flags being set.
	 */
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
