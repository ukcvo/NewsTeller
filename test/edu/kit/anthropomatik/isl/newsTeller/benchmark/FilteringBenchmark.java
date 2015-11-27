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
					log.info(eval.toSummaryString());
					log.info(eval.toClassDetailsString());
					log.info(eval.toMatrixString());
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

				RemoveWithValues trainFilter = new RemoveWithValues();
				trainFilter.setAttributeIndex("last");
				trainFilter.setNominalIndices(idx.toString());
				trainFilter.setInputFormat(modifedDataSet);
				Instances train = Filter.useFilter(modifedDataSet, trainFilter);
				train.deleteAttributeAt(train.numAttributes() - 1);
				train.deleteAttributeAt(train.numAttributes() - 1);

				RemoveWithValues testFilter = new RemoveWithValues();
				testFilter.setAttributeIndex("last");
				testFilter.setNominalIndices(idx.toString());
				testFilter.setInvertSelection(true);
				testFilter.setInputFormat(modifedDataSet);
				Instances test = Filter.useFilter(modifedDataSet, testFilter);
				test.deleteAttributeAt(test.numAttributes() - 1);
				test.deleteAttributeAt(test.numAttributes() - 1);

				Classifier c = Classifier.makeCopy(classifier);
				c.buildClassifier(train);
				eval.evaluateModel(c, test);
				evalLocal.evaluateModel(c, test);
				if (log.isInfoEnabled())
					log.info(evalLocal.toMatrixString(fileName));
			}

			if (log.isInfoEnabled()) {
				log.info(eval.toSummaryString());
				log.info(eval.toClassDetailsString());
				log.info(eval.toMatrixString());
			}
		}
	}
	// endregion

	public void run() throws Exception {
		if (this.doFeatureAnalysis)
			featureAnalysis();
		if (this.doCrossValidation)
			crossValidation();
		if (this.doLeaveOneOut)
			leaveOneOut();
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
