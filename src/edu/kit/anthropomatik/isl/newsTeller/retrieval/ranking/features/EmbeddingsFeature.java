package edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import edu.kit.anthropomatik.isl.newsTeller.data.Keyword;
import edu.kit.anthropomatik.isl.newsTeller.userModel.UserModel;

public class EmbeddingsFeature extends RankingFeature {

	@Override
	public double getValue(String eventURI, List<Keyword> keywords, UserModel userModel) {
		
		return 0;
	}
	
	public static void main(String[] args) {
		try {
			WordVectors wordVectors = WordVectorSerializer.loadTxtVectors(new File("resources/glove.6B.50d.txt"));
			System.out.println(String.format("erupt-volcano: %f", wordVectors.similarity("erupt", "volcano")));
			System.out.println(String.format("erupt-fire: %f", wordVectors.similarity("erupt", "fire")));
			System.out.println(String.format("erupt-riot: %f", wordVectors.similarity("erupt", "riot")));
			System.out.println(String.format("erupt-cat: %f", wordVectors.similarity("erupt", "cat")));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
