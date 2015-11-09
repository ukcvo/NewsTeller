package edu.kit.anthropomatik.isl.newsTeller.retrieval;

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
import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.newsTeller.NewsTellerTest;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.filtering.EventFilter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.finding.EventFinder;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.scoring.UsabilityScorer;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Performs the retrieval benchmark.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class RetrievalBenchmark {

	private static Log log;

	private EventFinder finder;
	private UsabilityScorer scorer;
	private EventFilter filter;
	private UserModel userModel;
	private String configFileName;

	// region setters
	public void setFinder(EventFinder finder) {
		this.finder = finder;
	}

	public void setScorer(UsabilityScorer scorer) {
		this.scorer = scorer;
	}

	public void setFilter(EventFilter filter) {
		this.filter = filter;
	}
	
	public void setUserModel(UserModel userModel) {
		this.userModel = userModel;
	}

	public void setConfigFileName(String configFileName) {
		this.configFileName = configFileName;
	}
	// endregion

	/**
	 * Run the benchmark.
	 */
	public void run() {
		if (log.isTraceEnabled())
			log.trace("run()");

		Map<String, List<Keyword>> keywordFiles = Util.readBenchmarkConfigFile(this.configFileName);

		double totalScore = 0;

		for (String fileName : keywordFiles.keySet()) {
			double fileScore = 0;
			Map<String,GroundTruth> groundTruth = Util.readBenchmarkQueryFromFile(fileName);
			List<Keyword> keywords = keywordFiles.get(fileName);

			// find events
			Set<NewsEvent> events = finder.findEvents(keywords, userModel);
			if (events.size() != groundTruth.size()) {
				if (log.isWarnEnabled())
					log.warn(String.format("unexpected number of events (expected %d, found %d): %s", groundTruth.size(), events.size(), fileName));
				if (log.isDebugEnabled()) {
					Set<String> difference = new HashSet<String>();
					difference.addAll(groundTruth.keySet());
					for (NewsEvent e : events) {
						difference.remove(e.toString());
					}
					log.debug(StringUtils.collectionToDelimitedString(difference, "\n"));
				}
			}
			
			// score and filter events
			scorer.scoreEvents(events, keywords, userModel);
			@SuppressWarnings("unused") //TODO: remove
			Set<NewsEvent> filteredEvents = filter.filterEvents(events);
						
			for (NewsEvent event : events) {
				
				double eventScore;
				
				if (groundTruth.containsKey(event.getEventURI())) {
					eventScore  = event.getTotalUsabilityScore() * groundTruth.get(event.getEventURI()).getUsabilityRating();
				} else {
					if (log.isWarnEnabled())
						log.warn(String.format("event not in ground truth, giving score 0: %s", event.getEventURI()));
					eventScore = 0;
				}
				
				if (log.isTraceEnabled())
					log.trace(String.format("%s,%f,%f,%f", 
								event.getEventURI(), event.getTotalUsabilityScore(), groundTruth.get(event.getEventURI()).getUsabilityRating(), eventScore));
				fileScore += eventScore;
			}
			fileScore /= events.size();
			if (log.isInfoEnabled())
				log.info(String.format("%s,%f", fileName, fileScore));
			
			totalScore += fileScore;
		}

		totalScore /= keywordFiles.size();
		
		if (log.isInfoEnabled())
			log.info(String.format("overall benchmark score: %f", totalScore));
	}

	public static void main(String[] args) {
		System.setProperty("java.util.logging.config.file", "./config/logging-test.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(NewsTellerTest.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/test.xml");
		RetrievalBenchmark benchmark = (RetrievalBenchmark) context.getBean("benchmark");
		((AbstractApplicationContext) context).close();

		benchmark.run();
	}
}
