#!/usr/bin/env bash
#
# Build (or rebuild) NGB's global Lucene indexes from the fixtures in the repo's test
# resources. Two uses:
#
#   * it is how .devenv/fixtures/pre-migration/lucene6/ was populated on the pre-Phase-6
#     tree, so a Lucene 6 index existed to test the D8 guard against;
#   * it is the "rebuild every global index" half of the reindex procedure documented in
#     docs/md/installation/lucene-reindex.md — run it after deleting the index directories.
#
#   ./build-global-indexes.sh [base-url]      # default http://localhost:8080/catgenome
#
# Requires: a running server, and `docker-compose` from .devenv/ to stage the sources.
#
# The source files are byte-identical copies of server/catgenome/src/test/resources/{taxonomy,
# homologene,ncbi,opentargets,pharmgkb,dgidb,pathway}. They are staged into
# .devenv/data/ngs/index-sources/ because that host directory is the only thing the ngb-h2
# container mounts (as /ngs); it does not mount /workspace.
#
# Not covered, deliberately:
#   * TTD  — the repo ships no TTD fixture, so targets/ttd.* indexes are never built here.
#   * per-file feature indexes — those are rebuilt through the REST reindex endpoints
#     (/vcf/{id}/index, /gene/{id}/index, /bed/{id}/index), not from these sources.

set -uo pipefail

DEVENV="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO="$(cd "$DEVENV/.." && pwd)"
BASE="${1:-http://localhost:8080/catgenome}/restapi"
SRC=/ngs/index-sources          # as the server sees it
STAGE="$DEVENV/data/ngs/index-sources"
FAILED=0

step() { printf '\n== %s\n' "$1"; }

# call <method> <url> [curl args...] -- expects {"status":"OK"}
call() {
    local method="$1" url="$2"; shift 2
    local out
    out=$(curl -sS --max-time 3600 -X "$method" "$@" "$url" 2>&1)
    if printf '%s' "$out" | grep -q '"status":"OK"'; then
        echo "   OK   $method ${url#"$BASE"}"
    else
        echo "   FAIL $method ${url#"$BASE"}"
        printf '%s\n' "$out" | head -c 500 | sed 's/^/        /'
        FAILED=$((FAILED + 1))
    fi
}

step "staging sources into $STAGE"
mkdir -p "$STAGE"
for d in taxonomy homologene ncbi opentargets pharmgkb dgidb pathway; do
    cp -R "$REPO/server/catgenome/src/test/resources/$d" "$STAGE/" || exit 1
done
find "$STAGE" -type f | wc -l | sed 's/^/   files staged: /'

step "taxonomy  -> taxonomy.index.directory"
call PUT "$BASE/taxonomy/upload?taxonomyFilePath=$SRC/taxonomy/names.dmp"

step "homologene  -> homologene.index.directory"
call PUT "$BASE/homologene/import?databasePath=$SRC/homologene/homologene.xml"

step "ncbi gene ids  -> ncbi.index.directory/gene.ids"
call PUT "$BASE/externaldb/ncbi/genes/import?path=$SRC/ncbi/gene2ensembl"

step "targets  -> targets.index.directory/{opentargets,dgidb,pharmgkb}.*"
call PUT "$BASE/target/import/opentargets?path=$SRC/opentargets"
call PUT "$BASE/target/import/dgidb?path=$SRC/dgidb/interactions.tsv"
call PUT "$BASE/target/import/pharmGKB?genePath=$SRC/pharmgkb/genes.tsv&drugPath=$SRC/pharmgkb/drugLabels.tsv&drugAssociationPath=$SRC/pharmgkb/drugLabels.byGene.tsv&diseaseAssociationPath=$SRC/pharmgkb/relationships.tsv"

# The two below own a database row as well as an index directory, so they are registrations,
# not imports: skip them if the row already exists or you will get a duplicate.
if [ "${SKIP_REGISTRATIONS:-}" != "1" ]; then
    step "pathway  -> pathway.index.directory   (registers pathway 'Glycolysis')"
    call POST "$BASE/pathway" -H 'Content-Type: application/json' \
        -d "{\"name\":\"Glycolysis\",\"prettyName\":\"Glycolysis\",\"path\":\"$SRC/pathway/Glycolysis.sbgn\",\"pathwayDesc\":\"fixture pathway\",\"taxIds\":[9606]}"

    step "bam coverage  -> bam.coverage.index.directory   (bam file 2, step 100)"
    call POST "$BASE/bam/coverage" -H 'Content-Type: application/json' -d '{"bamId":2,"step":100}'

    step "target  (a database row only; read by target identification)"
    call POST "$BASE/target" -H 'Content-Type: application/json' -d '{
        "targetName":"lucene6-fixture-target","type":"DEFAULT",
        "diseases":["neuropathic pain"],"products":[""],
        "targetGenes":[{"geneId":"ENSG00000006128","geneName":"TAC1","taxId":9606,
                        "speciesName":"Homo sapiens","priority":"HIGH"}]}'
else
    echo
    echo "== SKIP_REGISTRATIONS=1: leaving pathway, bam coverage and target alone"
fi

echo
if [ "$FAILED" -eq 0 ]; then
    echo "OK: every import returned status OK — now run verify-lucene.sh"
else
    echo "FAILED: $FAILED call(s)"
fi
exit "$FAILED"
