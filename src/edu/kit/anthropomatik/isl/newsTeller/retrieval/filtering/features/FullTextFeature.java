package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Checks if some kind of Strings (to be determined by subclass) appear in the full text.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public abstract class FullTextFeature extends UsabilityFeature {

	private static final Log log = LogFactory.getLog(FullTextFeature.class);
	
	private boolean doOnlyUseSentence;
	
	protected boolean doUseContainsInsteadOfRegex;
	
	private boolean usesKeyword;
	
	private ExecutorService threadPool;
	
	public void setDoOnlyUseSentence(boolean doOnlyUseSentence) {
		this.doOnlyUseSentence = doOnlyUseSentence;
	}
	
	public void setDoUseContainsInsteadOfRegex(boolean doUseContainsInsteadOfRegex) {
		this.doUseContainsInsteadOfRegex = doUseContainsInsteadOfRegex;
	}
	
	public void setUsesKeyword(boolean usesKeyword) {
		this.usesKeyword = usesKeyword;
	}
	
	public FullTextFeature() {
		super();
		this.threadPool = Executors.newCachedThreadPool();//Executors.newFixedThreadPool(5000);//Executors.newCachedThreadPool();
	}

	public void shutDown() {
		this.threadPool.shutdown();
	}
	
	
	private class LabelPartWorker implements Callable<Double> {
		
		private String labelPart;
		private String[] lowerCaseText;
		
		public LabelPartWorker(String labelPart, String[] lowerCaseText) {
			this.labelPart = labelPart;
			this.lowerCaseText = lowerCaseText;
		}

		@Override
		public Double call() throws Exception {
			String regex = Util.KEYWORD_REGEX_PREFIX_JAVA + labelPart.toLowerCase() + Util.KEYWORD_REGEX_SUFFIX_JAVA;
						
			for (String line : lowerCaseText) {
				if (line.isEmpty())
					continue;
				if ((doUseContainsInsteadOfRegex && line.contains(labelPart)) || (line.matches(regex))) { // ignoring case
					return 1.0;
				}
			}
			return 0.0;
		}
	}
	
	private class TextWorker implements Callable<Double> {
		
		private String text;
		private List<String> labelParts;
		
		public TextWorker(List<String> labelParts, String text) {
			this.labelParts = labelParts;
			this.text = text;
		}

		@Override
		public Double call() throws Exception {

			String[] lowerCaseText = text.toLowerCase().split("\n");
			double sum = 0.0;
			List<Future<Double>> futures = new ArrayList<Future<Double>>();
			for (String labelPart : labelParts) { // compute fraction of label parts that are actually mentioned
				if (labelPart.isEmpty())
					continue;
				futures.add(threadPool.submit(new LabelPartWorker(labelPart, lowerCaseText)));
//				String regex = Util.KEYWORD_REGEX_PREFIX_JAVA + labelPart.toLowerCase() + Util.KEYWORD_REGEX_SUFFIX_JAVA;
//				for (String line : lowerCaseText) {
//					if (line.isEmpty())
//						continue;
//					if ((doUseContainsInsteadOfRegex && line.contains(labelPart)) || (line.matches(regex))) { // ignoring case
//						sum++;
//						break;
//					}
//				}
				
			}
			
			for (Future<Double> future : futures) {
				try {
					sum += future.get();
				} catch (Exception e) {
					if (log.isWarnEnabled())
						log.warn("Thread execution somehow failed!");
					if (log.isDebugEnabled())
						log.debug("Thread execution error", e);
				}
			}
			
			sum /= labelParts.size();
			
			return sum;
		}
	}
	
	private double checkLabel(List<String> labelParts, Set<String> originalTexts) {
		double max = 0.0;
//		for (String text : originalTexts) { // take max over all texts
//			
//			String[] lowerCaseText = text.toLowerCase().split("\n");
//			double sum = 0.0;
//			for (String labelPart : labelParts) { // compute fraction of label parts that are actually mentioned
//				if (labelPart.isEmpty())
//					continue;
//				String regex = Util.KEYWORD_REGEX_PREFIX_JAVA + labelPart.toLowerCase() + Util.KEYWORD_REGEX_SUFFIX_JAVA;
//				for (String line : lowerCaseText) {
//					if (line.isEmpty())
//						continue;
//					if ((this.doUseContainsInsteadOfRegex && line.contains(labelPart)) || (line.matches(regex))) { // ignoring case
//						sum++;
//						break;
//					}
//				}
//				
//			}
//			sum /= labelParts.size();
//			if (sum > max)
//				max = sum;
//		}
		
		List<Future<Double>> futures = new ArrayList<Future<Double>>();
		for (String text : originalTexts)
			futures.add(threadPool.submit(new TextWorker(labelParts, text)));
		
		for (Future<Double> future : futures) {
			try {
				max = Math.max(max, future.get());
			} catch (Exception e) {
				if (log.isWarnEnabled())
					log.warn("Thread execution somehow failed!");
				if (log.isDebugEnabled())
					log.debug("Thread execution error", e);
			}
		}
		
		return max;
	}

	private class LabelWorker implements Callable<Double> {
		
		private List<String> labels;
		private Set<String> originalTexts;
		
		public LabelWorker(List<String> labels, Set<String> originalTexts) {
			this.labels = labels;
			this.originalTexts = originalTexts;
		}

		@Override
		public Double call() throws Exception {
			return checkLabel(labels, originalTexts);
		}
	}
	
	private class EntityWorker implements Callable<Double> {

		private List<List<String>> labels;
		private Set<String> originalTexts;
		
		public EntityWorker(List<List<String>> labels, Set<String> originalTexts) {
			this.labels = labels;
			this.originalTexts = originalTexts;
		}
		
		@Override
		public Double call() throws Exception {

			double appeared = 0;
			
			List<Future<Double>> futures = new ArrayList<Future<Double>>();
			
			for (List<String> labelParts : labels) 
				futures.add(threadPool.submit(new LabelWorker(labelParts, originalTexts)));
			
			for (Future<Double> future : futures) {
				try {
					appeared = Math.max(appeared, future.get());
				} catch (Exception e) {
					if (log.isWarnEnabled())
						log.warn("Thread execution somehow failed!");
					if (log.isDebugEnabled())
						log.debug("Thread execution error", e);
				}
			}
			
			return appeared;
			
//			double appeared = 0;
//			for (List<String> labelParts : labels) {
//				appeared = checkLabel(labelParts, originalTexts);
//				if (appeared > 0)
//					break;
//			}
//			return appeared;
		}
		
	}
	
	// entityLabels: keyword-dbPediaLabels-labelParts
	private List<Double> checkLabels(List<List<List<String>>> entityLabels, Set<String> originalTexts) {
		List<Double> result = new ArrayList<Double>();
		
		List<Future<Double>> futures = new ArrayList<Future<Double>>();
		for (List<List<String>> labels : entityLabels) {
			futures.add(this.threadPool.submit(new EntityWorker(labels, originalTexts)));
		}
		
		for (Future<Double> future : futures) {
			try {
				result.add(future.get());
			} catch (Exception e) {
				if (log.isWarnEnabled())
					log.warn("Thread execution somehow failed!");
				if (log.isDebugEnabled())
					log.debug("Thread execution error", e);
			}
		}
		
		return result;
	}

	public long getLabelTime = 0;
	public long getTextsTime = 0;
	public long checkLabelsTime = 0;
	public long aggregationTime = 0;
	
	@Override
	public double getValue(String eventURI, List<Keyword> keywords) {
		long t = System.currentTimeMillis();
		List<List<List<String>>> labels = getLabels(eventURI, keywords);
		getLabelTime += System.currentTimeMillis() - t;
		t = System.currentTimeMillis();
		String arbitraryKeyword = keywords.get(0).getWord();
		Set<String> originalTexts;
		if (this.doOnlyUseSentence)
			originalTexts = new HashSet<String>(ksAdapter.retrieveSentencesFromEvent(eventURI, arbitraryKeyword)); //use only sentence
		else
			originalTexts = ksAdapter.retrieveOriginalTexts(eventURI, arbitraryKeyword); //use complete text
		getTextsTime += System.currentTimeMillis() - t;
		t = System.currentTimeMillis();
		List<Double> appearances = checkLabels(labels, originalTexts);
		checkLabelsTime += System.currentTimeMillis() - t;
		t = System.currentTimeMillis();
		double averageAppearance;
		if (appearances.isEmpty())
			averageAppearance = 1.0;
		else {
			if (this.usesKeyword)
				averageAppearance = Util.maxFromCollection(appearances);
			else
				averageAppearance = Util.averageFromCollection(appearances);
		}
		aggregationTime += System.currentTimeMillis() - t;
		
		return averageAppearance;
	}
	
	@Override
	public Set<String> getRequiredMentionProperties() {
		return new HashSet<String>();
	}
	
	/**
	 * Finds the labels to check. Outermost list is for different keywords, middle-layer list is for different labels, innermost list is for label parts.
	 */
	protected abstract List<List<List<String>>> getLabels(String eventURI, List<Keyword> keywords);
}