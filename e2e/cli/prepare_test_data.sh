#!/usr/bin/env bash
#
# Stage the fixture set e2e/cli/testcases.csv expects into e2e/cli/test_data/.
#
#   ./prepare_test_data.sh [output-dir]
#
# Every fixture is derived from the server module's own test resources - copied, renamed, or with
# its contig column relabelled. Nothing is downloaded: integration_tests.sh used to wget this data
# from http://ngb.opensource.epam.com/distr/data/tests/, and that host stopped resolving, which is
# what made the suite unrunnable.
#
# The names below are the ones testcases.csv hard-codes, so they are kept even where the content no
# longer matches the name (the BAM is agnX1, not CantonS; the GTF is cat, not fruit fly). Renaming
# them in the CSV instead would have meant rewriting 145 test rows.
#
# What the test rows actually require of the data - checked against the server, not assumed:
#
#   * the reference must have a chromosome named X (the `url -loc X` rows) and must not have one
#     named A (the row that expects "Chromosome with name A is not found");
#   * a feature file must carry a contig name the reference knows, with or without a `chr` prefix.
#     Coordinates are not checked against the chromosome's length at registration time - the unit
#     suite registers this same GTF, whose features run to 500 kb, against a 10 kb chromosome - so
#     the reference here is deliberately small: it is registered as a precondition of ~130 rows.
#   * dmel-all-r6.06.sorted.cutted.gtf.tbi and CantonS.09-28.trim_X.dm606.realign.vcf.tbi must NOT
#     exist. Two rows pass those paths to `rf` to test what happens with an index that is not
#     there ("VCF Registration with unexisting index"); creating them would break both.
#
# So the reference gets two contigs: X, from dm606.X.fa, for the VCF, bedGraph and SEG fixtures,
# and A1, from A3.fa, for the GTF, GFF and BED ones. That is what lets those five files be used
# byte-for-byte, together with the real bgzip'd .gz siblings the templates directory already has.

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
TEMPLATES="$REPO/server/catgenome/src/test/resources/templates"
OUT="${1:-$HERE/test_data}"

X_BASES=200000          # first 200 kb of dm606.X.fa, 80 bases to the line
X_LINES=$((X_BASES / 80))

[ -d "$TEMPLATES" ] || { echo "no such directory: $TEMPLATES" >&2; exit 1; }
mkdir -p "$OUT"

report() { printf '   %-46s %s\n' "$1" "$(wc -c < "$OUT/$1" | tr -d ' ') bytes"; }

# -- reference ---------------------------------------------------------------------------------
# X for the fixtures labelled X, A1 for the ones labelled A1/chrA1. A3.fa's own contig is A1.
{
    printf '>X\n'
    # awk rather than `head`, which would SIGPIPE the reader and trip pipefail.
    awk -v lines="$X_LINES" 'NR > 1 {print; if (NR > lines) exit}' "$TEMPLATES/dm606.X.fa"
    printf '>A1\n'
    awk 'NR > 1' "$TEMPLATES/A3.fa"
} > "$OUT/dmel-all-chromosome-r6.06.cutted.fa"
report dmel-all-chromosome-r6.06.cutted.fa

# Plain gzip, deliberately: one row registers this and expects "genome data in a file which format
# isn't supported". A bgzip'd FASTA would be accepted.
gzip -cn "$OUT/dmel-all-chromosome-r6.06.cutted.fa" > "$OUT/dmel-all-chromosome-r6.06.cutted.fa.gz"
report dmel-all-chromosome-r6.06.cutted.fa.gz

# -- straight copies ---------------------------------------------------------------------------
copy() { cp "$TEMPLATES/$1" "$OUT/$2"; report "$2"; }

copy genes_sorted.gtf        dmel-all-r6.06.sorted.cutted.gtf
copy genes_sorted.gtf        dmel-all-r6.06.sorted.cutted_2.gtf
copy genes_sorted.gtf.gz     dmel-all-r6.06.sorted.cutted.gtf.gz
copy genes_sorted.bed        CantonS.09-28.trim_X_2L.bed
copy genes_sorted.bed        CantonS.09-28.trim_X_2L_2.bed
copy genes_sorted.bed.gz     CantonS.09-28.trim_X_2L.bed.gz
copy genes.gff               example.gff
copy bedGraph.bdg            bedGraph.bdg
copy bedGraph.bdg.gz         bedGraph.bdg.gz
copy CantonS.vcf.gz          CantonS.09-28.trim_X.dm606.realign.vcf.gz

# The BAM is registered with its index as `...X_2L.bai` (htsjdk finds either `x.bam.bai` or
# `x.bai`), and copied to `..._2.bam` with no index beside it - two rows check that a BAM without
# an index is refused.
copy agnX1.09-28.trim.dm606.realign.bam     CantonS.09-28.trim.dm606.realign_X_2L.bam
copy agnX1.09-28.trim.dm606.realign.bam.bai CantonS.09-28.trim.dm606.realign_X_2L.bai
copy agnX1.09-28.trim.dm606.realign.bam     CantonS.09-28.trim.dm606.realign_X_2L_2.bam

# -- generated ---------------------------------------------------------------------------------
# The GFF has to be *block*-compressed, and templates/genes.gff.gz is plain gzip - one of only two
# such files in that directory, the other being a GTF nothing here needs. NGB builds a tabix index
# for any .gz it registers, and FeatureIterator.createStream refuses a .gz whose extension promises
# block compression but whose content is not ("Input file is not in valid block compressed format").
# So compress it here. `bgzip` if htslib happens to be installed, otherwise 20 lines of python3:
# BGZF is just gzip members carrying a BC extra field with the member length, plus an empty member
# as the end-of-file marker, and neither the toolbox image nor a CI runner has htslib by default.
if command -v bgzip > /dev/null 2>&1; then
    bgzip -c "$TEMPLATES/genes.gff" > "$OUT/example.gff.gz"
else
    python3 - "$TEMPLATES/genes.gff" "$OUT/example.gff.gz" <<'PY'
import struct, sys, zlib

BLOCK = 0xff00      # the uncompressed chunk size bgzip itself uses


def member(data):
    deflate = zlib.compressobj(6, zlib.DEFLATED, -15)
    body = deflate.compress(data) + deflate.flush()
    # BSIZE is the whole member's length minus one: 18 bytes of header, the deflate stream,
    # then CRC32 and ISIZE.
    return (b'\x1f\x8b\x08\x04\x00\x00\x00\x00\x00\xff\x06\x00BC\x02\x00'
            + struct.pack('<H', len(body) + 25) + body
            + struct.pack('<II', zlib.crc32(data) & 0xffffffff, len(data)))


with open(sys.argv[1], 'rb') as src, open(sys.argv[2], 'wb') as dst:
    for chunk in iter(lambda: src.read(BLOCK), b''):
        dst.write(member(chunk))
    dst.write(member(b''))       # 28-byte EOF marker
PY
fi
report example.gff.gz

# Uncompressed VCF: several rows register it with no index at all, which makes NGB build a Tribble
# index of its own, and one row sorts it.
gzip -cd "$TEMPLATES/CantonS.vcf.gz" > "$OUT/CantonS.09-28.trim_X.dm606.realign.vcf"
report CantonS.09-28.trim_X.dm606.realign.vcf

# SEG: test_seg.seg is human (chrom 1..24); keep chrom 1 and relabel it X, the same relabelling
# .devenv/scripts/prepare-track-fixtures.sh does for the same reason.
awk 'BEGIN {FS = OFS = "\t"}
     NR == 1 {print; next}
     $2 == "1" {$2 = "X"; print}' "$TEMPLATES/test_seg.seg" > "$OUT/example.seg"
report example.seg

# A file whose extension the CLI does not know, so that its own check refuses it before anything
# is sent to the server. Not .txt, which the testcase used to name: since featureCounts support
# (9cfbda67) the CLI maps .txt to GENE, and a .txt file gets as far as the server's GffCodec.
printf 'Not a genomic file. Staged by prepare_test_data.sh so that the\n' \
       > "$OUT/bad_file_format.dat"
printf '"unsupported file format" test row has something to be refused.\n' \
       >> "$OUT/bad_file_format.dat"
report bad_file_format.dat

echo
echo "==> $(ls -1 "$OUT" | wc -l | tr -d ' ') fixtures in $OUT"
