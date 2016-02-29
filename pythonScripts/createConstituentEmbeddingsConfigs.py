import sys

def getAbbreviation(label):
    if (label == "entityPrefLabel"):
        return "p"
    if (label == "entityLabel"):
        return "l"
    if (label == "matchingEntityLabel"):
        return "m"
    if (label == "entityDescription"):
        return "d"

outputFileName = sys.argv[1]
userInterests = sys.argv[2]

with open(outputFileName, 'w') as f:
    for aggKey in ["0", "1", "2", "3"]:
        for aggSent in ["0", "1", "2", "3"]:
            for entity in ["entity", "keywordEntity"]:
                for label in ["entityPrefLabel", "entityLabel", "matchingEntityLabel", "entityDescription"]:
                    f.write('\t<bean id="constituentEmbeddingsFeature' + aggKey + aggSent + entity[0] + getAbbreviation(label) + userInterests + '" class="edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features.ConstituentEmbeddingsFeature">\n')
                    f.write('\t\t<constructor-arg name="stopWordsFileName" value="resources/features/stopwords.txt" />\n')
                    f.write('\t\t<property name="embeddings" ref="embeddings" />\n')
                    f.write('\t\t<property name="ksAdapter" ref="ksAdapter" />\n')
                    f.write('\t\t<property name="name" value="constituentEmbeddings' + aggKey + aggSent + entity[0] + getAbbreviation(label) + userInterests + '" />\n')
                    f.write('\t\t<property name="keywordAggregationType" value="' + aggKey + '" />\n')
                    f.write('\t\t<property name="innerAggregationType" value="' + aggSent + '" />\n')
                    f.write('\t\t<property name="entityName" value="' + entity + '" />\n')
                    f.write('\t\t<property name="valueName" value="' + label + '" />\n')
                    f.write('\t\t<property name="needsKeywordIteration" value="' + str(entity == "keywordEntity" or label == "matchingEntityLabel") + '" />\n')
                    f.write('\t\t<property name="useUserInterestsInsteadOfQuery" value="' + userInterests + '" />\n')
                    f.write('\t</bean>\n\n')
