import sys

inputFileName = sys.argv[1]
outputFileName = sys.argv[2]

with open(inputFileName, 'r') as f, open(outputFileName, 'w') as o:
    for line in f:
        if "anthropomatik" not in line and "SCHWERWIEGEND" not in line:
            o.write(line)
