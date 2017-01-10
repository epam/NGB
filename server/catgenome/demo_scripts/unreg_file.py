import requests
import json
import argparse
import sys

parser = argparse.ArgumentParser(description='Deletes files from NGB')
parser.add_argument('format', metavar='FORMAT', help='Format of files to delete: VCF, GENE, BAM, SEG, WIG, BED, MAF, REFERENCE')
parser.add_argument('ids', metavar='ID', type=int, nargs='+', help='an ID of file to delete')
args = parser.parse_args()

print("Delete {0} files with ids: {1}".format(args.format.upper(), ",".join(map(str, args.ids))))

file_types = {
    "vcf": "vcfFileId",
    "gene": "geneFileId",
    "bam": "bamFileId",
    "seg": "segFileId",
    "wig": "wigFileId",
    "bed" :"bedFileId",
    #"vg": "vgFileId",
    "maf": "vgFileId",
    "reference": "referenceId"
}

if not args.format.lower() in file_types:
    print("Unsupported file format '{0}'".format(args.format))
    sys.exit(1)

base_url = "http://localhost:8080/catgenome/restapi/secure/{0}/register?{1}={2}"
reference_url = "http://localhost:8080/catgenome/restapi/secure/reference/register/fasta?referenceId={0}"
authUrl = "http://localhost:8080/catgenome/oauth/token"

payload = "client_id=restapp&client_secret=restapp&username=default_admin&password=admin&grant_type=password"
headers = {
    'cache-control': "no-cache",
    'content-type': "application/x-www-form-urlencoded"
}

response = requests.request("POST", authUrl, data=payload, headers=headers)
authObj = json.loads(response.text)

if not 'access_token' in authObj:
    print 'No access token found: ' + response.text
    sys.exit(1);

headers = {
    'authorization': "Bearer " + authObj['access_token'],
    'content-type': "application/json",
    'cache-control': "no-cache"
    }

for fileId in args.ids:    
    url = ""
    if args.format.lower() != 'reference':
        url = base_url.format(args.format.lower(), file_types[args.format.lower()], fileId)
    else:
        url = reference_url.format(fileId)

    response = requests.request("DELETE", url, headers=headers)
    responseObj = json.loads(response.text)
    if responseObj['status'] == 'ERROR':
        print("{0}: {1}".format(responseObj['status'], responseObj['message']))
        sys.exit(1)
    else:
        print(responseObj['message'])
