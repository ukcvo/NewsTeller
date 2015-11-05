package edu.kit.anthropomatik.isl.newsTeller.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.newsTeller.NewsTeller;

/**
 * Main executable for interactive testing purposes.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class Main {

	private static Log log = LogFactory.getLog(Main.class);
	
	private NewsTeller newsTeller;
	
	public void setNewsTeller(NewsTeller newsTeller) {
		this.newsTeller = newsTeller;
	}
		
	// command-line interface to NewsTeller
	private void run() {
		if (log.isTraceEnabled())
			log.trace("run()");
		
		Scanner in = new Scanner(System.in);
		
		System.out.print("> ");
		String input = in.nextLine();
		
		while (!input.equalsIgnoreCase("quit")) {
			List<Keyword> keywords = new ArrayList<Keyword>();
			
			String[] words = input.split(",");
			for (String word : words) {
				keywords.add(new Keyword(word.trim()));
			}
			
			System.out.println(newsTeller.getNews(keywords));
			System.out.print("> ");
			input = in.nextLine();
		}
		
		in.close();
	}
	
	public static void main(String[] args) {
		if (log.isTraceEnabled())
			log.trace("starting the program");
		
		String configFile = "config/default.xml";
		if (args.length >= 1)
			configFile = args[0];
		
		ApplicationContext context = new FileSystemXmlApplicationContext(configFile);
		Main m = (Main) context.getBean("main");
		((AbstractApplicationContext) context).close();
		
		m.run();
		
		if (log.isTraceEnabled())
			log.trace("shutting down");
	}

}
