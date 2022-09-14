import sys
import os
import multiprocessing as mp
from os.path import exists
import time

def main(argv):
	with open('/home/khaes/phd/projects/explanation/code/tmp/didiffff/results/analysis.txt') as analysisFile:
		print('patch,no-diff,same-length,content-diff,length-diff,cnt')

		lines = analysisFile.readlines()
		lines = [line.rstrip() for line in lines]
		patch = 'undefined'
		bug = 'undefined'
		allCnt = 0
		allNoDiff = 0
		allSameLength = 0
		allContentDiff = 0
		allLengthDiff = 0
		bugRes = {}
		for line in lines:
			if 'no-diff' in line:
				noDiff = 'no-diff: true' in line
				sameLength = 'same-length: true' in line
				contentDiff = 'content-diff: true' in line
				lengthDiff = 'length-diff: true' in line
				print(f'{patch},{noDiff},{sameLength},{contentDiff},{lengthDiff},1')
				allCnt = allCnt + 1
				bugRes[bug]['cnt'] = bugRes[bug]['cnt'] + 1
				if noDiff:
					allNoDiff = allNoDiff + 1
					bugRes[bug]['no-diff'] = bugRes[bug]['no-diff'] + 1
				if sameLength:
					allSameLength = allSameLength + 1
					bugRes[bug]['same-length'] = bugRes[bug]['same-length'] + 1
				if contentDiff:
					allContentDiff = allContentDiff + 1
					bugRes[bug]['content-diff'] = bugRes[bug]['content-diff'] + 1
				if lengthDiff:
					allLengthDiff = allLengthDiff + 1
					bugRes[bug]['length-diff'] = bugRes[bug]['length-diff'] + 1
			else:
				patch = line.split(':')[0]
				bug = patch.split('-')[1] + '-' + patch.split('-')[2]
				if bug not in bugRes:
					bugRes[bug] = {'no-diff': 0, 'same-length': 0, 'content-diff': 0, 'length-diff': 0, 'cnt': 0}

		for bug in bugRes:
			print(f"{bug},{bugRes[bug]['no-diff']},{bugRes[bug]['same-length']},{bugRes[bug]['content-diff']},{bugRes[bug]['length-diff']},{bugRes[bug]['cnt']}")

		print(f"ALL,{allNoDiff},{allSameLength},{allContentDiff},{allLengthDiff},{allCnt}")

if __name__ == "__main__":
    main(sys.argv[1:])
