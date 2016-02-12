package edu.kit.anthropomatik.isl.newsTeller.benchmark;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.BenchmarkEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.GroundTruth;
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
import weka.filters.unsupervised.attribute.RemoveType;
import weka.filters.unsupervised.attribute.StringToNominal;
import weka.filters.unsupervised.instance.RemoveWithValues;

public class RankingBenchmark {

	private static Log log = LogFactory.getLog(RankingBenchmark.class);
	
	private Instances dataSet;
	
	private Map<String, Map<String, GroundTruth>> benchmark;
	
	private Map<String, Classifier> regressors;
	
	private List<AttributeSelection> featureSelectors;
	
	private String groundTruthAttributes;
	
	private boolean doFileBasedRegression;
	
	private boolean doFeatureSelection;
	
	private boolean doSearchBestNDCGFeature;
	
	private boolean outputRankings;
	
	public void setRegressors(Map<String, Classifier> regressors) {
		this.regressors = regressors;
	}
	
	public void setFeatureSelectors(List<AttributeSelection> featureSelectors) {
		this.featureSelectors = featureSelectors;
	}
	
	public void setGroundTruthAttributes(String groundTruthAttributes) {
		this.groundTruthAttributes = groundTruthAttributes;
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
	
	public void setOutputRankings(boolean outputRankings) {
		this.outputRankings = outputRankings;
	}
	
	public RankingBenchmark(String dataSetFileName, String configFileName) {
		try {
			XRFFLoader loader = new XRFFLoader();
			loader.setSource(new File(dataSetFileName));
			this.dataSet = loader.getDataSet();
			
			Set<String> fileNames = Util.readBenchmarkConfigFile(configFileName).keySet();
			this.benchmark = new HashMap<String, Map<String, GroundTruth>>();

			for (String fileName : fileNames) {
				Map<BenchmarkEvent, GroundTruth> fileContent = Util.readBenchmarkQueryFromFile(fileName);
				Map<String, GroundTruth> internalMap = new HashMap<String, GroundTruth>();
				for (Map.Entry<BenchmarkEvent, GroundTruth> entry : fileContent.entrySet()) {
					internalMap.put(entry.getKey().getEventURI(), entry.getValue());
				}
				this.benchmark.put(fileName, internalMap);
			}
			
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
	private void fileBasedRegression(Instances dataSet) { 
		
		try {
			
			StringToNominal filter = new StringToNominal();
			filter.setAttributeRange("last");
			filter.setInputFormat(dataSet);
			Instances modifedDataSet = Filter.useFilter(dataSet, filter);

			for (Map.Entry<String, Classifier> entry : this.regressors.entrySet()) {

				String classifierName = entry.getKey();
				Classifier regressor = entry.getValue();

				Evaluation eval = new Evaluation(modifedDataSet);
				double ndcg = 0;
				double relativeTopRank = 0;
				double topRank = 0;
				double expectedRank = 0;
				double top5relativeRank = 0;
				double top5expectedRank = 0;
				
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
					
					if (this.outputRankings && log.isInfoEnabled())
						logRankings(r, test);
				}

				ndcg /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
				topRank /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
				relativeTopRank /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
				expectedRank /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
				top5relativeRank /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
				top5expectedRank /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
				
				if (log.isInfoEnabled()) {
					log.info(String.format("%s (file-based)", classifierName));
					logEvalResults(eval, ndcg, topRank, relativeTopRank, expectedRank, top5relativeRank, top5expectedRank);
				}
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("cannot perform file-based regression!");
			if (log.isDebugEnabled())
				log.debug("cannot perform file-based regression", e);
		}
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
			
			
		} catch (Exception e) {
			if (log.isWarnEnabled())
				log.warn("Cannot output ranking");
			if (log.isDebugEnabled())
				log.debug("Cannot output ranking", e);
		}
		
	}
	
	// computes the expected rank when using the predictions as probabilites for selecting a random event.
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
	
	// computes the average rank of the n highest-ranked events (optionally normalized by maximum attainable for the given query)
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
	
	private void logEvalResults(Evaluation eval, double ndcg, double topRank, double relativeTopRank, double expectedRank, 
								double top5RelativeRank, double top5expectedRank) {
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
	
	public void run() {
		if (this.doFileBasedRegression)
			fileBasedRegression(this.dataSet);
		if (this.doFeatureSelection)
			featureSelection();
		if (this.doSearchBestNDCGFeature)
			searchBestNDCGFeature();
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
