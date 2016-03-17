package edu.kit.anthropomatik.isl.newsTeller.benchmark;

import java.util.Random;

import weka.classifiers.RandomizableClassifier;
import weka.core.Instance;
import weka.core.Instances;

public class RandomRanker extends RandomizableClassifier {

	private static final long serialVersionUID = 1L;

	private Random random;
	
	@Override
	public void buildClassifier(Instances data) throws Exception {
		// don't do anything
		this.random = new Random(m_Seed);
	}

	@Override
	public double classifyInstance(Instance instance) throws Exception {
		return this.random.nextDouble();
	}
	
}
