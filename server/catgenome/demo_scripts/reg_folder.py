import requests
import json
import argparse
import sys
import os.path
from os import listdir

parser = argparse.ArgumentParser(description='Registers all files in directory in NGB, then creates a project with them')
parser.add_argument('projectName', metavar='PROJECT_NAME', help='A name for the project')
parser.add_argument('dir', metavar='DIRECTORY_PATH', help='Path to directory, where files are located')
parser.add_argument('referenceId', metavar='REFERENCE_ID',  type=int, help='ID of the reference, for wich file is being registered')
args = parser.parse_args()

base_url = "http://localhost:8080/catgenome/restapi/{0}/register"
refUrl = "http://localhost:8080/catgenome/restapi/reference/{0}/load"
projectUrl = "http://localhost:8080/catgenome/restapi/project/save"
fileExtensions = {
	'vcf': "vcf",
	'vcf.gz': "vcf",
	'gff': "gene",
	'gtf': "gene",
	'gff.gz': "gene",
	'gtf.gz': "gene",
	'gff3': "gene",
	'gff3.gz': "gene",
	'bam':"bam",
	'seg':'seg',
	'seg.gz':'seg',
	'bw':'wig',
	'bed':'bed',
	'bed.gz':"bed",
	'vg': "vg",
	'maf': "maf",
	'maf.gz': "maf",
	'tbi': "index",
	'idx': "index",
	'bai': "index"
}

indexExtensions = {
	'vcf': "idx",
	'vcf.gz': "tbi",
	'gff': "tbi",
	'gtf': "tbi",
	'gff.gz': "tbi",
	'gtf.gz': "tbi",
	'gff3': "tbi",
	'gff3.gz': "tbi",
	'bam':"bai",
	'seg':'tbi',
	'seg.gz':'tbi',
	'bed':'tbi',
	'bed.gz':"tbi",
	'maf': "tbi",
	'maf.gz': "tbi"
}

def determinFileType(filePath):
	extension = os.path.splitext(filePath)[1][1:]
	if extension == 'gz':
		path = filePath[:-3]
		extension = os.path.splitext(path)[1][1:] + ".gz"

	if not extension in fileExtensions:
		print("Error: File extension '{0}' is not supported".format(extension))
		sys.exit(1)

	return extension

if not os.path.isdir(args.dir):
	print("{0} is not a valid directory path".format(args.dir))
	sys.exit(1)

#load reference
refResponse = requests.request("GET", refUrl.format(args.referenceId))
respObj = json.loads(refResponse.text)
if not 'payload' in respObj:
	print("Error: " + refResponse.text)
	sys.exit(1)

reference = respObj['payload']

#check all files
files = []
for f in listdir(args.dir):
	filePath = args.dir + "/" + f
	if os.path.isfile(filePath):
		determinFileType(filePath)
		files.append(filePath)

projectItems = [{"bioDataItemId":reference["bioDataItemId"]}]

headers = {
	    	'content-type': "application/json",
	    	'cache-control': "no-cache"
	    }

for f in listdir(args.dir):
	filePath = args.dir + "/" + f
	if os.path.isfile(filePath):
		extension = determinFileType(filePath)
		fType = fileExtensions[extension]
		
		if fType != 'index':
			payload_obj = {
				'path': filePath,
				'referenceId': args.referenceId
			}

			if extension in indexExtensions: # if this file can be indexed
				indexPath = "{0}.{1}".format(filePath, indexExtensions[extension])
				if indexPath in files:
					print("Index file {0} found".format(indexPath))
					payload_obj['indexPath'] = indexPath
				else:
					print("Index file for {0} not found".format(filePath))

			url = base_url.format(fType)
			response = requests.request("POST", url, data=json.dumps(payload_obj), headers=headers)
			resp_obj = json.loads(response.text)
			
			if not 'payload' in resp_obj:
				print("Error: " + response.text)
				sys.exit(1)

			payload = resp_obj['payload']
			print("Registered file '{0}' with ID '{1}'".format(payload['name'], payload['id']))
			projectItems.append({"bioDataItemId":payload['bioDataItemId']})

projectObj = {
  "name": args.projectName,
  "items": projectItems
}

print(projectObj)
response = requests.request("POST", projectUrl, data=json.dumps(projectObj), headers=headers)
resp_obj = json.loads(response.text)			
if not 'payload' in resp_obj:
	print("Error: " + response.text)
	sys.exit(1)

payload = resp_obj['payload']
print("Created projcet '{0}' with ID '{1}'".format(payload['name'], payload['id']))

