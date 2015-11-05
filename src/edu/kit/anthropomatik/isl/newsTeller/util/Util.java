package edu.kit.anthropomatik.isl.newsTeller.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jumpmind.symmetric.csv.CsvReader;

/**
 * Provides some static utility functions.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class Util {

	private static Log log = LogFactory.getLog(Util.class);
	
	public static final String PLACEHOLDER_EVENT = "*e*";
	public static final String PLACEHOLDER_KEYWORD = "*k*";
	public static final String PLACEHOLDER_HISTORICAL_EVENT = "*h*";
	public static final String PLACEHOLDER_MENTION = "*m*";
	
	public static final String VARIABLE_EVENT = "event";
	public static final String VARIABLE_NUMBER = "number";
	public static final String VARIABLE_MENTION = "mention";
	public static final String VARIABLE_RESOURCE = "resource";

	public static final String COLUMN_NAME_URI = "URI";
	public static final String COLUMN_NAME_RATING = "rating";
	
	public static final double EPSILON = 0.00001;
	
	// private constructor to prevent instantiation
	private Util() {}
	
	/**
	 * Reads the file given by the fileName and returns the contained String.
	 */
	public static String readStringFromFile(String fileName) {
		return readStringFromFile(new File(fileName));
	}
	
	/**
	 * Reads the given queryFile and returns the contained String.
	 */
	public static String readStringFromFile(File queryFile) {
		if (log.isTraceEnabled())
			log.trace(String.format("readQueryFromFile(queryFile = '%s')", queryFile.toString()));
		
		String result = "";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(queryFile), "UTF8"));
			String str;
	        while ((str = in.readLine()) != null) {
	            if (result.isEmpty())
	            	result = str;
	            else
	            	result = result + "\n" + str;
	        }
	        in.close();
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(String.format("could not read query file, returning empty string: '%s'", queryFile.toString()));
			if (log.isDebugEnabled())
				log.debug("cannnot read file", e);
		} 
		return result;
	}
	
	/**
	 * Reads all files from the given folder and returns the contained Strings.
	 */
	public static List<String> readStringsFromFolder(String folderName) {
		if (log.isTraceEnabled())
			log.trace(String.format("readQueriesFromFolder(folderName = '%s')", folderName));
		
		List<String> result = new ArrayList<String>();
		
		File queryFolder = new File(folderName);
		if (queryFolder.exists()) {
			for (File queryFile : queryFolder.listFiles(new FilenameFilter() 
			{public boolean accept(File dir, String name) {return name.toLowerCase().endsWith(".qry");}})) {
			String content = readStringFromFile(queryFile);
			if (!content.isEmpty()) // empty file --> 
				result.add(content);
			else if (log.isWarnEnabled())
				log.warn(String.format("empty query file, skipping: '%s'", queryFile.toString()));
			}
		} else {
			if (log.isErrorEnabled())
				log.error(String.format("folder does not exist, returning empty list: '%s'", folderName));
		}
		
		
		return result;
	}
	
	/**
	 * Reads a benchmark query csv file and returns a mapping from URI to Double.
	 */
	public static Map<String,Double> readBenchmarkQueryFromFile(String fileName) {
		if (log.isTraceEnabled())
			log.trace(String.format("readBenchmarkQueryFromFile(benchmarkFile = '%s')", fileName));
		
		Map<String,Double> result = new HashMap<String,Double>();
				
		try {
			CsvReader in = new CsvReader(fileName);
			in.readHeaders();
			
			while (in.readRecord()) {
				String eventURI = in.get(Util.COLUMN_NAME_URI);
				Double rating = Double.parseDouble(in.get(Util.COLUMN_NAME_RATING));
				result.put(eventURI, rating);
			}
			
			in.close();
			
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(String.format("could not read benchmark file, returning empty map: '%s'", fileName.toString()));
			if (log.isDebugEnabled())
				log.debug("cannnot read file", e);
		} 
		
		return result;
	}
}
