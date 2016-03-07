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
import edu.kit.anthropomatik.isl.newsTeller.userModel.ActualUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.DummyUserModel;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;

/**
 * Main executable for interactive testing purposes.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class Main {

	private static Log log = LogFactory.getLog(Main.class);
	
	private NewsTeller newsTellerUM;
	private NewsTeller newsTellerNoUM;
	
	public void setNewsTellerUM(NewsTeller newsTellerUM) {
		this.newsTellerUM = newsTellerUM;
	}
	
	public void setNewsTellerNoUM(NewsTeller newsTellerNoUM) {
		this.newsTellerNoUM = newsTellerNoUM;
	}
		
	// command-line interface to NewsTeller
	private void run() {
		if (log.isTraceEnabled())
			log.trace("run()");
		
		Scanner in = new Scanner(System.in);
		
		System.out.print("> ");
		String input = in.nextLine();
		
		UserModel um = new DummyUserModel();
		NewsTeller newsTeller = this.newsTellerNoUM;
		
		while (!input.equalsIgnoreCase("quit")) {
			
			if (input.equals("UM")) {
				List<Keyword> interests = new ArrayList<Keyword>();
				System.out.print("> ");
				input = in.nextLine();
				String[] tokens = input.split(",");
				for (String token : tokens) {
					Keyword k = new Keyword(token);
					Util.stemKeyword(k);
					interests.add(k);
				}
				um = new ActualUserModel(interests);
				newsTeller = this.newsTellerUM;
				
			} else if (input.equals("noUM")) {
				um = new DummyUserModel();
				newsTeller = this.newsTellerUM;
				
			} else {
				List<Keyword> keywords = new ArrayList<Keyword>();
				
				String[] words = input.split(",");
				for (String word : words) {
					keywords.add(new Keyword(word.trim()));
				}
				
				System.out.println(newsTeller.getNews(keywords, um));
				
			}
			
			System.out.print("> ");
			input = in.nextLine();
		}
		
		in.close();
		newsTellerUM.shutDown();
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
