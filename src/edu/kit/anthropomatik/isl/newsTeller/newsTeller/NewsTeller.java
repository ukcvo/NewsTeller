package edu.kit.anthropomatik.isl.newsTeller.newsTeller;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.generation.SummaryCreator;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.EventRetriever;
import edu.kit.anthropomatik.isl.newsTeller.selection.EventSelector;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

/**
 * High-level access to the NewsTeller system and its services. Entry-point from the outside.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class NewsTeller {
	
	private Log log;
	
	private UserModel userModel;
	
	private EventRetriever retriever;
	
	private EventSelector selector;
	
	private SummaryCreator generator;
	
	//region setters
	public void setUserModel(UserModel userModel) {
		this.userModel = userModel;
	}

	public void setRetriever(EventRetriever retriever) {
		this.retriever = retriever;
	}

	public void setSelector(EventSelector selector) {
		this.selector = selector;
	}

	public void setGenerator(SummaryCreator generator) {
		this.generator = generator;
	}
	//endregion 

	public NewsTeller(String loggingFileName) {
		// setting up logger configuration
		System.setProperty("java.util.logging.config.file", loggingFileName);
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(NewsTeller.class);
		} catch (SecurityException e) {
			log.error("Can't access logger config file! " + e.toString());
		} catch (IOException e) {
			log.error("Can't access logger config file! " + e.toString());
		}
	}
	
	public String getNews(List<Keyword> userQuery) {
		
		if (log.isInfoEnabled())
			log.info("user query: " + StringUtils.collectionToCommaDelimitedString(userQuery));
		
		List<URI> events = retriever.retrieveEvents(userQuery, userModel);
		URI selectedEvent = selector.selectEvent(events, userQuery, userModel);
		String summary = generator.summarizeEvent(selectedEvent);
		
		return summary;
	}
	
}
