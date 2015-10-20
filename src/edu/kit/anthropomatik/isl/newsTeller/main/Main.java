package edu.kit.anthropomatik.isl.newsTeller.main;

import java.io.IOException;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class Main {

	private static Log log;
	
	private String msg;
	
	public String getMsg() {
		return msg;
	}


	public void setMsg(String msg) {
		this.msg = msg;
	}

	public void beep() {
		System.out.println(msg);
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
		
		String configFile = "config/Scope0.xml"; // use Scope0.xml as default
		if (args.length >= 1)
			configFile = args[0];
		
		ApplicationContext context = new FileSystemXmlApplicationContext(configFile);
		Main m = (Main) context.getBean("main");
		((AbstractApplicationContext) context).close();
		
		m.beep();
		
		log.info("shutting down");
	}

}
