import sys

outputFileName = sys.argv[1]

with open(outputFileName, 'w') as f:
    for aggregationType in ["0", "1", "2"]:
        for useSentence in ["true", "false"]:
            for normalize in ["true", "false"]:
                f.write('\t<bean id="sameDocumentFeature' + aggregationType + useSentence[0] + normalize[0] + '" class="edu.kit.anthropomatik.isl.newsTeller.retrieval.ranking.features.SameDocumentFeature">\n')
                f.write('\t\t<property name="ksAdapter" ref="ksAdapter" />\n')
                f.write('\t\t<property name="name" value="sameDocument' + aggregationType + useSentence[0] + normalize[0] + '" />\n')
                f.write('\t\t<property name="aggregationType" value="' + aggregationType + '" />\n')
                f.write('\t\t<property name="useSentenceInsteadOfText" value="' + useSentence + '" />\n')
                f.write('\t\t<property name="normalizeOverNumberOfTotalDocuments" value="' + normalize + '" />\n')
                f.write('\t</bean>\n\n')
