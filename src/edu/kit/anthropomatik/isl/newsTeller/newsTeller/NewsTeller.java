package edu.kit.anthropomatik.isl.newsTeller.newsTeller;

import java.util.List;
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
	
	private UserModel userModel;
	
	private EventRetriever retriever;
	
	private EventSelector selector;
	
	private SummaryCreator generator;
	
	public UserModel getUserModel() {
		return userModel;
	}

	public void setUserModel(UserModel userModel) {
		this.userModel = userModel;
	}

	public EventRetriever getRetriever() {
		return retriever;
	}

	public void setRetriever(EventRetriever retriever) {
		this.retriever = retriever;
	}

	public EventSelector getSelector() {
		return selector;
	}

	public void setSelector(EventSelector selector) {
		this.selector = selector;
	}

	public SummaryCreator getGenerator() {
		return generator;
	}

	public void setGenerator(SummaryCreator generator) {
		this.generator = generator;
	}



	public String getNews(List<Keyword> userQuery) {
		return "";
	}
	
}
