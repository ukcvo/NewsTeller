package edu.kit.anthropomatik.isl.newsTeller.benchmark;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import weka.attributeSelection.AttributeSelection;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.XRFFLoader;
import weka.filters.Filter;

public class FilteringBenchmark {

	private static Log log = LogFactory.getLog(FilteringBenchmark.class);

	private Instances originalDataSet;

	private Instances cleanedDataSet; // w/o String attributes

	private Filter filter;

	private List<AttributeSelection> configurations;

	private boolean doFeatureAnalysis;

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	public void setConfigurations(List<AttributeSelection> configurations) {
		this.configurations = configurations;
	}

	public void setDoFeatureAnalysis(boolean doFeatureAnalysis) {
		this.doFeatureAnalysis = doFeatureAnalysis;
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

	public void run() {
		if (this.doFeatureAnalysis)
			featureAnalysis();
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
		FilteringBenchmark test = (FilteringBenchmark) context.getBean("filteringBenchmark");
		((AbstractApplicationContext) context).close();

		test.run();
	}
}
