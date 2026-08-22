#!/usr/bin/env bash
#
# Exercise every Lucene-backed read path in NGB against a running server.
#
# The 18 probes below cover every Lucene read path there is, and the recorded outputs in
# fixtures/pre-migration/lucene6/ are what a Lucene bump has to keep reproducing: run this before
# and after, and diff. Every request reads a Lucene index; none of them touch the network.
#
#   ./verify-lucene.sh                                  # http://localhost:8080/catgenome
#   ./verify-lucene.sh http://localhost:8090/catgenome  # the PostgreSQL instance
#
# The data it expects is what .devenv/fixtures/README.md's lucene6 recipe registers:
#   reference 1 test_ref (A3.fa)          gene file 1 demo_genes (genes_sorted.gtf.gz)
#   reference 3 dm6      (dm606.X.fa)     vcf file  1 demo_vcf   (Felis_catus.vcf.gz)
#   dataset 2 demo_ds, dataset 3 demo_ds_dm6, bam file 2 demo_bam, coverage step 100
#   target 2 lucene6-fixture-target       pathway 1 Glycolysis
# A missing prerequisite shows up as an empty result rather than an error, so read the
# counts, not just the exit code.

set -uo pipefail

BASE="${1:-http://localhost:8080/catgenome}/restapi"
FAILED=0

# probe <label> <python-expr over `p`, the response payload> <curl args...>
probe() {
    local label="$1"
    export NGB_EXTRACT="$2"
    shift 2
    local out
    out=$(curl -sS --max-time 600 "$@" 2>&1 | python3 -c '
import json, os, sys
raw = sys.stdin.read()
try:
    d = json.loads(raw)
except ValueError:
    print("NOT JSON: " + raw[:200].replace("\n", " ")); sys.exit(3)
if d.get("status") != "OK":
    detail = d.get("message") or d.get("detail") or d.get("title")
    print("STATUS %s: %s" % (d.get("status"), str(detail)[:200])); sys.exit(3)
p = d.get("payload")
try:
    print(eval(os.environ["NGB_EXTRACT"], {"p": p, "len": len, "str": str, "sorted": sorted}))
except Exception as e:
    print("EMPTY/UNEXPECTED (%s: %s) payload=%s" % (type(e).__name__, e, repr(p)[:160])); sys.exit(3)
')
    local rc=$?
    printf '  %-34s %s\n' "$label" "$out"
    [ "$rc" -ne 0 ] && FAILED=$((FAILED + 1))
    return 0
}

JSON=(-H 'Content-Type: application/json')

echo "NGB Lucene read paths: $BASE"
echo
echo "server"
probe "version" 'p' "$BASE/version"

echo
echo "per-file feature indexes  <files.base.directory.path>/<id>/<TYPE>/<fileId>/index.luc"
probe "variations, vcf 1 (dataset 2)" 'str(len(p["entries"])) + " entries, first " + p["entries"][0]["featureId"]' \
    -X POST "${JSON[@]}" -d '{"vcfFileIdsByProject":{"2":[1]},"page":1,"pageSize":5}' "$BASE/filter"
probe "variations, vcf 2 (dataset 3)" 'str(len(p["entries"])) + " entries, first at " + str(p["entries"][0]["startIndex"])' \
    -X POST "${JSON[@]}" -d '{"vcfFileIdsByProject":{"3":[2]},"page":1,"pageSize":5}' "$BASE/filter"
probe "variation info fields, vcf 1" 'str(len(p["infoItems"])) + " info field(s), " + str(sorted(i["name"] for i in p["infoItems"])[:4])' \
    -X POST "${JSON[@]}" -d '{"value":{"2":[1]}}' "$BASE/filter/info"
probe "variation grouping by type" 'str([(g["groupName"], g["entriesCount"]) for g in p])' \
    -X POST "${JSON[@]}" -d '{"vcfFileIdsByProject":{"2":[1]}}' "$BASE/filter/group?groupBy=VARIATION_TYPE"
probe "genes, gene file 1 (dataset 2)" 'str(len(p["entries"])) + " entries, first " + p["entries"][0]["featureType"]' \
    -X POST "${JSON[@]}" -d '{"geneFileIdsByProject":{"2":[1]},"page":1,"pageSize":5}' "$BASE/reference/1/filter/gene"
probe "gene index field values" 'str(sorted(p)[:6])' \
    -X POST "${JSON[@]}" -d '{"value":{"2":[1]}}' "$BASE/reference/1/filter/gene/values?fieldName=source"

echo
echo "global indexes"
probe "taxonomy by id" 'p["scientificName"] + " / " + str(p["commonName"])' \
    "$BASE/taxonomy/2"
probe "taxonomy by term" 'str([(o["taxId"], o["scientificName"]) for o in p])' \
    "$BASE/taxonomies/azor"
probe "homologene (+ ncbi gene.ids)" 'str(len(p["34"])) + " group(s), " + str(len(p["34"][0]["genes"])) + " genes in the first"' \
    "$BASE/homologene/search?geneIds=34"
probe "pathway" 'str(p["totalCount"]) + " pathway(s), first " + p["items"][0]["name"]' \
    -X POST "${JSON[@]}" -d '{"pagingInfo":{"pageSize":5,"pageNum":1}}' "$BASE/pathways"
probe "opentargets disease assoc" 'str(p["totalCount"]) + " assoc, first " + p["items"][0]["disease"]["id"]' \
    -X POST "${JSON[@]}" -d '{"geneIds":["ENSG00000006128"],"page":1,"pageSize":3}' "$BASE/target/opentargets/diseases"
probe "opentargets drug assoc" 'str(p["totalCount"]) + " assoc"' \
    -X POST "${JSON[@]}" -d '{"geneIds":["ENSG00000167325"],"page":1,"pageSize":3}' "$BASE/target/opentargets/drugs"
probe "opentargets disease ontology" 'str(len(p)) + " root disease(s)"' \
    "$BASE/target/opentargets/diseases/ontology"
probe "dgidb drug assoc" 'str(p["totalCount"]) + " assoc, first " + p["items"][0]["name"]' \
    -X POST "${JSON[@]}" -d '{"geneIds":["1022"],"page":1,"pageSize":3}' "$BASE/target/dgidb/drugs"
probe "pharmgkb drug assoc" 'str(p["totalCount"]) + " assoc, first " + p["items"][0]["id"]' \
    -X POST "${JSON[@]}" -d '{"geneIds":["ENSG00000085563"],"page":1,"pageSize":3}' "$BASE/target/pharmgkb/drugs"
probe "bam coverage" 'str(p["totalCount"]) + " intervals, first " + str(p["items"][0]["coverage"])' \
    -X POST "${JSON[@]}" -d '{"coverageId":1,"pagingInfo":{"pageSize":3,"pageNum":1}}' "$BASE/bam/coverage/search"

echo
echo "target identification  (reads opentargets, dgidb, pharmgkb, taxonomy and ncbi at once)"
probe "identification, target 2" 'str(p)' \
    -X POST "${JSON[@]}" -d '{"targetId":2,"genesOfInterest":["ENSG00000006128"],"translationalGenes":[]}' \
    "$BASE/target/identification"

echo
if [ "$FAILED" -eq 0 ]; then
    echo "OK: every Lucene read path answered"
else
    echo "FAILED: $FAILED probe(s)"
fi
exit "$FAILED"
