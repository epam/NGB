#!/usr/bin/env bash
#
# Stage one fixture of every track type NGB can read into .devenv/data/ngs/tracks/, which the
# server containers see as /ngs/tracks/. Companion to verify-tracks.sh, which registers and
# loads what this stages.
#
#   ./prepare-track-fixtures.sh
#
# Requires: `docker-compose` from .devenv/ (for the CRAM step only), and a built
# dist/catgenome-psql.jar or dist/catgenome-h2.jar to take htsjdk from.
#
# The unit suite reads these same fixtures, but through the managers, with a Spring test context
# and a temporary contents directory; it never boots the server. So anything that touches the
# parsers wants a second pass over a real instance, and that needs the fixtures somewhere the
# containers can see - they mount only ./data/ngs as /ngs, not /workspace.
#
# Three of the eleven files are not copies:
#
#   * the CRAM is generated, because the repo ships none - see BamToCram.java;
#   * test_seg.seg is human (chrom 1..24) and the only references we have registered are A3
#     (one 56 kb contig A1) and dm606.X (one 23.5 Mb contig X), so its chrom-1 rows that fall
#     inside 23.5 Mb are relabelled X;
#   * cat.genepred's chrM rows (max coordinate 16177) are relabelled chrA1 so they fall inside
#     A3's single 56 kb contig; its chrX rows run past 200 kb and are dropped.
#
# Relabelling a contig column is the smallest possible change that makes a real fixture
# loadable against a reference we have - it leaves every field the parser looks at untouched.

set -uo pipefail

DEVENV="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO="$(cd "$DEVENV/.." && pwd)"
TEMPLATES="$REPO/server/catgenome/src/test/resources/templates"
GENEPRED="$REPO/server/catgenome/src/test/resources/genepred"
STAGE="$DEVENV/data/ngs/tracks"

DM6_LENGTH=23542271     # dm606.X.fa, contig X
A3_LENGTH=56400         # A3.fa, contig A1

step() { printf '\n== %s\n' "$1"; }

step "staging copies into $STAGE"
mkdir -p "$STAGE" || exit 1
for f in \
    genes_sorted.bed genes_sorted.bed.gz genes_sorted.bed.tbi \
    genes_sorted.gtf genes_sorted.gtf.gz genes_sorted.gtf.tbi \
    CantonS.vcf.gz CantonS.vcf.gz.tbi \
    bedGraph.bdg agnX1.09-28.trim.dm606.realign.bw \
    agnX1.09-28.trim.dm606.realign.bam agnX1.09-28.trim.dm606.realign.bam.bai
do
    cp "$TEMPLATES/$f" "$STAGE/$f" || exit 1
    printf '   %-44s %s\n' "$f" "$(wc -c < "$STAGE/$f" | tr -d ' ') bytes"
done

step "SEG: test_seg.seg chrom 1 -> X, rows inside $DM6_LENGTH bp"
awk -v max="$DM6_LENGTH" 'BEGIN{FS=OFS="\t"}
     NR==1 {print; next}
     $2=="1" && $4+0 < max {$2="X"; print}' \
    "$TEMPLATES/test_seg.seg" > "$STAGE/p7_seg_X.seg" || exit 1
printf '   %-44s %s\n' p7_seg_X.seg \
    "$(($(wc -l < "$STAGE/p7_seg_X.seg") - 1)) segment(s), $(awk 'NR>1{print $1}' "$STAGE/p7_seg_X.seg" | sort -u | wc -l | tr -d ' ') sample(s)"

step "GenePred: cat.genepred chrM -> chrA1, rows inside $A3_LENGTH bp"
awk -v max="$A3_LENGTH" 'BEGIN{FS=OFS="\t"}
     /^#/ {print; next}
     $3=="chrM" && $6+0 < max {$3="chrA1"; print}' \
    "$GENEPRED/cat.genepred" > "$STAGE/p7_cat_chrA1.genepred" || exit 1
printf '   %-44s %s\n' p7_cat_chrA1.genepred \
    "$(grep -cv '^#' "$STAGE/p7_cat_chrA1.genepred") transcript(s)"

step "CRAM: agnX1...bam + dm606.X.fa -> p7_agnX1.cram (+ .crai), written by htsjdk itself"
JAR="dist/catgenome-h2.jar"
if [ ! -f "$REPO/$JAR" ]; then
    echo "   FAILED: no $JAR to take htsjdk from; run 'make jar-fast' first"
    exit 1
fi
# The fat jar nests its dependencies, and `java -cp` cannot read a jar inside a jar, so
# BOOT-INF/lib is unpacked into the container's /tmp first - unconditionally, because a cached
# unpack of a jar built before the htsjdk bump will silently write the fixture with the old
# library. Source-file mode (`java Foo.java`) keeps the converter one reviewable file, no build.
( cd "$DEVENV" && docker-compose exec -T builder bash -lc '
    set -e
    CP=/tmp/p7-htsjdk-cp
    rm -rf "$CP" && mkdir -p "$CP"
    ( cd "$CP" && unzip -qo -j "/workspace/'"$JAR"'" "BOOT-INF/lib/*.jar" )
    echo "   '"$JAR"' -> $(ls "$CP" | wc -l | tr -d " ") jars, $(basename $(ls "$CP"/htsjdk-*.jar))"
    cd /workspace/.devenv
    with-java21 java -cp "$CP/*" scripts/BamToCram.java \
        data/ngs/tracks/agnX1.09-28.trim.dm606.realign.bam \
        data/ngs/dm606.X.fa \
        data/ngs/tracks/p7_agnX1.cram' ) || exit 1

ls -l "$STAGE" | sed 's/^/   /'
