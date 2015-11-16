package edu.kit.anthropomatik.isl.newsTeller.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jumpmind.symmetric.csv.CsvReader;
import org.jumpmind.symmetric.csv.CsvWriter;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.retrieval.BenchmarkEvent;
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
	public static final String PLACEHOLDER_ENTITY = "*x*";

	public static final String VARIABLE_EVENT = "event";
	public static final String VARIABLE_NUMBER = "number";
	public static final String VARIABLE_MENTION = "mention";
	public static final String VARIABLE_RESOURCE = "resource";
	public static final String VARIABLE_LABEL = "label";
	public static final String VARIABLE_ENTITY = "entity";
	
	public static final String COLUMN_NAME_URI = "URI";
	public static final String COLUMN_NAME_USABILITY_RATING = "usabilityRating";
	public static final String COLUMN_NAME_RELEVANCE_RANK = "relevanceRank";
	public static final String COLUMN_NAME_FILENAME = "filename";
	public static final String COLUMN_NAME_KEYWORD = "keyword_";
	public static final String COLUMN_NAME_VALUE = "value";
	public static final String COLUMN_NAME_POSITIVE_PROBABILITY = "posProb";
	public static final String COLUMN_NAME_NEGATIVE_PROBABILITY = "negProb";
	public static final String COLUMN_NAME_OVERALL_PROBABILITY = "overallProb";
	public static final String COLUMN_NAME_PRIOR_PROBABILITY = "priorProb";
	

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

	// region reading/writing csv files
	/**
	 * Reads a benchmark query csv file and returns a mapping from URI to
	 * Double.
	 */
	public static Map<BenchmarkEvent, GroundTruth> readBenchmarkQueryFromFile(String fileName) {
		if (log.isTraceEnabled())
			log.trace(String.format("readBenchmarkQueryFromFile(fileName = '%s')", fileName));

		Map<BenchmarkEvent, GroundTruth> result = new HashMap<BenchmarkEvent, GroundTruth>();

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
					result.put(new BenchmarkEvent(fileName, eventURI), new GroundTruth(usabilityRating, relevanceRank));
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
	
	//region featureMap
	/**
	 * Writes the given featureMap to the given file (will be created/overwritten), using the given list of featureNames as headers (and for accessing the map).
	 */
	public static void writeFeatureMapToFile(Map<BenchmarkEvent,Map<String,Integer>> featureMap, List<String> featureNames, String fileName) {
		try {
			CsvWriter w = new CsvWriter(new FileWriter(fileName, false), ';');
			w.write(COLUMN_NAME_FILENAME);
			w.write(COLUMN_NAME_URI);
			for (String s : featureNames)
				w.write(s);
			w.endRecord();
			
			for(Map.Entry<BenchmarkEvent, Map<String,Integer>> entry : featureMap.entrySet()) {
				BenchmarkEvent event = entry.getKey();
				w.write(event.getFileName());
				w.write(event.getEventURI());
				for (String s: featureNames)
					w.write(entry.getValue().get(s).toString());
				w.endRecord();
			}
			
			w.close();
		} catch (IOException e) {
			if(log.isErrorEnabled())
				log.error(String.format("cannot write file '%s'", fileName));
			if(log.isDebugEnabled())
				log.debug("csv write error", e);
		}
	}
	
	/**
	 * Reads a featureMap from the given file and returns it.
	 */
	public static Map<BenchmarkEvent,Map<String,Integer>> readFeatureMapFromFile(String fileName) {
		
		Map<BenchmarkEvent,Map<String,Integer>> result = new HashMap<BenchmarkEvent, Map<String,Integer>>();
		
		try {
			CsvReader r = new CsvReader(new FileReader(fileName), ';');
			
			r.readHeaders();
			List<String> featureNames = new ArrayList<String>();
			for (int i = 2; i < r.getHeaderCount(); i++)
				featureNames.add(r.getHeader(i));
			
			while(r.readRecord()) {
				String eventFileName = r.get(COLUMN_NAME_FILENAME);
				String eventURI = r.get(COLUMN_NAME_URI);
				BenchmarkEvent event = new BenchmarkEvent(eventFileName, eventURI);
				Map<String,Integer> featureValues = new HashMap<String, Integer>();
				for (String s : featureNames)
					featureValues.put(s, Integer.parseInt(r.get(s)));
				result.put(event, featureValues);
			}
			
			r.close();
			
		} catch (IOException e) {
			if(log.isFatalEnabled())
				log.fatal(String.format("cannot read file '%s'", fileName));
			if(log.isDebugEnabled())
				log.debug("csv read error", e);
		}
		
		return result;
	}
	//endregion
	//region probability map
	/**
	 * Writes the given probability map to the given file (overrides/creates file).
	 */
	public static void writeProbabilityMapToFile(Map<Integer, Map<String,Double>> probabilityMap, String fileName, boolean inLogProbabilities) {
		try {
			CsvWriter w = new CsvWriter(new FileWriter(fileName, false), ';');
			w.write(COLUMN_NAME_VALUE);
			w.write(COLUMN_NAME_POSITIVE_PROBABILITY);
			w.write(COLUMN_NAME_NEGATIVE_PROBABILITY);
			w.write(COLUMN_NAME_OVERALL_PROBABILITY);
			w.endRecord();
			
			for(Map.Entry<Integer, Map<String,Double>> entry : probabilityMap.entrySet()) {
				w.write(entry.getKey().toString());
				Map<String,Double> valueMap = entry.getValue();
				Double posProb = inLogProbabilities ? Math.log(valueMap.get(COLUMN_NAME_POSITIVE_PROBABILITY)) : valueMap.get(COLUMN_NAME_POSITIVE_PROBABILITY);
				w.write(posProb.toString());
				Double negProb = inLogProbabilities ? Math.log(valueMap.get(COLUMN_NAME_NEGATIVE_PROBABILITY)) : valueMap.get(COLUMN_NAME_NEGATIVE_PROBABILITY);
				w.write(negProb.toString());
				Double overallProb = inLogProbabilities ? Math.log(valueMap.get(COLUMN_NAME_OVERALL_PROBABILITY)) : valueMap.get(COLUMN_NAME_OVERALL_PROBABILITY);
				w.write(overallProb.toString());
				w.endRecord();
			}
			w.close();
		} catch (IOException e) {
			if(log.isErrorEnabled())
				log.error(String.format("cannot write file '%s'", fileName));
			if(log.isDebugEnabled())
				log.debug("csv write error", e);
		}
	}
	
	/**
	 * Reads a probability map from the given file.
	 */
	public static Map<Integer, Map<String,Double>> readProbabilityMapFromFile(String fileName) {
		Map<Integer, Map<String,Double>> result = new HashMap<Integer, Map<String,Double>>();
		
		try {
			CsvReader r = new CsvReader(new FileReader(fileName), ';');
			
			r.readHeaders();
			
			while(r.readRecord()) {
				Integer value = Integer.parseInt(r.get(COLUMN_NAME_VALUE));
				
				Map<String,Double> internalMap = new HashMap<String, Double>();
				Double posProb = Double.parseDouble(r.get(COLUMN_NAME_POSITIVE_PROBABILITY));
				Double negProb = Double.parseDouble(r.get(COLUMN_NAME_NEGATIVE_PROBABILITY));
				Double overallProb = Double.parseDouble(r.get(COLUMN_NAME_OVERALL_PROBABILITY));
				
				internalMap.put(COLUMN_NAME_POSITIVE_PROBABILITY, posProb);
				internalMap.put(COLUMN_NAME_NEGATIVE_PROBABILITY, negProb);
				internalMap.put(COLUMN_NAME_OVERALL_PROBABILITY, overallProb);
				
				result.put(value, internalMap);
			}
			
			r.close();
			
		} catch (IOException e) {
			if(log.isFatalEnabled())
				log.fatal(String.format("cannot read file '%s'", fileName));
			if(log.isDebugEnabled())
				log.debug("csv read error", e);
		}
		
		return result;
	}
	
	/**
	 * Write the given priorProbabilityMap to the given file (creates/overrides).
	 */
	public static void writePriorProbabilityMapToFile(Map<String,Double> priorProbabilityMap, String fileName, boolean inLogProbabilities) {
		try {
			CsvWriter w = new CsvWriter(new FileWriter(fileName, false), ';');
			w.write(COLUMN_NAME_VALUE);
			w.write(COLUMN_NAME_PRIOR_PROBABILITY);
			w.endRecord();
			
			for(Map.Entry<String,Double> entry : priorProbabilityMap.entrySet()) {
				w.write(entry.getKey());
				Double value = inLogProbabilities ? Math.log(entry.getValue()) : entry.getValue();
				w.write(value.toString());
				w.endRecord();
			}
			w.close();
		} catch (IOException e) {
			if(log.isErrorEnabled())
				log.error(String.format("cannot write file '%s'", fileName));
			if(log.isDebugEnabled())
				log.debug("csv write error", e);
		}
	}
	
	/**
	 * Reads a map of prior probabilites from the given file.
	 */
	public static Map<String,Double> readPriorProbabilityMapFromFile(String fileName) {
		Map<String, Double> result = new HashMap<String, Double>();
		
		try {
			CsvReader r = new CsvReader(new FileReader(fileName), ';');
			
			r.readHeaders();
			
			while(r.readRecord()) {
				String value = r.get(COLUMN_NAME_VALUE);
				Double probability = Double.parseDouble(r.get(COLUMN_NAME_PRIOR_PROBABILITY));
				result.put(value, probability);
			}
			
			r.close();
			
		} catch (IOException e) {
			if(log.isFatalEnabled())
				log.fatal(String.format("cannot read file '%s'", fileName));
			if(log.isDebugEnabled())
				log.debug("csv read error", e);
		}
		
		return result;
	}
	//endregion
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
	
	/**
	 * Calculate the average value of the given collection. Returns NaN for empty collection.
	 */
	public static double averageFromCollection(Collection<Double> collection) {
		if (collection.isEmpty())
			return Double.NaN;
		
		double sum = 0;
		for (Double d : collection)
			sum += d;
		return sum / collection.size();
	}
}
