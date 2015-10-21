package edu.kit.anthropomatik.isl.newsTeller.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.generation.SummaryCreator;
import edu.kit.anthropomatik.isl.newsTeller.newsTeller.NewsTeller;

/**
 * Main executable for interactive testing purposes.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class Main {

	private static Log log;
	
	private NewsTeller newsTeller;
	
	// TODO: remove this part as soon as first test is written
	private String msg;
	
	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public void setNewsTeller(NewsTeller newsTeller) {
		this.newsTeller = newsTeller;
	}
	
	public void beep() {
		System.out.println(msg);
	}

	// command-line interface to NewsTeller
	private void run() {
		System.out.println(newsTeller.getNews(new ArrayList<Keyword>()));
	}
	
	public static void main(String[] args) {

		// setting up logger configuration
		System.setProperty("java.util.logging.config.file", "./config/logging.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(Main.class);
		} catch (SecurityException e) {
			log.error("Can't access logger config file! " + e.toString());
		} catch (IOException e) {
			log.error("Can't access logger config file! " + e.toString());
		}
		
		log.info("starting the program");
		
		String configFile = "config/default.xml";
		if (args.length >= 1)
			configFile = args[0];
		
		ApplicationContext context = new FileSystemXmlApplicationContext(configFile);
		Main m = (Main) context.getBean("main");
		((AbstractApplicationContext) context).close();
		
		m.run();
		
		log.info("shutting down");
	}

}
