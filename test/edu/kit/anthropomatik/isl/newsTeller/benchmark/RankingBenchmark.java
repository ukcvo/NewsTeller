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
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.Prediction;
import weka.core.Instances;
import weka.core.converters.XRFFLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToNominal;
import weka.filters.unsupervised.instance.RemoveWithValues;

public class RankingBenchmark {

	private static Log log = LogFactory.getLog(RankingBenchmark.class);
	
	private Instances dataSet;
	
	private Map<String, Classifier> regressors;
	
	private boolean doFileBasedRegression;
	
	private Map<String, Map<String, GroundTruth>> benchmark;
	
	public void setRegressors(Map<String, Classifier> regressors) {
		this.regressors = regressors;
	}
	
	public void setDoFileBasedRegression(boolean doFileBasedRegression) {
		this.doFileBasedRegression = doFileBasedRegression;
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
	
	// region fileBasedRegression
	private void fileBasedRegression() {
		
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
					topRank += computeTopRank(evalLocal.predictions(), false);
					relativeTopRank += computeTopRank(evalLocal.predictions(), true);
					expectedRank += computeExpectedRank(evalLocal.predictions());
				}

				ndcg /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
				topRank /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
				relativeTopRank /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
				expectedRank /= modifedDataSet.attribute(Util.ATTRIBUTE_FILE).numValues();
				
				if (log.isInfoEnabled()) {
					log.info(String.format("%s (file-based)", classifierName));
					logEvalResults(eval, ndcg, topRank, relativeTopRank, expectedRank);
				}
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("cannot perform file-based regression!");
			if (log.isDebugEnabled())
				log.debug("cannot perform file-based regression", e);
		}
		
	}
	
	// computes the expected rank when using the predictions as probabilites for selecting a random event.
	private double computeExpectedRank(List<Prediction> predictions) {
		double result = 0;
		double weightSum = 0;
		for (Prediction p : predictions) {
			result += Util.regressionValueToRank(p.actual()) * p.predicted();
			weightSum += p.predicted();
		}
		
		return (result / weightSum);
	}
	
	// computes the rank of the highest-ranked event (optionally normalized by maximum attainable for the given query)
	private double computeTopRank(List<Prediction> predictions, boolean shouldBeRelative) {
		
		Prediction topPrediction = null;
		double maxActualValue = 0;
		
		for (Prediction p : predictions) {
			if (topPrediction == null || p.predicted() > topPrediction.predicted())
				topPrediction = p;
			if (p.actual() > maxActualValue)
				maxActualValue = Util.regressionValueToRank(p.actual());
		}
		
		double topValue = Util.regressionValueToRank(topPrediction.actual());
		
		return shouldBeRelative ? topValue / maxActualValue : topValue;
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
	
	private void logEvalResults(Evaluation eval, double ndcg, double topRank, double relativeTopRank, double expectedRank) {
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
	}
	
	private Instances filterByFileName(Instances dataSet, Integer fileNameIdx, boolean isTest) throws Exception {

		RemoveWithValues filter = new RemoveWithValues();
		filter.setAttributeIndex("last");
		filter.setNominalIndices(fileNameIdx.toString());
		filter.setInvertSelection(isTest);
		filter.setInputFormat(dataSet);

		Instances result = Filter.useFilter(dataSet, filter);

		return result;
	}
	// endregion
	
	public void run() {
		if (this.doFileBasedRegression)
			fileBasedRegression();
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
