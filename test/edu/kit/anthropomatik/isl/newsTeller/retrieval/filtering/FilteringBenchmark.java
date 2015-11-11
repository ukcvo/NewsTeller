package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

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

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.GroundTruth;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.heuristics.WordNetVerbCountDeterminer;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

public class FilteringBenchmark {

	private static Log log;
	
	private boolean doWordNetAnalysis;
	
	private WordNetVerbCountDeterminer wordNetFeature;
	
	private Map<String,List<Keyword>> benchmarkFiles;
	
	private Map<String,Map<String,GroundTruth>> benchmark;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	public void setDoWordNetAnalysis(boolean doWordNetAnalysis) {
		this.doWordNetAnalysis = doWordNetAnalysis;
	}
	
	public void setWordNetFeature(WordNetVerbCountDeterminer wordNetFeature) {
		this.wordNetFeature = wordNetFeature;
	}
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	
	public FilteringBenchmark(String configFileName) {
		this.benchmarkFiles = Util.readBenchmarkConfigFile(configFileName);
		this.benchmark = new HashMap<String, Map<String,GroundTruth>>();
		for (String fileName : benchmarkFiles.keySet()) {
			this.benchmark.put(fileName, Util.readBenchmarkQueryFromFile(fileName));
		}
	}
	
	private void analyzeVerbNetFeature() {
		if(log.isTraceEnabled())
			log.trace("analyzeVerbNetFeature()");
		
		// collect labels
		Set<String> labels = new HashSet<String>();
		ksAdapter.openConnection();
		for (Map<String,GroundTruth> line : this.benchmark.values()) {
			for (String eventURI : line.keySet()) {
				labels.addAll(ksAdapter.runSingleVariableStringQuery(
						Util.readStringFromFile("resources/SPARQL/usability/areLabelsVerbs.qry").replace("*e*", eventURI), Util.VARIABLE_LABEL));
			}
		}
		ksAdapter.closeConnection();
		
		// collect values
		List<Double> values = new ArrayList<Double>();
		for (String label : labels) {
			values.add(wordNetFeature.getLabelVerbFrequency(label));
		}
		
		// compute mean
		double meanSum = 0;
		for (Double val : values)
			meanSum += val;
		double mean = meanSum / values.size();
		
		// compute variance
		double varSum = 0;
		for (Double val : values)
			varSum += Math.pow((mean - val), 2);
		double variance = varSum / values.size();
		
		// compute bins
		int[] bins = new int[20];
		for (Double val : values)
			bins[(int)(Math.min(val, 1.0 - Util.EPSILON)*20)]++;
		
		// get counts for 0.0 and 1.0
		int numberOfZeroes = 0;
		int numberOfOnes = 0;
		for (Double val : values) {
			if ((1.0 - val) < Util.EPSILON)
				numberOfOnes++;
			if (val < Util.EPSILON)
				numberOfZeroes++;
		}
		
		// output everything
		if (log.isInfoEnabled()) {
			log.info(String.format("mean: %f, variance %f", mean, variance));
			log.info(String.format("total: %d, zeroes: %d, ones: %d", values.size(), numberOfZeroes, numberOfOnes));
			for (int i = 0; i < bins.length; i++)
				log.info(String.format("bin %d (%f - %f): %d", i, (i / 20.0), ((i + 1) / 20.0), bins[i]));
		}
	}
	
	public void run() {
		if (this.doWordNetAnalysis)
			analyzeVerbNetFeature();
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
		FilteringBenchmark benchmark = (FilteringBenchmark) context.getBean("filteringBenchmark");
		((AbstractApplicationContext) context).close();

		benchmark.run();
	}

}
