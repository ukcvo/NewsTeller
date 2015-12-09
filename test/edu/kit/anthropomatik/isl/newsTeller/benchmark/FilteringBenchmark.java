package edu.kit.anthropomatik.isl.newsTeller.benchmark;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import weka.attributeSelection.AttributeSelection;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.MetaCost;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.XRFFLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToNominal;
import weka.filters.unsupervised.instance.RemoveWithValues;

public class FilteringBenchmark {

	private static Log log = LogFactory.getLog(FilteringBenchmark.class);

	private Instances originalDataSet;

	private Instances analysisDataSet; // w/o String attributes

	private Instances classificationDataSet; // only w/ relevant attributes
	
	private Filter analysisFilter;

	private List<AttributeSelection> configurations;

	private Filter classifierFilter;
	
	private Map<String, Classifier> classifiers;

	private Classifier costSensitiveWrapper;
	
	private boolean doFeatureAnalysis;

	private boolean doCrossValidation;

	private boolean doLeaveOneOut;

	private boolean outputMisclassified;
	
	private boolean useCostSensitiveWrapper;
	
	private String outputFileName;
	
	// region setters
	public void setAnalysisFilter(Filter analysisFilter) {
		this.analysisFilter = analysisFilter;
	}

	public void setConfigurations(List<AttributeSelection> configurations) {
		this.configurations = configurations;
	}

	public void setClassifierFilter(Filter classifierFilter) {
		this.classifierFilter = classifierFilter;
	}
	
	public void setClassifiers(Map<String, Classifier> classifiers) {
		this.classifiers = classifiers;
	}

	public void setCostSensitiveWrapper(Classifier costSensitiveWrapper) {
		this.costSensitiveWrapper = costSensitiveWrapper;
	}
	
	public void setDoFeatureAnalysis(boolean doFeatureAnalysis) {
		this.doFeatureAnalysis = doFeatureAnalysis;
	}

	public void setDoCrossValidation(boolean doCrossValidation) {
		this.doCrossValidation = doCrossValidation;
	}

	public void setDoLeaveOneOut(boolean doLeaveOneOut) {
		this.doLeaveOneOut = doLeaveOneOut;
	}

	public void setOutputMisclassified(boolean outputMisclassified) {
		this.outputMisclassified = outputMisclassified;
	}
	
	public void setUseCostSensitiveWrapper(boolean useCostSensitiveWrapper) {
		this.useCostSensitiveWrapper = useCostSensitiveWrapper;
	}
	
	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}
	//endregion
	
	public FilteringBenchmark(String instancesFileName) {
		try {
			XRFFLoader loader = new XRFFLoader();
			loader.setSource(new File(instancesFileName));
			this.originalDataSet = loader.getDataSet();
		} catch (IOException e) {
			if (log.isErrorEnabled())
				log.error("Can't read data set");
			if (log.isDebugEnabled())
				log.debug("Can't read data set", e);
		}
	}

	// region featureAnalysis
	private void featureAnalysis() {

		for (AttributeSelection config : configurations) {
			try {
				config.SelectAttributes(analysisDataSet);
				if (log.isInfoEnabled())
					log.info(config.toResultsString());

			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("Can't select attributes");
				if (log.isDebugEnabled())
					log.debug("Exception", e);
			}
		}

	}
	// endregion

	// region crossvalidation
	// do a crossvalidation for all classifiers and output the results
	private void crossValidation() {

		int seed = 1337;
		int numFolds = 10;
		
		List<String> columnNames = new ArrayList<String>();
		columnNames.add(Util.COLUMN_NAME_BALANCED_ACCURACY);
		columnNames.add(Util.COLUMN_NAME_KAPPA);
		columnNames.add(Util.COLUMN_NAME_AUC);
		columnNames.add(Util.COLUMN_NAME_FSCORE);
		
		Map<String, Map<String, Double>> overallResultMap = new HashMap<String, Map<String,Double>>();
		
		for (Map.Entry<String, Classifier> entry : this.classifiers.entrySet()) {
			try {
				String classifierName = entry.getKey();
				Classifier classifier = entry.getValue();
				Evaluation eval = new Evaluation(classificationDataSet);
				// do crossvalidation manually in order to output misclassified examples
				Random rand = new Random(seed);
				Instances randData = new Instances(classificationDataSet);
				randData.randomize(rand);
				randData.stratify(numFolds);
				
				for (int i = 0; i < numFolds; i++) {
					Instances train = randData.trainCV(numFolds, i);
					Instances test = randData.testCV(numFolds, i);
					
					Classifier c;
					if (this.useCostSensitiveWrapper) {
						MetaCost outer = (MetaCost) Classifier.makeCopy(this.costSensitiveWrapper);
						Classifier inner = Classifier.makeCopy(classifier);
						outer.setClassifier(inner);
						c = outer;
					} else {
						c = Classifier.makeCopy(classifier);
					}
					c.buildClassifier(train);
					eval.evaluateModel(c, test);
					
					if (outputMisclassified && log.isInfoEnabled()) {
						outputMisclassifiedInstances(test, c);
					}
					
				}
								
				if (log.isInfoEnabled()) {
					log.info(String.format("%s (cross-validation)", classifierName));
					logEvalResults(eval);
				}
				
				int positiveClassIdx = this.originalDataSet.attribute(Util.ATTRIBUTE_USABLE).indexOfValue(Util.CLASS_LABEL_POSITIVE);
				Map<String, Double> classifierMap = new HashMap<String, Double>();
				classifierMap.put(Util.COLUMN_NAME_BALANCED_ACCURACY, getBalancedAcc(eval, positiveClassIdx));
				classifierMap.put(Util.COLUMN_NAME_KAPPA, eval.kappa());
				classifierMap.put(Util.COLUMN_NAME_AUC, eval.areaUnderROC(positiveClassIdx));
				classifierMap.put(Util.COLUMN_NAME_FSCORE, eval.fMeasure(positiveClassIdx));
				
				overallResultMap.put(classifierName, classifierMap);
				
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("Can't cross-validate classifier");
				if (log.isDebugEnabled())
					log.debug("Can't cross-validate classifier", e);
			}
		}
		
		Util.writeEvaluationToCsv(this.outputFileName, columnNames, overallResultMap);
	}
	// endregion

	// region leaveOneOut
	// do leave one out evaluation, but based on files/keywords
	private void leaveOneOut() throws Exception {

		StringToNominal filter = new StringToNominal();
		filter.setAttributeRange("last");
		filter.setInputFormat(classificationDataSet);
		Instances modifedDataSet = Filter.useFilter(classificationDataSet, filter);

		for (Map.Entry<String, Classifier> entry : this.classifiers.entrySet()) {
			
			String classifierName = entry.getKey();
			Classifier classifier = entry.getValue();
			
			if (log.isInfoEnabled())
				log.info(String.format("%d (leave one out)", classifierName));
			
			Evaluation eval = new Evaluation(modifedDataSet);
			@SuppressWarnings("unchecked")
			Enumeration<String> enumeration = modifedDataSet.attribute(Util.ATTRIBUTE_FILE).enumerateValues();
			while (enumeration.hasMoreElements()) {
				Evaluation evalLocal = new Evaluation(modifedDataSet);
				
				String fileName = (String) enumeration.nextElement();
				Integer idx = modifedDataSet.attribute(Util.ATTRIBUTE_FILE).indexOfValue(fileName) + 1;

				Instances train = filterByFileName(modifedDataSet, idx, false);
				Instances test = filterByFileName(modifedDataSet, idx, true);

				Classifier c;
				if (this.useCostSensitiveWrapper) {
					MetaCost outer = (MetaCost) Classifier.makeCopy(this.costSensitiveWrapper);
					Classifier inner = Classifier.makeCopy(classifier);
					outer.setClassifier(inner);
					c = outer;
				} else {
					c = Classifier.makeCopy(classifier);
				}
				c.buildClassifier(train);
				eval.evaluateModel(c, test);
				evalLocal.evaluateModel(c, test);
				if (log.isInfoEnabled())
					log.info(evalLocal.toMatrixString(fileName));
				
				if (outputMisclassified && log.isInfoEnabled()) {
					outputMisclassifiedInstances(test, c);
				}
			}

			if (log.isInfoEnabled()) {
				logEvalResults(eval);
			}
		}
	}
	// endregion

	// region helper methods
	private void logEvalResults(Evaluation eval) throws Exception {
		// compute balanced accuracy
		int positiveClassIdx = this.originalDataSet.attribute(Util.ATTRIBUTE_USABLE).indexOfValue(Util.CLASS_LABEL_POSITIVE);
		double balancedAcc = getBalancedAcc(eval, positiveClassIdx);
		
		// output
		log.info(eval.toSummaryString());
		log.info(String.format("balanced accuracy: %f", balancedAcc));
		log.info(String.format("Cohen's kappa: %f", eval.kappa()));
		log.info(String.format("AUC for usable: %f", eval.areaUnderROC(positiveClassIdx)));
		log.info(eval.toClassDetailsString());
		log.info(eval.toMatrixString());
	}

	private double getBalancedAcc(Evaluation eval, int positiveClassIdx) {
		
		double tp = eval.numTruePositives(positiveClassIdx);
		double tn = eval.numTrueNegatives(positiveClassIdx);
		double fp = eval.numFalsePositives(positiveClassIdx);
		double fn = eval.numFalseNegatives(positiveClassIdx);
		
		double balancedAcc = ((0.5 * tp) / (tp + fn)) + ((0.5 * tn) / (tn + fp));
		return balancedAcc;
	}
	
	private void outputMisclassifiedInstances(Instances test, Classifier c) throws Exception {
		for (int i = 0; i < test.numInstances(); i++) {
			Instance inst = test.instance(i);
			double prediction = c.classifyInstance(inst);
			if (inst.classValue() != prediction) {
				if (inst.classValue() == inst.classAttribute().indexOfValue(Util.CLASS_LABEL_POSITIVE))
					log.info(String.format("False Negative: %s", inst.toString())); 
				else
					log.info(String.format("False Positive: %s", inst.toString()));
			}
		}
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
	
	private void filterDataSet() {
		try {
			this.analysisFilter.setInputFormat(originalDataSet);
			this.analysisDataSet = Filter.useFilter(this.originalDataSet, this.analysisFilter);
			
			this.classifierFilter.setInputFormat(originalDataSet);
			this.classificationDataSet = Filter.useFilter(this.originalDataSet, this.classifierFilter);
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("Can't apply filter");
			if (log.isDebugEnabled())
				log.debug("Can't apply filter", e);
		}
	}
	//endregion
	
	public void run() throws Exception {
		
		filterDataSet();
		
		if (this.doFeatureAnalysis)
			featureAnalysis();
		if (this.doCrossValidation)
			crossValidation();
		if (this.doLeaveOneOut)
			leaveOneOut();

	}

	public static void main(String[] args) throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-benchmark.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(FilteringBenchmark.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/benchmark.xml");
		FilteringBenchmark test = (FilteringBenchmark) context.getBean("filteringBenchmark");
		((AbstractApplicationContext) context).close();

		test.run();
	}
}
