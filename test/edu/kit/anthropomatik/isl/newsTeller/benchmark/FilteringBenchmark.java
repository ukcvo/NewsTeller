package edu.kit.anthropomatik.isl.newsTeller.benchmark;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.BenchmarkEvent;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.GroundTruth;
import edu.kit.anthropomatik.isl.newsTeller.data.benchmark.UsabilityRatingReason;
import edu.kit.anthropomatik.isl.newsTeller.util.Util;
import weka.attributeSelection.AttributeSelection;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.Bagging;
import weka.classifiers.meta.MetaCost;
import weka.classifiers.meta.Vote;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.XRFFLoader;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;
import weka.filters.unsupervised.attribute.StringToNominal;
import weka.filters.unsupervised.instance.RemoveWithValues;

public class FilteringBenchmark {

	private static Log log = LogFactory.getLog(FilteringBenchmark.class);

	private static final int seed = 1337;

	private static final int numFolds = 10;

	private Instances originalDataSet;

	private Instances analysisDataSet; // w/o String attributes

	private Instances classificationDataSet; // only w/ relevant attributes

	private Filter analysisFilter;

	private List<AttributeSelection> configurations;

	private Filter classifierFilter;

	private Map<String, Classifier> classifiers;

	private Classifier costSensitiveWrapper;

	private Classifier baggingWrapper;

	private List<Integer> trainingSetPercentages;

	private Map<UsabilityRatingReason, Classifier> reasonClassifiers;
	
	private Filter reasonFilter;
	
	private boolean doFeatureAnalysis;

	private boolean doCrossValidation;

	private boolean doLeaveOneOut;

	private boolean doEasyEnsemble;

	private boolean doBalanceCascade;

	private boolean doLearningCurve;
	
	private boolean doOverallCrossvalidation;

	private boolean outputMisclassified;

	private boolean outputMisclassifiedReasons;
	
	private boolean useCostSensitiveWrapper;

	private boolean useIndividualCostMatrices;
	
	private boolean useBaggingWrapper;

	private boolean storeFilteringResults;
	
	private String outputFileName;

	private int numberOfEnsembleMembers;

	private int positiveClassIdx;

	private Map<String, Map<String, GroundTruth>> benchmark;
	
	// region setters
	public void setAnalysisFilter(Filter analysisFilter) {
		this.analysisFilter = analysisFilter;
	}

	public void setConfigurations(List<AttributeSelection> configurations) {
		this.configurations = configurations;
	}

	public void setClassifierFilter(Filter classifierFilter) {
		this.classifierFilter = classifierFilter;
	}

	public void setClassifiers(Map<String, Classifier> classifiers) {
		this.classifiers = classifiers;
	}

	public void setCostSensitiveWrapper(Classifier costSensitiveWrapper) {
		this.costSensitiveWrapper = costSensitiveWrapper;
	}

	public void setBaggingWrapper(Classifier baggingWrapper) {
		this.baggingWrapper = baggingWrapper;
	}

	public void setTrainingSetPercentages(List<Integer> trainingSetPercentages) {
		this.trainingSetPercentages = trainingSetPercentages;
	}

	public void setReasonClassifiers(Map<Integer, Classifier> reasonClassifiers) {
		this.reasonClassifiers = new HashMap<UsabilityRatingReason, Classifier>();
		for (Map.Entry<Integer, Classifier> entry : reasonClassifiers.entrySet()) {
			this.reasonClassifiers.put(UsabilityRatingReason.fromInteger(entry.getKey()), entry.getValue());
		}
	}
	
	public void setReasonFilter(Filter reasonFilter) {
		this.reasonFilter = reasonFilter;
	}
	
	
	public void setDoFeatureAnalysis(boolean doFeatureAnalysis) {
		this.doFeatureAnalysis = doFeatureAnalysis;
	}

	public void setDoCrossValidation(boolean doCrossValidation) {
		this.doCrossValidation = doCrossValidation;
	}

	public void setDoLeaveOneOut(boolean doLeaveOneOut) {
		this.doLeaveOneOut = doLeaveOneOut;
	}

	public void setDoEasyEnsemble(boolean doEasyEnsemble) {
		this.doEasyEnsemble = doEasyEnsemble;
	}

	public void setDoBalanceCascade(boolean doBalanceCascade) {
		this.doBalanceCascade = doBalanceCascade;
	}

	public void setDoLearningCurve(boolean doLearningCurve) {
		this.doLearningCurve = doLearningCurve;
	}

	public void setDoOverallCrossvalidation(boolean doOverallCrossvalidation) {
		this.doOverallCrossvalidation = doOverallCrossvalidation;
	}

	
	public void setOutputMisclassified(boolean outputMisclassified) {
		this.outputMisclassified = outputMisclassified;
	}

	public void setOutputMisclassifiedReasons(boolean outputMisclassifiedReasons) {
		this.outputMisclassifiedReasons = outputMisclassifiedReasons;
	}
	
	public void setUseCostSensitiveWrapper(boolean useCostSensitiveWrapper) {
		this.useCostSensitiveWrapper = useCostSensitiveWrapper;
	}

	public void setUseIndividualCostMatrices(boolean useIndividualCostMatrices) {
		this.useIndividualCostMatrices = useIndividualCostMatrices;
	}

	public void setStoreFilteringResults(boolean storeFilteringResults) {
		this.storeFilteringResults = storeFilteringResults;
	}
	
	public void setUseBaggingWrapper(boolean useBaggingWrapper) {
		this.useBaggingWrapper = useBaggingWrapper;
	}

	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}

	public void setNumberOfEnsembleMembers(int numberOfEnsembleMembers) {
		this.numberOfEnsembleMembers = numberOfEnsembleMembers;
	}

	// endregion

	public FilteringBenchmark(String instancesFileName, String configFileName) {
		try {
			XRFFLoader loader = new XRFFLoader();
			loader.setSource(new File(instancesFileName));
			this.originalDataSet = loader.getDataSet();
			
			Set<String> fileNames = Util.readBenchmarkConfigFile(configFileName).keySet();
			this.benchmark = new HashMap<String, Map<String, GroundTruth>>();

			for (String fileName : fileNames) {
				Map<BenchmarkEvent, GroundTruth> fileContent = Util.readBenchmarkQueryFromFile(fileName);
				Map<String, GroundTruth> internalMap = new HashMap<String, GroundTruth>();
				for (Map.Entry<BenchmarkEvent, GroundTruth> entry : fileContent.entrySet()) {
					internalMap.put(entry.getKey().getEventURI(), entry.getValue());
				}
				this.benchmark.put(fileName, internalMap);
			}
			
		} catch (IOException e) {
			if (log.isErrorEnabled())
				log.error("Can't read data set");
			if (log.isDebugEnabled())
				log.debug("Can't read data set", e);
		}
		this.positiveClassIdx = this.originalDataSet.attribute(Util.ATTRIBUTE_USABLE).indexOfValue(Util.LABEL_TRUE);
	}

	// region featureAnalysis
	private void featureAnalysis() {

		for (AttributeSelection config : configurations) {
			try {
				config.SelectAttributes(analysisDataSet);
				if (log.isInfoEnabled())
					log.info(config.toResultsString());

			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("Can't select attributes");
				if (log.isDebugEnabled())
					log.debug("Exception", e);
			}
		}

	}
	// endregion

	// region crossvalidation
	// do a crossvalidation for all classifiers and output the results
	private void crossValidation() {

		List<String> columnNames = getColumnNames();

		Map<String, Map<String, Double>> overallResultMap = new HashMap<String, Map<String, Double>>();
		
		Map<String, Map<String, GroundTruth>> outputMap = new HashMap<String, Map<String, GroundTruth>>();
		for (String fileName : this.benchmark.keySet()) {
			outputMap.put(fileName, new HashMap<String, GroundTruth>(this.benchmark.get(fileName))); //copy benchmark
		}
		
		for (Map.Entry<String, Classifier> entry : this.classifiers.entrySet()) {
			try {
				String classifierName = entry.getKey();
				Classifier classifier = entry.getValue();
				Evaluation eval = new Evaluation(classificationDataSet);
				
				Map<UsabilityRatingReason, Integer> reasonMissclassification = new HashMap<UsabilityRatingReason, Integer>();
				for (UsabilityRatingReason r : UsabilityRatingReason.values())
					reasonMissclassification.put(r, 0);
			
				// do crossvalidation manually in order to output misclassified
				// examples
				Random rand = new Random(seed);
				Instances randData = new Instances(classificationDataSet);
				randData.randomize(rand);
				randData.stratify(numFolds);

				for (int i = 0; i < numFolds; i++) {
					Instances train = randData.trainCV(numFolds, i);
					Instances test = randData.testCV(numFolds, i);

					Classifier c;
					if (this.useBaggingWrapper) {
						Bagging outer = (Bagging) AbstractClassifier.makeCopy(this.baggingWrapper);
						Classifier inner = AbstractClassifier.makeCopy(classifier);
						outer.setClassifier(inner);
						c = outer;
					} else if (this.useCostSensitiveWrapper) {
						MetaCost outer = (MetaCost) AbstractClassifier.makeCopy(this.costSensitiveWrapper);
						Classifier inner = AbstractClassifier.makeCopy(classifier);
						outer.setClassifier(inner);
						c = outer;
					} else {
						c = AbstractClassifier.makeCopy(classifier);
					}
					c.buildClassifier(train);
					eval.evaluateModel(c, test);

					if (outputMisclassified && log.isInfoEnabled()) {
						outputMisclassifiedInstances(test, c);
					}
					
					if (storeFilteringResults) {
						for (Instance instance : test) {
							if (c.classifyInstance(instance) != this.positiveClassIdx) {
								// instance gets removed by classifier
								String fileName = instance.stringValue(test.attribute(Util.ATTRIBUTE_FILE));
								String eventURI = instance.stringValue(test.attribute(Util.ATTRIBUTE_URI));
								outputMap.get(fileName).remove(eventURI); // remove from output map
							}
						}
					}
					
					if (outputMisclassifiedReasons) {
						for (Instance instance : test) {
							if (instance.classValue() != this.positiveClassIdx && c.classifyInstance(instance) == this.positiveClassIdx) { //FP!
								for (UsabilityRatingReason r : UsabilityRatingReason.values()) {
									if (instance.value(train.attribute(Util.ATTRIBUTE_REASON + r.toString())) == this.positiveClassIdx) 
										reasonMissclassification.put(r, reasonMissclassification.get(r) + 1);
								}
							}
						}
					}

				}

				if (log.isInfoEnabled()) {
					log.info(String.format("%s (cross-validation)", classifierName));
					logEvalResults(eval);
				}

				Map<String, Double> classifierMap = getClassifierMap(eval);

				overallResultMap.put(classifierName, classifierMap);

				if (outputMisclassifiedReasons && log.isInfoEnabled()) {
					for (UsabilityRatingReason r : UsabilityRatingReason.values())
						log.info(String.format("%s: %d", r.toString(), reasonMissclassification.get(r)));
				}
				
				if (storeFilteringResults) {
					Util.writeBenchmark(outputMap, "out"); // write into "out" directory
				}
				
			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("Can't cross-validate classifier");
				if (log.isDebugEnabled())
					log.debug("Can't cross-validate classifier", e);
			}
		}

		Util.writeEvaluationToCsv(this.outputFileName, columnNames, overallResultMap);
	}
	// endregion

	// region overallCrossvalidation
	// do cross-validation on the complete data-set with the combined classifier
	private void overallCrossvalidation() {

		try {
			Random rand = new Random(seed);
			Instances randData = new Instances(originalDataSet);
			randData.randomize(rand);
			randData.stratify(numFolds);

			double tp = 0;
			double tn = 0;
			double fp = 0;
			double fn = 0;
			
			Map<UsabilityRatingReason, Integer> falsePositives = new HashMap<UsabilityRatingReason, Integer>();
			for (UsabilityRatingReason r : this.reasonClassifiers.keySet())
				falsePositives.put(r, 0);
			
			List<String> columnNames = getColumnNames();
			Map<String, Map<String, Double>> overallResultMap = new HashMap<String, Map<String, Double>>();
			
			for (int i = 0; i < numFolds; i++) {
				Instances train = randData.trainCV(numFolds, i);
				Instances originalTest = randData.testCV(numFolds, i);
				this.reasonFilter.setInputFormat(originalTest);
				Instances test = Filter.useFilter(originalTest, this.reasonFilter);
				
				// prepare the training data for each classifier
				Map<UsabilityRatingReason, Instances> classifierInstances = new HashMap<UsabilityRatingReason, Instances>();
				for (UsabilityRatingReason r : this.reasonClassifiers.keySet()) {
					classifierInstances.put(r, new Instances(train, 0));
				}
				for (Instance instance : train) {
					for (UsabilityRatingReason r : classifierInstances.keySet()) {
						if (instance.classValue() == this.positiveClassIdx || instance.value(train.attribute(Util.ATTRIBUTE_REASON + r.toString())) == this.positiveClassIdx) {
							UsabilityRatingReason toAdd = (r.equals(UsabilityRatingReason.MISSING_LOCATION)) ? UsabilityRatingReason.MISSING_OBJECT : r;
							classifierInstances.get(toAdd).add(instance);
						}
					}
				}
				
				List<Classifier> hardFilteringClassifiers = new ArrayList<Classifier>();
				
				// now train each classifier with its specified filter
				for (Map.Entry<UsabilityRatingReason, Classifier> entry : this.reasonClassifiers.entrySet()) {
					UsabilityRatingReason r = entry.getKey();
					Classifier c;
					if (this.useCostSensitiveWrapper) {
						MetaCost outer = (MetaCost) AbstractClassifier.makeCopy(this.costSensitiveWrapper);
						if (this.useIndividualCostMatrices) {
							String matrix;
							switch (r) {
							case MISSING_SUBJECT:
							case BROKEN_CONSTITUENT:
							case WRONG_PARSE:
								matrix = "[0.0 2.0; 1.0 0.0]";
								break;
							case NO_EVENT:
							case KEYWORD_ENTITY_CATEGORIZATION:
							case MISSING_OBJECT:
							case OTHER_ENTITY_CATEGORIZATION:
								matrix = "[0.0 4.0; 1.0 0.0]";
								break;
							case OVERLAPPING_CONSTITUENTS:
							case EVENT_MERGE:
							case KEYWORD_REGEX_MISMATCH:
								matrix = "[0.0 1.0; 1.0 0.0]";
								break;
							default:
								matrix = "[0.0 1.0; 1.0 0.0]";
								break;
							}
							String[] array = {"-cost-matrix", matrix};
							outer.setOptions(array);
						}
						Classifier inner = AbstractClassifier.makeCopy(entry.getValue());
						outer.setClassifier(inner);
						c = outer;
					} else {
						c = AbstractClassifier.makeCopy(entry.getValue());
					}
					Instances classifierTrain = classifierInstances.get(r);
					this.reasonFilter.setInputFormat(classifierTrain);
					Instances filtered = Filter.useFilter(classifierTrain, this.reasonFilter);
					c.buildClassifier(filtered);
					hardFilteringClassifiers.add(c);
				}
				
				
				// and manually test w/o the voting stuff but with a hard AND
				for (int j = 0; j < test.numInstances(); j++) { //Instance instance : originalTest) {
					Instance instance = test.get(j);
					Instance rawInstance = originalTest.get(j);
					
					boolean isPositiveExample = (instance.classValue() == this.positiveClassIdx);
					boolean isClassifiedPositive = true;
					
					for (Classifier c : hardFilteringClassifiers) {
						double prediction = c.classifyInstance(instance);
						isClassifiedPositive = isClassifiedPositive && (prediction == this.positiveClassIdx);
						if (!isClassifiedPositive)
							break;
					}
					if (isPositiveExample) {
						if (isClassifiedPositive)
							tp++;
						else {
							fn++;
						}
					} else {
						if (isClassifiedPositive) {
							fp++;
							if (outputMisclassifiedReasons) {
								for (UsabilityRatingReason r : falsePositives.keySet()) {
									if (rawInstance.value(train.attribute(Util.ATTRIBUTE_REASON + r.toString())) == this.positiveClassIdx)
										falsePositives.put(r, falsePositives.get(r) + 1);
								}
							}
						} else
							tn++;
					}
				}
				
				
			}
			
			Map<String, Double> filterMap = getClassifierMap(tp, fn, fp, tn);
			
			overallResultMap.put("hardFilter", filterMap);
			
			if (log.isInfoEnabled()) {
				log.info("FILTER");
				logEvalResults(tp, fn, fp, tn, filterMap);
				
				if (outputMisclassifiedReasons) {
					log.info(" ");
					for (Map.Entry<UsabilityRatingReason, Integer> entry : falsePositives.entrySet())
						log.info(String.format("%s: %d", entry.getKey().toString(), entry.getValue()));
				}
			}
			
			
			Util.writeEvaluationToCsv(outputFileName, columnNames, overallResultMap);
			
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("Can't do overall crossvalidation");
			if (log.isDebugEnabled())
				log.debug("Can't do overall crossvalidation", e);
		}
		
	}
	// endregion

	// region leaveOneOut
	// do leave one out evaluation, but based on files/keywords
	private void leaveOneOut() throws Exception {

		StringToNominal filter = new StringToNominal();
		filter.setAttributeRange("last");
		filter.setInputFormat(classificationDataSet);
		Instances modifedDataSet = Filter.useFilter(classificationDataSet, filter);

		for (Map.Entry<String, Classifier> entry : this.classifiers.entrySet()) {

			String classifierName = entry.getKey();
			Classifier classifier = entry.getValue();

			Evaluation eval = new Evaluation(modifedDataSet);
			Enumeration<Object> enumeration = modifedDataSet.attribute(Util.ATTRIBUTE_FILE).enumerateValues();
			while (enumeration.hasMoreElements()) {
				Evaluation evalLocal = new Evaluation(modifedDataSet);

				String fileName = (String) enumeration.nextElement();
				Integer idx = modifedDataSet.attribute(Util.ATTRIBUTE_FILE).indexOfValue(fileName) + 1;

				Instances train = filterByFileName(modifedDataSet, idx, false);
				Instances test = filterByFileName(modifedDataSet, idx, true);

				Classifier c = cloneAndWrapClassifier(classifier);
				c.buildClassifier(train);
				eval.evaluateModel(c, test);
				evalLocal.evaluateModel(c, test);
				if (log.isInfoEnabled())
					log.info(evalLocal.toMatrixString(fileName));

				if (outputMisclassified && log.isInfoEnabled()) {
					outputMisclassifiedInstances(test, c);
				}
			}

			if (log.isInfoEnabled()) {
				log.info(String.format("%s (leave one out)", classifierName));
				logEvalResults(eval);
			}
		}
	}
	// endregion

	// region easyEnsemble
	private void easyEnsemble(int numberOfIterations) {

		List<String> columnNames = getColumnNames();

		Map<String, Map<String, Double>> overallResultMap = new HashMap<String, Map<String, Double>>();

		for (Map.Entry<String, Classifier> entry : this.classifiers.entrySet()) {
			try {
				String classifierName = entry.getKey();
				Classifier classifier = entry.getValue();
				Evaluation eval = new Evaluation(classificationDataSet);
				// do crossvalidation manually in order to output misclassified
				// examples
				Random rand = new Random(seed);
				Instances randData = new Instances(classificationDataSet);
				randData.randomize(rand);
				randData.stratify(numFolds);

				for (int i = 0; i < numFolds; i++) {
					Instances train = randData.trainCV(numFolds, i);
					Instances test = randData.testCV(numFolds, i);

					Vote votedClassifier = new Vote();
					Instances trainPos = new Instances(train, 0);
					Instances trainNeg = new Instances(train, 0);

					for (int j = 0; j < train.numInstances(); j++) {
						Instance inst = train.get(j);
						if (inst.classValue() == train.classAttribute().indexOfValue(Util.LABEL_TRUE))
							trainPos.add(inst);
						else
							trainNeg.add(inst);
					}

					for (int j = 0; j < numberOfIterations; j++) {

						Instances tempTrain = getBalancedTrainingSubset(trainPos, trainNeg, j);

						Classifier c = cloneAndWrapClassifier(classifier);
						c.buildClassifier(tempTrain);

						votedClassifier.addPreBuiltClassifier(c);
					}

					eval.evaluateModel(votedClassifier, test);

					if (outputMisclassified && log.isInfoEnabled()) {
						outputMisclassifiedInstances(test, votedClassifier);
					}

				}

				if (log.isInfoEnabled()) {
					log.info(String.format("%s (easyEnsemble)", classifierName));
					logEvalResults(eval);
				}

				Map<String, Double> classifierMap = getClassifierMap(eval);

				overallResultMap.put(classifierName, classifierMap);

			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("Can't do easyEnsemble on classifier");
				if (log.isDebugEnabled())
					log.debug("Can't do easyEnsemble on classifier", e);
			}
		}

		Util.writeEvaluationToCsv(this.outputFileName, columnNames, overallResultMap);
	}
	// endregion

	// region balanceCascade
	private void balanceCascade() {

		List<String> columnNames = getColumnNames();

		Map<String, Map<String, Double>> overallResultMap = new HashMap<String, Map<String, Double>>();

		for (Map.Entry<String, Classifier> entry : this.classifiers.entrySet()) {
			try {
				String classifierName = entry.getKey();
				Classifier classifier = entry.getValue();
				Evaluation eval = new Evaluation(classificationDataSet);
				// do crossvalidation manually in order to output misclassified
				// examples
				Random rand = new Random(seed);
				Instances randData = new Instances(classificationDataSet);
				randData.randomize(rand);
				randData.stratify(numFolds);

				for (int i = 0; i < numFolds; i++) {
					Instances train = randData.trainCV(numFolds, i);
					Instances test = randData.testCV(numFolds, i);

					Vote votedClassifier = new Vote();
					Instances trainPos = new Instances(train, 0);
					Instances trainNeg = new Instances(train, 0);

					for (int j = 0; j < train.numInstances(); j++) {
						Instance inst = train.get(j);
						if (inst.classValue() == train.classAttribute().indexOfValue(Util.LABEL_TRUE))
							trainPos.add(inst);
						else
							trainNeg.add(inst);
					}

					int j = 0;
					while ((trainNeg.numInstances() >= trainPos.numInstances()) && (j < 100)) {

						Instances tempTrain = getBalancedTrainingSubset(trainPos, trainNeg, j);

						Classifier c = cloneAndWrapClassifier(classifier);
						c.buildClassifier(tempTrain);

						votedClassifier.addPreBuiltClassifier(c);

						Instances newTrainNeg = new Instances(trainNeg, 0);
						for (int k = 0; k < trainNeg.numInstances(); k++) {
							Instance inst = trainNeg.get(k);
							double pred = c.classifyInstance(inst);
							if (pred != inst.classValue())
								newTrainNeg.add(inst);
						}

						trainNeg = newTrainNeg;
						j++;
					}

					eval.evaluateModel(votedClassifier, test);

					if (outputMisclassified && log.isInfoEnabled()) {
						outputMisclassifiedInstances(test, votedClassifier);
					}

				}

				if (log.isInfoEnabled()) {
					log.info(String.format("%s (cross-validation)", classifierName));
					logEvalResults(eval);
				}

				Map<String, Double> classifierMap = getClassifierMap(eval);

				overallResultMap.put(classifierName, classifierMap);

			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("Can't cross-validate classifier");
				if (log.isDebugEnabled())
					log.debug("Can't cross-validate classifier", e);
			}
		}

		Util.writeEvaluationToCsv(this.outputFileName, columnNames, overallResultMap);
	}
	// endregion

	// region learningCurve
	private void learningCurve() {

		List<String> performanceMeasures = new ArrayList<String>();
		performanceMeasures.add(Util.COLUMN_NAME_BALANCED_ACCURACY);
		performanceMeasures.add(Util.COLUMN_NAME_KAPPA);
		performanceMeasures.add(Util.COLUMN_NAME_AUC);
		performanceMeasures.add(Util.COLUMN_NAME_FSCORE);

		for (Map.Entry<String, Classifier> entry : this.classifiers.entrySet()) {

			try {
				String classifierName = entry.getKey();
				Classifier classifier = entry.getValue();

				if (log.isInfoEnabled())
					log.info(classifierName);

				Map<Integer, Map<String, Map<String, Double>>> overallMap = new HashMap<Integer, Map<String, Map<String, Double>>>();

				for (Integer percentage : this.trainingSetPercentages) {
					Random rand = new Random(seed);
					Instances randData = new Instances(classificationDataSet);
					randData.randomize(rand);
					randData.stratify(numFolds);

					Map<String, Map<String, Double>> percentageMap = new HashMap<String, Map<String, Double>>();
					Map<String, Double> trainMap = new HashMap<String, Double>();
					Map<String, Double> testMap = new HashMap<String, Double>();
					percentageMap.put(Util.COLUMN_NAME_TRAINING, trainMap);
					percentageMap.put(Util.COLUMN_NAME_TEST, testMap);

					trainMap.put(Util.COLUMN_NAME_BALANCED_ACCURACY, 0.0);
					trainMap.put(Util.COLUMN_NAME_KAPPA, 0.0);
					trainMap.put(Util.COLUMN_NAME_AUC, 0.0);
					trainMap.put(Util.COLUMN_NAME_FSCORE, 0.0);

					testMap.put(Util.COLUMN_NAME_BALANCED_ACCURACY, 0.0);
					testMap.put(Util.COLUMN_NAME_KAPPA, 0.0);
					testMap.put(Util.COLUMN_NAME_AUC, 0.0);
					testMap.put(Util.COLUMN_NAME_FSCORE, 0.0);

					for (int i = 0; i < numFolds; i++) {
						Instances train = randData.trainCV(numFolds, i);
						Instances test = randData.testCV(numFolds, i);

						Resample resample = new Resample();
						resample.setSampleSizePercent(percentage);
						resample.setNoReplacement(true);
						resample.setInputFormat(train);
						train = Filter.useFilter(train, resample);

						Classifier c;
						if (this.useBaggingWrapper) {
							Bagging outer = (Bagging) AbstractClassifier.makeCopy(this.baggingWrapper);
							Classifier inner = AbstractClassifier.makeCopy(classifier);
							outer.setClassifier(inner);
							c = outer;
						} else if (this.useCostSensitiveWrapper) {
							MetaCost outer = (MetaCost) AbstractClassifier.makeCopy(this.costSensitiveWrapper);
							Classifier inner = AbstractClassifier.makeCopy(classifier);
							outer.setClassifier(inner);
							c = outer;
						} else {
							c = AbstractClassifier.makeCopy(classifier);
						}
						c.buildClassifier(train);
						Evaluation evalTrain = new Evaluation(train);
						Evaluation evalTest = new Evaluation(test);
						evalTrain.evaluateModel(c, train);
						evalTest.evaluateModel(c, test);

						trainMap.put(Util.COLUMN_NAME_BALANCED_ACCURACY, trainMap.get(Util.COLUMN_NAME_BALANCED_ACCURACY) + (getBalancedAcc(evalTrain, this.positiveClassIdx) / numFolds));
						trainMap.put(Util.COLUMN_NAME_KAPPA, trainMap.get(Util.COLUMN_NAME_KAPPA) + (evalTrain.kappa() / numFolds));
						trainMap.put(Util.COLUMN_NAME_AUC, trainMap.get(Util.COLUMN_NAME_AUC) + (evalTrain.areaUnderROC(this.positiveClassIdx) / numFolds));
						trainMap.put(Util.COLUMN_NAME_FSCORE, trainMap.get(Util.COLUMN_NAME_FSCORE) + (evalTrain.fMeasure(this.positiveClassIdx) / numFolds));

						testMap.put(Util.COLUMN_NAME_BALANCED_ACCURACY, testMap.get(Util.COLUMN_NAME_BALANCED_ACCURACY) + (getBalancedAcc(evalTest, this.positiveClassIdx) / numFolds));
						testMap.put(Util.COLUMN_NAME_KAPPA, testMap.get(Util.COLUMN_NAME_KAPPA) + (evalTest.kappa() / numFolds));
						testMap.put(Util.COLUMN_NAME_AUC, testMap.get(Util.COLUMN_NAME_AUC) + (evalTest.areaUnderROC(this.positiveClassIdx) / numFolds));
						testMap.put(Util.COLUMN_NAME_FSCORE, testMap.get(Util.COLUMN_NAME_FSCORE) + (evalTest.fMeasure(this.positiveClassIdx) / numFolds));

					}

					overallMap.put(percentage, percentageMap);
				}

				String fileName = String.format("out/%s.csv", classifierName);
				Util.writeLearningCurvesToCsv(fileName, performanceMeasures, overallMap);

			} catch (Exception e) {
				if (log.isErrorEnabled())
					log.error("Can't create learning curve");
				if (log.isDebugEnabled())
					log.debug("Can't create learning curve", e);
			}
		}
	}
	// endregion

	// region helper methods
	private Instances getBalancedTrainingSubset(Instances trainPos, Instances trainNeg, int j) {
		Random rand2 = new Random(j);
		trainNeg.randomize(rand2);
		Instances negSample = new Instances(trainNeg, 0, trainPos.numInstances());

		Instances tempTrain = new Instances(trainPos, 0);
		tempTrain.addAll(negSample);
		tempTrain.addAll(trainPos);
		return tempTrain;
	}

	private Map<String, Double> getClassifierMap(Evaluation eval) {
		Map<String, Double> classifierMap = new HashMap<String, Double>();
		classifierMap.put(Util.COLUMN_NAME_BALANCED_ACCURACY, getBalancedAcc(eval, this.positiveClassIdx));
		classifierMap.put(Util.COLUMN_NAME_KAPPA, eval.kappa());
		classifierMap.put(Util.COLUMN_NAME_FSCORE, eval.fMeasure(this.positiveClassIdx));
		classifierMap.put(Util.COLUMN_NAME_PRECISION, eval.precision(this.positiveClassIdx));
		classifierMap.put(Util.COLUMN_NAME_RECALL, eval.recall(this.positiveClassIdx));
		classifierMap.put(Util.COLUMN_NAME_ACCURACY, eval.pctCorrect()/100.0);
		return classifierMap;
	}

	private Map<String, Double> getClassifierMap(double tp, double fn, double fp, double tn) {
		double n = tp + fn + fp + tn;
		double accuracy = (tp + tn) / n;
		double balancedAcc = ((0.5 * tp) / (tp + fn)) + ((0.5 * tn) / (tn + fp));
		double precision = tp / (tp + fp);
		double recall = tp / (tp + fn);
		double fScore = (2 * precision * recall) / (precision + recall);
		double pc = (((tp + fp) * (tp + fn)) + ((tn + fp) * (tn + fn))) / (n * n);
		double kappa = (accuracy - pc) / (1 - pc);
		
		
		Map<String, Double> classifierMap = new HashMap<String, Double>();
		
		classifierMap.put(Util.COLUMN_NAME_BALANCED_ACCURACY, balancedAcc);
		classifierMap.put(Util.COLUMN_NAME_KAPPA, kappa);
		classifierMap.put(Util.COLUMN_NAME_FSCORE, fScore);
		classifierMap.put(Util.COLUMN_NAME_PRECISION, precision);
		classifierMap.put(Util.COLUMN_NAME_RECALL, recall);
		classifierMap.put(Util.COLUMN_NAME_ACCURACY, accuracy);
		
		return classifierMap;
	}
	
	private List<String> getColumnNames() {
		List<String> columnNames = new ArrayList<String>();
		columnNames.add(Util.COLUMN_NAME_BALANCED_ACCURACY);
		columnNames.add(Util.COLUMN_NAME_KAPPA);
		columnNames.add(Util.COLUMN_NAME_FSCORE);
		columnNames.add(Util.COLUMN_NAME_PRECISION);
		columnNames.add(Util.COLUMN_NAME_RECALL);
		columnNames.add(Util.COLUMN_NAME_ACCURACY);
		return columnNames;
	}

	private Classifier cloneAndWrapClassifier(Classifier classifier) throws Exception {
		Classifier c;
		if (this.useCostSensitiveWrapper) {
			MetaCost outer = (MetaCost) AbstractClassifier.makeCopy(this.costSensitiveWrapper);
			Classifier inner = AbstractClassifier.makeCopy(classifier);
			outer.setClassifier(inner);
			c = outer;
		} else {
			c = AbstractClassifier.makeCopy(classifier);
		}
		return c;
	}

	private void logEvalResults(double tp, double fn, double fp, double tn, Map<String, Double> classifierMap) {
		
		log.info("AND of individual classifiers");
		log.info("-----------------------------");
		log.info(String.format("accuracy: %f", classifierMap.get(Util.COLUMN_NAME_ACCURACY)));
		log.info(String.format("balanced accuracy: %f", classifierMap.get(Util.COLUMN_NAME_BALANCED_ACCURACY)));
		log.info(String.format("precision: %f", classifierMap.get(Util.COLUMN_NAME_PRECISION)));
		log.info(String.format("recall: %f", classifierMap.get(Util.COLUMN_NAME_RECALL)));
		log.info(String.format("f score: %f", classifierMap.get(Util.COLUMN_NAME_FSCORE)));
		log.info(String.format("Cohen's kappa: %f", classifierMap.get(Util.COLUMN_NAME_KAPPA)));
		log.info("    a     b    <-- classified as");
		log.info(String.format("%d %d | a = true", (int) tp, (int) fn));
		log.info(String.format("%d %d | b = false", (int) fp, (int) tn));
	}
	
	private void logEvalResults(Evaluation eval) throws Exception {
		// compute balanced accuracy
		double balancedAcc = getBalancedAcc(eval, this.positiveClassIdx);

		// output
		log.info(eval.toSummaryString());
		log.info(String.format("balanced accuracy: %f", balancedAcc));
		log.info(String.format("Cohen's kappa: %f", eval.kappa()));
		log.info(eval.toClassDetailsString());
		log.info(eval.toMatrixString());
	}

	private double getBalancedAcc(Evaluation eval, int positiveClassIdx) {

		double tp = eval.numTruePositives(positiveClassIdx);
		double tn = eval.numTrueNegatives(positiveClassIdx);
		double fp = eval.numFalsePositives(positiveClassIdx);
		double fn = eval.numFalseNegatives(positiveClassIdx);

		double balancedAcc = ((0.5 * tp) / (tp + fn)) + ((0.5 * tn) / (tn + fp));
		return balancedAcc;
	}

	private void outputMisclassifiedInstances(Instances test, Classifier c) throws Exception {
		for (int i = 0; i < test.numInstances(); i++) {
			Instance inst = test.instance(i);
			double prediction = c.classifyInstance(inst);
			if (inst.classValue() != prediction) {

				String str = String.format("%s ; %s", inst.stringValue(test.attribute(Util.ATTRIBUTE_URI)), inst.stringValue(test.attribute(Util.ATTRIBUTE_FILE)));

				if (inst.classValue() == inst.classAttribute().indexOfValue(Util.LABEL_TRUE))
					log.info(String.format("False Negative: %s", str));
				else
					log.info(String.format("False Positive: %s", str));
			}
		}
	}

	private Instances filterByFileName(Instances dataSet, Integer fileNameIdx, boolean isTest) throws Exception {

		RemoveWithValues filter = new RemoveWithValues();
		filter.setAttributeIndex("last");
		filter.setNominalIndices(fileNameIdx.toString());
		filter.setInvertSelection(isTest);
		filter.setInputFormat(dataSet);

		Instances result = Filter.useFilter(dataSet, filter);

		return result;
	}

	private void filterDataSet() {
		try {
			this.analysisFilter.setInputFormat(originalDataSet);
			this.analysisDataSet = Filter.useFilter(this.originalDataSet, this.analysisFilter);

			this.classifierFilter.setInputFormat(originalDataSet);
			this.classificationDataSet = Filter.useFilter(this.originalDataSet, this.classifierFilter);
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("Can't apply filter");
			if (log.isDebugEnabled())
				log.debug("Can't apply filter", e);
		}
	}
	// endregion

	public void run() throws Exception {

		filterDataSet();

		if (this.useBaggingWrapper && this.useCostSensitiveWrapper && log.isWarnEnabled())
			log.warn("both useBaggingWrapper and useCostSensitiveWrapper active!");

		if (this.doFeatureAnalysis)
			featureAnalysis();
		if (this.doCrossValidation)
			crossValidation();
		if (this.doLeaveOneOut)
			leaveOneOut();
		if (this.doEasyEnsemble)
			easyEnsemble(this.numberOfEnsembleMembers);
		if (this.doBalanceCascade)
			balanceCascade();
		if (this.doLearningCurve)
			learningCurve();
		if (this.doOverallCrossvalidation)
			overallCrossvalidation();
	}

	public static void main(String[] args) throws Exception {
		System.setProperty("java.util.logging.config.file", "./config/logging-benchmark.properties");
		try {
			LogManager.getLogManager().readConfiguration();
			log = LogFactory.getLog(FilteringBenchmark.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationContext context = new FileSystemXmlApplicationContext("config/benchmark.xml");
		FilteringBenchmark test = (FilteringBenchmark) context.getBean("filteringBenchmark");
		((AbstractApplicationContext) context).close();

		test.run();
	}
}
