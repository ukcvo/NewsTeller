import sys

outputFileName = sys.argv[1]

with open(outputFileName, 'w') as f:
    for aggKey in ["0", "1", "2"]:
        for aggSent in ["0", "1", "2"]:
            for title in ["false", "true"]:
                for contains in ["false", "true"]:
                    for stem in ["false", "true"]:
                        for split in ["false", "true"]:
                            f.write('\t<bean id="keywordInSentenceFeature' + aggKey + aggSent + title[0] + contains[0] + stem[0] + split[0]
                                    + '" class="edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features.KeywordsInSentenceFeature">\n')
                            f.write('\t\t<property name="ksAdapter" ref="ksAdapter" />\n')
                            f.write('\t\t<property name="name" value="keywordInSentence' + aggKey + aggSent + title[0] + contains[0] + stem[0] + split[0] + '" />\n')
                            f.write('\t\t<property name="aggregationTypeKeyword" value="' + aggKey + '" />\n')
                            f.write('\t\t<property name="aggregationTypeSentence" value="' + aggSent + '" />\n')
                            f.write('\t\t<property name="useTitleInsteadOfSentence" value="' + title + '" />\n')
                            f.write('\t\t<property name="useContainsInsteadOfRegex" value="' + contains + '" />\n')
                            f.write('\t\t<property name="useStemInsteadOfWord" value="' + stem + '" />\n')
                            f.write('\t\t<property name="splitKeywords" value="' + split + '" />\n')
                            f.write('\t</bean>\n\n')
