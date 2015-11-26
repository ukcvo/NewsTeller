package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features.UsabilityFeature;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

public class ParallelEventFilter implements IEventFilter {

private static Log log = LogFactory.getLog(SequentialEventFilter.class);
	
	private Classifier classifier;
	
	private Instances header;
	
	private List<UsabilityFeature> features;
	
	private ExecutorService threadPool;
	
	public void setFeatures(List<UsabilityFeature> features) {
		this.features = features;
	}
	
	public void setNThreads(int nThreads) {
		this.threadPool = Executors.newFixedThreadPool(nThreads);
	}
	
	public ParallelEventFilter(String classifierFileName) {
		try {
			Object[] input = SerializationHelper.readAll(classifierFileName);
			this.classifier = (Classifier) input[0];
			this.header = (Instances) input[1];
		} catch (Exception e) {
			if (log.isFatalEnabled())
				log.fatal(String.format("Can't read classifier from file: '%s'", classifierFileName));
			if (log.isDebugEnabled())
				log.debug("can't read classifier from file", e);
		}
	}
	
	private class EventWorker implements Runnable {

		private NewsEvent event;
		
		private ConcurrentMap<NewsEvent, Instance> map;
		
		public EventWorker(NewsEvent event, ConcurrentMap<NewsEvent, Instance> map) {
			this.event = event;
			this.map = map;
		}
		
		public void run() {
			double[] values = new double[features.size() + 1];
			
			for (int i = 0; i < features.size(); i++) {
				UsabilityFeature f = features.get(i);
				values[i] = f.getValue(event.getEventURI());
			}
			
			Instance example = new Instance(1.0, values);
			
			map.putIfAbsent(event, example);
		}
		
	}
	
	public Set<NewsEvent> filterEvents(Set<NewsEvent> events) {
		
		Set<NewsEvent> result = new HashSet<NewsEvent>();
		
		List<Future<?>> futures = new ArrayList<Future<?>>();
		ConcurrentMap<NewsEvent, Instance> resultMap = new ConcurrentHashMap<NewsEvent, Instance>();
		
		// parallel feature extraction
		for (NewsEvent e : events) {
			EventWorker w = new EventWorker(e, resultMap);
			futures.add(threadPool.submit(w));
		}
		
		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("thread execution somehow failed!");
				if (log.isDebugEnabled())
					log.debug("thread execution exception", e);
			}
		}
		
		// sequential classification
		for (NewsEvent event : events) {
			boolean isUsable;
			try {
				double label = classifier.classifyInstance(resultMap.get(event));
				isUsable = (label == header.attribute("usable").indexOfValue("true"));
			} catch (Exception e) {
				if (log.isWarnEnabled())
					log.warn(String.format("Could not classify event, setting classification to false: %s", event.toVerboseString()));
				isUsable = false;
			}
			
			if (isUsable)
				result.add(event);
		}
		
		return result;
	}

	public void shutDown() {
		this.threadPool.shutdown();
	}

}
