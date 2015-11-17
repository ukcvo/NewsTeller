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
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.BenchmarkEvent;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.GroundTruth;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features.FeatureMapFeature;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features.UsabilityFeature;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class FilteringBenchmark {

	private static Log log;

	private static final String FEATURE_MAP_FILENAME = "resources/benchmark/featureMap.csv";

	private boolean useLogarithmicProbabilities;
	
	private boolean outputOnlyFalsePositives;
	
	private boolean doFeatureAnalysis;

	private boolean doCreateFeatureMap;

	private boolean doResubstitutionTest;
	
	private boolean doIndividualResubstitutionTests;
	
	private Map<String, List<Keyword>> benchmarkKeywords;

	private Map<BenchmarkEvent, GroundTruth> benchmark;

	private Set<BenchmarkEvent> positiveEvents;

	private Set<BenchmarkEvent> negativeEvents;

	private Set<BenchmarkEvent> allEvents;

	Map<BenchmarkEvent, Map<String, Integer>> featureMap;

	private KnowledgeStoreAdapter ksAdapter;

	private List<UsabilityFeature> features;

	private NaiveBayesFusion bayesFusion;
	
	// region setters
	public void setDoFeatureAnalysis(boolean doFeatureAnalysis) {
		this.doFeatureAnalysis = doFeatureAnalysis;
	}

	public void setDoCreateFeatureMap(boolean doCreateFeatureMap) {
		this.doCreateFeatureMap = doCreateFeatureMap;
	}
	
	public void setDoResubstitutionTest(boolean doResubstitutionTest) {
		this.doResubstitutionTest = doResubstitutionTest;
	}
	
	public void setDoIndividualResubstitutionTests(boolean doIndividualResubstitutionTests) {
		this.doIndividualResubstitutionTests = doIndividualResubstitutionTests;
	}
	
	public void setUseLogarithmicProbabilities(boolean useLogarithmicProbabilities) {
		this.useLogarithmicProbabilities = useLogarithmicProbabilities;
	}

	public void setOutputOnlyFalsePositives(boolean outputOnlyFalsePositives) {
		this.outputOnlyFalsePositives = outputOnlyFalsePositives;
	}
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}

	public void setFeatures(List<UsabilityFeature> features) {
		this.features = features;
	}
	
	public void setBayesFusion(NaiveBayesFusion bayesFusion) {
		this.bayesFusion = bayesFusion;
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

	// region analzyeFeatures
	private void analyzeFeatures() {

		double overallPositiveProbability = (1.0 * positiveEvents.size()) / (positiveEvents.size() + negativeEvents.size());
		double overallNegativeProbability = 1.0 - overallPositiveProbability;

		writePriorProbabilityMap(overallPositiveProbability, overallNegativeProbability, useLogarithmicProbabilities);
		
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
			Map<Integer, Map<String, Double>> probabilityMap = createProbabilityMap(positiveValueCounts, negativeValueCounts, 
																	positiveEvents.size(), negativeEvents.size(), possibleValues, false);

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
			Util.writeProbabilityMapToFile(probabilityMap, fileName, useLogarithmicProbabilities);
		}
	}

	// writes the given prior probabilities to a csv file
	private void writePriorProbabilityMap(double overallPositiveProbability, double overallNegativeProbability, boolean inLogProbabilities) {
		Map<String,Double> priorProbabilityMap = new HashMap<String, Double>();
		priorProbabilityMap.put(Util.COLUMN_NAME_POSITIVE_PROBABILITY, overallPositiveProbability);
		priorProbabilityMap.put(Util.COLUMN_NAME_NEGATIVE_PROBABILITY, overallNegativeProbability);
		priorProbabilityMap.put(Util.COLUMN_NAME_OVERALL_PROBABILITY, 1.0);
		Util.writePriorProbabilityMapToFile(priorProbabilityMap, "csv-out/priors.csv", inLogProbabilities);
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
																	int numberOfPosEvents, int numberOfNegEvents,
																	Set<Integer> possibleValues, boolean useLogProbabilities) {
		
		Map<Integer, Map<String, Double>> probabilityMap = new HashMap<Integer, Map<String, Double>>();
		
		for (Integer value : possibleValues) {
			Map<String, Double> valueMap = new HashMap<String, Double>();

			int posCount = (posCounts.containsKey(value) ? posCounts.get(value) : 0);
			double posProbability = (posCount + 1.0) / (numberOfPosEvents + possibleValues.size());
			valueMap.put(Util.COLUMN_NAME_POSITIVE_PROBABILITY, useLogProbabilities ? Math.log(posProbability): posProbability);

			int negCount = (negCounts.containsKey(value) ? negCounts.get(value) : 0);
			double negProbability = (negCount + 1.0) / (numberOfNegEvents + possibleValues.size());
			valueMap.put(Util.COLUMN_NAME_NEGATIVE_PROBABILITY, useLogProbabilities ? Math.log(negProbability) : negProbability);

			double overallProbabiliy = (1.0 * (negCount + posCount + 1.0)) / (numberOfPosEvents + numberOfNegEvents + possibleValues.size());
			valueMap.put(Util.COLUMN_NAME_OVERALL_PROBABILITY, useLogProbabilities ? Math.log(overallProbabiliy) : overallProbabiliy);

			probabilityMap.put(value, valueMap);
		}
		
		return probabilityMap;
	}
	// endregion
	// endregion

	private PerformanceMeasure runTest(Set<BenchmarkEvent> events, double[] thresholds) {

		int[] tp = new int[thresholds.length];
		int[] fp = new int[thresholds.length];
		int[] fn = new int[thresholds.length];
		double[] precision = new double[thresholds.length];
		double[] recall = new double[thresholds.length];
		double[] fscore = new double[thresholds.length];
		
		Set<String> falsePositiveURIs = new HashSet<String>();
		
		ksAdapter.openConnection();
		for (BenchmarkEvent event : events) {
			double probability = bayesFusion.getProbabilityOfEvent(new NewsEvent(event.getEventURI()));
			if (positiveEvents.contains(event)) {
				for (int i = 0; i < thresholds.length; i++) {
					if (probability >= thresholds[i])
						tp[i]++;
					else
						fn[i]++;
				}
			} else if (negativeEvents.contains(event)) {
				for (int i = 0; i < thresholds.length; i++) {
					if (probability >= thresholds[i]) {
						falsePositiveURIs.add(event.getEventURI());
						fp[i]++;
					}
					// ignore tn, as they are not used for precision and recall
				}
			}
		}
		ksAdapter.closeConnection();
		
		for (int i = 0; i < thresholds.length; i++) {
			precision[i] = (1.0 * tp[i]) / (tp[i] + fp[i]);
			recall[i] = (1.0 * tp[i]) / (tp[i] + fn[i]);
			fscore[i] = 2 * (precision[i] * recall[i]) / (precision[i] + recall[i]);
		}
		
		if (log.isInfoEnabled()) {
			if (this.outputOnlyFalsePositives) {
				for (String uri : falsePositiveURIs)
					log.info(uri);
			} else {
				for (int i = 0; i < thresholds.length; i++)
					log.info(String.format("THRESHOLD %f: precision: %f, recall: %f, fscore: %f", thresholds[i], precision[i], recall[i], fscore[i]));
			}
		}
		
		return new PerformanceMeasure(tp, fp, fn, precision, recall, fscore);		
	}
	
	// get the probabilities for a certain set of positive and negative events and a certain feature
	private Map<Integer, Map<String, Double>> estimateProbabilities(Set<BenchmarkEvent> positiveEvents, Set<BenchmarkEvent> negativeEvents, UsabilityFeature feature, boolean useLogProbabilities) {
	
		// count #occurences for each feature value - separate for positive and negative training examples
		Map<Integer, Integer> positiveValueCounts = countValues(feature, positiveEvents);
		Map<Integer, Integer> negativeValueCounts = countValues(feature, negativeEvents);
			
		// collect set of all possible feature values observed in the training set
		Set<Integer> possibleValues = new HashSet<Integer>();
		possibleValues.addAll(positiveValueCounts.keySet());
		possibleValues.addAll(negativeValueCounts.keySet());

		// calculate the probability map based on the counts 
		// (contains overall probability and conditional probability for positive and negative examples)
		Map<Integer, Map<String, Double>> probabilityMap = createProbabilityMap(positiveValueCounts, negativeValueCounts, 
																positiveEvents.size(), negativeEvents.size(), possibleValues, true);

		return probabilityMap;
	}
	
	// region resubstitutionTest
	// do a resubstitution test - use the extracted probabilities on overall set w/ different thresholds and see what you get
	private void resubstitutionTest(boolean isIndividualized) {
		
		double[] thresholds = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9};
		List<PerformanceMeasure> results = new ArrayList<PerformanceMeasure>();
		
		if (log.isInfoEnabled())
			log.info(isIndividualized ? "individualized resubstitution test" : "resubstitution test");
		
		for (String fileName : this.benchmarkKeywords.keySet()) {
			if (log.isInfoEnabled())
				log.info(fileName);
			
			Set<BenchmarkEvent> fileEvents = new HashSet<BenchmarkEvent>();
			for (BenchmarkEvent e : allEvents) {
				if (e.getFileName().equals(fileName))
					fileEvents.add(e);
			}
			
			Set<UsabilityFeature> fileFeatures = new HashSet<UsabilityFeature>();
			for (UsabilityFeature feature : this.features) {
				// set dummy features for speedup!
				UsabilityFeature newFeature = new FeatureMapFeature(this.featureMap, fileName, feature.getName());
				newFeature.setProbabilityMap(feature.getProbabilityMap());
				fileFeatures.add(newFeature); 
			}
					
			if (isIndividualized) {
				// recompute the probabilityMaps!
				Set<BenchmarkEvent> posEvents = new HashSet<BenchmarkEvent>();
				Set<BenchmarkEvent> negEvents = new HashSet<BenchmarkEvent>();
				for (BenchmarkEvent e : fileEvents) {
					if (positiveEvents.contains(e))
						posEvents.add(e);
					else if (negativeEvents.contains(e))
						negEvents.add(e);
				}			

				for (UsabilityFeature feature : fileFeatures) {
					feature.setProbabilityMap(estimateProbabilities(posEvents, negEvents, feature, true));
				}
				
				// also recompute the priors!
				Map<String, Double> priors = new HashMap<String, Double>();
				double posProb = (1.0 * posEvents.size()) / (posEvents.size() + negEvents.size());
				double negProb = 1 - posProb;
				priors.put(Util.COLUMN_NAME_POSITIVE_PROBABILITY, Math.log(posProb));
				priors.put(Util.COLUMN_NAME_NEGATIVE_PROBABILITY, Math.log(negProb));
				priors.put(Util.COLUMN_NAME_OVERALL_PROBABILITY, 0.0);
				bayesFusion.setPriorProbabilityMap(priors);
			}
			
			this.bayesFusion.setFeatures(fileFeatures);
			
			PerformanceMeasure p = runTest(fileEvents, thresholds);
			results.add(p);
		}
		
		//region aggregate results
		int[] overallTp = new int[thresholds.length];
		int[] overallFp = new int[thresholds.length];
		int[] overallFn = new int[thresholds.length];
		double[] averagePrecision = new double[thresholds.length];
		double[] averageRecall = new double[thresholds.length];
		double[] averageFscore = new double[thresholds.length];
		double[] overallPrecision = new double[thresholds.length];
		double[] overallRecall = new double[thresholds.length];
		double[] overallFscore = new double[thresholds.length];
		
		for (PerformanceMeasure p : results) {
			for (int i = 0; i < thresholds.length; i++) {
				overallTp[i] += p.getTp()[i];
				overallFp[i] += p.getFp()[i];
				overallFn[i] += p.getFn()[i];
				
				averagePrecision[i] += p.getPrecision()[i];
				averageRecall[i] += p.getRecall()[i];
				averageFscore[i] += p.getFscore()[i];
			}
		}
		
		for (int i = 0; i < thresholds.length; i++) {
			overallPrecision[i] = (1.0 * overallTp[i]) / (overallTp[i] + overallFp[i]);
			overallRecall[i] = (1.0 * overallTp[i]) / (overallTp[i] + overallFn[i]);
			overallFscore[i] = 2 * (overallPrecision[i] * overallRecall[i]) / (overallPrecision[i] + overallRecall[i]);
			
			averagePrecision[i] /= results.size();
			averageRecall[i] /= results.size();
			averageFscore[i] /= results.size();
		}
		//endregion
		
		if (log.isInfoEnabled()) {
			log.info("overall evaluation");
			for (int i = 0; i < thresholds.length; i++)
				log.info(String.format("THRESHOLD %f: precision: %f, recall: %f, fscore: %f", thresholds[i], overallPrecision[i], overallRecall[i], overallFscore[i]));
			log.info("average evaluation");
			for (int i = 0; i < thresholds.length; i++)
				log.info(String.format("THRESHOLD %f: precision: %f, recall: %f, fscore: %f (ref: %f)", thresholds[i], averagePrecision[i], averageRecall[i], averageFscore[i],
						2 * (averagePrecision[i] * averageRecall[i]) / (averagePrecision[i] + averageRecall[i])));
		}
	}
	// endregion
	
	/**
	 * Runs the benchmark, depending on the boolean flags being set.
	 */
	public void run() {
		if (this.doCreateFeatureMap)
			createFeatureMap();
		else
			readFeatureMap();

		if (this.doFeatureAnalysis)
			analyzeFeatures();
		if (this.doResubstitutionTest)
			resubstitutionTest(false);
		if (this.doIndividualResubstitutionTests)
			resubstitutionTest(true);
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
