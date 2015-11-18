package edu.kit.anthropomatik.isl.newsTeller.newsTeller;

import java.util.List;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import edu.kit.anthropomatik.isl.newsTeller.data.NewsEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.generation.SummaryCreator;
import edu.kit.anthropomatik.isl.newsTeller.knowledgeStore.KnowledgeStoreAdapter;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.EventRetriever;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.selecting.EventSelector;
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
	
	private SummaryCreator generator;
	
	private KnowledgeStoreAdapter ksAdapter;
	
	//region setters
	public void setUserModel(UserModel userModel) {
		this.userModel = userModel;
	}

	public void setRetriever(EventRetriever retriever) {
		this.retriever = retriever;
	}

	public void setSelector(EventSelector selector) {
	}

	public void setGenerator(SummaryCreator generator) {
		this.generator = generator;
	}
	
	public void setKsAdapter(KnowledgeStoreAdapter ksAdapter) {
		this.ksAdapter = ksAdapter;
	}
	//endregion 

	public NewsTeller(String loggingFileName) {
		// setting up logger configuration
		System.setProperty("java.util.logging.config.file", loggingFileName);
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(NewsTeller.class);
		} catch (Exception e) {
			if (log.isWarnEnabled())
				log.warn("Can't access logger config file!");
			if (log.isDebugEnabled())
				log.debug("Logger config file access failed", e);
		}
	}
	
	public String getNews(List<Keyword> userQuery) {
		if (log.isTraceEnabled())
			log.trace(String.format("getNews(user query = <%s>)", StringUtils.collectionToCommaDelimitedString(userQuery)));
		
		if (!this.ksAdapter.isConnectionOpen())
			ksAdapter.openConnection();
		
		NewsEvent selectedEvent = retriever.retrieveEvent(userQuery, userModel);
		String summary = generator.summarizeEvent(selectedEvent);
		
		return summary;
	}
	
	/**
	 * Takes care of shutting down all threadpools.
	 */
	public void shutDown() {
		this.retriever.shutDown();
		this.ksAdapter.closeConnection();
	}
}
