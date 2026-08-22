#!/usr/bin/env bash
#
# Read tracks out of an S3-compatible object store through a running server, and compare what
# comes back with the same file read from disk.
#
# The unit suite barely touches S3 - S3ManagerTest and S3ObjectChunkInputStreamTest are all of it,
# and both stub the client away - so a green suite says nothing about whether the SDK client, the
# pre-signer or the URI parser actually work. This does that pass against MinIO, which needs no
# AWS account:
#
#   cd .devenv
#   make up-cloud            # MinIO + fixtures + NGB restarted with s3:// and sws:// pointed at it
#   make verify-cloud        # this script
#
# It expects the dm6 reference registered (see fixtures/README.md) and the four objects
# minio-init uploads. Everything it registers is named p8c_*; leftovers under those names are
# deleted first, so it can be re-run.
#
# The five paths it covers, which are five different pieces of code:
#
#   1. a registered s3://  file  - S3Client.headObject + ranged getObject through S3SeekableStream,
#                                  addressed virtual-host style (aws.endpointUrlS3)
#   2. a registered sws:// file  - the same, path-style, through the swift-stack client and its
#                                  own credentials profile (swift.stack.*)
#   3. /dataitem/{id}/downloadUrl - S3Presigner. The URL is fetched from inside the docker
#                                  network, and HEAD on it is expected to be refused with 403 -
#                                  that is the condition IOHelper.getContentLength exists for.
#   4. a track read by fileUrl=  - not registered, so Utils.processUrl pre-signs the s3:// URI and
#                                  the read happens over https through UrlSeekableStream. NGB is
#                                  pointed at MinIO under the name `minio`, so this is also what
#                                  proves EnhancedUrlHelper keys its 403 tolerance to the
#                                  pre-signature and not to an amazonaws.com hostname.
#   5. a registered sws:// VCF   - a *bgzip'd* file, which reads the object right up to its last
#                                  byte and then asks for one more. That read used to return 0xff
#                                  instead of an end of stream, and BGZF rejected the trailing
#                                  block with "invalid uncompressedLength: -1".

set -uo pipefail

DEVENV="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE="${1:-http://localhost:8080/catgenome}/restapi"
STATE="$(mktemp -d)"
JSON=(-H 'Content-Type: application/json')
TRACKS=/ngs/tracks
BUCKET="${MINIO_BUCKET:-ngb-cloud}"
BAM=agnX1.09-28.trim.dm606.realign.bam
VCF=CantonS.vcf.gz
BAM_OPTION='"option":{"trackDirection":"LEFT","mode":"FULL","showClipping":false,"showSpliceJunction":false}'

FAILED=0
SKIPPED=0

trap 'rm -rf "$STATE"' EXIT

step() { printf '\n%s\n' "$1"; }

note() { printf '  %-34s %s\n' "" "$1"; }

# probe <slug> <label> <python expr over `p`> <curl args...>
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
    printf '  %-34s %s\n' "$label" "$(printf '%s' "$out" | cut -c1-140)"
    [ "$rc" -ne 0 ] && FAILED=$((FAILED + 1))
    return 0
}

# register <slug> <label> <endpoint> <json body>
register() {
    probe "$1.id" "$2" 'str(p["id"])' -X POST "${JSON[@]}" -d "$4" "$BASE$3"
}

id_of() { cat "$STATE/$1.id"; }

# The id in a register response is the *format-specific* id (bam_file.bam_id), while /dataitem
# addresses things by bio_data_item_id. They are drawn from the same sequence but are not equal,
# and /dataitem/{id}/downloadUrl on the wrong one answers about some unrelated file rather than
# failing, so look the item up by name instead of reusing the register id.
bio_id_of() {
    curl -sS "$BASE/dataitem/search?name=$1&strict=true" | python3 -c '
import json, sys
items = json.load(sys.stdin).get("payload") or []
print(items[0]["bioDataItemId"] if items else "")'
}

first_read() {
    echo 'str(len(p["blocks"])) + " read(s), first " + json.dumps(p["blocks"][0], sort_keys=True)'
}

first_variation() {
    echo 'str(len(p["blocks"])) + " variation(s), first " + json.dumps(p["blocks"][0], sort_keys=True)'
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

# Runs curl inside the docker network, because the pre-signed URL names the store by a hostname
# that only resolves there.
in_network() {
    docker run --rm --network ngb-dev --entrypoint bash ngb-dev-toolbox:latest -c "$1"
}

# ---------------------------------------------------------------- prerequisites

echo "NGB cloud track reading: $BASE  (bucket $BUCKET)"

step "reference"
REF_DM6=$(curl -sS "$BASE/reference/loadAll" | python3 -c '
import json, sys
print(next((r["id"] for r in json.load(sys.stdin)["payload"] if r["name"] == "dm6"), ""))')
if [ -z "$REF_DM6" ]; then
    echo "  dm6 is not registered - see .devenv/fixtures/README.md" >&2
    exit 1
fi
CHR_X=$(curl -sS "$BASE/reference/$REF_DM6/loadChromosomes" | python3 -c '
import json, sys
print(json.load(sys.stdin)["payload"][0]["id"])')
note "reference $REF_DM6 chromosome $CHR_X (X)"

step "clearing anything left by a previous run"
for name in p8c_bam_local p8c_vcf_local p8c_bam_s3 p8c_bam_sws p8c_vcf_sws; do
    ids=$(curl -sS "$BASE/dataitem/search?name=$name&strict=true" | python3 -c '
import json, sys
print(" ".join(str(i["bioDataItemId"]) for i in json.load(sys.stdin).get("payload") or []))')
    for id in $ids; do
        curl -sS -X DELETE "$BASE/dataitem/delete?id=$id" > /dev/null
        printf '  %-34s %s\n' "$name" "removed item $id"
    done
done

# ---------------------------------------------------------------- the baseline

step "the same files from disk, for comparison"
register bam_local "register $BAM" /bam/register \
    "{\"path\":\"$TRACKS/$BAM\",\"indexPath\":\"$TRACKS/$BAM.bai\",\"referenceId\":$REF_DM6,\"name\":\"p8c_bam_local\"}"
register vcf_local "register $VCF" /vcf/register \
    "{\"path\":\"$TRACKS/$VCF\",\"indexPath\":\"$TRACKS/$VCF.tbi\",\"referenceId\":$REF_DM6,\"name\":\"p8c_vcf_local\"}"
probe bam_disk "bam from disk" "$(first_read)" \
    -X POST "${JSON[@]}" -d "{\"id\":$(id_of bam_local),\"chromosomeId\":$CHR_X,\"startIndex\":12585000,\"endIndex\":12585100,\"scaleFactor\":1,$BAM_OPTION}" \
    "$BASE/bam/track/get"
probe vcf_disk "vcf from disk" "$(first_variation)" \
    -X POST "${JSON[@]}" -d "{\"id\":$(id_of vcf_local),\"chromosomeId\":$CHR_X,\"startIndex\":12585000,\"endIndex\":12600000,\"scaleFactor\":1}" \
    "$BASE/vcf/track/get"

# ------------------------------------------------------- registered cloud files

# `type` has to be given explicitly. BamHelper takes the resource type from the request and
# defaults it to FILE, so without this the path is opened as a local file and the register call
# fails with FileNotFoundException on "s3:/ngb-cloud/..." - one slash, because that is what
# File(String) does to a URI. Only the readers that go through Utils.createNonRegisteredFile
# guess the type from the path.
for scheme in s3 sws; do
    step "registered $scheme:// BAM"
    register "bam_$scheme" "register $scheme://$BUCKET/tracks/$BAM" /bam/register \
        "{\"path\":\"$scheme://$BUCKET/tracks/$BAM\",\"indexPath\":\"$scheme://$BUCKET/tracks/$BAM.bai\",\"type\":\"S3\",\"indexType\":\"S3\",\"referenceId\":$REF_DM6,\"name\":\"p8c_bam_$scheme\"}"
    probe "bam_${scheme}_track" "track" "$(first_read)" \
        -X POST "${JSON[@]}" -d "{\"id\":$(id_of "bam_$scheme"),\"chromosomeId\":$CHR_X,\"startIndex\":12585000,\"endIndex\":12585100,\"scaleFactor\":1,$BAM_OPTION}" \
        "$BASE/bam/track/get"
    same "registered $scheme bam == disk" bam_disk "bam_${scheme}_track"

    step "pre-signed download url for the $scheme:// BAM"
    BIO_ID=$(bio_id_of "p8c_bam_$scheme")
    URL=$(curl -sS "$BASE/dataitem/$BIO_ID/downloadUrl" | python3 -c '
import json, sys
d = json.load(sys.stdin)
p = d.get("payload") or {}
print(p.get("url", "") if p.get("type") == "S3" else "")')
    if [ -z "$URL" ]; then
        printf '  %-34s %s\n' "url" "NOT PRE-SIGNED (item $BIO_ID answered as a local file?)"
        FAILED=$((FAILED + 1))
    else
        note "$(printf '%s' "$URL" | cut -c1-110)..."
        SIZE=$(stat -f %z "$DEVENV/data/ngs/tracks/$BAM" 2>/dev/null || stat -c %s "$DEVENV/data/ngs/tracks/$BAM")
        RESULT=$(in_network "
            get=\$(curl -sS -o /dev/null -w '%{http_code}:%{size_download}' '$URL')
            head=\$(curl -sS -I -o /dev/null -w '%{http_code}' '$URL')
            echo \"\$get \$head\"")
        GET_RESULT="${RESULT%% *}"
        HEAD_CODE="${RESULT##* }"
        if [ "$GET_RESULT" = "200:$SIZE" ]; then
            printf '  %-34s %s\n' "GET it" "200, $SIZE bytes - the whole object"
        else
            printf '  %-34s %s\n' "GET it" "UNEXPECTED $GET_RESULT (wanted 200:$SIZE)"
            FAILED=$((FAILED + 1))
        fi
        if [ "$HEAD_CODE" = "403" ]; then
            printf '  %-34s %s\n' "HEAD it" "403 - signed for GET only, as on AWS"
        else
            printf '  %-34s %s\n' "HEAD it" "$HEAD_CODE - expected 403; the 403 tolerance is untested"
            FAILED=$((FAILED + 1))
        fi
    fi

    step "not registered, read by fileUrl=$scheme://  (pre-signed, HEAD 403)"
    probe "bam_${scheme}_url" "bam" "$(first_read)" \
        -X POST "${JSON[@]}" -d "{\"chromosomeId\":$CHR_X,\"startIndex\":12585000,\"endIndex\":12585100,\"scaleFactor\":1,$BAM_OPTION}" \
        "$BASE/bam/track/get?fileUrl=$scheme://$BUCKET/tracks/$BAM&indexUrl=$scheme://$BUCKET/tracks/$BAM.bai"
    probe "vcf_${scheme}_url" "vcf" "$(first_variation)" \
        -X POST "${JSON[@]}" -d "{\"chromosomeId\":$CHR_X,\"startIndex\":12585000,\"endIndex\":12600000,\"scaleFactor\":1}" \
        "$BASE/vcf/track/get?fileUrl=$scheme://$BUCKET/tracks/$VCF&indexUrl=$scheme://$BUCKET/tracks/$VCF.tbi"
    same "fileUrl $scheme bam == disk" bam_disk "bam_${scheme}_url"
    same "fileUrl $scheme vcf == disk" vcf_disk "vcf_${scheme}_url"
done

# ------------------------------------------------------- a bgzip'd feature file

# Registering a feature file reads its header, which means reading the object to its exact end.
# This is where every bgzip'd file in cloud storage used to fail - see path 5 in the header.
step "registered sws:// VCF  (bgzip, read to the last byte)"
register vcf_sws "register sws://$BUCKET/tracks/$VCF" /vcf/register \
    "{\"path\":\"sws://$BUCKET/tracks/$VCF\",\"indexPath\":\"sws://$BUCKET/tracks/$VCF.tbi\",\"type\":\"S3\",\"indexType\":\"S3\",\"referenceId\":$REF_DM6,\"name\":\"p8c_vcf_sws\"}"
probe vcf_sws_track "track" "$(first_variation)" \
    -X POST "${JSON[@]}" -d "{\"id\":$(id_of vcf_sws),\"chromosomeId\":$CHR_X,\"startIndex\":12585000,\"endIndex\":12600000,\"scaleFactor\":1}" \
    "$BASE/vcf/track/get"
same "registered sws vcf == disk" vcf_disk vcf_sws_track

step "result"
if [ "$FAILED" -eq 0 ]; then
    echo "OK: every cloud read path answered with the same data as the local read ($SKIPPED skipped)"
else
    echo "FAILED: $FAILED check(s), $SKIPPED skipped"
fi
exit "$FAILED"
