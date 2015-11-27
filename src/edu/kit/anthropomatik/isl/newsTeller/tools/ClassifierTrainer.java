package edu.kit.anthropomatik.isl.newsTeller.tools;

import java.io.File;
import java.io.IOException;
import java.util.List;
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

/**
 * Trains the given classifiers on the given training data set and stores them in the respective files.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class ClassifierTrainer {

	private static Log log = LogFactory.getLog(ClassifierTrainer.class);
	
	private List<Classifier> classifiers;
	
	private List<String> fileNames;
	
	private Instances dataSet;
	
	public void setClassifiers(List<Classifier> classifiers) {
		this.classifiers = classifiers;
	}

	public void setFileNames(List<String> fileNames) {
		this.fileNames = fileNames;
	}

	public ClassifierTrainer(String dataFileName) {
		try {
			XRFFLoader loader = new XRFFLoader();
			loader.setSource(new File(dataFileName));
			this.dataSet = loader.getDataSet();
		} catch (IOException e) {
			if (log.isErrorEnabled())
				log.error("Can't read data set");
			if (log.isDebugEnabled())
				log.debug("Can't read data set", e);
		}
		
	}

	public void run() {
		
		for (int i = 0; i < classifiers.size(); i++) {
			Classifier c = classifiers.get(i);
			String fileName = fileNames.get(i);
			
			try {
				c.buildClassifier(dataSet);
				Instances header = new Instances(dataSet, 0);
				SerializationHelper.writeAll(fileName, new Object[] { c, header });
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("Can't create classifier");
				if (log.isDebugEnabled())
					log.debug("Exception", e);
			}
		}
	}

	public static void main(String[] args) {
		
		System.setProperty("java.util.logging.config.file", "./config/logging.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(ClassifierTrainer.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/default.xml");
		ClassifierTrainer trainer = (ClassifierTrainer) context.getBean("classifierTrainer");
		((AbstractApplicationContext) context).close();
		
		trainer.run();
	}

}
