import requests
import json
import argparse
import sys
import os.path
import urlparse
import urllib

base_url = "http://localhost:8080/catgenome/restapi/{0}/register"
file_types = {
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
	'maf.gz': "maf"
}

def checkBamIndex(fileType, args):
	if fileType == 'bam':
		if args.index == None or not os.path.isfile(args.index):
			print("Error: Index file is required for BAM files")
			sys.exit(1)
		
		indexExtension = os.path.splitext(args.index)[1][1:]	
		if indexExtension != 'bai':
			print("Unsupported BAM index extension: reqired BAI file, found {0}".format(indexExtension))
			sys.exit(1)

def determinFileType(args):
	if not os.path.isfile(args.path):
		print("Error: File " + args.path + " does not exist")
		sys.exit(1)

	if args.index != None and not os.path.isfile(args.index):
		print("Error: Index file " + args.index + " does not exist")
		sys.exit(1)

	extension = os.path.splitext(args.path)[1][1:]
	if extension == 'gz':
		path = args.path[:-3]
		print(path)
		extension = os.path.splitext(path)[1][1:]

	if not extension in file_types:
		print("Error: File extension '{0}' is not supported".format(extension))
		sys.exit(1)

	return extension

def determinUrlType(args):
	path = urlparse.urlparse(args.path).path

	extension = os.path.splitext(path)[1][1:]
	if extension == 'gz':
		p = path[:-3]
		extension = os.path.splitext(p)[1][1:]

	if not extension in file_types:
		print("Error: File extension '{0}' is not supported".format(extension))
		sys.exit(1)

	return extension

def checkBamUrlIndex(fileType, args):
	if fileType == 'bam':
		if args.index == None:
			print("Error: Index file is required for BAM files")
			sys.exit(1)
		
		path = urlparse.urlparse(args.index).path
		indexExtension = os.path.splitext(path)[1][1:]	
		if indexExtension != 'bai':
			print("Error: Unsupported BAM index extension: reqired BAI file, found {0}".format(indexExtension))
			sys.exit(1)

def main():
	parser = argparse.ArgumentParser(description='Registers reference in NGB')
	parser.add_argument('path', metavar='PATH', help='Path to file')
	parser.add_argument('referenceId', metavar='REFERENCE_ID',  type=int, help='ID of the reference, for wich file is being registered')
	parser.add_argument('--name', metavar='NAME', help='Desired name for file in the system')
	parser.add_argument('--index', metavar='INDEX_PATH', help='Path to index file')
	parser.add_argument('--type', metavar='TYPE', help='Type of a registered resource: FILE, URL')
	parser.add_argument('--indexType', metavar='INDEX_TYPE', help='Type of a registered index resource: FILE, URL')
	args = parser.parse_args()

	#print(args)

	fileType = None
	if args.type == None or args.type.lower() == 'file':
		extension = determinFileType(args)
		fileType = file_types[extension];
		checkBamIndex(fileType, args)
	elif args.type.lower() == 'url':
		extension = determinUrlType(args)
		fileType = file_types[extension]
		checkBamUrlIndex(fileType, args)


	url = base_url.format(fileType)

	payload_obj = {
		'path': args.path,
		'referenceId': args.referenceId
		}

	if args.name != None:
		payload_obj['name'] = args.name
		print("Registering file '{0}'' with name '{1}', please wait...".format(args.path, args.name))
	else:
		print("Registering file '{0}', please wait...".format(args.path))

	if args.index != None:
		payload_obj['indexPath'] = args.index

	if args.type != None:
		payload_obj['type'] = args.type.upper()
		if (args.indexType == None):
			payload_obj['indexType'] = args.type.upper()
		else:
			payload_obj['indexType'] = args.indexType.upper()

	headers = {
	    'content-type': "application/json",
	    'cache-control': "no-cache"
	    }

	response = requests.request("POST", url, data=json.dumps(payload_obj), headers=headers)
	resp_obj = json.loads(response.text)
	if not 'payload' in resp_obj:
		print("Error: " + response.text)
		sys.exit(1)

	payload = resp_obj['payload']
	print("Registered file '{0}' with ID '{1}'".format(payload['name'], payload['id']))

if __name__ == "__main__":
    main()