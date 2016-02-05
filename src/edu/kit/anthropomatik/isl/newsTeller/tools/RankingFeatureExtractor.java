package edu.kit.anthropomatik.isl.newsTeller.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.BenchmarkEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.GroundTruth;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features.RankingFeature;
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.XRFFSaver;

/**
 * Extracts features for the regression ranking problem.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class RankingFeatureExtractor {

	private static Log log = LogFactory.getLog(UsabilityFeatureExtractor.class);

	private Map<List<Keyword>, Map<BenchmarkEvent, GroundTruth>> benchmark; // mapping
																			// from
																			// keywords
																			// to
																			// events
																			// and
																			// corresponding
																			// labels
																			// /
																			// ratings

	private Set<String> eventURIs;

	private List<RankingFeature> features;

	private String outputFileName;

	private KnowledgeStoreAdapter ksAdapter;

	public void setFeatures(List<RankingFeature> features) {
		this.features = features;
	}

	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}

	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}

	public RankingFeatureExtractor(String configFileName) {
		Map<String, List<Keyword>> benchmarkKeywords = Util.readBenchmarkConfigFile(configFileName);
		this.benchmark = new HashMap<List<Keyword>, Map<BenchmarkEvent, GroundTruth>>();
		this.eventURIs = new HashSet<String>();

		for (String fileName : benchmarkKeywords.keySet()) {
			Map<BenchmarkEvent, GroundTruth> fileContent = Util.readBenchmarkQueryFromFile(fileName);
			this.benchmark.put(benchmarkKeywords.get(fileName), fileContent);
			for (BenchmarkEvent e : fileContent.keySet())
				this.eventURIs.add(e.getEventURI());
		}
	}

	private Instances createDataSetSkeleton() {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();

		for (RankingFeature feature : this.features) {
			Attribute attr = new Attribute(feature.getName());
			attributes.add(attr);
		}

		attributes.add(new Attribute(Util.ATTRIBUTE_RELEVANCE));

		int numberOfExpectedExamples = this.benchmark.size();
		Instances dataSet = new Instances("rankingTest", attributes, numberOfExpectedExamples);
		dataSet.setClass(dataSet.attribute(Util.ATTRIBUTE_RELEVANCE));

		return dataSet;
	}

	private void writeDataSet(Instances dataSet) {
		try {
			XRFFSaver saver = new XRFFSaver();
			saver.setInstances(dataSet);
			saver.setFile(new File(outputFileName));
			saver.writeBatch();
		} catch (IOException e) {
			if (log.isErrorEnabled())
				log.error("Could not write extracted features to file");
			if (log.isDebugEnabled())
				log.debug("I/O exception", e);
		}
	}

	public void run() {
		Instances dataSet = createDataSetSkeleton();
		this.ksAdapter.openConnection();

		for (Map.Entry<List<Keyword>, Map<BenchmarkEvent, GroundTruth>> entry : this.benchmark.entrySet()) {
			List<Keyword> keywords = entry.getKey();
			Map<BenchmarkEvent, GroundTruth> content = entry.getValue();
			if (log.isInfoEnabled())
				log.info(StringUtils.collectionToCommaDelimitedString(keywords));
			
			// run the queries
			ksAdapter.runKeyValueMentionFromEventQuery(eventURIs, keywords);
			Set<String> resourceURIs = Util.resourceURIsFromMentionURIs(ksAdapter.getAllRelationValues(Util.getRelationName("event", "mention", keywords.get(0).getWord())));
			ksAdapter.runKeyValueResourceTextQuery(resourceURIs);
			if (log.isInfoEnabled())
				log.info("...queries done");
			
			// compute the features
			for (Map.Entry<BenchmarkEvent, GroundTruth> innerEntry : content.entrySet()) {

				BenchmarkEvent event = innerEntry.getKey();
				GroundTruth gt = innerEntry.getValue();
				double relevance = gt.getRegressionRelevanceValue();
				UserModel userModel = new DummyUserModel();

				double[] values = new double[this.features.size() + 1];
				for (int i = 0; i < this.features.size(); i++) {
					RankingFeature f = features.get(i);
					values[i] = f.getValue(event.getEventURI(), keywords, userModel);
				}
				values[this.features.size()] = relevance;

				Instance instance = new DenseInstance(1.0, values);
				dataSet.add(instance);
			}
			if (log.isInfoEnabled())
				log.info("...features done");
		}

		if (log.isInfoEnabled())
			log.info("done");

		this.ksAdapter.closeConnection();
		writeDataSet(dataSet);
	}

	public static void main(String[] args) {
		System.setProperty("java.util.logging.config.file", "./config/logging.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(UsabilityFeatureExtractor.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/default.xml");
		RankingFeatureExtractor extractor = (RankingFeatureExtractor) context.getBean("rankingFeatureExtractor");
		((AbstractApplicationContext) context).close();

		extractor.run();
	}

}
