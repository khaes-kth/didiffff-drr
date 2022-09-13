import sys
import os
import multiprocessing as mp
from os.path import exists
import time

MAINDIR = '/home/khaes/phd/projects/explanation/code/tmp/didiffff'

def process(bugId, patchName, commit, patchFile, lineStart, lineEnd, testClass, testFile):
	print(bugId, patchName, commit, patchFile, lineStart, lineEnd, testClass, testFile)
	os.chdir(f'{MAINDIR}/drr-execdiff')
	os.system(f'git checkout -f {commit}')
	os.system(f'rm -rf ../didiffff/sample/{patchName}')
	os.system(f'cp -r {bugId} ../didiffff/sample/{patchName}')
	os.chdir(f'../didiffff/sample')
	os.system(f'cp {bugId}/pom.xml {patchName}/pom.xml')
	os.system(f'cp {bugId}/{testFile} {patchName}/{testFile}')
	os.system(f'rm selogger/*')
	os.chdir(f'{patchName}')
	os.system(f'mvn test -Dtest="{testClass}"')
	os.chdir(f'..')
	os.system(f'java -jar nod4j-0.2.3-t.jar ./{patchName}/ ./selogger/ ../didiffff/public/assets/{patchName}')
	os.chdir(f'../didiffff')
	os.system(f'rm -rf public/assets/proj1')
	os.system(f'rm -rf public/assets/proj2')
	os.system(f'cp -r public/assets/{bugId} public/assets/proj1')
	os.system(f'cp -r public/assets/{patchName} public/assets/proj2')
	os.system(f'rm -rf public/assets/{patchName}')
	os.system(f'npm run load')
	os.system(f'npm run build')
	os.system(f'zip -r ../../results/{patchName}.zip build/*')
	os.chdir(f'../..')
	os.system(f'echo "{patchName}:" >> results/analysis.txt')
	os.system(f'java -jar didi-analyzer.jar http://localhost {patchFile} {lineStart} {lineEnd} >> results/analysis.txt')

def main(argv):
	with open(argv[0]) as bugDataFile:
		lines = bugDataFile.readlines()
		lines = [line.rstrip() for line in lines]
		bugData = {}
		for line in lines:
			bugId = line.split(',')[0]
			bugData[bugId] = {}
			patchFile = line.split(',')[1]
			lineStart = line.split(',')[2]
			lineEnd = line.split(',')[3]
			testClass = line.split(',')[4]
			testFile = line.split(',')[5]
			bugData[bugId]['patchFile'] = patchFile
			bugData[bugId]['lineStart'] = lineStart
			bugData[bugId]['lineEnd'] = lineEnd
			bugData[bugId]['testClass'] = testClass
			bugData[bugId]['testFile'] = testFile

	with open(argv[1]) as patchList:
		lines = patchList.readlines()
		lines = [line.rstrip() for line in lines]
		for line in lines:
			patchName = line.split(',')[0]
			commit = line.split(',')[1]
			bugId = patchName.split('-')[1] + '-' + patchName.split('-')[2]

			if not (bugId in bugData):
				continue
			process(bugId, patchName, commit, bugData[bugId]['patchFile'], bugData[bugId]['lineStart'], bugData[bugId]['lineEnd'], bugData[bugId]['testClass'], bugData[bugId]['testFile'])

if __name__ == "__main__":
    main(sys.argv[1:])
