package edu.kit.anthropomatik.isl.newsTeller.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jumpmind.symmetric.csv.CsvReader;
import org.jumpmind.symmetric.csv.CsvWriter;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.BenchmarkEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.GroundTruth;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.UsabilityRatingReason;

/**
 * Provides some static utility functions.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public class Util {

	private static Log log = LogFactory.getLog(Util.class);

	private static SnowballStemmer stemmer = null;

	private static SnowballStemmer getStemmer() {
		if (stemmer == null)
			stemmer = new englishStemmer();
		return stemmer;
	}

	public static final String PLACEHOLDER_EVENT = "*e*";
	public static final String PLACEHOLDER_KEYWORD = "*k*";
	public static final String PLACEHOLDER_HISTORICAL_EVENT = "*h*";
	public static final String PLACEHOLDER_MENTION = "*m*";
	public static final String PLACEHOLDER_ENTITY = "*x*";
	public static final String PLACEHOLDER_LINK = "*l*";
	public static final String PLACEHOLDER_KEYS = "*keys*";
	public static final String PLACEHOLDER_BIF_CONTAINS = "*b*";

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
	public static final String COLUMN_NAME_CLASSIFIER_NAME = "classifier";
	public static final String COLUMN_NAME_BALANCED_ACCURACY = "BA";
	public static final String COLUMN_NAME_KAPPA = "kappa";
	public static final String COLUMN_NAME_AUC = "AUC";
	public static final String COLUMN_NAME_FSCORE = "F";
	public static final String COLUMN_NAME_PRECISION = "p";
	public static final String COLUMN_NAME_RECALL = "r";
	public static final String COLUMN_NAME_ACCURACY = "acc";
	public static final String COLUMN_NAME_TRAINING = "train_";
	public static final String COLUMN_NAME_TEST = "test_";
	public static final String COLUMN_NAME_PERCENTAGE = "percentage";
	public static final String COLUMN_NAME_CONSTITUENT = "constituent";
	public static final String COLUMN_NAME_REASON = "reason_";

	public static final String LABEL_FALSE = "false";
	public static final String LABEL_TRUE = "true";

	public static final String ATTRIBUTE_USABLE = "usable";
	public static final String ATTRIBUTE_URI = "eventURI";
	public static final String ATTRIBUTE_FILE = "fileName";
	public static final String ATTRIBUTE_REASON = "reason_";
	
	public static final String MENTION_PROPERTY_POS = "http://dkm.fbk.eu/ontologies/newsreader#pos";
	public static final String MENTION_PROPERTY_POS_VERB = "http://dkm.fbk.eu/ontologies/newsreader#pos_verb";
	public static final String MENTION_PROPERTY_POS_NOUN = "http://dkm.fbk.eu/ontologies/newsreader#pos_noun";
	public static final String MENTION_PROPERTY_PROPBANK = "http://dkm.fbk.eu/ontologies/newsreader#propbankRef";
	public static final String MENTION_PROPERTY_NOMBANK = "http://dkm.fbk.eu/ontologies/newsreader#nombankRef";
	public static final String MENTION_PROPERTY_PRED = "http://dkm.fbk.eu/ontologies/newsreader#pred";
	public static final String MENTION_PROPERTY_ANCHOR_OF = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#anchorOf";
	
	public static final double EPSILON = 0.00001;

	public static final int MAX_NUMBER_OF_BENCHMARK_KEYWORDS = 5;
	public static final int MAX_NUMBER_OF_REASONS = 6;

	public static final String EMPTY_EVENT_RESPONSE = "I'm sorry, but there's nothing I can tell you about this topic.";

	public static final List<Character> STOP_CHARS = Arrays.asList('.', '!', '?', ' ');
	public static final String SPLIT_REGEX = "[ .,;:?!\"'\\[\\]\\(\\)\\{\\}\\|\\\\]";
	public static final String KEYWORD_REGEX_PREFIX = "(-| |^|\\\\()";
	public static final String KEYWORD_REGEX_PREFIX_JAVA = ".*(-| |^|\\()";
	public static final String KEYWORD_REGEX_SUFFIX = "(-| |$|\\\\))";
	public static final String KEYWORD_REGEX_SUFFIX_JAVA = "(-| |$|\\)).*";
	public static final String KEYWORD_REGEX_LETTERS = "(\\\\w)*";
	public static final String KEYWORD_REGEX_LETTERS_JAVA = "(\\w)*";

	public static final String RELATION_NAME_EVENT_MENTION = "event-mention-";
	public static final String RELATION_NAME_EVENT_CONSTITUENT = "event-constituent-";
	public static final String RELATION_NAME_EVENT_LABEL = "event-label-";
	public static final String RELATION_NAME_EVENT_NUMBER = "event-number-";
	public static final String RELATION_NAME_CONSTITUENT_LABEL = "constituent-label-";
	public static final String RELATION_NAME_CONSTITUENT_MENTION = "constituent-mention-";
	public static final String RELATION_NAME_MENTION_PROPERTY = "mention-property-";
	public static final String RELATION_NAME_RESOURCE_TEXT = "resource-text-";
		
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
				log.error(String.format("could not read file, returning empty string: '%s'", file.toString()));
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
	 * Reads the given config file, interprets each line as file name and reads
	 * all these files.
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
	
	/**
	 * Reads the given file and returns a list of the lines
	 */
	public static List<String> readStringListFromFile(String fileName) {
		return Arrays.asList(readStringFromFile(fileName).split("\n"));
	}
	// endregion

	// region reading/writing csv files
	
	/**
	 * Reads a benchmark query csv file and returns a mapping of its content. Uses comma (',') as delimiter.
	 */
	public static Map<BenchmarkEvent, GroundTruth> readBenchmarkQueryFromFile(String fileName) {
		return readBenchmarkQueryFromFile(fileName, ',');
	}
	
	/**
	 * Reads a benchmark query csv file and returns a mapping of its content. Uses the specified delimiter.
	 */
	public static Map<BenchmarkEvent, GroundTruth> readBenchmarkQueryFromFile(String fileName, char delimiter) {
		if (log.isTraceEnabled())
			log.trace(String.format("readBenchmarkQueryFromFile(fileName = '%s')", fileName));

		Map<BenchmarkEvent, GroundTruth> result = new HashMap<BenchmarkEvent, GroundTruth>();

		try {
			CsvReader in = new CsvReader(fileName);
			in.setDelimiter(delimiter);
			in.setTextQualifier('"');
			in.setUseTextQualifier(true);
			in.readHeaders();

			while (in.readRecord()) {
				try {
					String eventURI = in.get(Util.COLUMN_NAME_URI);
					double usabilityRating = Double.parseDouble(in.get(Util.COLUMN_NAME_USABILITY_RATING));
					int relevanceRank = Integer.parseInt(in.get(Util.COLUMN_NAME_RELEVANCE_RANK));
					
					Set<UsabilityRatingReason> reasons = new HashSet<>();
					for (int i = 1; i <= Util.MAX_NUMBER_OF_REASONS; i++) {
						String s = in.get(Util.COLUMN_NAME_REASON + i);
						if (s != null && !s.isEmpty())
							reasons.add(UsabilityRatingReason.fromInteger(Integer.parseInt(s)));
					}
					
					result.put(new BenchmarkEvent(fileName, eventURI), new GroundTruth(usabilityRating, relevanceRank, reasons));
					
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
					if (s != null && !s.isEmpty()) {
						Keyword k = new Keyword(s);
						stemKeyword(k);
						queryKeywords.add(k);
					}
				}
				result.put(queryFileName, queryKeywords);
			}

			in.close();

		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(String.format("could not read benchmark config file, returning empty result: '%s'", fileName));
			if (log.isDebugEnabled())
				log.debug("cannnot read file", e);
		}

		return result;
	}

	/**
	 * Writes the given evaluation results to a csv file, using the specified column names.
	 */
	public static void writeEvaluationToCsv(String fileName, List<String> columnNames, Map<String, Map<String, Double>> evaluationResults) {

		try {
			CsvWriter out = new CsvWriter(new FileWriter(fileName, false), ';');

			out.write(Util.COLUMN_NAME_CLASSIFIER_NAME);
			for (String header : columnNames)
				out.write(header);
			out.endRecord();
			
			for (Map.Entry<String, Map<String, Double>> entry : evaluationResults.entrySet()) {
				String classifierName = entry.getKey();
				Map<String, Double> map = entry.getValue();
				
				out.write(classifierName);
				for (String header : columnNames)
					out.write(map.get(header).toString());
				
				out.endRecord();
			}
			
			out.close();
			
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(String.format("could not write evaluation file: '%s'", fileName));
			if (log.isDebugEnabled())
				log.debug("cannnot write file", e);
		}

	}
	
	/**
	 * Writes the measurments made for learning curve plotting to the given filename.
	 */
	public static void writeLearningCurvesToCsv(String fileName, List<String> performanceMeasures, Map<Integer, Map<String, Map<String, Double>>> data) {
		
		try {
			CsvWriter out = new CsvWriter(new FileWriter(fileName, false), ';');

			out.write(Util.COLUMN_NAME_PERCENTAGE);
			for (String header : performanceMeasures) {
				out.write(Util.COLUMN_NAME_TRAINING + header);
				out.write(Util.COLUMN_NAME_TEST + header);
			}
				
			out.endRecord();
			
			for (Map.Entry<Integer, Map<String, Map<String, Double>>> entry : data.entrySet()) {
				
				Integer percentage = entry.getKey();
				Map<String, Map<String, Double>> map = entry.getValue();
				
				out.write(percentage.toString());
				for (String header : performanceMeasures) {
					out.write(map.get(Util.COLUMN_NAME_TRAINING).get(header).toString());
					out.write(map.get(Util.COLUMN_NAME_TEST).get(header).toString());
				}
				
				out.endRecord();
			}
			
			out.close();
			
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(String.format("could not write learning curve file: '%s'", fileName));
			if (log.isDebugEnabled())
				log.debug("cannnot write file", e);
		}

	}
	
	/**
	 * Reads the given config file and returns a map of constituents to queries.
	 */
	public static Map<String, String> readNLGQueries(String configFileName) {
		
		if (log.isTraceEnabled())
			log.trace(String.format("readNLGQueries(configFileName = '%s')", configFileName));

		Map<String, String> result = new HashMap<String, String>();
		
		try {
			CsvReader in = new CsvReader(configFileName);
			in.readHeaders();

			while (in.readRecord()) {
				String constituentName = in.get(Util.COLUMN_NAME_CONSTITUENT);
				String queryFileName = in.get(Util.COLUMN_NAME_FILENAME);
				
				String query = readStringFromFile(queryFileName);
				if (!query.isEmpty())
					result.put(constituentName, query);
				else if (log.isWarnEnabled())
					log.warn(String.format("empty query file, skipping: '%s'", queryFileName));
			}

			in.close();

		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(String.format("could not read benchmark config file, returning empty result: '%s'", configFileName));
			if (log.isDebugEnabled())
				log.debug("cannnot read file", e);
		}
		
		return result;
	}
	
	/**
	 * Writes the given benchmark into the given folder.
	 */
	public static void writeBenchmark(Map<String, Map<String, GroundTruth>> benchmark, String folderName) {
		
		try {
			for (Map.Entry<String, Map<String, GroundTruth>> entry : benchmark.entrySet()) {
				String fileName = folderName + "/" + entry.getKey().substring(entry.getKey().lastIndexOf('/'));
				
				CsvWriter out = new CsvWriter(new FileWriter(fileName, false), ';');

				out.write(Util.COLUMN_NAME_URI);
				out.write(Util.COLUMN_NAME_USABILITY_RATING);
				out.write(Util.COLUMN_NAME_RELEVANCE_RANK);
				for (int i = 1; i <= Util.MAX_NUMBER_OF_REASONS; i++)
					out.write(Util.COLUMN_NAME_REASON + i);
				out.endRecord();
				
				for (Map.Entry<String, GroundTruth> innerEntry : entry.getValue().entrySet()) {
					
					String eventURI = innerEntry.getKey();
					GroundTruth gt = innerEntry.getValue();
					out.write(eventURI);					
					out.write(Double.toString(gt.getUsabilityRating()));
					out.write(Integer.toString(gt.getRelevanceRank()));
					for (UsabilityRatingReason r : gt.getReasons())
						out.write(r.toNumberString());
					
					out.endRecord();
				}
				
				out.close();
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("could not write benchmark");
			if (log.isDebugEnabled())
				log.debug("cannnot write file", e);
		}
	}
	
	public static void writeRankingSentencesToCsv(String outputFileName, Map<String, Map<BenchmarkEvent, List<String>>> outputMap) {
		try {
			CsvWriter out = new CsvWriter(new FileWriter(outputFileName, false), ';');

			List<String> fileNames = new ArrayList<String>(outputMap.keySet());
			Collections.sort(fileNames);
			
			for (String fileName : fileNames) {
				out.write(" ");
				out.endRecord();
				out.write(fileName);
				out.endRecord();
				
				Map<BenchmarkEvent, List<String>> content = outputMap.get(fileName);
				List<BenchmarkEvent> sortedEvents = new ArrayList<BenchmarkEvent>(content.keySet());
				Collections.sort(sortedEvents);
				
				for (BenchmarkEvent event : sortedEvents) {
					String eventURI = event.getEventURI();
					out.write(eventURI);
					List<String> sentences = content.get(event);
					boolean isFirst = true;
					for (String s : sentences) {
						if (!isFirst)
							out.write(" ");
						out.write(s);
						out.endRecord();
						if (isFirst)
							isFirst = false;
					}
				}
			}

			out.close();
			
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("could not write ranking sentences");
			if (log.isDebugEnabled())
				log.debug("cannnot write file", e);
		}
	}
	
	// endregion

	// region other stuff
	/**
	 * Parses an XML-style double like ""2
	 * "^^<http://www.w3.org/2001/XMLSchema#short>".
	 */
	public static double parseXMLDouble(String str) {
		String substring;
		if (str.contains("^"))
			substring = str.substring(0, str.indexOf("^")).replace("\"", "");
		else
			substring = str;
		double result = Double.parseDouble(substring);
		if (Double.isNaN(result)) {
			if (log.isWarnEnabled())
				log.warn(String.format("error parsing double: '%s'", str));
			result = 0;
		}

		return result;
	}

	/**
	 * Parses the first string from the given set, returns 0 for empty set.
	 */
	public static double parseXMLDoubleFromSet(Set<String> strings) {
		for (String str : strings)
			return parseXMLDouble(str);
		return 0; // return 0 for empty set
	}
	
	/**
	 * Calculate the average value of the given collection. Returns NaN for
	 * empty collection.
	 */
	public static double averageFromCollection(Collection<Double> collection) {
		if (collection.isEmpty())
			return Double.NaN;

		double sum = 0;
		for (Double d : collection)
			sum += d;
		return sum / collection.size();
	}

	/**
	 * Stems the given keyword.
	 */
	public static void stemKeyword(Keyword keyword) {
		SnowballStemmer stemmer = getStemmer();
		stemmer.setCurrent(keyword.getWord());
		stemmer.stem();
		String stemmedKeyword = stemmer.getCurrent();
		keyword.setStem(stemmedKeyword);

		String[] tokens = stemmedKeyword.split(" ");
		StringBuilder regexBuilder = new StringBuilder();
		StringBuilder bifBuilder = new StringBuilder();
		regexBuilder.append(KEYWORD_REGEX_PREFIX);
		bifBuilder.append("\"");
		for (int i = 0; i < tokens.length; i++) {
			String regexToken = tokens[i];
			String bifToken = tokens[i];
			if (regexToken.endsWith("i")) {
				regexToken = regexToken.substring(0, regexToken.length() - 1) + "(i|y)";
				bifToken = bifToken.substring(0, bifToken.length() - 1);
			}
			regexBuilder.append(regexToken);
			regexBuilder.append(KEYWORD_REGEX_LETTERS);
			bifBuilder.append(bifToken);
			bifBuilder.append("*\"");
			if (i < tokens.length - 1) {
				regexBuilder.append(" ");
				bifBuilder.append(" and \"");
			}
				
		}
		regexBuilder.append(KEYWORD_REGEX_SUFFIX);
		String stemmedRegex = regexBuilder.toString();
		keyword.setStemmedRegex(stemmedRegex);
		keyword.setWordRegex(KEYWORD_REGEX_PREFIX_JAVA + keyword.getWord().toLowerCase() + KEYWORD_REGEX_SUFFIX_JAVA);
		String bifContainsString = bifBuilder.toString();
		keyword.setBifContainsString(bifContainsString);
	}
	
	/**
	 * Escapes all special characters in the text that would cause trouble when used inside a regex.
	 */
	public static String escapeText(String text) {
		String result = text;
		String[] charsToReplace = "\\.[]{}()*+-?^$|".split("");
		
		for (String c : charsToReplace)
			result = result.replace(c, "\\" + c);
		
		return result;
	}
	
	/**
	 * Abbreviates the full path of a query filename to just the file name itself.
	 */
	public static String queryNameFromFileName(String queryFileName) {
		String result = queryFileName;
		if (result.contains("/"))
			result = result.substring(result.lastIndexOf('/') + 1);
		if (result.contains("."))
			result = result.substring(0, result.indexOf('.'));
		return result;
	}
	
	/**
	 * Get the resourceURI from the mentionURI by simple string manipulation.
	 */
	public static String resourceURIFromMentionURI(String mentionURI) {
		return mentionURI.substring(0, mentionURI.indexOf("#"));
	}
	
	/**
	 * Convert a set of mentionURIs into a set of corresponding resourceURIs.
	 */
	public static Set<String> resourceURIsFromMentionURIs(Set<String> mentionURIs) {
		Set<String> result = new HashSet<String>();
		for (String mentionURI : mentionURIs)
			result.add(resourceURIFromMentionURI(mentionURI));
		return result;
	}
	
	/**
	 * Constructs a relation name for the given key, value, and keyword.
	 */
	public static String getRelationName(String key, String value, String keyword) {
		return String.format("%s-%s-%s", key, value, keyword);
	}
	// endregion
}
