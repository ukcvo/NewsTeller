import sys

def getAbbreviation(label):
    if (label == "entityPrefLabel"):
        return "p"
    if (label == "entityLabel"):
        return "l"
    if (label == "matchingEntityLabel"):
        return "m"

outputFileName = sys.argv[1]

with open(outputFileName, 'w') as f:
    for fileName in ["quotation", "I", "you", "heSheIt", "heShe", "it", "we", "they", "thisThat", "stopwords", "pronoun"]:
        for entity in ["entity", "keywordEntity", "nonEntity"]:
            for label in ["entityPrefLabel", "entityLabel", "matchingEntityLabel"]:
                f.write('\t<bean id="constituentContainsFeature_' + fileName + '_' + entity[0] + getAbbreviation(label) + '" class="edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features.ConstituentContainsStringsFeature">\n')
                f.write('\t\t<constructor-arg name="stringFileName" value="resources/features/' + fileName + '.txt" />\n')
                f.write('\t\t<property name="ksAdapter" ref="ksAdapter" />\n')
                f.write('\t\t<property name="name" value="constituentContains_' + fileName + '_' + entity[0] + getAbbreviation(label) + '" />\n')
                f.write('\t\t<property name="entityName" value="' + entity + '" />\n')
                f.write('\t\t<property name="valueName" value="' + label + '" />\n')
                f.write('\t\t<property name="needsKeywordIteration" value="' + str(entity == "keywordEntity" or label == "matchingEntityLabel") + '" />\n')
                f.write('\t</bean>\n\n')
