import sys

outputFileName = sys.argv[1]
userInterests = sys.argv[2]

with open(outputFileName, 'w') as f:
    for aggKey in ["0", "1", "2", "3"]:
        for aggSent in ["0", "1", "2", "3"]:
            for title in ["false", "true"]:
                f.write('\t<bean id="textEmbeddingsFeature' + aggKey + aggSent + title[0] + userInterests + '" class="edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features.TextEmbeddingsFeature">\n')
                f.write('\t\t<constructor-arg name="stopWordsFileName" value="resources/features/stopwords.txt" />\n')
                f.write('\t\t<property name="embeddings" ref="embeddings" />\n')
                f.write('\t\t<property name="ksAdapter" ref="ksAdapter" />\n')
                f.write('\t\t<property name="name" value="textEmbeddings' + aggKey + aggSent + title[0] + userInterests + '" />\n')
                f.write('\t\t<property name="keywordAggregationType" value="' + aggKey + '" />\n')
                f.write('\t\t<property name="innerAggregationType" value="' + aggSent + '" />\n')
                f.write('\t\t<property name="useTitleInsteadOfSentence" value="' + title + '" />\n')
                f.write('\t\t<property name="useUserInterestsInsteadOfQuery" value="' + userInterests + '" />\n')
                f.write('\t</bean>\n\n')
