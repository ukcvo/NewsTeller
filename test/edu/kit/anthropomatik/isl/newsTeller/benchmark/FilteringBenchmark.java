package edu.kit.anthropomatik.isl.newsTeller.benchmark;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
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
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.XRFFLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToNominal;
import weka.filters.unsupervised.instance.RemoveWithValues;

public class FilteringBenchmark {

	private static Log log = LogFactory.getLog(FilteringBenchmark.class);

	private Instances originalDataSet;

	private Instances cleanedDataSet; // w/o String attributes

	private Filter filter;

	private List<AttributeSelection> configurations;

	private List<Classifier> classifiers;

	private boolean doFeatureAnalysis;

	private boolean doCrossValidation;

	private boolean doLeaveOneOut;

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	public void setConfigurations(List<AttributeSelection> configurations) {
		this.configurations = configurations;
	}

	public void setClassifiers(List<Classifier> classifiers) {
		this.classifiers = classifiers;
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

	public FilteringBenchmark(String instancesFileName) {
		try {
			XRFFLoader loader = new XRFFLoader();
			loader.setSource(new File(instancesFileName));
			this.originalDataSet = loader.getDataSet();
			this.cleanedDataSet = new Instances(this.originalDataSet);
			this.cleanedDataSet.deleteAttributeType(Attribute.STRING);
		} catch (IOException e) {
			if (log.isErrorEnabled())
				log.error("Can't read data set");
			if (log.isDebugEnabled())
				log.debug("Can't read data set", e);
		}
	}

	// region featureAnalysis
	private void featureAnalysis() {

		if (log.isInfoEnabled())
			log.info("unfiltered attributes");

		for (AttributeSelection config : configurations) {
			try {
				config.SelectAttributes(cleanedDataSet);
				if (log.isInfoEnabled())
					log.info(config.toResultsString());

			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("Can't select attributes");
				if (log.isDebugEnabled())
					log.debug("Exception", e);
			}
		}

		if (log.isInfoEnabled())
			log.info("filtered attributes");
		try {
			this.filter.setInputFormat(cleanedDataSet);
			Instances filtered = Filter.useFilter(cleanedDataSet, filter);

			for (AttributeSelection config : configurations) {
				config.SelectAttributes(filtered);
				if (log.isInfoEnabled())
					log.info(config.toResultsString());
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("Can't select filtered attributes");
			if (log.isDebugEnabled())
				log.debug("Exception", e);
		}

	}
	// endregion

	// region crossvalidation
	// do a crossvalidation for all classifiers and output the results
	private void crossValidation() {

		for (Classifier classifier : this.classifiers) {
			try {
				Evaluation eval = new Evaluation(cleanedDataSet);
				eval.crossValidateModel(classifier, cleanedDataSet, 10, new Random(1337));
				if (log.isInfoEnabled()) {
					log.info(classifier.getClass().getName());
					logEvalResults(eval);
				}

			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("Can't cross-validate classifier");
				if (log.isDebugEnabled())
					log.debug("Can't cross-validate classifier", e);
			}
		}
	}
	// endregion

	// region leaveOneOut
	// do leave one out evaluation, but based on files/keywords
	private void leaveOneOut() throws Exception {

		StringToNominal filter = new StringToNominal();
		filter.setAttributeRange("last");
		filter.setInputFormat(originalDataSet);
		Instances modifedDataSet = Filter.useFilter(originalDataSet, filter);

		for (Classifier classifier : classifiers) {
			if (log.isInfoEnabled())
				log.info(classifier.getClass().getName() + "(leaveOneOut)");
			
			Evaluation eval = new Evaluation(modifedDataSet);
			@SuppressWarnings("unchecked")
			Enumeration<String> enumeration = modifedDataSet.attribute(Util.ATTRIBUTE_FILE).enumerateValues();
			while (enumeration.hasMoreElements()) {
				Evaluation evalLocal = new Evaluation(modifedDataSet);
				
				String fileName = (String) enumeration.nextElement();
				Integer idx = modifedDataSet.attribute(Util.ATTRIBUTE_FILE).indexOfValue(fileName) + 1;

				Instances train = filterByFileName(modifedDataSet, idx, false);
				Instances test = filterByFileName(modifedDataSet, idx, true);

				Classifier c = Classifier.makeCopy(classifier);
				c.buildClassifier(train);
				eval.evaluateModel(c, test);
				evalLocal.evaluateModel(c, test);
				if (log.isInfoEnabled())
					log.info(evalLocal.toMatrixString(fileName));
			}

			if (log.isInfoEnabled()) {
				logEvalResults(eval);
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
		result.deleteAttributeAt(result.numAttributes() - 1);
		result.deleteAttributeAt(result.numAttributes() - 1);
		
		return result;
	}
	
	// endregion

	private void logEvalResults(Evaluation eval) throws Exception {
		// compute balanced accuracy
		int positiveClassIdx = this.originalDataSet.attribute(Util.ATTRIBUTE_USABLE).indexOfValue(Util.CLASS_LABEL_POSITIVE);
		double tp = eval.numTruePositives(positiveClassIdx);
		double tn = eval.numTrueNegatives(positiveClassIdx);
		double fp = eval.numFalsePositives(positiveClassIdx);
		double fn = eval.numFalseNegatives(positiveClassIdx);
		
		double balancedAcc = ((0.5 * tp) / (tp + fn)) + ((0.5 * tn) / (tn + fp));
		
		// output
		log.info(eval.toSummaryString());
		log.info(String.format("balanced accuracy: %f", balancedAcc));
		log.info(eval.toClassDetailsString());
		log.info(eval.toMatrixString());
	}
	
	public void run() throws Exception {
		if (this.doFeatureAnalysis)
			featureAnalysis();
		if (this.doCrossValidation)
			crossValidation();
		if (this.doLeaveOneOut)
			leaveOneOut();
		
		for (int i = 0; i < originalDataSet.numInstances(); i++) {
			Instance inst = originalDataSet.instance(i);
			if ((inst.value(originalDataSet.attribute("propBankFeature")) < 0.2) && (inst.value(inst.classAttribute()) == originalDataSet.classAttribute().indexOfValue(Util.CLASS_LABEL_POSITIVE)))
				System.out.println(inst.stringValue(originalDataSet.attribute(Util.ATTRIBUTE_URI)));
		}
	}

	public static void main(String[] args) throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(FilteringBenchmark.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		FilteringBenchmark test = (FilteringBenchmark) context.getBean("filteringBenchmark");
		((AbstractApplicationContext) context).close();

		test.run();
	}
}
