package edu.kit.anthropomatik.isl.newsTeller.main;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class Main {

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

		Log log = LogFactory.getLog(Main.class);
		log.info("starting");
		
		ApplicationContext context = new FileSystemXmlApplicationContext("config/Scope0.xml");
		Main m = (Main) context.getBean("main");
		((AbstractApplicationContext) context).close();
		log.debug("about to beep");
		m.beep();
		log.warn("shutting down!");
		
	}

}
