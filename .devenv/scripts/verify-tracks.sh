#!/usr/bin/env bash
#
# Load a track of every type NGB can read through a running server, and print the data that came
# back.
#
# Written for migration Phase 7 (htsjdk 2.2.4 -> 5.0.0). The unit suite reads the same fixtures,
# but through the managers with a Spring test context; it never boots the server, and a green
# suite says nothing about whether an actual instance still parses a BAM. This does the second
# pass: register each fixture over REST, ask for a window, and show a real record from it.
#
#   ./prepare-track-fixtures.sh                            # stages the fixtures into /ngs/tracks
#   ./verify-tracks.sh                                     # http://localhost:8080/catgenome
#   ./verify-tracks.sh http://localhost:8090/catgenome     # the PostgreSQL instance
#
# It expects the two references .devenv/fixtures/README.md registers - 1 test_ref (A3.fa,
# chromosome A1) and 3 dm6 (dm606.X.fa, chromosome X) - and discovers their chromosome ids rather
# than assuming them. Everything it registers is named p7v_*; leftovers under those names are
# deleted first, so the script can be re-run, and what it registers is left in place afterwards
# so the tracks can also be looked at in the UI.
#
# Two things are checked by comparison rather than by eye, because that is where a parser
# regression would hide:
#
#   * the CRAM and the BAM it was made from must return the identical first read, including the
#     differentBase list - which NGB computes against its own registered reference, so agreement
#     means reference-compressed decoding still works;
#   * the same VCF read locally, over plain HTTP and over a URL whose HEAD is answered with 403
#     must return the identical first variation.
#
# The remote probes need docker: they start a container serving .devenv/data/ngs over HTTP with
# two aliases on the ngb-dev network, one of which looks like S3 to NGB and refuses HEAD - see
# fake-remote-files.py. Without docker they are skipped, and the script says so. The S3-looking
# alias is what exercises the hostname clause of EnhancedUrlHelper.headMayBeRefused, which Phase 9
# kept alongside the signature clause; `make verify-cloud` covers the signature clause.
#
# MAF has no REST surface at all: MafController and MafSecurityService were deleted in 562b6a6d
# (Dec 2018), leaving MafManager reachable only from Java. It cannot be verified here, and
# MafManagerTest.testRegisterMaf is the whole of its coverage.

set -uo pipefail

DEVENV="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE="${1:-http://localhost:8080/catgenome}/restapi"
STATE="$(mktemp -d)"
JSON=(-H 'Content-Type: application/json')
TRACKS=/ngs/tracks
REMOTE=p7-remote

FAILED=0
SKIPPED=0

trap 'rm -rf "$STATE"' EXIT

step() { printf '\n%s\n' "$1"; }

note() { printf '  %-34s %s\n' "" "$1"; }

# probe <slug> <label> <python expr over `p`> <curl args...>
# Stores the extracted text in $STATE/<slug> so later steps can compare it.
probe() {
    local slug="$1" label="$2"
    export NGB_EXTRACT="$3"
    shift 3
    local out rc
    out=$(curl -sS --max-time 600 "$@" 2>&1 | python3 -c '
import json, os, sys
raw = sys.stdin.read()
# /bam/track/get streams several JSON documents one after another
docs, decoder, i = [], json.JSONDecoder(), 0
while i < len(raw):
    while i < len(raw) and raw[i] in " \r\n\t":
        i += 1
    if i >= len(raw):
        break
    try:
        doc, i = decoder.raw_decode(raw, i)
    except ValueError:
        print("NOT JSON: " + raw[:200].replace("\n", " "))
        sys.exit(3)
    docs.append(doc)
if not docs:
    print("EMPTY RESPONSE")
    sys.exit(3)
for d in docs:
    if d.get("status") != "OK":
        detail = d.get("message") or d.get("detail") or d.get("title")
        print("STATUS %s: %s" % (d.get("status"), str(detail)[:200]))
        sys.exit(3)
p = docs[0].get("payload")
for d in docs:                        # the first document with any blocks in it
    body = d.get("payload")
    if isinstance(body, dict) and body.get("blocks"):
        p = body
        break
try:
    print(eval(os.environ["NGB_EXTRACT"],
               {"p": p, "len": len, "str": str, "sorted": sorted, "max": max, "json": json}))
except Exception as e:
    print("EMPTY/UNEXPECTED (%s: %s) payload=%s" % (type(e).__name__, e, repr(p)[:160]))
    sys.exit(3)
')
    rc=$?
    printf '%s' "$out" > "$STATE/$slug"
    printf '  %-34s %s\n' "$label" "$(printf '%s' "$out" | cut -c1-150)"
    [ "$rc" -ne 0 ] && FAILED=$((FAILED + 1))
    return 0
}

# register <slug> <label> <endpoint> <json body> - stores the new file id in $STATE/<slug>.id
register() {
    local slug="$1" label="$2" endpoint="$3" body="$4"
    probe "$slug.id" "$label" 'str(p["id"])' -X POST "${JSON[@]}" -d "$body" "$BASE$endpoint"
}

id_of() { cat "$STATE/$1.id"; }

# track <slug> <label> <endpoint> <file id> <chromosome id> <start> <end> <scale> <extract>
track() {
    local slug="$1" label="$2" endpoint="$3" file="$4" chr="$5" from="$6" to="$7" scale="$8"
    probe "$slug" "$label" "$9" -X POST "${JSON[@]}" \
        -d "{\"id\":$file,\"chromosomeId\":$chr,\"startIndex\":$from,\"endIndex\":$to,\"scaleFactor\":$scale}" \
        "$BASE$endpoint"
}

same() {
    local label="$1" a="$2" b="$3"
    if [ -s "$STATE/$a" ] && cmp -s "$STATE/$a" "$STATE/$b"; then
        printf '  %-34s %s\n' "$label" "identical"
    else
        printf '  %-34s %s\n' "$label" "DIFFERENT"
        FAILED=$((FAILED + 1))
    fi
}

# ---------------------------------------------------------------- prerequisites

echo "NGB track parsing: $BASE"

step "references"
probe refs "test_ref / dm6" \
    'str(sorted((r["id"], r["name"]) for r in p))' "$BASE/reference/loadAll"
REF_A3=$(curl -sS "$BASE/reference/loadAll" | python3 -c '
import json, sys
print(next(r["id"] for r in json.load(sys.stdin)["payload"] if r["name"] == "test_ref"))')
REF_DM6=$(curl -sS "$BASE/reference/loadAll" | python3 -c '
import json, sys
print(next(r["id"] for r in json.load(sys.stdin)["payload"] if r["name"] == "dm6"))')
CHR_A1=$(curl -sS "$BASE/reference/$REF_A3/loadChromosomes" | python3 -c '
import json, sys
print(json.load(sys.stdin)["payload"][0]["id"])')
CHR_X=$(curl -sS "$BASE/reference/$REF_DM6/loadChromosomes" | python3 -c '
import json, sys
print(json.load(sys.stdin)["payload"][0]["id"])')
note "reference $REF_A3 chromosome $CHR_A1 (A1), reference $REF_DM6 chromosome $CHR_X (X)"

step "clearing anything left by a previous run"
for name in p7v_bed p7v_bed_tabix p7v_gtf p7v_gtf_tabix p7v_genepred p7v_vcf \
            p7v_bedgraph p7v_bigwig p7v_seg p7v_bam p7v_cram; do
    ids=$(curl -sS "$BASE/dataitem/search?name=$name&strict=true" | python3 -c '
import json, sys
print(" ".join(str(i["bioDataItemId"]) for i in json.load(sys.stdin).get("payload") or []))')
    for id in $ids; do
        curl -sS -X DELETE "$BASE/dataitem/delete?id=$id" > /dev/null
        printf '  %-34s %s\n' "$name" "removed item $id"
    done
done

# ---------------------------------------------------------------- local fixtures

step "BED  (tribble, plain and bgzip+tabix)"
register bed "register genes_sorted.bed" /bed/register \
    "{\"path\":\"$TRACKS/genes_sorted.bed\",\"referenceId\":$REF_A3,\"name\":\"p7v_bed\"}"
register bed_tabix "register genes_sorted.bed.gz" /bed/register \
    "{\"path\":\"$TRACKS/genes_sorted.bed.gz\",\"referenceId\":$REF_A3,\"name\":\"p7v_bed_tabix\"}"
track bed_track "plain" /bed/track/get "$(id_of bed)" "$CHR_A1" 35000 56400 1 \
    'str(len(p["blocks"])) + " block(s), first " + str(p["blocks"][0]["startIndex"]) + "-" + str(p["blocks"][0]["endIndex"]) + " " + str(p["blocks"][0].get("name"))'
track bed_tabix_track "bgzip+tabix" /bed/track/get "$(id_of bed_tabix)" "$CHR_A1" 35000 56400 1 \
    'str(len(p["blocks"])) + " block(s), first " + str(p["blocks"][0]["startIndex"]) + "-" + str(p["blocks"][0]["endIndex"]) + " " + str(p["blocks"][0].get("name"))'

step "GFF/GTF  (tribble, plain and bgzip+tabix)"
register gtf "register genes_sorted.gtf" /gene/register \
    "{\"path\":\"$TRACKS/genes_sorted.gtf\",\"referenceId\":$REF_A3,\"name\":\"p7v_gtf\"}"
register gtf_tabix "register genes_sorted.gtf.gz" /gene/register \
    "{\"path\":\"$TRACKS/genes_sorted.gtf.gz\",\"referenceId\":$REF_A3,\"name\":\"p7v_gtf_tabix\"}"
track gtf_track "plain" "/gene/$REF_A3/track/get" "$(id_of gtf)" "$CHR_A1" 35000 56400 1 \
    'str(len(p["blocks"])) + " gene(s), first " + str(p["blocks"][0]["groupId"]) + " " + str(p["blocks"][0]["feature"]) + " " + str(p["blocks"][0]["startIndex"]) + "-" + str(p["blocks"][0]["endIndex"]) + ", " + str(len(p["blocks"][0].get("items") or [])) + " transcript(s)"'
track gtf_tabix_track "bgzip+tabix" "/gene/$REF_A3/track/get" "$(id_of gtf_tabix)" "$CHR_A1" 35000 56400 1 \
    'str(len(p["blocks"])) + " gene(s), first " + str(p["blocks"][0]["groupId"]) + " " + str(p["blocks"][0]["feature"]) + " " + str(p["blocks"][0]["startIndex"]) + "-" + str(p["blocks"][0]["endIndex"]) + ", " + str(len(p["blocks"][0].get("items") or [])) + " transcript(s)"'

step "GenePred  (converted to GFF on registration)"
register genepred "register cat.genepred" /gene/register \
    "{\"path\":\"$TRACKS/p7_cat_chrA1.genepred\",\"referenceId\":$REF_A3,\"name\":\"p7v_genepred\"}"
track genepred_track "track" "/gene/$REF_A3/track/get" "$(id_of genepred)" "$CHR_A1" 3000 20000 1 \
    'str(len(p["blocks"])) + " gene(s), first " + str(p["blocks"][0]["groupId"]) + " " + str(p["blocks"][0]["attributes"].get("gene_name")) + " " + str(p["blocks"][0]["startIndex"]) + "-" + str(p["blocks"][0]["endIndex"])'

step "VCF  (bgzip+tabix)"
register vcf "register CantonS.vcf.gz" /vcf/register \
    "{\"path\":\"$TRACKS/CantonS.vcf.gz\",\"referenceId\":$REF_DM6,\"name\":\"p7v_vcf\"}"
track vcf_local "track" /vcf/track/get "$(id_of vcf)" "$CHR_X" 12585000 12600000 1 \
    'str(len(p["blocks"])) + " variation(s), first " + json.dumps(p["blocks"][0], sort_keys=True)'

step "WIG  (BedGraph and BigWig)"
register bedgraph "register bedGraph.bdg" /wig/register \
    "{\"path\":\"$TRACKS/bedGraph.bdg\",\"referenceId\":$REF_DM6,\"name\":\"p7v_bedgraph\"}"
register bigwig "register agnX1...bw" /wig/register \
    "{\"path\":\"$TRACKS/agnX1.09-28.trim.dm606.realign.bw\",\"referenceId\":$REF_DM6,\"name\":\"p7v_bigwig\"}"
track bedgraph_track "bedgraph" /wig/track/get "$(id_of bedgraph)" "$CHR_X" 564000 565000 1 \
    'str(len(p["blocks"])) + " block(s), values up to " + str(max(b["value"] for b in p["blocks"]))'
track bigwig_track "bigwig" /wig/track/get "$(id_of bigwig)" "$CHR_X" 12585000 12590000 1 \
    'str(len(p["blocks"])) + " block(s), values up to " + str(max(b["value"] for b in p["blocks"]))'

step "SEG"
register seg "register test_seg.seg" /seg/register \
    "{\"path\":\"$TRACKS/p7_seg_X.seg\",\"referenceId\":$REF_DM6,\"name\":\"p7v_seg\"}"
track seg_track "track" /seg/track/get "$(id_of seg)" "$CHR_X" 1 16100000 0.001 \
    'str(sorted(p["tracks"])) + ", " + str(sum(len(v) for v in p["tracks"].values())) + " segment(s)"'

step "BAM and CRAM  (the CRAM is the same reads, reference-compressed)"
register bam "register agnX1...bam" /bam/register \
    "{\"path\":\"$TRACKS/agnX1.09-28.trim.dm606.realign.bam\",\"indexPath\":\"$TRACKS/agnX1.09-28.trim.dm606.realign.bam.bai\",\"referenceId\":$REF_DM6,\"name\":\"p7v_bam\"}"
register cram "register p7_agnX1.cram" /bam/register \
    "{\"path\":\"$TRACKS/p7_agnX1.cram\",\"indexPath\":\"$TRACKS/p7_agnX1.cram.crai\",\"referenceId\":$REF_DM6,\"name\":\"p7v_cram\"}"
BAM_OPTION='"option":{"trackDirection":"LEFT","mode":"FULL","showClipping":false,"showSpliceJunction":false}'
probe bam_track "bam" \
    'str(len(p["blocks"])) + " read(s), first " + json.dumps(p["blocks"][0], sort_keys=True)' \
    -X POST "${JSON[@]}" -d "{\"id\":$(id_of bam),\"chromosomeId\":$CHR_X,\"startIndex\":12585000,\"endIndex\":12585100,\"scaleFactor\":1,$BAM_OPTION}" \
    "$BASE/bam/track/get"
probe cram_track "cram" \
    'str(len(p["blocks"])) + " read(s), first " + json.dumps(p["blocks"][0], sort_keys=True)' \
    -X POST "${JSON[@]}" -d "{\"id\":$(id_of cram),\"chromosomeId\":$CHR_X,\"startIndex\":12585000,\"endIndex\":12585100,\"scaleFactor\":1,$BAM_OPTION}" \
    "$BASE/bam/track/get"
same "cram == bam" bam_track cram_track

step "MAF"
note "no REST surface since 562b6a6d - see the header; covered by MafManagerTest only"
SKIPPED=$((SKIPPED + 1))

# ---------------------------------------------------------------- remote fixtures

step "remote tracks  (fileUrl/indexUrl, nothing registered)"
if ! command -v docker > /dev/null 2>&1; then
    note "skipped: docker is not on PATH, cannot serve the fixtures over HTTP"
    SKIPPED=$((SKIPPED + 1))
else
    docker rm -f "$REMOTE" > /dev/null 2>&1
    docker run -d --name "$REMOTE" --network ngb-dev \
        --network-alias p7-files --network-alias s3.amazonaws.com \
        -v "$DEVENV/data/ngs:/srv:ro" -v "$DEVENV/scripts:/scripts:ro" \
        --entrypoint python3 ngb-dev-toolbox:latest \
        /scripts/fake-remote-files.py 8000 /srv > /dev/null
    sleep 2
    note "$REMOTE serving /ngs as p7-files:8000 and s3.amazonaws.com:8000"

    HTTP_VCF=http://p7-files:8000/tracks/CantonS.vcf.gz
    S3_VCF=http://s3.amazonaws.com:8000/tracks/CantonS.vcf.gz
    S3_BAM=http://s3.amazonaws.com:8000/tracks/agnX1.09-28.trim.dm606.realign.bam

    probe vcf_http "vcf over http (HEAD 200)" \
        'str(len(p["blocks"])) + " variation(s), first " + json.dumps(p["blocks"][0], sort_keys=True)' \
        -X POST "${JSON[@]}" -d "{\"chromosomeId\":$CHR_X,\"startIndex\":12585000,\"endIndex\":12600000,\"scaleFactor\":1}" \
        "$BASE/vcf/track/get?fileUrl=$HTTP_VCF&indexUrl=$HTTP_VCF.tbi"
    probe vcf_s3 "vcf over signed s3 (HEAD 403)" \
        'str(len(p["blocks"])) + " variation(s), first " + json.dumps(p["blocks"][0], sort_keys=True)' \
        -X POST "${JSON[@]}" -d "{\"chromosomeId\":$CHR_X,\"startIndex\":12585000,\"endIndex\":12600000,\"scaleFactor\":1}" \
        "$BASE/vcf/track/get?fileUrl=$S3_VCF&indexUrl=$S3_VCF.tbi"
    probe bam_s3 "bam over signed s3 (HEAD 403)" \
        'str(len(p["blocks"])) + " read(s), first " + json.dumps(p["blocks"][0], sort_keys=True)' \
        -X POST "${JSON[@]}" -d "{\"chromosomeId\":$CHR_X,\"startIndex\":12585000,\"endIndex\":12585100,\"scaleFactor\":1,$BAM_OPTION}" \
        "$BASE/bam/track/get?fileUrl=$S3_BAM&indexUrl=$S3_BAM.bai"
    same "http vcf == local vcf" vcf_local vcf_http
    same "signed s3 vcf == local vcf" vcf_local vcf_s3
    same "signed s3 bam == local bam" bam_track bam_s3

    step "what the remote server was asked  (the 403s are the point)"
    docker logs "$REMOTE" 2>&1 | grep -oE '(HEAD|GET) [^ ]+( bytes=[0-9-]+)? -> [0-9]+' \
        | sed -e 's#/tracks/##' -e 's/^/  /' | sort | uniq -c | sort -rn | head -20
    docker rm -f "$REMOTE" > /dev/null
fi

step "result"
if [ "$FAILED" -eq 0 ]; then
    echo "OK: every track type answered with data ($SKIPPED skipped)"
else
    echo "FAILED: $FAILED probe(s), $SKIPPED skipped"
fi
exit "$FAILED"
