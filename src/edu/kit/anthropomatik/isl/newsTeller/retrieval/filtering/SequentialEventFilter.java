package edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.features.UsabilityFeature;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

/**
 * Filter events in a sequential manner using a WEKA classifier.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class SequentialEventFilter implements IEventFilter {

	private static Log log = LogFactory.getLog(SequentialEventFilter.class);
	
	private Classifier classifier;
	
	private Instances header;
	
	private List<UsabilityFeature> features;
	
	public void setFeatures(List<UsabilityFeature> features) {
		this.features = features;
	}
	
	public SequentialEventFilter(String classifierFileName) {
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
	
	public Set<NewsEvent> filterEvents(Set<NewsEvent> events) {
		
		Set<NewsEvent> result = new HashSet<NewsEvent>();
		
		for (NewsEvent event : events) {
			double[] values = new double[features.size() + 1];
			
			for (int i = 0; i < features.size(); i++) {
				UsabilityFeature f = features.get(i);
				values[i] = f.getValue(event.getEventURI());
			}
			
			Instance example = new Instance(1.0, values);
			
			//TODO: feature preprocessing (binning etc)
			boolean isUsable;
			try {
				double label = classifier.classifyInstance(example);
				isUsable = (label == header.attribute(Util.ATTRIBUTE_USABLE).indexOfValue(Util.CLASS_LABEL_POSITIVE));
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
		// nothing to do
	}

}
