import sys

outputFileName = sys.argv[1]

with open(outputFileName, 'w') as f:
    for aggKey in ["0", "1", "2", "3"]:
        for aggSent in ["0", "1", "2", "3"]:
            f.write('\t<bean id="interestQueryEmbeddingsFeature' + aggKey + aggSent + '" class="edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features.InterestQueryEmbeddingsFeature">\n')
            f.write('\t\t<constructor-arg name="stopWordsFileName" value="resources/features/stopwords.txt" />\n')
            f.write('\t\t<property name="embeddings" ref="embeddings" />\n')
            f.write('\t\t<property name="ksAdapter" ref="ksAdapter" />\n')
            f.write('\t\t<property name="name" value="interestQueryEmbeddings' + aggKey + aggSent + '" />\n')
            f.write('\t\t<property name="keywordAggregationType" value="' + aggKey + '" />\n')
            f.write('\t\t<property name="innerAggregationType" value="' + aggSent + '" />\n')
            f.write('\t</bean>\n\n')
