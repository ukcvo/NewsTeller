package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;

/**
 * Filters events based on the posterior probability estimated by naive bayes and a threshold.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class ParallelBayesEventFilter implements IEventFilter {

	private static Log log = LogFactory.getLog(ParallelBayesEventFilter.class);
	
	private double threshold;
	
	private NaiveBayesFusion bayes;
	
	private ExecutorService threadPool;
	
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public void setBayes(NaiveBayesFusion bayes) {
		this.bayes = bayes;
	}
	
	public void setNThreads(int nThreads) {
		if (nThreads == 0)
			this.threadPool = Executors.newCachedThreadPool();
		else
			this.threadPool = Executors.newFixedThreadPool(nThreads);
	}
	
	private class FilterWorker implements Runnable {

		private NewsEvent event;
		
		private ConcurrentMap<NewsEvent, Double> resultMap;
		
		public FilterWorker(NewsEvent event, ConcurrentMap<NewsEvent, Double> resultMap) {
			this.event = event;
			this.resultMap = resultMap;
		}
		
		public void run() {
			double probability = bayes.getProbabilityOfEvent(event);
			resultMap.putIfAbsent(event, probability);
		}
		
	}
	
	/**
	 * Filter the given set of events based on the aggregated scores.
	 */
	public Set<NewsEvent> filterEvents(Set<NewsEvent> events) {
		if (log.isTraceEnabled())
			log.trace(String.format("filterEvents(events = <%s>)", StringUtils.collectionToCommaDelimitedString(events)));
			
		Set<NewsEvent> result = new HashSet<NewsEvent>();
		ConcurrentMap<NewsEvent, Double> probabilityMap = new ConcurrentHashMap<NewsEvent, Double>();
		List<Future<?>> futures = new ArrayList<Future<?>>();
		
		for (NewsEvent event : events) {
			FilterWorker w = new FilterWorker(event, probabilityMap);
			futures.add(threadPool.submit(w));
		}
		
		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("Thread execution somehow failed");
				if (log.isDebugEnabled())
					log.debug("Thread execution exception", e);
			} 
		}
		
		for (Map.Entry<NewsEvent, Double> entry : probabilityMap.entrySet()) {
			if (entry.getValue() >= threshold)
				result.add(entry.getKey());
		}
		
		if(log.isDebugEnabled())
			log.debug(String.format("keeping %d out of %d events", result.size(), events.size()));
		
		if(log.isTraceEnabled())
			log.trace(String.format("events kept: <%s>", StringUtils.collectionToCommaDelimitedString(result)));
		
		return result;
	}
	
	public void shutDown() {
		this.threadPool.shutdown();
	}
}
