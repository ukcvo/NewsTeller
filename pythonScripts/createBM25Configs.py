import sys

outputFileName = sys.argv[1]
userInterests = sys.argv[2]

with open(outputFileName, 'w') as f:
    for stringType in ["Sentence", "Text", "Title"]:
        for k1 in ["1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "1.9", "2.0"]:
           f.write('\t<bean id="BM25Feature' + stringType + k1 + str(userInterests) + '" class="edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features.BM25Feature">\n')
           f.write('\t\t<property name="ksAdapter" ref="ksAdapter" />\n')
           f.write('\t\t<property name="name" value="BM25' + stringType + k1 + str(userInterests) + '" />\n')
           f.write('\t\t<property name="k1" value="' + k1 + '" />\n')
           f.write('\t\t<property name="useTextInsteadOfSentence" value="' + str(stringType == "Text") + '" />\n')
           f.write('\t\t<property name="useTitleInsteadOfSentence" value="' + str(stringType == "Title") + '" />\n')
           f.write('\t\t<property name="useUserInterestsInsteadOfQuery" value="' + str(userInterests) + '" />\n')
           f.write('\t</bean>\n\n')
