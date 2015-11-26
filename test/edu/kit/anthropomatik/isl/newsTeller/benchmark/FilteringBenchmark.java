package edu.kit.anthropomatik.isl.newsTeller.benchmark;

import java.io.File;
import java.io.IOException;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import weka.classifiers.rules.ZeroR;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.XRFFLoader;

public class FilteringBenchmark {

	private static Log log = LogFactory.getLog(FilteringBenchmark.class);
	
	private Instances dataSet;
	
	private String classifierFileName;
	
	public void setClassifierFileName(String classifierFileName) {
		this.classifierFileName = classifierFileName;
	}
	
	public FilteringBenchmark(String instancesFileName) {
		try {
			XRFFLoader loader = new XRFFLoader();
			loader.setSource(new File(instancesFileName));
			this.dataSet = loader.getDataSet();
		} catch (IOException e) {
			if (log.isErrorEnabled())
				log.error("Can't read data set");
			if (log.isDebugEnabled())
				log.debug("Can't read data set", e);
		}
	}
	
	public void run() {
		try {
			ZeroR classifier = new ZeroR();
			classifier.buildClassifier(dataSet);
			SerializationHelper.write(classifierFileName, classifier);
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("Exception during run()");
			if (log.isDebugEnabled())
				log.debug("Exception", e);
		}
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
