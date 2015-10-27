package edu.kit.anthropomatik.isl.newsTeller.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides some static utility functions.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class Util {

	private static Log log = LogFactory.getLog(Util.class);
	
	// private constructor to prevent instantiation
	private Util() {}
	
	/**
	 * Reads the file given by the fileName and returns the contained SPARQL query as String.
	 */
	public static String readQueryFromFile(String fileName) {
		if (log.isInfoEnabled())
			log.info(String.format("readQueryFromFile(fileName = '%s')", fileName));
		
		File queryFile = new File(fileName);
		
		return readQueryFromFile(queryFile);
	}
	
	/**
	 * Reads the given queryFile and returns the contained SPARQL query as String.
	 */
	public static String readQueryFromFile(File queryFile) {
		if (log.isInfoEnabled())
			log.info(String.format("readQueryFromFile(queryFile = '%s')", queryFile.toString()));
		
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
				log.error(String.format("could not read query file, returning empty string: %s", queryFile.toString()));
			if (log.isDebugEnabled())
				log.debug("cannnot read file", e);
		} 
		return result;
	}
	
	/**
	 * Reads all files from the given folder and returns the contained SPARQL queries as a list of Strings.
	 */
	public static List<String> readQueriesFromFolder(String folderName) {
		if (log.isInfoEnabled())
			log.info(String.format("readQueriesFromFolder(folderName = '%s')", folderName));
		
		List<String> result = new ArrayList<String>();
		
		File queryFolder = new File(folderName);
		if (queryFolder.exists()) {
			for (File queryFile : queryFolder.listFiles(new FilenameFilter() 
			{public boolean accept(File dir, String name) {return name.toLowerCase().endsWith(".qry");}})) {
			String content = readQueryFromFile(queryFile);
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
}
