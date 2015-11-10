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

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.GroundTruth;

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
	public static final String VARIABLE_LABEL = "label";

	public static final String COLUMN_NAME_URI = "URI";
	public static final String COLUMN_NAME_USABILITY_RATING = "usabilityRating";
	public static final String COLUMN_NAME_RELEVANCE_RANK = "relevanceRank";
	public static final String COLUMN_NAME_FILENAME = "filename";
	public static final String COLUMN_NAME_KEYWORD = "keyword_";

	public static final double EPSILON = 0.00001;

	public static final int MAX_NUMBER_OF_BENCHMARK_KEYWORDS = 5;

	// private constructor to prevent instantiation
	private Util() {
	}

	// region reading strings from files
	/**
	 * Reads the file given by the fileName and returns the contained String.
	 */
	public static String readStringFromFile(String fileName) {
		return readStringFromFile(new File(fileName));
	}

	/**
	 * Reads the given queryFile and returns the contained String.
	 */
	public static String readStringFromFile(File file) {
		if (log.isTraceEnabled())
			log.trace(String.format("readStringFromFile(file = '%s')", file.toString()));

		String result = "";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
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
				log.error(
						String.format("could not read file, returning empty string: '%s'", file.toString()));
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
			log.trace(String.format("readStringsFromFolder(folderName = '%s')", folderName));

		List<String> result = new ArrayList<String>();

		File queryFolder = new File(folderName);
		if (queryFolder.exists()) {
			for (File queryFile : queryFolder.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".qry");
				}
			})) {
				String content = readStringFromFile(queryFile);
				if (!content.isEmpty())
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
	 * Reads the given config file, interprets each line as file name and reads all these files.
	 */
	public static List<String> readQueriesFromConfigFile(String configFileName) {
		if (log.isTraceEnabled())
			log.trace(String.format("readQueriesFromConfigFile(configFileName = '%s')", configFileName));

		List<String> result = new ArrayList<String>();

		String[] queryFileNames = readStringFromFile(configFileName).split("\n");

		for (String queryFileName : queryFileNames) {
			String content = readStringFromFile(queryFileName);
			if (!content.isEmpty())
				result.add(content);
			else if (log.isWarnEnabled())
				log.warn(String.format("empty query file, skipping: '%s'", queryFileName));
		}

		return result;
	}
	// endregion

	// region reading csv files
	/**
	 * Reads a benchmark query csv file and returns a mapping from URI to
	 * Double.
	 */
	public static Map<String, GroundTruth> readBenchmarkQueryFromFile(String fileName) {
		if (log.isTraceEnabled())
			log.trace(String.format("readBenchmarkQueryFromFile(fileName = '%s')", fileName));

		Map<String, GroundTruth> result = new HashMap<String, GroundTruth>();

		try {
			CsvReader in = new CsvReader(fileName);
			in.setTextQualifier('"');
			in.setUseTextQualifier(true);
			in.readHeaders();

			while (in.readRecord()) {
				try {
					String eventURI = in.get(Util.COLUMN_NAME_URI);
					double usabilityRating = Double.parseDouble(in.get(Util.COLUMN_NAME_USABILITY_RATING));
					int relevanceRank = Integer.parseInt(in.get(Util.COLUMN_NAME_RELEVANCE_RANK));
					result.put(eventURI, new GroundTruth(usabilityRating, relevanceRank));
				} catch (NumberFormatException e) {
					if (log.isWarnEnabled())
						log.warn(String.format("malformed entry, skipping: '%s'", in.getRawRecord()));
				}
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

	/**
	 * Reads a benchmark config file and returns a map of benchmark query files
	 * and corresponding keywords.
	 */
	public static Map<String, List<Keyword>> readBenchmarkConfigFile(String fileName) {
		if (log.isTraceEnabled())
			log.trace(String.format("readBenchmarkConfigFile(fileName = '%s')", fileName));

		Map<String, List<Keyword>> result = new HashMap<String, List<Keyword>>();

		try {
			CsvReader in = new CsvReader(fileName);
			in.readHeaders();

			while (in.readRecord()) {
				String queryFileName = in.get(Util.COLUMN_NAME_FILENAME);
				List<Keyword> queryKeywords = new ArrayList<Keyword>(Util.MAX_NUMBER_OF_BENCHMARK_KEYWORDS);
				for (int i = 1; i <= Util.MAX_NUMBER_OF_BENCHMARK_KEYWORDS; i++) {
					String s = in.get(Util.COLUMN_NAME_KEYWORD + i);
					if (s != null && !s.isEmpty())
						queryKeywords.add(new Keyword(s));
				}
				result.put(queryFileName, queryKeywords);
			}

			in.close();

		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(String.format("could not read benchmark config file, returning empty result: '%s'",
						fileName.toString()));
			if (log.isDebugEnabled())
				log.debug("cannnot read file", e);
		}

		return result;
	}
	// endregion

	/**
	 * Parses an XML-style double like ""2"^^<http://www.w3.org/2001/XMLSchema#short>".
	 */
	public static double parseXMLDouble(String str) {
		String substring = str.substring(0, str.indexOf("^")).replace("\"", "");
		double result = Double.parseDouble(substring);
		if (Double.isNaN(result)) {
			if (log.isWarnEnabled())
				log.warn(String.format("error parsing double: '%s'", str));
			result = 0;
		}
			
		return result;
	}
}
