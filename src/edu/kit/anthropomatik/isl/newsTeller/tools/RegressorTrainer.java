package edu.kit.anthropomatik.isl.newsTeller.tools;

import java.io.File;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.XRFFLoader;

public class RegressorTrainer {

	private static Log log = LogFactory.getLog(RegressorTrainer.class);

	private Classifier regressor;

	private String fileName;

	private Instances dataSet;

	public void setRegressor(Classifier regressor) {
		this.regressor = regressor;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public RegressorTrainer(String dataSetFileName) {
		try {
			XRFFLoader loader = new XRFFLoader();
			loader.setSource(new File(dataSetFileName));
			this.dataSet = loader.getDataSet();
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("Can't read data set");
			if (log.isDebugEnabled())
				log.debug("Can't read data set", e);
		}

	}

	public void run() {

		try {
			regressor.buildClassifier(dataSet);
			Instances header = new Instances(dataSet, 0);
			SerializationHelper.writeAll(fileName, new Object[] { regressor, header });
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("Can't train regressor!");
			if (log.isDebugEnabled())
				log.debug("Exception", e);
		}
	}

	public static void main(String[] args) {

		System.setProperty("java.util.logging.config.file", "./config/logging.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(RegressorTrainer.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/tools-noEmbeddings.xml");
		RegressorTrainer trainer = (RegressorTrainer) context.getBean("regressorTrainer");
		((AbstractApplicationContext) context).close();

		trainer.run();
	}
}
