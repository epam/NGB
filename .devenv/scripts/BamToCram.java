/*
 * BAM -> CRAM + .crai, using the same htsjdk the server is built against.
 *
 * Written for migration Phase 7. The repo ships no CRAM fixture anywhere, and NGB's BAM path
 * accepts `.cram` (BamHelper.BAM_EXTENSIONS) and decodes it through a ReferenceSource backed by
 * NGB's own registered reference - so "does CRAM still work" is a question no test in the suite
 * answers. The toolbox image has no samtools, and pulling one in would put a second htsjdk
 * implementation in the loop; converting with the jar's own htsjdk means the fixture is written
 * and read by the same version the phase is verifying.
 *
 *   java -cp '<jar dir>/*' BamToCram.java <in.bam> <reference.fa> <out.cram>
 *
 * The reference needs a .fai beside it; a .dict is not required. Reads whose contig is missing
 * from the reference are dropped and counted, because a CRAM record cannot be encoded without
 * its reference bases - the dm606 BAM's header names ~1800 contigs and the fixture reference has
 * only X.
 *
 * The index is written separately, as a real .crai: SAMFileWriterFactory.setCreateIndex(true)
 * over a CRAM writer produces a BAI, and BamHelper.BAI_EXTENSIONS maps `.cram` to `.crai`.
 */

import htsjdk.samtools.CRAMCRAIIndexer;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import htsjdk.samtools.util.Log;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public final class BamToCram {

    /** A window the dm606 fixture has reads in, and the one verify-tracks.sh asks NGB for. */
    private static final int QUERY_START = 12_585_000;
    private static final int QUERY_END = 12_600_000;

    private BamToCram() {
    }

    public static void main(final String[] args) throws Exception {
        Log.setGlobalLogLevel(Log.LogLevel.WARNING);   // else one DEBUG line per CRAM container

        final File bam = new File(args[0]);
        final File fasta = new File(args[1]);
        final File cram = new File(args[2]);
        final File crai = new File(cram.getPath() + ".crai");

        // The contig list is read straight out of the .fai. NGB's fixtures ship no .dict, so
        // ReferenceSequenceFile.getSequenceDictionary() is null, and htsjdk's own index entry type
        // is package-private. Neither troubles the CRAM writer, which only ever asks the fasta for
        // the bases of a named contig.
        final Set<String> referenceContigs = new HashSet<>();
        try (Stream<String> fai = Files.lines(new File(fasta.getPath() + ".fai").toPath())) {
            fai.forEach(line -> referenceContigs.add(line.split("\t")[0]));
        }
        System.out.println("   reference contigs: " + referenceContigs);

        long written = 0;
        long droppedNoReference = 0;
        long unmapped = 0;

        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .open(bam)) {

            final SAMFileHeader header = reader.getFileHeader().clone();
            header.setSortOrder(SAMFileHeader.SortOrder.coordinate);
            System.out.println("   bam header names " + header.getSequenceDictionary().size() + " contig(s)");

            try (SAMFileWriter writer = new SAMFileWriterFactory().makeCRAMWriter(header, true, cram, fasta)) {
                for (final SAMRecord record : reader) {
                    if (record.getReadUnmappedFlag()) {
                        unmapped++;
                        continue;
                    }
                    if (!referenceContigs.contains(record.getReferenceName())) {
                        droppedNoReference++;
                        continue;
                    }
                    writer.addAlignment(record);
                    written++;
                }
            }
        }

        try (SeekableFileStream cramStream = new SeekableFileStream(cram);
             OutputStream index = Files.newOutputStream(crai.toPath())) {
            CRAMCRAIIndexer.writeIndex(cramStream, index);
        }

        System.out.printf("   wrote %d record(s), %d bytes, %d byte index; "
                        + "dropped %d unmapped and %d off-reference%n",
                written, cram.length(), crai.length(), unmapped, droppedNoReference);

        // Read it straight back through the index, so that a track that fails to load later is
        // known to be NGB's problem and not a fixture that was never queryable.
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .referenceSequence(fasta)
                .open(cram)) {
            final String contig = referenceContigs.iterator().next();
            long inWindow = 0;
            try (var query = reader.query(contig, QUERY_START, QUERY_END, false)) {
                while (query.hasNext()) {
                    query.next();
                    inWindow++;
                }
            }
            System.out.printf("   read back: %d record(s) in %s:%d-%d via the .crai%n",
                    inWindow, contig, QUERY_START, QUERY_END);
            if (inWindow == 0) {
                throw new IllegalStateException("the CRAM answered an indexed query with nothing");
            }
        }
    }
}
