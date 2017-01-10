import requests
import json
import argparse
import sys
import os.path

parser = argparse.ArgumentParser(description='Registers reference in NGB')
parser.add_argument('name', metavar='NAME', help='Desired name for reference in the system')
parser.add_argument('fasta', metavar='FASTA_PATH', help='Path to fasta file')
args = parser.parse_args()

if not os.path.isfile(args.fasta):
	print("File " + args.fasta + " does no exist")
	sys.exit(1)

url = "http://localhost:8080/catgenome/oauth/token"

payload = "client_id=restapp&client_secret=restapp&username=default_admin&password=admin&grant_type=password"
headers = {
    'cache-control': "no-cache",
    'content-type': "application/x-www-form-urlencoded"
    }

response = requests.request("POST", url, data=payload, headers=headers)
auth_obj = json.loads(response.text)

if not 'access_token' in auth_obj:
  print 'No access token found: ' + response.text
  sys.exit(1);

url = "http://localhost:8080/catgenome/restapi/secure/reference/register/fasta"

payload_obj = {
	'path': args.fasta,
	'name': args.name
	}

headers = {
    'authorization': "Bearer " + auth_obj['access_token'],
    'content-type': "application/json",
    'cache-control': "no-cache"
    }

print("Registering reference '" + args.name + "' from file '" + args.fasta + "', please wait...")
response = requests.request("POST", url, data=json.dumps(payload_obj), headers=headers)
reference_resp = json.loads(response.text)['payload']

print("Registered reference '" + reference_resp['name'] + "' with ID " + str(reference_resp['id']));