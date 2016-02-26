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
import weka.core.Utils;
import weka.core.converters.XRFFLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.RemoveType;
import weka.filters.unsupervised.attribute.StringToNominal;
import weka.filters.unsupervised.instance.RemoveWithValues;

public class RankingBenchmark {

	private static Log log = LogFactory.getLog(RankingBenchmark.class);

	private Instances dataSet;

	private Map<String, Classifier> regressors;

	private List<AttributeSelection> featureSelectors;

	private String groundTruthAttributes;

	private int numberOfGroundTruthAttributes;

	private List<Integer> featureIndices;

	private List<Integer> newFeatureIndices;

	private String outputFileName;

	private int minimumNumberOfFeatures;

	private boolean doFileBasedRegression;

	private boolean doFeatureSelection;

	private boolean doSearchBestNDCGFeature;

	private boolean doFeatureElimination;

	private boolean doRecursiveFeatureElimination;

	private boolean doFeatureAddition;

	private boolean outputRankings;

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

	public void setDoFileBasedRegression(boolean doFileBasedRegression) {
		this.doFileBasedRegression = doFileBasedRegression;
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

	public void setOutputRankings(boolean outputRankings) {
		this.outputRankings = outputRankings;
	}

	public RankingBenchmark(String dataSetFileName) {
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

	// region fileBasedRegression
	private void fileBasedRegression() {

		Map<String, Map<String, Double>> resultMap = new HashMap<String, Map<String, Double>>();

		for (Map.Entry<String, Classifier> entry : this.regressors.entrySet()) {
			String regressorName = entry.getKey();
			Classifier regressor = entry.getValue();
			resultMap.put(regressorName, fileBasedRegressionOneRegressor(this.dataSet, regressor, regressorName));
		}
		
		List<String> columnNames = Lists.newArrayList("RMSE", "correlation", "NDCG", "avg top 1", "avg top 1 norm", "expected", "avg top 5 norm", "expected top 5", ">0 precision @1", ">0 precision @5", ">1 precision @1", ">1 precision @5");
		Util.writeEvaluationToCsv(this.outputFileName, columnNames, resultMap);

	}

	private Map<String, Double> fileBasedRegressionOneRegressor(Instances dataSet) {
		if (this.regressors.size() != 1)
			return new HashMap<String, Double>();
		String regressorName = this.regressors.keySet().toArray(new String[1])[0];
		Classifier regressor = this.regressors.get(regressorName);
		return fileBasedRegressionOneRegressor(dataSet, regressor, regressorName);
	}
	
	private Map<String, Double> fileBasedRegressionOneRegressor(Instances dataSet, Classifier regressor, String regressorName) {

		Map<String, Double> resultMap = new HashMap<String, Double>();

		try {

			StringToNominal filter = new StringToNominal();
			filter.setAttributeRange("last");
			filter.setInputFormat(dataSet);
			Instances modifedDataSet = Filter.useFilter(dataSet, filter);

			Evaluation eval = new Evaluation(modifedDataSet);
			double ndcg = 0;
			double relativeTopRank = 0;
			double topRank = 0;
			double expectedRank = 0;
			double top5relativeRank = 0;
			double top5expectedRank = 0;
			double precision0At1 = 0;
			double precision0At5 = 0;
			double precision1At1 = 0;
			double precision1At5 = 0;

			Enumeration<Object> enumeration = modifedDataSet.attribute(Util.ATTRIBUTE_FILE).enumerateValues();
			while (enumeration.hasMoreElements()) {

				Evaluation evalLocal = new Evaluation(modifedDataSet);
				String fileName = (String) enumeration.nextElement();
				Integer idx = modifedDataSet.attribute(Util.ATTRIBUTE_FILE).indexOfValue(fileName) + 1;

				Instances train = filterByFileName(modifedDataSet, idx, false);
				Instances test = filterByFileName(modifedDataSet, idx, true);

				Classifier r = AbstractClassifier.makeCopy(regressor);
				r.buildClassifier(train);
				eval.evaluateModel(r, test);
				evalLocal.evaluateModel(r, test);

				ndcg += computeNDCG(evalLocal.predictions());
				topRank += computeTopRank(evalLocal.predictions(), false, 1);
				relativeTopRank += computeTopRank(evalLocal.predictions(), true, 1);
				expectedRank += computeExpectedRank(evalLocal.predictions(), evalLocal.predictions().size());
				top5relativeRank += computeTopRank(evalLocal.predictions(), true, 5);
				top5expectedRank += computeExpectedRank(evalLocal.predictions(), 5);

				precision0At1 += computePrecision(evalLocal.predictions(), 1, 0);
				precision0At5 += computePrecision(evalLocal.predictions(), 5, 0);
				precision1At1 += computePrecision(evalLocal.predictions(), 1, 1);
				precision1At5 += computePrecision(evalLocal.predictions(), 5, 1);

				if (this.outputRankings && log.isInfoEnabled())
					logRankings(r, test);
			}

			ndcg /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
			topRank /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
			relativeTopRank /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
			expectedRank /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
			top5relativeRank /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
			top5expectedRank /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
			precision0At1 /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
			precision0At5 /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
			precision1At1 /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
			precision1At5 /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();

			resultMap.put("RMSE", eval.rootMeanSquaredError());
			resultMap.put("correlation", eval.correlationCoefficient());
			resultMap.put("NDCG", ndcg);
			resultMap.put("avg top 1", topRank);
			resultMap.put("avg top 1 norm", relativeTopRank);
			resultMap.put("expected", expectedRank);
			resultMap.put("avg top 5 norm", top5relativeRank);
			resultMap.put("expected top 5", top5expectedRank);
			resultMap.put(">0 precision @1", precision0At1);
			resultMap.put(">0 precision @5", precision0At5);
			resultMap.put(">1 precision @1", precision1At1);
			resultMap.put(">1 precision @5", precision1At5);

			if (log.isInfoEnabled()) {
				log.info(String.format("%s (file-based)", regressorName));
				logEvalResults(eval, ndcg, topRank, relativeTopRank, expectedRank, top5relativeRank, top5expectedRank, precision0At1, precision0At5, precision1At1, precision1At5);
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("cannot perform file-based regression!");
			if (log.isDebugEnabled())
				log.debug("cannot perform file-based regression", e);
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

	// computes the expected rank when using the predictions as probabilites for
	// selecting a random event.
	private double computeExpectedRank(List<Prediction> predictions, int n) {

		List<Prediction> sortedByPrediction = new ArrayList<Prediction>(predictions);
		Collections.sort(sortedByPrediction, new Comparator<Prediction>() {
			@Override
			public int compare(Prediction o1, Prediction o2) {
				return (-1) * Double.compare(o1.predicted(), o2.predicted());
			}
		});
		double result = 0;
		double weightSum = 0;
		for (int i = 0; i < Math.min(n, predictions.size()); i++) {
			Prediction p = sortedByPrediction.get(i);
			result += Util.regressionValueToRank(p.actual()) * p.predicted();
			weightSum += p.predicted();
		}

		return (weightSum == 0) ? 0 : (result / weightSum);
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

	private double computePrecision(List<Prediction> predictions, int n, double threshold) {
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
			if (sortedByPrediction.get(i).actual() > threshold)
				positive++;
		}

		return (positive / k);
	}

	private void logEvalResults(Evaluation eval, double ndcg, double topRank, double relativeTopRank, double expectedRank, double top5RelativeRank, double top5expectedRank, double precision0At1, double precision0At5, double precision1At1,
			double precision1At5) {
		log.info(String.format("RMSE: %f", eval.rootMeanSquaredError()));
		try {
			log.info(String.format("correlation coefficient: %f", eval.correlationCoefficient()));
		} catch (Exception e) {
			if (log.isWarnEnabled())
				log.warn("cannot compute correlation coefficient!");
		}
		log.info(String.format("NDCG: %f", ndcg));
		log.info(String.format("average rank of top 1: %f", topRank));
		log.info(String.format("average normalized rank of top 1: %f", relativeTopRank));
		log.info(String.format("expected rank (probability distribution): %f", expectedRank));
		log.info(String.format("average normalized rank of top 5: %f", top5RelativeRank));
		log.info(String.format("expected rank (probability distribution) for top 5: %f", top5expectedRank));
		log.info(String.format(">0 precision at top 1: %f", precision0At1));
		log.info(String.format(">0 precision at top 5: %f", precision0At5));
		log.info(String.format(">1 precision at top 1: %f", precision1At1));
		log.info(String.format(">1 precision at top 5: %f", precision1At5));
	}

	private Instances filterByFileName(Instances dataSet, Integer fileNameIdx, boolean isTest) throws Exception {

		RemoveWithValues filter = new RemoveWithValues();
		filter.setAttributeIndex("last");
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
			Map<String, Double> baselineResults = this.fileBasedRegressionOneRegressor(baselineData);
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
				Map<String, Double> localResults = this.fileBasedRegressionOneRegressor(filtered);
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
			Map<String, Double> baselineResults = this.fileBasedRegressionOneRegressor(baselineData);
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
							localResults = this.fileBasedRegressionOneRegressor(filtered);
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
			Map<String, Double> baselineResults = this.fileBasedRegressionOneRegressor(baselineData);
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
				Map<String, Double> localResults = this.fileBasedRegressionOneRegressor(filtered);
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

	public void run() {
		if (this.doFileBasedRegression)
			fileBasedRegression();
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
