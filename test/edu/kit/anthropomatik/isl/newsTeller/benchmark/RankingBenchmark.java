package edu.kit.anthropomatik.isl.newsTeller.benchmark;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;

import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import weka.attributeSelection.AttributeSelection;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.evaluation.Prediction;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Randomizable;
import weka.core.SerializationHelper;
import weka.core.Utils;
import weka.core.converters.XRFFLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.RemoveByName;
import weka.filters.unsupervised.attribute.RemoveType;
import weka.filters.unsupervised.attribute.StringToNominal;
import weka.filters.unsupervised.instance.RemoveWithValues;

public class RankingBenchmark {

	private static Log log = LogFactory.getLog(RankingBenchmark.class);

	private Instances dataSet;

	private Classifier pretrainedRegressor;
	
	private Map<String, Classifier> regressors;

	private List<AttributeSelection> featureSelectors;

	private String groundTruthAttributes;

	private int numberOfGroundTruthAttributes;

	private List<Integer> featureIndices;

	private List<Integer> newFeatureIndices;

	private String outputFileName;

	private int minimumNumberOfFeatures;

	private boolean doQueryBasedRegression;

	private boolean doFeatureSelection;

	private boolean doSearchBestNDCGFeature;

	private boolean doFeatureElimination;

	private boolean doRecursiveFeatureElimination;

	private boolean doFeatureAddition;

	private boolean doTestPretrainedRegressor;
	
	private boolean doUserBasedRegression;
	
	private boolean outputRankings;

	// region setters
	public void setRegressors(Map<String, Classifier> regressors) {
		this.regressors = regressors;
	}

	public void setFeatureSelectors(List<AttributeSelection> featureSelectors) {
		this.featureSelectors = featureSelectors;
	}

	public void setGroundTruthAttributes(String groundTruthAttributes) {
		this.groundTruthAttributes = groundTruthAttributes;
		this.numberOfGroundTruthAttributes = StringUtils.commaDelimitedListToStringArray(groundTruthAttributes).length;
	}

	public void setFeatureIndices(String featureIndices) {
		String[] array = StringUtils.commaDelimitedListToStringArray(featureIndices);
		this.featureIndices = new ArrayList<Integer>(array.length);
		for (String s : array)
			this.featureIndices.add(Integer.parseInt(s));
	}

	public void setNewFeatureIndices(String newFeatureIndices) {
		String[] array = StringUtils.commaDelimitedListToStringArray(newFeatureIndices);
		this.newFeatureIndices = new ArrayList<Integer>(array.length);
		for (String s : array)
			this.newFeatureIndices.add(Integer.parseInt(s));
	}

	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}

	public void setMinimumNumberOfFeatures(int minimumNumberOfFeatures) {
		this.minimumNumberOfFeatures = minimumNumberOfFeatures;
	}

	public void setDoQueryBasedRegression(boolean doQueryBasedRegression) {
		this.doQueryBasedRegression = doQueryBasedRegression;
	}

	public void setDoFeatureSelection(boolean doFeatureSelection) {
		this.doFeatureSelection = doFeatureSelection;
	}

	public void setDoSearchBestNDCGFeature(boolean doSearchBestNDCGFeature) {
		this.doSearchBestNDCGFeature = doSearchBestNDCGFeature;
	}

	public void setDoFeatureElimination(boolean doFeatureElimination) {
		this.doFeatureElimination = doFeatureElimination;
	}

	public void setDoRecursiveFeatureElimination(boolean doRecursiveFeatureElimination) {
		this.doRecursiveFeatureElimination = doRecursiveFeatureElimination;
	}

	public void setDoFeatureAddition(boolean doFeatureAddition) {
		this.doFeatureAddition = doFeatureAddition;
	}

	public void setDoTestPretrainedRegressor(boolean doTestPretrainedRegressor) {
		this.doTestPretrainedRegressor = doTestPretrainedRegressor;
	}
	
	public void setDoUserBasedRegression(boolean doUserBasedRegression) {
		this.doUserBasedRegression = doUserBasedRegression;
	}
	
	public void setOutputRankings(boolean outputRankings) {
		this.outputRankings = outputRankings;
	}
	// endregion
	
	public RankingBenchmark(String dataSetFileName, String pretrainedRegressorFileName) {
		try {
			XRFFLoader loader = new XRFFLoader();
			loader.setSource(new File(dataSetFileName));
			this.dataSet = loader.getDataSet();
		} catch (IOException e) {
			if (log.isErrorEnabled())
				log.error("Can't read data set");
			if (log.isDebugEnabled())
				log.debug("Can't read data set", e);
		}
		
		try {
			Object[] input = SerializationHelper.readAll(pretrainedRegressorFileName);
			this.pretrainedRegressor = (Classifier) input[0];
			//this.pretrainedRegressorHeader = (Instances) input[1];
		} catch (Exception e) {
			if (log.isFatalEnabled())
				log.fatal(String.format("Can't read classifier from file: '%s'", pretrainedRegressorFileName));
			if (log.isDebugEnabled())
				log.debug("can't read classifier from file", e);
		}
	}

	// region featureSelection
	private void featureSelection() {

		try {
			RemoveType stringFilter = new RemoveType();
			stringFilter.setOptions(Utils.splitOptions("-T string"));
			stringFilter.setInputFormat(this.dataSet);
			Instances filtered = Filter.useFilter(this.dataSet, stringFilter);

			for (AttributeSelection config : this.featureSelectors) {
				config.SelectAttributes(filtered);
				if (log.isInfoEnabled())
					log.info(config.toResultsString());
			}

		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("Can't select features");
			if (log.isDebugEnabled())
				log.debug("Exception", e);
		}
	}
	// endregion

	// region queryBasedRegression
	private void queryBasedRegression() {

		Map<String, Map<String, Double>> resultMap = new HashMap<String, Map<String, Double>>();

		for (Map.Entry<String, Classifier> entry : this.regressors.entrySet()) {
			String regressorName = entry.getKey();
			Classifier regressor = entry.getValue();
			resultMap.put(regressorName, queryBasedRegressionOneRegressor(this.dataSet, regressor, regressorName));
		}
		
		List<String> columnNames = Lists.newArrayList("RMSE", "correlation", "NDCG", "avg top 1", "avg top 1 norm", ">0 precision @1", ">0 precision @1 norm", 
														">1 precision @1", ">1 precision @1 norm");
		Util.writeEvaluationToCsv(this.outputFileName, columnNames, resultMap);

	}

	private Map<String, Double> queryBasedRegressionOneRegressor(Instances dataSet) {
		if (this.regressors.size() != 1)
			return new HashMap<String, Double>();
		String regressorName = this.regressors.keySet().toArray(new String[1])[0];
		Classifier regressor = this.regressors.get(regressorName);
		return queryBasedRegressionOneRegressor(dataSet, regressor, regressorName);
	}
	
	private Map<String, Double> queryBasedRegressionOneRegressor(Instances dataSet, Classifier regressor, String regressorName) {

		Map<String, Double> resultMap = new HashMap<String, Double>();

		try {

			StringToNominal filter = new StringToNominal();
			int attributeIdx = dataSet.attribute(Util.ATTRIBUTE_FILE).index() + 1; // conversion from 0-based to 1-based indices...
			filter.setAttributeRange(Integer.toString(attributeIdx));
			filter.setInputFormat(dataSet);
			Instances modifiedDataSet = Filter.useFilter(dataSet, filter);

			double rmse = 0;
			double correlation = 0;
			double ndcg = 0;
			double relativeTopRank = 0;
			double topRank = 0;
			double precision0At1 = 0;
			double precision0At1Norm = 0;
			double precision1At1 = 0;
			double precision1At1Norm = 0;
			
			List<Classifier> regressorsToAverage = new ArrayList<Classifier>();
			if (regressor instanceof Randomizable) { 
				// randomizable classifier, so take average over run with 10 different deterministic seeds
				Random rand = new Random(1);
				for (int i = 0; i < 10; i++) {
					Classifier copy = AbstractClassifier.makeCopy(regressor);
					((Randomizable) copy).setSeed(rand.nextInt());
					regressorsToAverage.add(copy);
				}
			} else {
				// not randomizable, so only do work once
				regressorsToAverage.add(AbstractClassifier.makeCopy(regressor));
			}
			
			for (Classifier r : regressorsToAverage) {
				
				Evaluation eval = new Evaluation(modifiedDataSet);
				double regNDCG = 0;
				int regNdcgNaNs = 0;
				double regRelativeTopRank = 0;
				int regRelativeTopRankNaNs = 0;
				double regTopRank = 0;
				double regPrecision0At1 = 0;
				double regPrecision0At1Norm = 0;
				int regPrecision0At1NormNaNs = 0;
				double regPrecision1At1 = 0;
				double regPrecision1At1Norm = 0;
				int regPrecision1At1NormNaNs = 0;
				
				Enumeration<Object> enumeration = modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).enumerateValues();
				while (enumeration.hasMoreElements()) {

					Evaluation evalLocal = new Evaluation(modifiedDataSet);
					String fileName = (String) enumeration.nextElement();
					Integer idx = modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).indexOfValue(fileName) + 1;

					Instances train = filterByFileName(modifiedDataSet, idx, false);
					Instances test = filterByFileName(modifiedDataSet, idx, true);
					
					r.buildClassifier(train);
					eval.evaluateModel(r, test);
					evalLocal.evaluateModel(r, test);

					double localNDCG = computeNDCG(evalLocal.predictions());
					if (Double.isNaN(localNDCG))
						regNdcgNaNs = 1;
					else
						regNDCG += localNDCG;
					regTopRank += computeTopRank(evalLocal.predictions(), false, 1);
					double localRelativeTopRank = computeTopRank(evalLocal.predictions(), true, 1);
					if (Double.isNaN(localRelativeTopRank))
						regRelativeTopRankNaNs++;
					else
						regRelativeTopRank += localRelativeTopRank;
					
					regPrecision0At1 += computePrecision(evalLocal.predictions(), 1, 0, false);
					double localPrecision0At1Norm = computePrecision(evalLocal.predictions(), 1, 0, true);
					if (Double.isNaN(localPrecision0At1Norm))
						regPrecision0At1NormNaNs++;
					else
						regPrecision0At1Norm += localPrecision0At1Norm;
					
					regPrecision1At1 += computePrecision(evalLocal.predictions(), 1, 1, false);
					double localPrecision1At1Norm = computePrecision(evalLocal.predictions(), 1, 1, true);
					if (Double.isNaN(localPrecision1At1Norm))
						regPrecision1At1NormNaNs++;
					else
						regPrecision1At1Norm += localPrecision1At1Norm;
					
					if (this.outputRankings && log.isInfoEnabled())
						logRankings(r, test);
				}
				if (this.outputRankings && log.isInfoEnabled())
					log.info("-----------------------------");
				rmse += eval.rootMeanSquaredError();
				
				correlation += eval.correlationCoefficient();
				
				regNDCG /= (modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues() - regNdcgNaNs);
				ndcg += regNDCG;
				
				regRelativeTopRank /= (modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues() - regRelativeTopRankNaNs);
				relativeTopRank += regRelativeTopRank;
				
				regTopRank /= modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
				topRank += regTopRank;
				
				regPrecision0At1 /= modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
				precision0At1 += regPrecision0At1;
				
				regPrecision0At1Norm /= (modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues() - regPrecision0At1NormNaNs);
				precision0At1Norm += regPrecision0At1Norm;
				
				regPrecision1At1 /= modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
				precision1At1 += regPrecision1At1;
				
				regPrecision1At1Norm /= (modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues() - regPrecision1At1NormNaNs);
				precision1At1Norm += regPrecision1At1Norm;
			}

			rmse /= regressorsToAverage.size();
			correlation /= regressorsToAverage.size();
			ndcg /= regressorsToAverage.size();
			topRank /= regressorsToAverage.size();
			relativeTopRank /= regressorsToAverage.size();
			precision0At1 /= regressorsToAverage.size();
			precision0At1Norm /= regressorsToAverage.size();
			precision1At1 /= regressorsToAverage.size();
			precision1At1Norm /= regressorsToAverage.size();
			
			resultMap.put("RMSE", rmse);
			resultMap.put("correlation", correlation);
			resultMap.put("NDCG", ndcg);
			resultMap.put("avg top 1", topRank);
			resultMap.put("avg top 1 norm", relativeTopRank);
			resultMap.put(">0 precision @1", precision0At1);
			resultMap.put(">0 precision @1 norm", precision0At1Norm);
			resultMap.put(">1 precision @1", precision1At1);
			resultMap.put(">1 precision @1 norm", precision1At1Norm);
			
			if (log.isInfoEnabled()) {
				log.info(String.format("%s (query-based)", regressorName));
				logEvalResults(rmse, correlation, ndcg, topRank, relativeTopRank, precision0At1, precision0At1Norm, precision1At1, precision1At1Norm);
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("cannot perform query-based regression!");
			if (log.isDebugEnabled())
				log.debug("cannot perform query-based regression", e);
		}

		return resultMap;
	}

	private class RankEntry {
		public String eventURI;
		public double actual;
		public double predicted;

		public RankEntry(String eventURI, double actual, double predicted) {
			this.eventURI = eventURI;
			this.actual = actual;
			this.predicted = predicted;
		}

		@Override
		public String toString() {
			return String.format("\"%s\";%f;%f", eventURI, Util.regressionValueToRank(predicted), Util.regressionValueToRank(actual));
		}
	}

	// log the rankings to the console
	private void logRankings(Classifier regressor, Instances testData) {

		try {
			List<RankEntry> ranking = new ArrayList<RankEntry>();
			for (Instance instance : testData) {
				String eventURI = instance.stringValue(testData.attribute(Util.ATTRIBUTE_URI));
				double actual = instance.classValue();
				double predicted = regressor.classifyInstance(instance);
				ranking.add(new RankEntry(eventURI, actual, predicted));
			}

			Collections.sort(ranking, new Comparator<RankEntry>() {
				@Override
				public int compare(RankEntry o1, RankEntry o2) {
					return (-1) * Double.compare(o1.predicted, o2.predicted);
				}
			});

			log.info(testData.get(0).stringValue(testData.attribute(Util.ATTRIBUTE_FILE)));
			log.info("event;predicted;actual");
			for (RankEntry e : ranking)
				log.info(e.toString());
			log.info("\n");

		} catch (Exception e) {
			if (log.isWarnEnabled())
				log.warn("Cannot output ranking");
			if (log.isDebugEnabled())
				log.debug("Cannot output ranking", e);
		}

	}

	// computes the average rank of the n highest-ranked events (optionally
	// normalized by maximum attainable for the given query)
	private double computeTopRank(List<Prediction> predictions, boolean shouldBeRelative, int n) {

		List<Prediction> sortedByPrediction = new ArrayList<Prediction>(predictions);
		Collections.sort(sortedByPrediction, new Comparator<Prediction>() {
			@Override
			public int compare(Prediction o1, Prediction o2) {
				return (-1) * Double.compare(o1.predicted(), o2.predicted());
			}
		});

		double maxActualValue = 0;
		double topAvgValue = 0;

		for (int i = 0; i < sortedByPrediction.size(); i++) {
			Prediction p = sortedByPrediction.get(i);
			if (Util.regressionValueToRank(p.actual()) > maxActualValue)
				maxActualValue = Util.regressionValueToRank(p.actual());
			if (i < n)
				topAvgValue += Util.regressionValueToRank(p.actual());
		}

		topAvgValue /= Math.min(n, sortedByPrediction.size());

		return shouldBeRelative ? topAvgValue / maxActualValue : topAvgValue;
	}

	private double computeNDCG(List<Prediction> predictions) {

		List<Prediction> sortedByGroundTruth = new ArrayList<Prediction>(predictions);
		Collections.sort(sortedByGroundTruth, new Comparator<Prediction>() {
			@Override
			public int compare(Prediction o1, Prediction o2) {
				return (-1) * Double.compare(o1.actual(), o2.actual());
			}
		});
		List<Prediction> sortedByPrediction = new ArrayList<Prediction>(predictions);
		Collections.sort(sortedByPrediction, new Comparator<Prediction>() {
			@Override
			public int compare(Prediction o1, Prediction o2) {
				return (-1) * Double.compare(o1.predicted(), o2.predicted());
			}
		});

		double idealDCG = 0;
		for (int i = 0; i < sortedByGroundTruth.size(); i++) {
			int r = i + 1; // rank starts counting at 1, not at 0
			Prediction p = sortedByGroundTruth.get(i);
			idealDCG += p.actual() / (Util.log2(r + 1));
		}

		double rawDCG = 0;
		for (int i = 0; i < sortedByPrediction.size(); i++) {
			int r = i + 1;
			Prediction p = sortedByPrediction.get(i);
			rawDCG += p.actual() / (Util.log2(r + 1));
		}

		double nDCG = rawDCG / idealDCG;
		return nDCG;
	}

	private double computePrecision(List<Prediction> predictions, int n, double threshold, boolean normalize) {
		List<Prediction> sortedByPrediction = new ArrayList<Prediction>(predictions);
		Collections.sort(sortedByPrediction, new Comparator<Prediction>() {
			@Override
			public int compare(Prediction o1, Prediction o2) {
				return (-1) * Double.compare(o1.predicted(), o2.predicted());
			}
		});

		int k = Math.min(n, sortedByPrediction.size());
		double positive = 0;
		for (int i = 0; i < k; i++) {
			if (Util.regressionValueToRank(sortedByPrediction.get(i).actual()) > threshold)
				positive++;
		}

		double result = positive / k;
		if (normalize) {
			double maxAttainable = 0;
			for (Prediction p : sortedByPrediction) {
				if (p.actual() > maxAttainable)
					maxAttainable = Util.regressionValueToRank(p.actual());
			}
			
			if (maxAttainable <= threshold) // e.g. if normalizing precision>0 and there is nothing > 0: return NaN
				result = Double.NaN;
		}
		
		return result;
	}

	private Instances filterByFileName(Instances dataSet, Integer fileNameIdx, boolean isTest) throws Exception {

		RemoveWithValues filter = new RemoveWithValues();
		int attributeIdx = dataSet.attribute(Util.ATTRIBUTE_FILE).index() + 1; // conversion from 0-based to 1-based indices...
		filter.setAttributeIndex(Integer.toString(attributeIdx));
		//filter.setAttributeIndex("last");
		filter.setNominalIndices(fileNameIdx.toString());
		filter.setInvertSelection(isTest);
		filter.setInputFormat(dataSet);

		Instances filtered = Filter.useFilter(dataSet, filter);

		return filtered;
	}
	// endregion

	// region searchBestNDCGFeature
	private void searchBestNDCGFeature() {
		try {
			Map<String, Double> ndcgMap = new HashMap<String, Double>();

			int numberOfAttributesToCheck = this.dataSet.numAttributes() - this.groundTruthAttributes.split(",").length;

			for (int i = 0; i < numberOfAttributesToCheck; i++) {

				List<Prediction> predictions = new ArrayList<Prediction>();
				for (Instance instance : this.dataSet) {
					Prediction pred = new NumericPrediction(instance.classValue(), instance.value(i));
					predictions.add(pred);
				}

				double ndcg = computeNDCG(predictions);
				String attributeName = this.dataSet.attribute(i).name();
				ndcgMap.put(attributeName, ndcg);
			}

			// random baseline
			List<Prediction> randomPredictions = new ArrayList<Prediction>();
			for (Instance instance : this.dataSet) {
				Prediction pred = new NumericPrediction(instance.classValue(), Math.random());
				randomPredictions.add(pred);
			}
			double randomNDCG = computeNDCG(randomPredictions);
			ndcgMap.put("random baseline", randomNDCG);

			if (log.isInfoEnabled()) {
				log.info("attribute;ndcg");
				for (Map.Entry<String, Double> entry : ndcgMap.entrySet())
					log.info(String.format("%s;%f", entry.getKey(), entry.getValue()));
			}

		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("Can't select best NDCG Feature");
			if (log.isDebugEnabled())
				log.debug("Exception", e);
		}
	}
	// endregion

	// region featureElimination
	private void featureElimination() {

		try {
			Map<String, Map<String, Double>> resultMap = new HashMap<String, Map<String, Double>>();

			Remove removeFilter = new Remove();
			removeFilter.setAttributeIndices(StringUtils.collectionToCommaDelimitedString(featureIndices));
			removeFilter.setInvertSelection(true);
			removeFilter.setInputFormat(this.dataSet);
			Instances baselineData = Filter.useFilter(this.dataSet, removeFilter);
			Map<String, Double> baselineResults = this.queryBasedRegressionOneRegressor(baselineData);
			resultMap.put("baseline", baselineResults);

			for (int i = 0; i < featureIndices.size(); i++) {
				if (StringUtils.commaDelimitedListToSet(this.groundTruthAttributes).contains(featureIndices.get(i).toString()))
					continue; // skip ground truth entries
				Remove filter = new Remove();
				List<Integer> indices = new ArrayList<Integer>(featureIndices);
				indices.remove(featureIndices.get(i));
				filter.setAttributeIndices(StringUtils.collectionToCommaDelimitedString(indices));
				filter.setInvertSelection(true);
				filter.setInputFormat(this.dataSet);
				Instances filtered = Filter.useFilter(this.dataSet, filter);
				Map<String, Double> localResults = this.queryBasedRegressionOneRegressor(filtered);
				resultMap.put(String.format("without %d", featureIndices.get(i)), localResults);
			}

			List<String> columnNames = Lists.newArrayList("RMSE", "correlation", "NDCG", "avg top 1", "avg top 1 norm", "expected", "avg top 5 norm", "expected top 5", ">0 precision @1", ">0 precision @5", ">1 precision @1", ">1 precision @5");
			Util.writeEvaluationToCsv(this.outputFileName, columnNames, resultMap);
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("Can't do feature elimination");
			if (log.isDebugEnabled())
				log.debug("Can't do feature elimination", e);
		}

	}
	// endregion

	// region recursiveFeatureElimination

	private class FeatureSet {

		public List<Integer> features;
		public Map<String, Double> results;
		public double max;
		public double min;
		public double avg;

		public FeatureSet(List<Integer> features, Map<String, Double> results, double max, double min, double avg) {
			this.features = features;
			this.results = results;
			this.max = max;
			this.min = min;
			this.avg = avg;
		}

		@Override
		public boolean equals(Object obj) {
			// equal iff same set of features
			return ((obj instanceof FeatureSet) && this.features.equals(((FeatureSet) obj).features));
		}
	}

	// do recursive feature elimination, always looking at most at 9 child
	// hypotheses
	private void recursiveFeatureElimination() {

		try {
			Map<String, Map<String, Double>> resultMap = new HashMap<String, Map<String, Double>>();
			Map<String, Map<String, Double>> resultBuffer = new HashMap<String, Map<String, Double>>();
			List<String> metrics = Lists.newArrayList("avg top 1 norm", ">0 precision @1", ">1 precision @1");

			Queue<FeatureSet> queue = new LinkedList<FeatureSet>();
			List<FeatureSet> nextLevel = new LinkedList<FeatureSet>();

			// compute baseline results
			Remove removeFilter = new Remove();
			removeFilter.setAttributeIndices(StringUtils.collectionToCommaDelimitedString(featureIndices));
			removeFilter.setInvertSelection(true);
			removeFilter.setInputFormat(this.dataSet);
			Instances baselineData = Filter.useFilter(this.dataSet, removeFilter);
			Map<String, Double> baselineResults = this.queryBasedRegressionOneRegressor(baselineData);
			resultMap.put(StringUtils.collectionToCommaDelimitedString(featureIndices), baselineResults);
			resultBuffer.put(StringUtils.collectionToCommaDelimitedString(featureIndices), baselineResults);

			FeatureSet baselineFS = new FeatureSet(featureIndices, baselineResults, 1, 1, 1);
			queue.add(baselineFS);
			while (queue.peek().features.size() - this.numberOfGroundTruthAttributes > this.minimumNumberOfFeatures) {

				if (log.isInfoEnabled())
					log.info(String.format("#features: %d, #sets: %d", queue.peek().features.size() - this.numberOfGroundTruthAttributes, queue.size()));

				// do bfs on feature sets
				while (!queue.isEmpty()) {

					FeatureSet current = queue.poll();

					List<FeatureSet> candidates = new ArrayList<FeatureSet>();

					for (Integer featureIdx : current.features) {
						if (StringUtils.commaDelimitedListToSet(this.groundTruthAttributes).contains(featureIdx.toString()))
							continue; // skip ground truth entries

						// remove feature
						List<Integer> indices = new ArrayList<Integer>(current.features);
						indices.remove(featureIdx);
						String indexString = StringUtils.collectionToCommaDelimitedString(indices);

						Map<String, Double> localResults;
						if (resultBuffer.containsKey(indexString)) {
							// we already computed this set earlier!
							localResults = resultBuffer.get(indexString);

						} else {

							// compute results from scratch and store them
							Remove filter = new Remove();
							filter.setAttributeIndices(indexString);
							filter.setInvertSelection(true);
							filter.setInputFormat(this.dataSet);
							Instances filtered = Filter.useFilter(this.dataSet, filter);
							localResults = this.queryBasedRegressionOneRegressor(filtered);
							resultBuffer.put(indexString, localResults);
						}

						// compute fraction result/baseline
						List<Double> relativeResults = new ArrayList<Double>();
						for (String column : metrics) {
							relativeResults.add(localResults.get(column) / baselineResults.get(column));
						}

						// compute max, min, and avg
						double max = Util.maxFromCollection(relativeResults);
						double min = Util.minFromCollection(relativeResults);
						double avg = Util.averageFromCollection(relativeResults);

						// store the results
						FeatureSet fs = new FeatureSet(indices, localResults, max, min, avg);
						candidates.add(fs);
					}

					// sort features by max/min/avg and keep top 3 for each
					Set<FeatureSet> selected = new HashSet<FeatureSet>();
					Collections.sort(candidates, new Comparator<FeatureSet>() {

						@Override
						public int compare(FeatureSet o1, FeatureSet o2) {
							return (-1) * Double.compare(o1.max, o2.max);
						}
					});
					selected.addAll(candidates.subList(0, Math.min(3, candidates.size())));
					Collections.sort(candidates, new Comparator<FeatureSet>() {

						@Override
						public int compare(FeatureSet o1, FeatureSet o2) {
							return (-1) * Double.compare(o1.min, o2.min);
						}
					});
					selected.addAll(candidates.subList(0, Math.min(3, candidates.size())));
					Collections.sort(candidates, new Comparator<FeatureSet>() {

						@Override
						public int compare(FeatureSet o1, FeatureSet o2) {
							return (-1) * Double.compare(o1.avg, o2.avg);
						}
					});
					selected.addAll(candidates.subList(0, Math.min(3, candidates.size())));

					for (FeatureSet fs : selected) {
						resultMap.put(StringUtils.collectionToCommaDelimitedString(fs.features), fs.results);
						if (!nextLevel.contains(fs))
							nextLevel.add(fs);
					}
				}

				// sort features by max/min/avg and keep top 7 for each
				Set<FeatureSet> levelSelected = new HashSet<FeatureSet>();
				Collections.sort(nextLevel, new Comparator<FeatureSet>() {
					@Override
					public int compare(FeatureSet o1, FeatureSet o2) {
						return (-1) * Double.compare(o1.max, o2.max);
					}
				});
				levelSelected.addAll(nextLevel.subList(0, Math.min(5, nextLevel.size())));
				Collections.sort(nextLevel, new Comparator<FeatureSet>() {
					@Override
					public int compare(FeatureSet o1, FeatureSet o2) {
						return (-1) * Double.compare(o1.min, o2.min);
					}
				});
				levelSelected.addAll(nextLevel.subList(0, Math.min(5, nextLevel.size())));
				Collections.sort(nextLevel, new Comparator<FeatureSet>() {
					@Override
					public int compare(FeatureSet o1, FeatureSet o2) {
						return (-1) * Double.compare(o1.avg, o2.avg);
					}
				});
				levelSelected.addAll(nextLevel.subList(0, Math.min(5, nextLevel.size())));

				queue.addAll(levelSelected);
				nextLevel.clear();
			}

			// output results
			List<String> columnNames = Lists.newArrayList("RMSE", "correlation", "NDCG", "avg top 1", "avg top 1 norm", "expected", "avg top 5 norm", "expected top 5", ">0 precision @1", ">0 precision @5", ">1 precision @1", ">1 precision @5");
			Util.writeEvaluationToCsv(this.outputFileName, columnNames, resultMap);

		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("Can't do recursive feature elimination");
			if (log.isDebugEnabled())
				log.debug("Can't do recursive feature elimination", e);
		}
	}
	// endregion

	// region featureAddition
	private void featureAddition() {

		try {
			Map<String, Map<String, Double>> resultMap = new HashMap<String, Map<String, Double>>();

			Remove removeFilter = new Remove();
			removeFilter.setAttributeIndices(StringUtils.collectionToCommaDelimitedString(featureIndices));
			removeFilter.setInvertSelection(true);
			removeFilter.setInputFormat(this.dataSet);
			Instances baselineData = Filter.useFilter(this.dataSet, removeFilter);
			Map<String, Double> baselineResults = this.queryBasedRegressionOneRegressor(baselineData);
			resultMap.put("baseline", baselineResults);

			for (int i = 0; i < this.newFeatureIndices.size(); i++) {
				Remove filter = new Remove();
				List<Integer> indices = new ArrayList<Integer>(featureIndices);
				indices.add(0, newFeatureIndices.get(i));
				Collections.sort(indices);
				// indices.add(newFeatureIndices.get(i));
				filter.setAttributeIndices(StringUtils.collectionToCommaDelimitedString(indices));
				filter.setInvertSelection(true);
				filter.setInputFormat(this.dataSet);
				Instances filtered = Filter.useFilter(this.dataSet, filter);
				Map<String, Double> localResults = this.queryBasedRegressionOneRegressor(filtered);
				resultMap.put(String.format("with %d", newFeatureIndices.get(i)), localResults);
			}

			List<String> columnNames = Lists.newArrayList("RMSE", "correlation", "NDCG", "avg top 1", "avg top 1 norm", "expected", "avg top 5 norm", "expected top 5", ">0 precision @1", ">0 precision @5", ">1 precision @1", ">1 precision @5");
			Util.writeEvaluationToCsv(this.outputFileName, columnNames, resultMap);
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("Can't do feature elimination");
			if (log.isDebugEnabled())
				log.debug("Can't do feature elimination", e);
		}

	}
	// endregion

	// region testPretrainedRegressor
	private void testPretrainedRegressor() {
		
		try {

			StringToNominal filter = new StringToNominal();
			int attributeIdx = dataSet.attribute(Util.ATTRIBUTE_FILE).index() + 1; // conversion from 0-based to 1-based indices...
			filter.setAttributeRange(Integer.toString(attributeIdx));
			filter.setInputFormat(dataSet);
			Instances modifiedDataSet = Filter.useFilter(dataSet, filter);

			Evaluation eval = new Evaluation(modifiedDataSet);
			double ndcg = 0;
			int ndcgNaNs = 0;
			double relativeTopRank = 0;
			int relativeTopRankNaNs = 0;
			double topRank = 0;
			double precision0At1 = 0;
			double precision0At1Norm = 0;
			int precision0At1NormNaNs = 0;
			double precision1At1 = 0;
			double precision1At1Norm = 0;
			int precision1At1NormNaNs = 0;
			
			Enumeration<Object> enumeration = modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).enumerateValues();
			while (enumeration.hasMoreElements()) {

				Evaluation evalLocal = new Evaluation(modifiedDataSet);
				String fileName = (String) enumeration.nextElement();
				Integer idx = modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).indexOfValue(fileName) + 1;

				Instances filtered = filterByFileName(modifiedDataSet, idx, true);
				RemoveByName stringFilter = new RemoveByName();
				stringFilter.setExpression("(eventURI|fileName|user)");
				stringFilter.setInputFormat(filtered);
				Instances test = Filter.useFilter(filtered, stringFilter);
				
				
				eval.evaluateModel(this.pretrainedRegressor, test);
				evalLocal.evaluateModel(this.pretrainedRegressor, test);

				double localNDCG = computeNDCG(evalLocal.predictions());
				if (Double.isNaN(localNDCG))
					ndcgNaNs++;
				else
					ndcg += localNDCG;
				topRank += computeTopRank(evalLocal.predictions(), false, 1);
				double localRelativeTopRank = computeTopRank(evalLocal.predictions(), true, 1);
				if (Double.isNaN(localRelativeTopRank))
					relativeTopRankNaNs++;
				else
					relativeTopRank += localRelativeTopRank;
				
				precision0At1 += computePrecision(evalLocal.predictions(), 1, 0, false);
				double localPrecision0At1 = computePrecision(evalLocal.predictions(), 1, 0, true);
				if (Double.isNaN(localPrecision0At1))
					precision0At1NormNaNs++;
				else
					precision0At1Norm += localPrecision0At1;
				
				precision1At1 += computePrecision(evalLocal.predictions(), 1, 1, false);
				double localPrecision1At1 = computePrecision(evalLocal.predictions(), 1, 1, true);
				if (Double.isNaN(localPrecision1At1))
					precision1At1NormNaNs++;
				else
					precision1At1Norm += localPrecision1At1;
				
				if (this.outputRankings && log.isInfoEnabled())
					logRankings(this.pretrainedRegressor, test);
			}

			ndcg /= (modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues() - ndcgNaNs);
			topRank /= modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
			relativeTopRank /= (modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues() - relativeTopRankNaNs);
			precision0At1 /= modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
			precision0At1Norm /= (modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues() - precision0At1NormNaNs);
			precision1At1 /= modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
			precision1At1Norm /= (modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues() - precision1At1NormNaNs);
			
			if (log.isInfoEnabled()) {
				log.info(String.format("pretrained regressor (file-based)"));
				logEvalResults(eval, ndcg, topRank, relativeTopRank, precision0At1, precision0At1Norm, precision1At1, precision1At1Norm);
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("cannot perform file-based evaluation of pre-trained regressor!");
			if (log.isDebugEnabled())
				log.debug("cannot perform file-based evaluation of pre-trained regressor!", e);
		}

	}
	// endregion
	
	// region userBasedRegression
	private void userBasedRegression() {

		Map<String, Map<String, Double>> resultMap = new HashMap<String, Map<String, Double>>();

		for (Map.Entry<String, Classifier> entry : this.regressors.entrySet()) {
			String regressorName = entry.getKey();
			Classifier regressor = entry.getValue();
			resultMap.put(regressorName, userBasedRegressionOneRegressor(this.dataSet, regressor, regressorName));
		}
		
		List<String> columnNames = Lists.newArrayList("RMSE", "correlation", "NDCG", "avg top 1", "avg top 1 norm", ">0 precision @1", ">0 precision @1 norm", 
														">1 precision @1", ">1 precision @1 norm");
		Util.writeEvaluationToCsv(this.outputFileName, columnNames, resultMap);

	}

	@SuppressWarnings("unused")
	private Map<String, Double> userBasedRegressionOneRegressor(Instances dataSet) {
		if (this.regressors.size() != 1)
			return new HashMap<String, Double>();
		String regressorName = this.regressors.keySet().toArray(new String[1])[0];
		Classifier regressor = this.regressors.get(regressorName);
		return userBasedRegressionOneRegressor(dataSet, regressor, regressorName);
	}
	
	private Map<String, Double> userBasedRegressionOneRegressor(Instances dataSet, Classifier regressor, String regressorName) {
		
		Map<String, Double> resultMap = new HashMap<String, Double>();

		try {

			StringToNominal userFilter = new StringToNominal();
			int userAttributeIdx = dataSet.attribute(Util.ATTRIBUTE_USER).index() + 1; // conversion from 0-based to 1-based indices...
			userFilter.setAttributeRange(Integer.toString(userAttributeIdx));
			userFilter.setInputFormat(dataSet);
			Instances modifiedDataSet = Filter.useFilter(dataSet, userFilter);

			double rmse = 0;
			double correlation = 0;
			double ndcg = 0;
			double relativeTopRank = 0;
			double topRank = 0;
			double precision0At1 = 0;
			double precision0At1Norm = 0;
			double precision1At1 = 0;
			double precision1At1Norm = 0;
			
			List<Classifier> regressorsToAverage = new ArrayList<Classifier>();
			if (regressor instanceof Randomizable) { 
				// randomizable classifier, so take average over run with 10 different deterministic seeds
				Random rand = new Random(1);
				for (int i = 0; i < 10; i++) {
					Classifier copy = AbstractClassifier.makeCopy(regressor);
					((Randomizable) copy).setSeed(rand.nextInt());
					regressorsToAverage.add(copy);
				}
			} else {
				// not randomizable, so only do work once
				regressorsToAverage.add(AbstractClassifier.makeCopy(regressor));
			}
			
			for (Classifier r : regressorsToAverage) {

				Evaluation eval = new Evaluation(modifiedDataSet);
				double regNDCG = 0;
				int regNdcgNaNs = 0;
				double regRelativeTopRank = 0;
				int regRelativeTopRankNaNs = 0;
				double regTopRank = 0;
				double regPrecision0At1 = 0;
				double regPrecision0At1Norm = 0;
				int regPrecision0At1NormNaNs = 0;
				double regPrecision1At1 = 0;
				double regPrecision1At1Norm = 0;
				int regPrecision1At1NormNaNs = 0;
				
				Enumeration<Object> userEnumeration = modifiedDataSet.attribute(Util.ATTRIBUTE_USER).enumerateValues();
				while (userEnumeration.hasMoreElements()) {

					String userName = (String) userEnumeration.nextElement();
					Integer userIdx = modifiedDataSet.attribute(Util.ATTRIBUTE_USER).indexOfValue(userName) + 1;
	
					Instances train = filterByUserName(modifiedDataSet, userIdx, false);
					Instances test = filterByUserName(modifiedDataSet, userIdx, true);
	
					r.buildClassifier(train);
					
					StringToNominal fileFilter = new StringToNominal();
					int fileAttributeIdx = test.attribute(Util.ATTRIBUTE_FILE).index() + 1; // conversion from 0-based to 1-based indices...
					fileFilter.setAttributeRange(Integer.toString(fileAttributeIdx));
					fileFilter.setInputFormat(test);
					Instances modifiedTest = Filter.useFilter(test, fileFilter);

					Enumeration<Object> fileEnumeration = modifiedTest.attribute(Util.ATTRIBUTE_FILE).enumerateValues();
					while (fileEnumeration.hasMoreElements()) {
						Evaluation evalLocal = new Evaluation(modifiedTest);
						String fileName = (String) fileEnumeration.nextElement();
						Integer fileIdx = modifiedTest.attribute(Util.ATTRIBUTE_FILE).indexOfValue(fileName) + 1;
						
						Instances query = filterByFileName(modifiedTest, fileIdx, true);
						eval.evaluateModel(r, query);
						evalLocal.evaluateModel(r, query);

						double localNDCG = computeNDCG(evalLocal.predictions());
						if (Double.isNaN(localNDCG))
							regNdcgNaNs++;
						else
							regNDCG += localNDCG;
						double localRelativeTopRank = computeTopRank(evalLocal.predictions(), true, 1);
						if (Double.isNaN(localRelativeTopRank))
							regRelativeTopRankNaNs++;
						else
							regRelativeTopRank += localRelativeTopRank;
						regTopRank += computeTopRank(evalLocal.predictions(), false, 1);
						
						regPrecision0At1 += computePrecision(evalLocal.predictions(), 1, 0, false);
						double localPrecision0At1Norm = computePrecision(evalLocal.predictions(), 1, 0, true);
						if (Double.isNaN(localPrecision0At1Norm))
							regPrecision0At1NormNaNs++;
						else
							regPrecision0At1Norm += localPrecision0At1Norm;
						
						regPrecision1At1 += computePrecision(evalLocal.predictions(), 1, 1, false);
						double localPrecision1At1Norm = computePrecision(evalLocal.predictions(), 1, 1, true);
						if (Double.isNaN(localPrecision1At1Norm))
							regPrecision1At1NormNaNs++;
						else
							regPrecision1At1Norm += localPrecision1At1Norm;
						
						if (this.outputRankings && log.isInfoEnabled())
							logRankings(r, query);
					}
					
					
				}
				
				rmse += eval.rootMeanSquaredError();
				
				correlation += eval.correlationCoefficient();
				
				regNDCG /= (modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues() - regNdcgNaNs);
				ndcg += regNDCG;
				
				regRelativeTopRank /= (modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues() - regRelativeTopRankNaNs);
				relativeTopRank += regRelativeTopRank;
				
				regTopRank /= modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
				topRank += regTopRank;
				
				regPrecision0At1 /= modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
				precision0At1 += regPrecision0At1;
				
				regPrecision0At1Norm /= (modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues() - regPrecision0At1NormNaNs);
				precision0At1Norm += regPrecision0At1Norm;
				
				regPrecision1At1 /= modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
				precision1At1 += regPrecision1At1;
				
				regPrecision1At1Norm /= (modifiedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues() - regPrecision1At1NormNaNs);
				precision1At1Norm += regPrecision1At1Norm;
				
			}

			rmse /= regressorsToAverage.size();
			correlation /= regressorsToAverage.size();
			ndcg /= regressorsToAverage.size();
			relativeTopRank /= regressorsToAverage.size();
			topRank /= regressorsToAverage.size();
			precision0At1 /= regressorsToAverage.size();
			precision0At1Norm /= regressorsToAverage.size();
			precision1At1 /= regressorsToAverage.size();
			precision1At1Norm /= regressorsToAverage.size();
			
			resultMap.put("RMSE", rmse);
			resultMap.put("correlation", correlation);
			resultMap.put("NDCG", ndcg);
			resultMap.put("avg top 1 norm", relativeTopRank);
			resultMap.put("avg top 1", topRank);
			resultMap.put(">0 precision @1", precision0At1);
			resultMap.put(">0 precision @1 norm", precision0At1Norm);
			resultMap.put(">1 precision @1", precision1At1);
			resultMap.put(">1 precision @1 norm", precision1At1Norm);
			
			if (log.isInfoEnabled()) {
				log.info(String.format("%s (user-based)", regressorName));
				logEvalResults(rmse, correlation, ndcg, topRank, relativeTopRank, precision0At1, precision0At1Norm, precision1At1, precision1At1Norm);
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("cannot perform user-based regression!");
			if (log.isDebugEnabled())
				log.debug("cannot perform user-based regression", e);
		}

		return resultMap;

	}
	
	private void logEvalResults(Evaluation eval, double ndcg, double topRank, double relativeTopRank, double precision0At1, double precision0At1Norm, 
								double precision1At1, double precision1At1Norm) {
		double rmse = eval.rootMeanSquaredError();
		double correlation = 0;
		try {
			correlation = eval.correlationCoefficient();
		} catch (Exception e) {
			if (log.isWarnEnabled())
				log.warn("cannot compute correlation coefficient!");
		}
		logEvalResults(rmse, correlation, ndcg, topRank, relativeTopRank, precision0At1, precision0At1Norm, precision1At1, precision1At1Norm);
	}
	private void logEvalResults(double rmse, double correlation, double ndcg, double topRank, double relativeTopRank, double precision0At1, double precision0At1Norm, double precision1At1, double precision1At1Norm) {
		log.info(String.format("RMSE: %f", rmse));
		log.info(String.format("correlation coefficient: %f", correlation));
		log.info(String.format("NDCG: %f", ndcg));
		log.info(String.format("average rank of top 1: %f", topRank));
		log.info(String.format("average normalized rank of top 1: %f", relativeTopRank));
		log.info(String.format(">0 precision at top 1: %f", precision0At1));
		log.info(String.format("normalized >0 precision at top 1: %f", precision0At1Norm));
		log.info(String.format(">1 precision at top 1: %f", precision1At1));
		log.info(String.format("normalized >1 precision at top 1: %f", precision1At1Norm));
	}

	private Instances filterByUserName(Instances dataSet, Integer userNameIdx, boolean isTest) throws Exception {

		RemoveWithValues filter = new RemoveWithValues();
		int attributeIdx = dataSet.attribute(Util.ATTRIBUTE_USER).index() + 1; // conversion from 0-based to 1-based indices...
		filter.setAttributeIndex(Integer.toString(attributeIdx));
		filter.setNominalIndices(userNameIdx.toString());
		filter.setInvertSelection(isTest);
		filter.setInputFormat(dataSet);

		Instances filtered = Filter.useFilter(dataSet, filter);

		return filtered;
	}
	// endregion
	
	public void run() {
		if (this.doQueryBasedRegression)
			queryBasedRegression();
		if (this.doFeatureSelection)
			featureSelection();
		if (this.doSearchBestNDCGFeature)
			searchBestNDCGFeature();
		if (this.doFeatureElimination)
			featureElimination();
		if (this.doRecursiveFeatureElimination)
			recursiveFeatureElimination();
		if (this.doFeatureAddition)
			featureAddition();
		if (this.doTestPretrainedRegressor)
			testPretrainedRegressor();
		if (this.doUserBasedRegression)
			userBasedRegression();
	}

	public static void main(String[] args) {
		System.setProperty("java.util.logging.config.file", "./config/logging-benchmark.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(RankingBenchmark.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/benchmark.xml");
		RankingBenchmark test = (RankingBenchmark) context.getBean("rankingBenchmark");
		((AbstractApplicationContext) context).close();

		test.run();
	}

}
