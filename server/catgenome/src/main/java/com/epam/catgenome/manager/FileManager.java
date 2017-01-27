/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epam.catgenome.manager;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.manager.FileManager.FilePathFormat.*;
import static com.epam.catgenome.manager.FileManager.FilePathPlaceholder.*;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.PostConstruct;

import com.epam.catgenome.util.AuthUtils;
import com.epam.catgenome.util.BlockCompressedDataInputStream;
import com.epam.catgenome.util.BlockCompressedDataOutputStream;
import com.epam.catgenome.util.IndexUtils;
import com.epam.catgenome.util.PositionalOutputStream;
import com.epam.catgenome.util.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.store.SimpleFSDirectory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.bio.CompressionType;
import org.jetbrains.bio.big.BigWigFile;
import org.jetbrains.bio.big.WigSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.epam.catgenome.component.MessageCode;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.bed.BedFile;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneFileType;
import com.epam.catgenome.entity.maf.MafFile;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.seg.SegFile;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.entity.wig.WigFile;
import com.epam.catgenome.exception.HistogramWritingException;
import com.epam.catgenome.exception.UnsupportedGeneFileTypeException;
import com.epam.catgenome.manager.bed.parser.NggbBedCodec;
import com.epam.catgenome.manager.bed.parser.NggbBedFeature;
import com.epam.catgenome.manager.gene.parser.GeneFeature;
import com.epam.catgenome.manager.gene.parser.GffCodec;
import com.epam.catgenome.manager.gene.parser.GtfFeature;
import com.epam.catgenome.manager.maf.parser.MafCodec;
import com.epam.catgenome.manager.maf.parser.MafFeature;
import com.epam.catgenome.manager.seg.parser.SegCodec;
import com.epam.catgenome.manager.seg.parser.SegFeature;
import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedOutputStream;
import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.AsciiFeatureCodec;
import htsjdk.tribble.Feature;
import htsjdk.tribble.FeatureReader;
import htsjdk.tribble.index.Index;
import htsjdk.tribble.index.IndexFactory;
import htsjdk.tribble.index.interval.IntervalTreeIndex;
import htsjdk.tribble.index.tabix.TabixFormat;
import htsjdk.tribble.index.tabix.TabixIndex;
import htsjdk.tribble.index.tabix.TabixIndexCreator;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.tribble.util.LittleEndianOutputStream;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;

/**
 * Source:      FileManager.java
 * Created:     10/12/15, 7:53 PM
 * Project:     CATGenome BrowserF
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code FileManager} represents a service class designed to manage content files contained
 * data corresponded to tracks and also any other related resources that should be read and/or
 * written from/to certain places under provided base directory path.
 * <p>
 * In fact it's designed to generalize any operations concerned with content files in one
 * place and provide general approach to handle file resources.
 */
@Service
public class FileManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileManager.class);
    private static final TabixFormat SEG_TABIX_FORMAT = new TabixFormat(TabixFormat.UCSC_FLAGS, 2, 3, 4, '\'', 0);
    private static final TabixFormat MAF_TABIX_FORMAT = new TabixFormat(TabixFormat.UCSC_FLAGS, 5, 6, 7, '#', 0);
    private static final TabixFormat BIGMAF_TABIX_FORMAT = new TabixFormat(TabixFormat.UCSC_FLAGS, 6, 7, 8, '#', 0);

    /**
     * Provides paths' patterns that have to be used to construct real relative paths
     * for file resources of any types.
     */
    enum FilePathFormat {

        // describes a temporary catalogue, used to handle file uploads etc.
        TMP_DIR("/tmp"),

        USER_DIR("/${USER_ID}"),

        // describes the structure of catalogues used to store any resources concerned
        // with managed reference genomes and chromosomes
        REFERENCE_DIR("/references/${DIR_ID}"),
        REF_CHROMOSOMES_DIR("/references/${DIR_ID}/chromosomes"),
        REF_CHROMOSOME_DIR("/references/${DIR_ID}/chromosomes/${CHROMOSOME_NAME}"),
        CHROMOSOME_GC_CONTENT_FILE("/references/${DIR_ID}/chromosomes/${CHROMOSOME_NAME}/content.gccont"),
        CHROMOSOME_GC_CONTENT_INDEX_FILE("/references/${DIR_ID}/chromosomes/${CHROMOSOME_NAME}/content.gccont.ind"),
        REF_CHROMOSOME_SEQUENCE_FILE("/references/${DIR_ID}/chromosomes/${CHROMOSOME_NAME}/sequences.nib"),
        REF_CHROMOSOME_SEQUENCE_INDEX_FILE("/references/${DIR_ID}/chromosomes/${CHROMOSOME_NAME}/sequences.nib.ind"),
        REF_CHROMOSOME_CYTOBAND_FILE("/references/${DIR_ID}/chromosomes/${CHROMOSOME_NAME}/cytobands.txt"),

        // think to do it in other way?

        REF_CYTOBANDS_FILE("/references/${DIR_ID}/cytobands/bands.txt"),

        VCF_DIR("/${USER_ID}/VCF/${DIR_ID}"),
        VCF_FILE("/${USER_ID}/VCF/${DIR_ID}/${FILE_NAME}"),
        VCF_INDEX("/${USER_ID}/VCF/${DIR_ID}/variants.idx"),
        VCF_COMPRESSED_INDEX("/${USER_ID}/VCF/${DIR_ID}/variants.gz.tbi"),
        VCF_METADATA_FILE("/${USER_ID}/VCF/${DIR_ID}/variants.bounds"),
        VCF_FEATURE_INDEX_FILE("/${USER_ID}/VCF/${DIR_ID}/variants.feature"),
        VCF_ROOT_DIR("/${USER_ID}/VCF"),
        VCF_HISTOGRAM_DIR("/${USER_ID}/VCF/${DIR_ID}/histogram"),
        VCF_HISTOGRAM_FILE("/${USER_ID}/VCF/${DIR_ID}/histogram/${CHROMOSOME_NAME}.hg"),

        GENE_DIR("/${USER_ID}/genes/${DIR_ID}"),
        GENE_FILE("/${USER_ID}/genes/${DIR_ID}/genes${GENE_EXTENSION}"),
        GENE_LARGE_SCALE_FILE("/${USER_ID}/genes/${DIR_ID}/genes_large_scale${GENE_EXTENSION}"),
        GENE_TRANSCRIPT_FILE("/${USER_ID}/genes/${DIR_ID}/transcript${GENE_EXTENSION}"),
        GENE_INDEX("/${USER_ID}/genes/${DIR_ID}/genes.tbi"),
        GENE_LARGE_SCALE_INDEX("/${USER_ID}/genes/${DIR_ID}/genes_large_scale.tbi"),
        GENE_TRANSCRIPT_INDEX("/${USER_ID}/genes/${DIR_ID}/transcript.tbi"),
        GENE_METADATA_FILE("/${USER_ID}/genes/${DIR_ID}/genes.bounds"),
        GENE_FEATURE_INDEX_FILE("/${USER_ID}/genes/${DIR_ID}/genes.feature"),
        GENE_HISTOGRAM_DIR("/${USER_ID}/genes/${DIR_ID}/histogram"),
        GENE_HISTOGRAM_FILE("/${USER_ID}/genes/${DIR_ID}/histogram/${CHROMOSOME_NAME}.hg"),

        BAM_DIR("/${USER_ID}/BAM/${DIR_ID}"),
        BAM_FILE("/${USER_ID}/BAM/${DIR_ID}/${FILE_NAME}"),

        BED_DIR("/${USER_ID}/bed/${DIR_ID}"),
        BED_INDEX("/${USER_ID}/bed/${DIR_ID}/bed.tbi"),
        BED_HISTOGRAM_DIR("/${USER_ID}/bed/${DIR_ID}/histogram"),
        BED_HISTOGRAM_FILE("/${USER_ID}/bed/${DIR_ID}/histogram/${CHROMOSOME_NAME}.hg"),

        SEG_DIR("/${USER_ID}/seg/${DIR_ID}"),
        SEG_INDEX("/${USER_ID}/seg/${DIR_ID}/seg.tbi"),
        SEG_SAMPLE_FILE("/${USER_ID}/seg/${DIR_ID}/${SAMPLE_NAME}.seg"),
        SEG_FILE("/${USER_ID}/seg/${DIR_ID}/segments.seg"),
        SEG_SAMPLE_COMPRESSED_FILE("/${USER_ID}/seg/${DIR_ID}/${SAMPLE_NAME}.seg.gz"),
        SEG_SAMPLE_INDEX("/${USER_ID}/seg/${DIR_ID}/${SAMPLE_NAME}.tbi"),

        MAF_DIR("/${USER_ID}/maf/${DIR_ID}"),
        MAF_TEMP_DIR("/${USER_ID}/maf/${DIR_ID}/tmp"),
        MAF_INDEX("/${USER_ID}/maf/${DIR_ID}/maf.tbi"),
        MAF_TEMP_INDEX("/${USER_ID}/maf/${DIR_ID}/tmp/${FILE_NAME}.tbi"),
        MAF_FILE("/${USER_ID}/maf/${DIR_ID}/maf.bmaf.gz"),

        WIG_DIR("/${USER_ID}/wig/${DIR_ID}/downsampled"),
        WIG_FILE("/${USER_ID}/wig/${DIR_ID}/downsampled/${CHROMOSOME_NAME}.wig"),

        VG_DIR("/${USER_ID}/vg/${DIR_ID}"),

        FEATURE_INDEX_DIR("${FEATURE_FILE_DIR}/index.luc"),

        PROJECT_FEATURE_INDEX_FILE("/projects/${PROJECT_ID}/index.luc"),
        PROJECT_DIR("/projects/${PROJECT_ID}");

        /**
         * {@code String} represents formatted related path to the given resource in
         * file system. This format will be substituted by real parameters value that should
         * be provided to a call.
         */
        private final String path;

        FilePathFormat(final String path) {
            this.path = path;
        }

        public final String getPath() {
            return path;
        }
    }

    private enum VcfFileNames {
        VCF_FILE_NAME("variants.vcf"),
        VCF_COMPRESSED_FILE_NAME("variants.gz");

        private final String name;

        VcfFileNames(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static final String[] GENE_FILE_EXTENSIONS = {".gff.gz", ".gtf.gz", ".gff", ".gtf", ".gff3", ".gff3.gz"};

    /**
     * {@code String} specifies the content root path of a directory, which is used to
     * store uploaded content files and any other file resources related to them.
     */
    @Value("#{catgenome['files.base.directory.path']}")
    private String baseDirPath;

    /**
     * Returns the real path of a directory used as the content root to store uploaded content
     * files and any immediate post-processing file resources related to them.
     *
     * @return {@code String}
     */
    public String getBaseDirPath() {
        return baseDirPath;
    }

    /**
     * Returns a reference on a catalogue that should be used to handle any temporary resources,
     * e.g. to handle file uploads
     *
     * @return {@code File}
     */
    public File getTempDir() {
        return new File(toRealPath(TMP_DIR.getPath()));
    }

    /**
     * Creates in the file system initial catalogue structure used to manage all information associated
     * with the provided reference.
     *
     * @param reference {@code Reference} represents a container that provides access to major properties
     *                  and can be updated by metadata produced as the result of the current call
     */
    public void makeReferenceDir(final Reference reference) {
        Assert.notNull(reference, getMessage(MessageCode.NO_SUCH_REFERENCE));
        Assert.notNull(reference.getId(), getMessage(MessageCode.UNKNOWN_REFERENCE_ID));
        // defines map of parameters, used to substitute the given path format
        final Map<String, Object> params = new HashMap<>();
        final Long dirId = reference.getId();
        params.put(DIR_ID.name(), dirId);
        // makes the content root directory to manage resources related to the reference with the given ID
        reference.setId(dirId);
        if (reference.getType() != BiologicalDataItemResourceType.GA4GH) {
            reference.setPath(substitute(REFERENCE_DIR, params));
            makeDir(reference.getPath());
            // makes a directory to manage chromosomes
            makeDir(substitute(REF_CHROMOSOMES_DIR, params));
        }
    }

    /**
     * Creates in the file system initial catalogue structure used to manage all VCF files associated
     * with the provided VCF id and provided user ID.
     *
     * @param fileId {@code long} represents a VCF file id in the system
     * @param userId {@code long} represents ID of the user, who uploaded the file
     */
    public void makeVcfDir(long fileId, Long userId) {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), fileId);
        params.put(USER_ID.name(), userId);
        // create a directory for VCF files, associated with the given reference id
        makeDir(substitute(VCF_DIR, params));
    }

    /**
     * Creates in the file system initial catalogue structure used to manage all gene files associated
     * with the provided gene file id and provided user ID.
     *
     * @param fileId {@code long} represents a gene file id in the system
     * @param userId {@code long} represents ID of the user, who uploaded the file
     */
    public void makeGeneDir(long fileId, Long userId) {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), fileId);
        params.put(USER_ID.name(), userId);
        // create a directory for Gene files, associated with the given reference id
        makeDir(substitute(GENE_DIR, params));
    }

    /**
     * Creates in the file system initial catalogue structure used to manage all BED files associated
     * with the provided BED file id and provided user ID.
     *
     * @param fileId {@code long} represents a BED file id in the system
     * @param userId {@code long} represents ID of the user, who uploaded the file
     */
    public void makeBedDir(long fileId, Long userId) {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), fileId);
        params.put(USER_ID.name(), userId);
        // create a directory for BED files, associated with the given reference id
        makeDir(substitute(BED_DIR, params));
    }

    /**
     * Creates in the file system initial catalogue structure used to manage all SEG files associated
     * with the provided SEG file id and provided user ID.
     *
     * @param fileId {@code long} represents a SEG file id in the system
     * @param userId {@code long} represents ID of the user, who uploaded the file
     */
    public void makeSegDir(long fileId, Long userId) {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), fileId);
        params.put(USER_ID.name(), userId);
        // create a directory for SEG files, associated with the given reference id
        makeDir(substitute(SEG_DIR, params));
    }

    /**
     * Creates in the file system initial catalogue structure used to manage all MAF files associated
     * with the provided MAF file id and provided user ID.
     *
     * @param fileId {@code long} represents a MAF file id in the system
     * @param userId {@code long} represents ID of the user, who uploaded the file
     */
    public void makeMafDir(long fileId, Long userId) {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), fileId);
        params.put(USER_ID.name(), userId);
        makeDir(substitute(MAF_DIR, params));
    }

    private void makeMafTempDir(long fileId, Long userId) {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), fileId);
        params.put(USER_ID.name(), userId);
        makeDir(substitute(MAF_TEMP_DIR, params));
    }

    /**
     * Deletes from the file system temporary catalogue used to manage MAF files, before merging
     *
     * @param fileId {@code long} represents a MAF file id in the system
     * @param userId {@code long} represents ID of the user, who uploaded the file
     */
    public void deleteMafTempDir(long fileId, Long userId) throws IOException {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), fileId);
        params.put(USER_ID.name(), userId);
        deleteDir(substitute(MAF_TEMP_DIR, params));
    }

    /**
     * Creates in the file system initial catalogue structure used to manage all WIG files associated
     * with the provided WIG file id and provided user ID.
     *
     * @param fileId {@code long} represents a WIG file id in the system
     * @param userId {@code long} represents ID of the user, who uploaded the file
     */
    public void makeWigDir(long fileId, Long userId) {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), fileId);
        params.put(USER_ID.name(), userId);
        makeDir(substitute(WIG_DIR, params));
    }

    /**
     * Creates in the file system a resource used to write NT sequence associated with the given chromosome.
     *
     * @param referenceId {@code Long} represents ID of a reference in the system
     * @param chromosome  {@code Chromosome} represents a container that provides access to major properties
     *                    and can be updated by metadata produced as the result of the current call
     * @return {@code OutputStream} represents a stream resource associated with a file used to manage NT
     * sequence of a chromosome in *.nib format
     * @throws FileNotFoundException throws in a case if system can't create a stream resource associated with
     *                               a file used to store NT sequence
     */
    public BlockCompressedDataOutputStream makeRefOutputStream(final Long referenceId, final Chromosome chromosome)
            throws IOException {
        // checks that all mandatory parameters are passed among provided arguments
        Assert.notNull(referenceId, getMessage(MessageCode.NO_SUCH_REFERENCE));
        Assert.isTrue(StringUtils.isNotBlank(chromosome.getName()),
                getMessage(MessagesConstants.ERROR_NO_CHROMOSOME_NAME));
        // makes directory used to store data for the given chromosome
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), referenceId);
        params.put(CHROMOSOME_NAME.name(), chromosome.getName());
        makeDir(substitute(REF_CHROMOSOME_DIR, params));
        // makes a stream resource to write NT sequence corresponded to the given chromosome
        chromosome.setPath(substitute(REF_CHROMOSOME_SEQUENCE_FILE, params));

        LOGGER.debug(getMessage(MessagesConstants.DEBUG_FILE_OPENING, toRealPath(chromosome.getPath())));
        return new BlockCompressedDataOutputStream(new File(toRealPath(chromosome.getPath())));
    }

    /**
     * Creates a {@code BlockCompressedDataOutputStream} for writing reference genome's GC content for specified
     * reference ID and chromosome
     * @param referenceId ID of a reference genome to write GC for
     * @param chromosome chromosome to write GC for
     * @return a {@code BlockCompressedDataOutputStream} that allows writing of GC content of specified chromosome
     * for specified reference
     * @throws IOException
     */
    public BlockCompressedDataOutputStream makeGCOutputStream(final Long referenceId, final Chromosome chromosome)
            throws IOException {
        Assert.notNull(referenceId, getMessage(MessageCode.NO_SUCH_REFERENCE));
        Assert.isTrue(StringUtils.isNotBlank(chromosome.getName()),
                getMessage(MessagesConstants.ERROR_NO_CHROMOSOME_NAME));
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), referenceId);
        params.put(CHROMOSOME_NAME.name(), chromosome.getName());
        // makes a stream resource to write NT sequence corresponded to the given chromosome
        chromosome.setPath(substitute(CHROMOSOME_GC_CONTENT_FILE, params));
        LOGGER.debug(getMessage(MessagesConstants.DEBUG_FILE_OPENING), toRealPath(chromosome.getPath()));
        return new BlockCompressedDataOutputStream(new File(toRealPath(chromosome.getPath())));
    }

    /**
     * Creates a BufferedInputStream for a file with reference (.nib)
     *
     * @param referenceId {@code Long} represents ID of a reference in the system
     * @return {@code InputStream} a reader for a file with reference .nib
     * @throws FileNotFoundException if th file with reference(.nib) is missing
     */
    public BlockCompressedDataInputStream makeRefInputStream(final Long referenceId, final String chromosomeName)
            throws IOException {
        return makeBlockCompressedDataInputStream(referenceId, chromosomeName, REF_CHROMOSOME_SEQUENCE_FILE);
    }

    /**
     * Creates a {@code BlockCompressedDataInputStream} for reading GC content data of a chromosome, specified by
     * reference ID and chromosome name
     *
     * @param referenceId ID of a reference, from which to read GC content
     * @param chromosomeName name of a chromosome, from which to read GC content
     * @return a {@code BlockCompressedDataInputStream} for reading GC content data
     * @throws IOException
     */
    public BlockCompressedDataInputStream makeGCInputStream(final Long referenceId, final String chromosomeName)
            throws IOException {
        return makeBlockCompressedDataInputStream(referenceId, chromosomeName, CHROMOSOME_GC_CONTENT_FILE);
    }

    private DataOutputStream makeGCIndexOutputStream(final Long referenceId, final String chromosomeName)
            throws IOException {
        return makeDataOutputStream(referenceId, chromosomeName, CHROMOSOME_GC_CONTENT_INDEX_FILE);
    }

    private DataOutputStream makeRefIndexOutputStream(final Long referenceId, final String chromosomeName)
            throws IOException {
        return makeDataOutputStream(referenceId, chromosomeName, REF_CHROMOSOME_SEQUENCE_INDEX_FILE);
    }

    /**
     * Creates a {@code DataInputStream} for reading GC content index of chromosome, specified by reference ID and
     * chromosome name
     *
     * @param referenceId ID of a reference, from which to read GC content index
     * @param chromosomeName name of a chromosome, from which to read GC content index
     * @return a {@code DataInputStream} for reading GC index
     * @throws IOException
     */
    public DataInputStream makeGCIndexInputStream(final Long referenceId, final String chromosomeName)
            throws IOException {
        return makeDataInputStream(referenceId, chromosomeName, CHROMOSOME_GC_CONTENT_INDEX_FILE);
    }

    /**
     * Creates a {@code DataInputStream} for reading reference index of chromosome, specified by reference ID and
     * chromosome name
     *
     * @param referenceId ID of a reference, from which to read GC content index
     * @param chromosomeName name of a chromosome, from which to read GC content index
     * @return a {@code DataInputStream} for reading GC index
     * @throws IOException
     */
    public DataInputStream makeRefIndexInputStream(final Long referenceId, final String chromosomeName)
            throws IOException {
        return makeDataInputStream(referenceId, chromosomeName, REF_CHROMOSOME_SEQUENCE_INDEX_FILE);
    }

    private DataInputStream makeDataInputStream(final Long referenceId, final String chromosomeName,
                                                FilePathFormat path) throws IOException {
        Assert.notNull(referenceId, getMessage(MessageCode.NO_SUCH_REFERENCE));
        final Map<String, Object> params = new HashMap<>();

        params.put(DIR_ID.name(), referenceId);
        params.put(CHROMOSOME_NAME.name(), chromosomeName);
        LOGGER.debug(getMessage(MessagesConstants.DEBUG_FILE_OPENING),
                     toRealPath(substitute(path, params)));
        return new DataInputStream(new GZIPInputStream(
                new FileInputStream(toRealPath(substitute(path, params)))));
    }

    private DataOutputStream makeDataOutputStream(final Long referenceId, final String chromosomeName,
                                                  FilePathFormat path) throws IOException {
        Assert.notNull(referenceId, getMessage(MessageCode.NO_SUCH_REFERENCE));
        final Map<String, Object> params = new HashMap<>();

        params.put(DIR_ID.name(), referenceId);
        params.put(CHROMOSOME_NAME.name(), chromosomeName);
        LOGGER.debug(getMessage(MessagesConstants.DEBUG_FILE_OPENING),
                     toRealPath(substitute(path, params)));
        return new DataOutputStream(new GZIPOutputStream(
                new FileOutputStream(toRealPath(substitute(path, params)))));
    }

    private BlockCompressedDataInputStream makeBlockCompressedDataInputStream(final Long referenceId,
                                                                              final String chromosomeName,
                                                                              FilePathFormat path) throws IOException {
        Assert.notNull(referenceId, getMessage(MessageCode.NO_SUCH_REFERENCE));

        final Map<String, Object> params = new HashMap<>();

        params.put(DIR_ID.name(), referenceId);
        params.put(CHROMOSOME_NAME.name(), chromosomeName);

        LOGGER.debug(getMessage(MessagesConstants.DEBUG_FILE_OPENING),
                     toRealPath(substitute(path, params)));
        final File file = new File(toRealPath(substitute(path, params)));
        Assert.isTrue(file.exists(), getMessage(MessagesConstants.ERROR_NO_SUCH_FILE, referenceId, chromosomeName));
        return new BlockCompressedDataInputStream(file);
    }


    /**
     * Returns a reference on {@code File}, used to store cytobands data that corresponds to the
     * given chromosome.
     *
     * @param chromosome @code Chromosome} represents a container that provides access to major properties
     *                   and can be updated by metadata produced as the result of the current call
     * @return {@code File} represents a reference on a file used to manage cytobands for a chromosome
     * in *.txt format
     */
    public File makeCytobandsFile(final Chromosome chromosome) {
        Assert.notNull(chromosome.getReferenceId(), getMessage(MessageCode.NO_SUCH_REFERENCE));
        Assert.isTrue(StringUtils.isNotBlank(chromosome.getName()), getMessage(MessageCode.NO_CHROMOSOME_NAME));
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), chromosome.getReferenceId());
        params.put(CHROMOSOME_NAME.name(), chromosome.getName());
        return new File(toRealPath(substitute(REF_CHROMOSOME_CYTOBAND_FILE, params)));
    }

    /**
     * Create a VCFFile reader for given reference and fileName in application's file system, optionally uses index.
     *
     * @param vcfFile  {@code VcfFile} a VcfFile object, representing a file in the system with set id, name and
     *                 compressed options.
     * @return {@code VCFFileReader} a reader to work with VCF file
     */
    public FeatureReader<VariantContext> makeVcfReader(VcfFile vcfFile) {
        File file = new File(vcfFile.getPath());
        Assert.isTrue(file.exists(), getMessage(MessagesConstants.ERROR_FILE_NOT_FOUND, vcfFile.getPath()));

        FeatureReader<VariantContext> reader;
        double time1;
        double time2;
        if (vcfFile.getIndex() != null && StringUtils.isNotBlank(vcfFile.getIndex().getPath())) {
            Assert.notNull(vcfFile.getIndex(), "VCF file should have an index");
            File indexFile = new File(vcfFile.getIndex().getPath());
            Assert.isTrue(indexFile.exists(), getMessage(MessagesConstants.ERROR_FILE_NOT_FOUND,
                                                         vcfFile.getIndex().getPath()));
            time1 = Utils.getSystemTimeMilliseconds();
            reader = AbstractFeatureReader.getFeatureReader(vcfFile.getPath(), vcfFile.getIndex().getPath(),
                                                            new VCFCodec(), true);
            time2 = Utils.getSystemTimeMilliseconds();
        } else {
            time1 = Utils.getSystemTimeMilliseconds();
            reader = AbstractFeatureReader.getFeatureReader(vcfFile.getPath(), new VCFCodec(), false);
            time2 = Utils.getSystemTimeMilliseconds();
        }
        LOGGER.debug(getMessage(MessagesConstants.DEBUG_FILE_OPENING, vcfFile.getPath(), time2 - time1));
        return reader;
    }

    /**
     * Creates a Tribble index for an uncompressed VCF file and a Tabix index for a compressed one. Writes it
     * to .idx or .tbi file
     *
     * @param vcfFile {@code VcfFile} a VcfFile object, representing a file in the system with set id, name and
     *                compressed options
     * @param userId  {@code Long} a user for whom file was saved.
     * @throws IOException
     */
    public void makeVcfIndex(VcfFile vcfFile, Long userId) throws IOException {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), vcfFile.getId());
        params.put(FILE_NAME.name(), vcfFile.getCompressed() ? VcfFileNames.VCF_COMPRESSED_FILE_NAME.getName() :
                VcfFileNames.VCF_FILE_NAME.getName());
        params.put(USER_ID.name(), userId);

        File file = new File(vcfFile.getPath());
        VCFCodec codec = new VCFCodec();
        File indexFile;

        if (vcfFile.getCompressed()) {
            indexFile = new File(toRealPath(substitute(VCF_COMPRESSED_INDEX, params)));
            LOGGER.info(getMessage(MessagesConstants.INFO_VCF_INDEX_WRITING, indexFile.getAbsolutePath()));
            TabixIndex index = IndexUtils.createTabixIndex(file, codec, TabixFormat.VCF);
            index.write(indexFile);
        } else {
            indexFile = new File(toRealPath(substitute(VCF_INDEX, params)));
            LOGGER.info(getMessage(MessagesConstants.INFO_VCF_INDEX_WRITING, indexFile.getAbsolutePath()));
            IntervalTreeIndex intervalTreeIndex = IndexFactory.createIntervalIndex(file, codec); // Create an index
            IndexFactory.writeIndex(intervalTreeIndex, indexFile); // Write it to a file
        }

        BiologicalDataItem indexItem = new BiologicalDataItem();
        indexItem.setCreatedDate(new Date());
        indexItem.setPath(indexFile.getAbsolutePath());
        indexItem.setFormat(BiologicalDataItemFormat.VCF_INDEX);
        indexItem.setType(BiologicalDataItemResourceType.FILE);
        indexItem.setName("");
        indexItem.setCreatedBy(AuthUtils.getCurrentUserId());

        vcfFile.setIndex(indexItem);
    }

    /**
     * Saves metadata to .bounds file to store start indexes of first and last feature for each chromosome
     *
     * @param featureFile a fileId in the system
     * @param metaData    {@code Map&lt;String, Pair&lt;Integer, Integer&gt;&gt;} map of start indexes of first
     *                    and last feature of each chromosome.
     * @throws IOException
     */
    public void makeIndexMetadata(final FeatureFile featureFile, final Map<String, Pair<Integer, Integer>>
            metaData)
            throws IOException {
        LOGGER.info(getMessage(MessagesConstants.INFO_BOUNDS_METADATA_WRITE, featureFile.getId(),
                               featureFile.getName()));

        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), featureFile.getId());
        params.put(USER_ID.name(), featureFile.getCreatedBy());

        FilePathFormat filePathFormat = null;
        if (featureFile instanceof VcfFile) {
            filePathFormat = VCF_METADATA_FILE;
        }
        if (featureFile instanceof GeneFile) {
            filePathFormat = GENE_METADATA_FILE;
        }
        if (filePathFormat == null) {
            throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_FEATURE_FILE_TYPE,
                    featureFile.getClass().getName()));
        }

        File file = new File(toRealPath(substitute(filePathFormat, params)));
        if (!file.createNewFile()) {
            LOGGER.info(getMessage(MessagesConstants.INFO_FILES_STATUS_ALREADY_EXISTS,
                                   substitute(filePathFormat, params)));
        }

        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file))) {
            for (Map.Entry<String, Pair<Integer, Integer>> entry : metaData.entrySet()) {
                dataOutputStream.writeUTF(entry.getKey());
                dataOutputStream.writeInt(entry.getValue().getLeft());
                dataOutputStream.writeInt(entry.getValue().getRight());
            }
        }
    }

    /**
     * Loads VCF metadata from .bounds file
     *
     * @param featureFile a fileId in the system
     * @return {@code Map&lt;String, Pair&lt;Integer, Integer&gt;&gt;} map of start indexes of first and last variation
     * of each chromosome
     * @throws IOException
     */
    public Map<String, Pair<Integer, Integer>> loadIndexMetadata(FeatureFile featureFile) throws IOException {
        LOGGER.info(getMessage(MessagesConstants.INFO_BOUNDS_METADATA_LOAD, featureFile.getId(),
                               featureFile.getName()));

        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), featureFile.getId());
        params.put(USER_ID.name(), featureFile.getCreatedBy());

        FilePathFormat filePathFormat = null;
        if (featureFile instanceof VcfFile) {
            filePathFormat = VCF_METADATA_FILE;
        }
        if (featureFile instanceof GeneFile) {
            filePathFormat = GENE_METADATA_FILE;
        }
        if (filePathFormat == null) {
            throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_FEATURE_FILE_TYPE,
                                                          featureFile.getClass().getName()));
        }

        Map<String, Pair<Integer, Integer>> metaMap = new HashMap<>();
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(toRealPath(substitute(
            filePathFormat, params))))) {
            while (dataInputStream.available() > 0) {
                String chrId = dataInputStream.readUTF();
                int startPosition = dataInputStream.readInt();
                int endPosition = dataInputStream.readInt();

                metaMap.put(chrId, new ImmutablePair<>(startPosition, endPosition));
            }
        }

        return metaMap;
    }

    /**
     * Creates a {@code SimpleFSDirectory} object, representing existing Lucene index directory for feature index
     * for desired project ID. Checks if that directory exists
     *
     * @param projectId     an ID of a project, which feature index directory to fetch
     * @return an {@code SimpleFSDirectory} object, representing Lucene index directory for feature index
     * @throws IOException
     */
    public SimpleFSDirectory getIndexForProject(final long projectId) throws IOException {
        final Map<String, Object> params = new HashMap<>();
        params.put(PROJECT_ID.name(), projectId);

        File file = new File(toRealPath(substitute(PROJECT_FEATURE_INDEX_FILE, params)));
        Assert.isTrue(file.exists(), getMessage(MessagesConstants.ERROR_PROJECT_FEATURE_INDEX_NOT_FOUND, projectId));

        return new SimpleFSDirectory(file.toPath());
    }

    public SimpleFSDirectory[] getIndexesForFiles(final List<? extends FeatureFile> featureFiles) throws IOException {
        if (CollectionUtils.isEmpty(featureFiles)) {
            return null;
        }

        List<SimpleFSDirectory> indexes = new ArrayList<>();
        try {
            for (int i = 0; i < featureFiles.size(); i++) {
                FeatureFile featureFile = featureFiles.get(i);

                final Map<String, Object> params = new HashMap<>();
                params.put(USER_ID.name(), featureFile.getCreatedBy());
                params.put(DIR_ID.name(), featureFile.getId());

                FilePathFormat format = determineFilePathFormat(featureFile);

                params.put(FEATURE_FILE_DIR.name(), substitute(format, params));
                File file = new File(toRealPath(substitute(FEATURE_INDEX_DIR, params)));
                if (file.exists()) {
                    indexes.add(new SimpleFSDirectory(file.toPath()));
                }
            }
        } catch (IOException e) {
            for (SimpleFSDirectory index : indexes) {
                if (index != null) {
                    IOUtils.closeQuietly(index);
                }
            }

            throw e;
        }

        Assert.isTrue(!indexes.isEmpty(), getMessage(MessagesConstants.ERROR_FEATURE_INDEX_NOT_FOUND,
                                 featureFiles.stream().map(f -> f.getId().toString()).collect(Collectors.joining(", "))));

        return indexes.toArray(new SimpleFSDirectory[indexes.size()]);
    }

    /**
     * Creates a {@code SimpleFSDirectory} object, representing a new Lucene index directory for feature index for
     * desired project ID
     *
     * @param projectId     an ID of a project, which feature index directory to fetch
     * @return an {@code SimpleFSDirectory} object, representing Lucene index directory for feature index
     * @throws IOException if something is wrong with access to file system
     */
    public SimpleFSDirectory createIndexForProject(final long projectId) throws IOException {
        final Map<String, Object> params = new HashMap<>();
        params.put(PROJECT_ID.name(), projectId);

        File file = new File(toRealPath(substitute(PROJECT_FEATURE_INDEX_FILE, params)));

        return new SimpleFSDirectory(file.toPath());
    }

    /**
     * Creates index for a FeatureFile
     * @param featureFile a file to create index for
     * @return an index, represented by {@code SimpleFSDirectory} object
     * @throws IOException if something is wrong with access to file system
     */
    public SimpleFSDirectory createIndexForFile(FeatureFile featureFile) throws IOException {
        final Map<String, Object> params = new HashMap<>();
        params.put(USER_ID.name(), featureFile.getCreatedBy());
        params.put(DIR_ID.name(), featureFile.getId());

        FilePathFormat format = determineFilePathFormat(featureFile);

        params.put(FEATURE_FILE_DIR.name(), substitute(format, params));
        File file = new File(toRealPath(substitute(FEATURE_INDEX_DIR, params)));

        return new SimpleFSDirectory(file.toPath());
    }

    /**
     * Deletes feature index of a FeatureFile
     * @param featureFile a file to delete index
     * @throws IOException if something is wrong with access to file system
     */
    public void deleteFileFeatureIndex(FeatureFile featureFile) throws IOException {
        final Map<String, Object> params = new HashMap<>();
        params.put(USER_ID.name(), featureFile.getCreatedBy());
        params.put(DIR_ID.name(), featureFile.getId());

        FilePathFormat format = determineFilePathFormat(featureFile);

        params.put(FEATURE_FILE_DIR.name(), substitute(format, params));
        File dir = new File(toRealPath(substitute(FEATURE_INDEX_DIR, params)));

        if (dir.exists()) {
            deleteDir(substitute(FEATURE_INDEX_DIR, params));
        }
    }

    /**
     * Checks if Lucene index directory for desired project ID exists
     *
     * @param projectId ID of a project to check
     * @return {@code true} if index directory exists, {@code false} if not
     */
    public boolean indexForProjectExists(final long projectId) {
        final Map<String, Object> params = new HashMap<>();
        params.put(PROJECT_ID.name(), projectId);

        File file = new File(toRealPath(substitute(PROJECT_FEATURE_INDEX_FILE, params)));
        return file.exists();
    }

    /**
     * Removes a directory for selected project
     *
     * @param project a {@code Project} to delete
     * @throws IOException if something is wrong with access to file system
     */
    public void deleteProjectDirectory(Project project) throws IOException {
        final Map<String, Object> params = new HashMap<>();
        params.put(PROJECT_ID.name(), project.getId());

        File dir = new File(toRealPath(substitute(PROJECT_DIR, params)));
        if (dir.exists()) {
            deleteDir(substitute(PROJECT_DIR, params));
        }
    }

    /**
     * Saves a histogram as a {@code List} of {@code Wig} objects for a specified {@code FeatureFile} and
     * under a chromosome name
     *
     * @param featureFile    a {@code FeatureFile} to save histogram for
     * @param chromosomeName {@code String} a name of a chromosome to save histogram for
     * @param histogram      a histogram as a {@code List} of {@code Wig} objects
     * @throws HistogramWritingException
     */
    public void writeHistogram(final FeatureFile featureFile, final String chromosomeName, final List<Wig>
            histogram)
        throws IOException {
        LOGGER.info(getMessage(MessagesConstants.INFO_HISTOGRAM_WRITE, featureFile.getId(),
                               featureFile.getName()));

        try (DataOutputStream dataOutputStream = makeHistogramOutputStream(featureFile, chromosomeName)) {
            for (Wig wig : histogram) {
                dataOutputStream.writeInt(wig.getStartIndex());
                dataOutputStream.writeInt(wig.getEndIndex());
                dataOutputStream.writeFloat(wig.getValue());
            }
        }
    }

    private DataOutputStream makeHistogramOutputStream(FeatureFile featureFile, final String chromosomeName)
            throws IOException {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), featureFile.getId());
        params.put(USER_ID.name(), featureFile.getCreatedBy());

        FilePathFormat dirPathFormat = null;
        FilePathFormat filePathFormat = null;
        if (featureFile instanceof VcfFile) {
            dirPathFormat = VCF_HISTOGRAM_DIR;
            filePathFormat = VCF_HISTOGRAM_FILE;
        }
        if (featureFile instanceof GeneFile) {
            dirPathFormat = GENE_HISTOGRAM_DIR;
            filePathFormat = GENE_HISTOGRAM_FILE;
        }
        if (featureFile instanceof BedFile) {
            dirPathFormat = BED_HISTOGRAM_DIR;
            filePathFormat = BED_HISTOGRAM_FILE;
        }
        if (dirPathFormat == null) {
            throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_FEATURE_FILE_TYPE,
                    featureFile.getClass().getName()));
        }

        makeDir(substitute(dirPathFormat, params));

        params.put(CHROMOSOME_NAME.name(), chromosomeName);
        File histogramFile = new File(toRealPath(substitute(filePathFormat, params)));
        Assert.isTrue(histogramFile.createNewFile(), "Can't create histogram file " + histogramFile.getAbsolutePath());

        return new DataOutputStream(new FileOutputStream(histogramFile));
    }

    private FilePathFormat getHistogramDirPathFormat(final FeatureFile featureFile) {
        FilePathFormat dirPathFormat = null;
        if (featureFile instanceof VcfFile) {
            dirPathFormat = VCF_HISTOGRAM_DIR;
        }
        if (featureFile instanceof GeneFile) {
            dirPathFormat = GENE_HISTOGRAM_DIR;
        }
        if (featureFile instanceof BedFile) {
            dirPathFormat = BED_HISTOGRAM_DIR;
        }
        if (dirPathFormat == null) {
            throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_FEATURE_FILE_TYPE,
                                                          featureFile.getClass().getName()));
        }

        return dirPathFormat;
    }

    private FilePathFormat getHistogramFilePathFormat(final FeatureFile featureFile) {
        FilePathFormat filePathFormat = null;
        if (featureFile instanceof VcfFile) {
            filePathFormat = VCF_HISTOGRAM_FILE;
        }
        if (featureFile instanceof GeneFile) {
            filePathFormat = GENE_HISTOGRAM_FILE;
        }
        if (featureFile instanceof BedFile) {
            filePathFormat = BED_HISTOGRAM_FILE;
        }
        if (filePathFormat == null) {
            throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_FEATURE_FILE_TYPE,
                                                          featureFile.getClass().getName()));
        }

        return filePathFormat;
    }

    /**
     * Loads histogram as a {@code List} of {@code Wig} objects for a specified {@code FeatureFile} and chromosome name
     *
     * @param featureFile    a {@code FeatureFile} to load histogram for
     * @param chromosomeName {@code String} a name of a chromosome to load histogram for
     * @return a histogram as a {@code List} of {@code Wig} objects
     * @throws IOException
     */
    public List<Wig> loadHistogram(final FeatureFile featureFile, final String chromosomeName) throws IOException {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), featureFile.getId());
        params.put(USER_ID.name(), featureFile.getCreatedBy());
        params.put(CHROMOSOME_NAME.name(), chromosomeName);

        FilePathFormat filePathFormat = getHistogramFilePathFormat(featureFile);
        File histogramFile = new File(toRealPath(substitute(filePathFormat, params)));

        List<Wig> histogram = new ArrayList<>();
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(histogramFile))) {
            while (dataInputStream.available() > 0) {
                Wig wig = new Wig();

                wig.setStartIndex(dataInputStream.readInt());
                wig.setEndIndex(dataInputStream.readInt());
                wig.setValue(dataInputStream.readFloat());

                histogram.add(wig);
            }
        }
        return histogram;
    }

    /**
     * Checks if histogram exists for a specified FeatureFile and chromosome
     *
     * @param featureFile a FeatureFile, for which to check histogram existence
     * @param chromosomeName a name of a chromosome, for which to check histogram existence
     * @return tru if histogram exists
     */
    public boolean checkHistogramExists(final FeatureFile featureFile, final String chromosomeName) {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), featureFile.getId());
        params.put(USER_ID.name(), featureFile.getCreatedBy());

        FilePathFormat dirPathFormat = getHistogramDirPathFormat(featureFile);
        File histogramDir = new File(toRealPath(substitute(dirPathFormat, params)));

        LOGGER.debug("Loading histogram from {}", histogramDir.getAbsolutePath());
        if (!histogramDir.exists()) {
            LOGGER.error("Histogram directory {} does no exist", histogramDir.getAbsolutePath());
            return false;
        }

        FilePathFormat filePathFormat = getHistogramFilePathFormat(featureFile);
        params.put(CHROMOSOME_NAME.name(), chromosomeName);
        File histogramFile = new File(toRealPath(substitute(filePathFormat, params)));

        if (!histogramFile.exists()) {
            LOGGER.error("Histogram file {} does no exist", histogramFile.getAbsolutePath());
            return false;
        }

        return true;
    }

    /**
     * Create a {@code AbstractFeatureReader&lt;GeneFeature, LineIterator&gt;} reader for given file, optionally
     * uses an index
     *
     * @param file      a gene {@code File} with .gff, .gtf, .gff.gz or .gtf.gz extensions
     * @param indexFile {@code File} an index file whit .tbi extension, set null if no index should be used
     * @param useIndex  {@code boolean} determines if index should be used
     * @return {@code AbstractFeatureReader&lt;GeneFeature, LineIterator&gt;} a reader to work with gene file
     */
    public AbstractFeatureReader<GeneFeature, LineIterator> makeGeneReader(File file, File indexFile,
                                                                           boolean useIndex) {
        String extension = getGeneFileExtension(file.getName());
        Assert.notNull(extension, getMessage(MessagesConstants.ERROR_UNSUPPORTED_GENE_FILE_EXTESION));

        String indexPath = null;
        if (useIndex && indexFile != null) {
            indexPath = indexFile.getAbsolutePath();
        }

        AsciiFeatureCodec<GeneFeature> codec = new GffCodec(GffCodec.GffType.forExt(extension));
        return AbstractFeatureReader.getFeatureReader(file.getAbsolutePath(), indexPath, codec, useIndex);
    }

    /**
     * Create a {@code AbstractFeatureReader&lt;GeneFeature, LineIterator&gt;} reader for given {@code GeneFile}
     * object, uses an index
     *
     * @param geneFile a {@code GeneFile} instance, representing file in the system
     * @return {@code AbstractFeatureReader&lt;GeneFeature, LineIterator&gt;} a reader to work with gene file
     */
    public AbstractFeatureReader<GeneFeature, LineIterator> makeGeneReader(final GeneFile geneFile,
                                                                           final GeneFileType type) {
        String realFileName = geneFile.getPath() != null ? geneFile.getPath() : geneFile.getName();
        String extension = Utils.getFileExtension(realFileName);
        extension = GffCodec.GffType.forExt(extension).getExtensions()[0];

        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), geneFile.getId());
        params.put(USER_ID.name(), geneFile.getCreatedBy());
        params.put(GENE_EXTENSION.name(), extension);

        Assert.notNull(extension, getMessage(MessagesConstants.ERROR_UNSUPPORTED_GENE_FILE_EXTESION));
        Assert.notNull(geneFile.getIndex(), "Gene file should have an index");

        File file;
        File indexFile;

        switch (type) {
            case ORIGINAL:
                file = new File(geneFile.getPath());
                indexFile = new File(geneFile.getIndex().getPath());
                break;
            case LARGE_SCALE:
                file = tryGetHelperGeneFile(GENE_LARGE_SCALE_FILE, geneFile, type, params);
                indexFile = tryGetHelperGeneIndex(GENE_LARGE_SCALE_INDEX, GENE_LARGE_SCALE_FILE, geneFile, type,
                                                  params);
                break;
            case TRANSCRIPT:
                file = tryGetHelperGeneFile(GENE_TRANSCRIPT_FILE, geneFile, type, params);
                indexFile = tryGetHelperGeneIndex(GENE_TRANSCRIPT_INDEX, GENE_TRANSCRIPT_FILE, geneFile, type,
                                                  params);
                break;
            default:
                throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_GENE_FILE_TYPE,
                                                              type));
        }

        return makeGeneReader(file, indexFile, true);
    }

    private File tryGetHelperGeneFile(FilePathFormat helperFormat, GeneFile geneFile, GeneFileType type,
                                      Map<String, Object> params) {
        File file = new File(toRealPath(substitute(helperFormat, params)));

        if (!file.exists()) {
            LOGGER.error(getMessage(MessagesConstants.ERROR_HELPER_FILE_DOES_NOT_EXIST, type, geneFile.getName()));
            file = new File(geneFile.getPath());
        }

        return file;
    }

    private File tryGetHelperGeneIndex(FilePathFormat helperIndexFormat, FilePathFormat helperFormat, GeneFile geneFile,
                                       GeneFileType type, Map<String, Object> params) {
        File file = new File(toRealPath(substitute(helperFormat, params)));

        if (!file.exists()) {
            LOGGER.error(getMessage(MessagesConstants.ERROR_HELPER_FILE_DOES_NOT_EXIST, type, geneFile.getName()));
            return new File(geneFile.getIndex().getPath());
        } else {
            return new File(toRealPath(substitute(helperIndexFormat, params)));
        }
    }

    /**
     * Checks that gene file of specified GeneFileType exists
     *
     * @param geneFile GeneFile to check
     * @param geneFileType GeneFileType of gene file to check
     * @return true if gene file of specified GeneFileType exists
     */
    public boolean checkGeneFileExists(GeneFile geneFile, GeneFileType geneFileType) {
        String realFileName = geneFile.getPath() != null ? geneFile.getPath() : geneFile.getName();
        String extension = Utils.getFileExtension(realFileName);
        extension = GffCodec.GffType.forExt(extension).getExtensions()[0];

        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), geneFile.getId());
        params.put(USER_ID.name(), geneFile.getCreatedBy());
        params.put(GENE_EXTENSION.name(), extension);

        File file;
        switch (geneFileType) {
            case ORIGINAL:
                file = new File(geneFile.getPath());
                break;
            case LARGE_SCALE:
                file = new File(toRealPath(substitute(GENE_LARGE_SCALE_FILE, params)));
                break;
            case TRANSCRIPT:
                file = new File(toRealPath(substitute(GENE_TRANSCRIPT_FILE, params)));
                break;
            default:
                return false;
        }

        return file.exists();
    }

    /**
     * Creates a {@code BufferedWriter} to write genes to file, determined by a {@code GeneFile} object
     *
     * @param geneFeatureClass {@code Class&lt;? extends GeneFeature&gt;} defines GeneFeature type that will be
     *                         written, and therefore, gene file extension.
     * @param geneFile         {@code GeneFile} that represents a file in the system
     * @return {@code BufferedWriter} to write genes
     * @throws IOException
     */
    public BufferedWriter makeGenesFileWriter(Class<? extends GeneFeature> geneFeatureClass, GeneFile geneFile,
                                              GeneFileType type) throws IOException {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), geneFile.getId());
        params.put(USER_ID.name(), geneFile.getCreatedBy());

        String extension = getGeneFileExtension(geneFeatureClass, geneFile);

        params.put(GENE_EXTENSION.name(), extension);
        File file = createGeneFileByType(type, params);

        if (type.equals(GeneFileType.ORIGINAL)) {
            geneFile.setPath(file.getAbsolutePath());
        }

        return geneFile.getCompressed() ?
                new BufferedWriter(new OutputStreamWriter(new BlockCompressedOutputStream(file),
                        Charset.defaultCharset())) :
                new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.defaultCharset()));
    }

    /**
     * Creates a {@code BlockCompressedOutputStream} to write gene file of specified GeneFileType
     * @param gffType a type of gene file
     * @param geneFile a GeneFile, for which data to write
     * @param type a GeneFileType of helper file to create
     * @return a {@code BlockCompressedOutputStream} to write gene file of specified GeneFileType
     * @throws FileNotFoundException
     */
    public BlockCompressedOutputStream makeGeneBlockCompressedOutputStream(
        GffCodec.GffType gffType, GeneFile geneFile, GeneFileType type)
        throws FileNotFoundException {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), geneFile.getId());
        params.put(USER_ID.name(), geneFile.getCreatedBy());

        String extension = gffType.getExtensions()[0];

        params.put(GENE_EXTENSION.name(), extension);
        File file = createGeneFileByType(type, params);

        if (type.equals(GeneFileType.ORIGINAL)) {
            geneFile.setPath(file.getAbsolutePath());
        }

        return new BlockCompressedOutputStream(file);
    }

    /**
     * Creates a {@code PositionalOutputStream} to write gene file of specified GeneFileType
     * @param gffType a type of gene file
     * @param geneFile a GeneFile, for which data to write
     * @param type a GeneFileType of helper file to create
     * @return a {@code PositionalOutputStream} to write gene file of specified GeneFileType
     * @throws FileNotFoundException
     */
    public PositionalOutputStream makePositionalOutputStream(GffCodec.GffType gffType,
                                         GeneFile geneFile, GeneFileType type) throws FileNotFoundException {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), geneFile.getId());
        params.put(USER_ID.name(), geneFile.getCreatedBy());

        String extension = gffType.getExtensions()[0];

        params.put(GENE_EXTENSION.name(), extension);
        File file = createGeneFileByType(type, params);

        if (type.equals(GeneFileType.ORIGINAL)) {
            geneFile.setPath(file.getAbsolutePath());
        }

        return new PositionalOutputStream(new FileOutputStream(file));
    }

    /**
     * Cleans up helper files for a specified GeneFile, for example, if they are empty
     * @param geneFile a GeneFile, for which helper files to delete
     * @param geneFeatureClass class of a GeneFeature, that specified GeneFIle contains
     * @param geneFileType type of helper file to delete
     * @throws IOException
     */
    public void deleteGeneHelperFile(final GeneFile geneFile, Class<? extends GeneFeature> geneFeatureClass,
                                     final GeneFileType geneFileType) throws IOException {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), geneFile.getId());
        params.put(USER_ID.name(), geneFile.getCreatedBy());

        String extension = getGeneFileExtension(geneFeatureClass, geneFile);

        params.put(GENE_EXTENSION.name(), extension);
        File file;
        File indexFile;
        switch (geneFileType) {
            case LARGE_SCALE:
                file = new File(toRealPath(substitute(GENE_LARGE_SCALE_FILE, params)));
                indexFile = new File(toRealPath(substitute(GENE_LARGE_SCALE_INDEX, params)));
                break;
            case TRANSCRIPT:
                file = new File(toRealPath(substitute(GENE_TRANSCRIPT_FILE, params)));
                indexFile = new File(toRealPath(substitute(GENE_TRANSCRIPT_INDEX, params)));
                break;
            default:
                throw new IllegalArgumentException("Unsupported Gene helper file type: " + geneFileType.name());
        }

        boolean deleted = file.delete();
        deleted = deleted && indexFile.delete();
        if (!deleted) {
            throw new IOException("Could not delete helper files for GeneFile: " + geneFile.getPath());
        }
    }

    /**
     * Creates an index for reference file
     *
     * @param referenceId    {@code Long} represents ID of a reference in the system
     * @param chromosomeName {@code String} represents a container that provides access to major properties
     *                       and can be updated by metadata produced as the result of the current call
     */
    public void makeNibIndex(final Long referenceId, final String chromosomeName) throws IOException {
        try (BlockCompressedDataInputStream streamGC = makeGCInputStream(referenceId, chromosomeName);
             DataOutputStream indexStream = makeGCIndexOutputStream(referenceId, chromosomeName)) {
            fillSimpleIndexFile(streamGC, indexStream);
        }
        try (BlockCompressedDataInputStream refStream = makeRefInputStream(referenceId, chromosomeName);
             DataOutputStream indexStream = makeRefIndexOutputStream(referenceId, chromosomeName)) {
            fillSimpleIndexFile(refStream, indexStream);
        }
    }

    /**
     * Creates an index for gene file, determined by a {@code GeneFile} object
     *
     * @param geneFeatureClass {@code Class&lt;? extends GeneFeature&gt;} defines gene file type, that will be indexed
     * @param geneFile         {@code GeneFile} that represents a file in the system
     */
    public void makeGeneIndex(Class<? extends GeneFeature> geneFeatureClass, final GeneFile geneFile, final GeneFileType
            type) throws IOException {
        GffCodec.GffType gffType = GffCodec.GffType.forGeneFile(geneFeatureClass, geneFile);

        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), geneFile.getId());
        params.put(USER_ID.name(), geneFile.getCreatedBy());
        params.put(GENE_EXTENSION.name(), gffType.getExtensions()[0]);

        File file;
        File indexFile;
        switch (type) {
            case ORIGINAL:
                file = new File(geneFile.getPath());
                indexFile = new File(toRealPath(substitute(GENE_INDEX, params)));
                break;
            case LARGE_SCALE:
                file = new File(toRealPath(substitute(GENE_LARGE_SCALE_FILE, params)));
                indexFile = new File(toRealPath(substitute(GENE_LARGE_SCALE_INDEX, params)));
                break;
            case TRANSCRIPT:
                file = new File(toRealPath(substitute(GENE_TRANSCRIPT_FILE, params)));
                indexFile = new File(toRealPath(substitute(GENE_TRANSCRIPT_INDEX, params)));
                break;
            default:
                throw new UnsupportedGeneFileTypeException(type);
        }

        LOGGER.info(getMessage(MessagesConstants.INFO_GENE_INDEX_WRITING, indexFile.getAbsolutePath()));

        if (geneFile.getCompressed()) {
            createGeneCompressedIndex(indexFile, file, gffType);
        } else {
            AsciiFeatureCodec<GeneFeature> codec = new GffCodec(gffType);
            TabixIndex index = IndexUtils.createTabixIndex(file, codec, TabixFormat.GFF);
            index.write(indexFile);
        }

        if (type.equals(GeneFileType.ORIGINAL)) {
            BiologicalDataItem indexItem = new BiologicalDataItem();
            indexItem.setCreatedDate(new Date());
            indexItem.setPath(indexFile.getAbsolutePath());
            indexItem.setFormat(BiologicalDataItemFormat.GENE_INDEX);
            indexItem.setType(BiologicalDataItemResourceType.FILE);
            indexItem.setName("");
            indexItem.setCreatedBy(AuthUtils.getCurrentUserId());

            geneFile.setIndex(indexItem);
        }
    }

    private void createGeneCompressedIndex(File indexFile, File file, GffCodec.GffType gffType) throws IOException {
        AsciiFeatureCodec<GeneFeature> codec = new GffCodec(gffType);
        TabixIndexCreator indexCreator = new TabixIndexCreator(TabixFormat.GFF);

        try (
            BlockCompressedInputStream inputStream = new BlockCompressedInputStream(new FileInputStream(file));
            LittleEndianOutputStream outputStream = new LittleEndianOutputStream(
                new BlockCompressedOutputStream(indexFile))
        ) {
            long p = 0;
            String line = inputStream.readLine();

            while (line != null) {
                //add the feature to the index
                GeneFeature decode = codec.decode(line);
                if (decode != null) {
                    indexCreator.addFeature(decode, p);
                }
                // read the next line if available
                p = inputStream.getFilePointer();
                line = inputStream.readLine();
            }

            // write the index to a file
            Index index = indexCreator.finalizeIndex(p);
            // VERY important! either use write based on input file or pass the little endian a BGZF stream
            index.write(outputStream);
        }
    }

    /**
     * Creates File object for specified GeneFile of specified GeneFileType
     * @param geneFile GeneFile, for which to create index File
     * @param type GeneFileType, of which index File to create
     * @return a File object for specified GeneFile of specified GeneFileType
     */
    public File makeFileForGeneIndex(final GeneFile geneFile, final GeneFileType
            type) {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), geneFile.getId());
        params.put(USER_ID.name(), geneFile.getCreatedBy());

        File indexFile;
        switch (type) {
            case ORIGINAL:
                indexFile = new File(toRealPath(substitute(GENE_INDEX, params)));
                break;
            case LARGE_SCALE:
                indexFile = new File(toRealPath(substitute(GENE_LARGE_SCALE_INDEX, params)));
                break;
            case TRANSCRIPT:
                indexFile = new File(toRealPath(substitute(GENE_TRANSCRIPT_INDEX, params)));
                break;
            default:
                throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_GENE_FILE_TYPE,
                                                              type));
        }

        return indexFile;
    }

    /**
     * Creates a reader of specified BedFile
     * @param bedFile a BedFile, from which reader to create
     * @return a reader of specified BedFile
     */
    public AbstractFeatureReader<NggbBedFeature, LineIterator> makeBedReader(final BedFile bedFile) {
        NggbBedCodec nggbBedCodec = new NggbBedCodec();
        return AbstractFeatureReader.getFeatureReader(bedFile.getPath(), bedFile.getIndex().getPath(),
                nggbBedCodec, true);
    }

    /**
     * Creates an index for a specified BedFile
     * @param bedFile BedFile to create index for
     */
    public void makeBedIndex(final BedFile bedFile) {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), bedFile.getId());
        params.put(USER_ID.name(), bedFile.getCreatedBy());

        File file = new File(bedFile.getPath());
        File indexFile = new File(toRealPath(substitute(BED_INDEX, params)));
        NggbBedCodec bedCodec = new NggbBedCodec();

        TabixIndex index = IndexUtils.createTabixIndex(file, bedCodec, TabixFormat.BED);
        index.write(indexFile);

        BiologicalDataItem indexItem = new BiologicalDataItem();
        indexItem.setCreatedDate(new Date());
        indexItem.setPath(indexFile.getAbsolutePath());
        indexItem.setFormat(BiologicalDataItemFormat.BED_INDEX);
        indexItem.setType(BiologicalDataItemResourceType.FILE);
        indexItem.setName("");
        indexItem.setCreatedBy(AuthUtils.getCurrentUserId());

        bedFile.setIndex(indexItem);
    }

    /**
     * Creates a reader for specified SegFile
     * @param segFile a SegFile to read
     * @return a reader for specified SegFile
     */
    public AbstractFeatureReader<SegFeature, LineIterator> makeSegReader(final SegFile segFile) {
        SegCodec segCodec = new SegCodec();
        if (segFile.getIndex() != null) {
            return AbstractFeatureReader.getFeatureReader(segFile.getPath(), segFile.getIndex().getPath(), segCodec,
                    true);
        } else {
            return AbstractFeatureReader.getFeatureReader(segFile.getPath(), segCodec, false);
        }
    }

    /**
     * Creates an index for a specified SegFile
     * @param segFile SegFile to create index for
     */
    public void makeSegIndex(final SegFile segFile) {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), segFile.getId());
        params.put(USER_ID.name(), segFile.getCreatedBy());

        File file = new File(segFile.getPath());
        File indexFile = new File(toRealPath(substitute(SEG_INDEX, params)));
        LOGGER.debug("Writing SEG index at {}", indexFile.getAbsolutePath());
        SegCodec segCodec = new SegCodec();

        TabixIndex index = IndexUtils.createTabixIndex(file, segCodec, SEG_TABIX_FORMAT);
        index.write(indexFile);

        BiologicalDataItem indexItem = new BiologicalDataItem();
        indexItem.setCreatedDate(new Date());
        indexItem.setPath(indexFile.getAbsolutePath());
        indexItem.setFormat(BiologicalDataItemFormat.SEG_INDEX);
        indexItem.setType(BiologicalDataItemResourceType.FILE);
        indexItem.setName("");
        indexItem.setCreatedBy(AuthUtils.getCurrentUserId());

        segFile.setIndex(indexItem);
    }

    /**
     * Creates a writer for a specified SegFile
     *
     * @param segFile a SegFile to create writer for
     * @return a SegFile to write
     * @throws IOException
     */
    public BufferedWriter makeSegFileWriter(SegFile segFile) throws IOException {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), segFile.getId());
        params.put(USER_ID.name(), segFile.getCreatedBy());

        File file = new File(toRealPath(substitute(SEG_FILE, params)));
        Assert.isTrue(file.createNewFile());

        LOGGER.debug("Writing SEG Sample file at {}", file.getAbsolutePath());

        segFile.setPath(file.getAbsolutePath());

        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.defaultCharset()));
    }

    /**
     * Creates a reader for specified MafFile
     * @param mafFile a MafFile to read
     * @return a reader for specified MafFile
     */
    public AbstractFeatureReader<MafFeature, LineIterator> makeMafReader(final MafFile mafFile) {
        MafCodec mafCodec = new MafCodec(mafFile.getPath());
        if (mafFile.getIndex() != null) {
            return AbstractFeatureReader.getFeatureReader(mafFile.getPath(), mafFile.getIndex().getPath(), mafCodec,
                    true);
        } else {
            return AbstractFeatureReader.getFeatureReader(mafFile.getPath(), mafCodec, false);
        }
    }

    /**
     * Creates an index for a specified MafFile
     * @param mafFile MafFile to create index for
     */
    public void makeMafIndex(final MafFile mafFile) throws IOException {
        makeMafIndex(mafFile, MAF_TABIX_FORMAT);
    }

    /**
     * Create a temporary index for a MAF file. Required for MAF files merging during registration
     *
     * @param file a MAF file
     * @param mafFile a MafFile object form database. This one will represent merged MAF file after registration
     * @throws IOException
     */
    public void makeMafTempIndex(File file, MafFile mafFile) throws IOException {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), mafFile.getId());
        params.put(USER_ID.name(), mafFile.getCreatedBy());

        File tempDir = new File(toRealPath(substitute(MAF_TEMP_DIR, params)));
        if (!tempDir.exists()) {
            makeMafTempDir(mafFile.getId(), mafFile.getCreatedBy());
        }

        params.put(FILE_NAME.name(), file.getName());

        File indexFile = new File(toRealPath(substitute(MAF_TEMP_INDEX, params)));
        LOGGER.debug("Writing temporary MAF index at {}", indexFile.getAbsolutePath());

        boolean compressed = file.getAbsoluteFile().getPath().endsWith(".gz");
        MafCodec codec = new MafCodec(file.getAbsolutePath());

        if (compressed) {
            makeTabixCompressedIndex(file, indexFile, codec, MAF_TABIX_FORMAT);
        } else {
            makeTabixIndex(file, indexFile, codec, MAF_TABIX_FORMAT);
        }
    }

    /**
     * Gets temporary MAF index file
     * @param file original MAF file
     * @param mafFile a MafFile object form database. This one will represent merged MAF file after registration
     * @return temporary MAF index file
     */
    public File getMafTempIndex(File file, MafFile mafFile) {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), mafFile.getId());
        params.put(USER_ID.name(), mafFile.getCreatedBy());
        params.put(FILE_NAME.name(), file.getName());

        return new File(toRealPath(substitute(MAF_TEMP_INDEX, params)));
    }

    /**
     * Creates an index for a specified MafFile, representing BigMaf file, merged form several MAF files
     * @param mafFile MafFile to create index for
     */
    public void makeBigMafIndex(final MafFile mafFile) throws IOException {
        makeMafIndex(mafFile, BIGMAF_TABIX_FORMAT);
    }

    private void makeMafIndex(final MafFile mafFile, final TabixFormat tabixFormat) throws IOException {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), mafFile.getId());
        params.put(USER_ID.name(), mafFile.getCreatedBy());

        File file = new File(mafFile.getPath());
        File indexFile = new File(toRealPath(substitute(MAF_INDEX, params)));
        LOGGER.debug("Writing MAF index at {}", indexFile.getAbsolutePath());

        if (mafFile.getCompressed()) {
            makeTabixCompressedIndex(file, indexFile, new MafCodec(mafFile.getPath()), tabixFormat);
        } else {
            makeTabixIndex(file, indexFile, new MafCodec(mafFile.getPath()), tabixFormat);
        }

        BiologicalDataItem indexItem = new BiologicalDataItem();
        indexItem.setCreatedDate(new Date());
        indexItem.setPath(indexFile.getAbsolutePath());
        indexItem.setFormat(BiologicalDataItemFormat.MAF_INDEX);
        indexItem.setType(BiologicalDataItemResourceType.FILE);
        indexItem.setName("");
        indexItem.setCreatedBy(AuthUtils.getCurrentUserId());

        mafFile.setIndex(indexItem);
    }

    /**
     * Creates a writer for a specified MafFile
     *
     * @param mafFile a MafFile to create writer for
     * @return a MafFile to write
     * @throws IOException
     */
    public BufferedWriter makeMafFileWriter(MafFile mafFile) throws IOException {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), mafFile.getId());
        params.put(USER_ID.name(), mafFile.getCreatedBy());

        File file = new File(toRealPath(substitute(MAF_FILE, params)));
        Assert.isTrue(file.createNewFile());

        LOGGER.debug("Writing MAF file at {}", file.getAbsolutePath());

        mafFile.setPath(file.getAbsolutePath());
        mafFile.setCompressed(true);

        return new BufferedWriter(new OutputStreamWriter(
                new BlockCompressedOutputStream(file), Charset.defaultCharset()));
    }

    /**
     * Writes list of WigSection objects to a BIGWIG file, specified by WigFile. Used for BIGWIG downsampling
     *
     * @param wigFile a WigFile to write into
     * @param wigSections a List of WigSection objects, representing BIGWIG records to write
     * @param chromSizes List of Pairs of chromosome sizes
     * @param chromosomeName a name of a chromosome, for which to write BIGWIG file
     * @throws IOException
     */
    public void writeToBigWigFile(WigFile wigFile, List<WigSection> wigSections, List<kotlin.Pair<String, Integer>>
            chromSizes, String chromosomeName) throws IOException {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), wigFile.getId());
        params.put(USER_ID.name(), wigFile.getCreatedBy());
        params.put(CHROMOSOME_NAME.name(), chromosomeName);

        File file = new File(toRealPath(substitute(WIG_FILE, params)));
        Assert.isTrue(file.createNewFile());

        BigWigFile.write(wigSections, chromSizes, file.toPath(), 0, CompressionType.DEFLATE, ByteOrder.nativeOrder());
    }

    /**
     * Gets path to a downsampled BIGWIG file, specified by WigFile and Chromosome
     *
     * @param wigFile a WigFile, for which to get downsampled BIGWIG file path
     * @param chromosome a Chromosome, for which to get downsampled BIGWIG file path
     * @return path to a downsampled BIGWIG file, specified by WigFile and Chromosome
     */
    public String getWigFilePath(WigFile wigFile, Chromosome chromosome) {
        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), wigFile.getId());
        params.put(USER_ID.name(), wigFile.getCreatedBy());
        params.put(CHROMOSOME_NAME.name(), chromosome.getName());

        File file = new File(toRealPath(substitute(WIG_FILE, params)));
        if (file.exists()) {
            return file.getAbsolutePath();
        } else {
            return null;
        }
    }

    /**
     * Deletes a directory, containing all the stuff, related to a feature file
     *
     * @param featureFile {@code FeatureFile}, whose directory should be deleted
     * @throws IOException
     */
    public void deleteFeatureFileDirectory(FeatureFile featureFile) throws IOException {
        FilePathFormat filePathFormat = determineFilePathFormat(featureFile);

        final Map<String, Object> params = new HashMap<>();
        params.put(DIR_ID.name(), featureFile.getId());
        params.put(USER_ID.name(), featureFile.getCreatedBy());

        File dir = new File(toRealPath(substitute(filePathFormat, params)));
        if (dir.exists()) {
            deleteDir(substitute(filePathFormat, params));
        }
    }

    private FilePathFormat determineFilePathFormat(FeatureFile featureFile) {
        FilePathFormat filePathFormat;
        switch (featureFile.getFormat()) {
            case VCF:
                filePathFormat = VCF_DIR;
                break;
            case GENE:
                filePathFormat = GENE_DIR;
                break;
            case SEG:
                filePathFormat = SEG_DIR;
                break;
            case MAF:
                filePathFormat = MAF_DIR;
                break;
            case BED:
                filePathFormat = BED_DIR;
                break;
            default:
                throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_FEATURE_FILE_TYPE,
                                                              featureFile.getFormat()));
        }

        return filePathFormat;
    }

    /**
     * Determines gene file extension
     *
     * @param fileName gene file name
     * @return gene extension
     */
    public static String getGeneFileExtension(final String fileName) {
        for (String e : GENE_FILE_EXTENSIONS) {
            if (fileName.endsWith(e)) {
                return e;
            }
        }

        return null;
    }

    private static String getGeneFileExtension(Class<? extends GeneFeature> geneFeatureClass, GeneFile geneFile) {
        return geneFeatureClass == GtfFeature.class ?
               (geneFile.getCompressed() ? GffCodec.GffType.COMPRESSED_GTF.getExtensions()[0] :
                GffCodec.GffType.GTF.getExtensions()[0]) :
               (geneFile.getCompressed() ? GffCodec.GffType.COMPRESSED_GFF.getExtensions()[0] :
                GffCodec.GffType.GFF.getExtensions()[0]);
    }

    /**
     * Deletes a directory by it's path, relative to application's contents root
     * @param relativePath a path, relative to application's contents root
     * @throws IOException
     */
    public void deleteDir(final String relativePath) throws IOException {
        final String realPath = toRealPath(relativePath);
        FileUtils.deleteDirectory(new File(realPath));
    }

    /**
     * Configures application's root directory
     */
    @PostConstruct
    public void configure() {
        makeDir("");
        makeDir(TMP_DIR.getPath());
        final Map<String, Object> params = new HashMap<>();
        params.put(USER_ID.name(), AuthUtils.getCurrentUserId());

        makeDir(substitute(USER_DIR, params));
    }

    private void makeTabixIndex(final File sourceFile, final File indexFile,
                               final AsciiFeatureCodec codec, final TabixFormat format) throws IOException {
        TabixIndex index = IndexFactory.createTabixIndex(sourceFile, codec, format, null);
        index.write(indexFile);
    }

    private void makeTabixCompressedIndex(final File sourceFile, final File indexFile, final AsciiFeatureCodec codec,
                                          final TabixFormat format) throws IOException {
        TabixIndexCreator indexCreator = new TabixIndexCreator(format);

        try (
            BlockCompressedInputStream inputStream = new BlockCompressedInputStream(
                new FileInputStream(sourceFile));
            LittleEndianOutputStream outputStream = new LittleEndianOutputStream(
                new BlockCompressedOutputStream(indexFile))
        ) {
            long p = 0;
            String line = inputStream.readLine();

            while (line != null) {
                //add the feature to the index
                Feature decode = codec.decode(line);
                if (decode != null) {
                    indexCreator.addFeature(decode, p);
                }
                // read the next line if available
                p = inputStream.getFilePointer();
                line = inputStream.readLine();
            }

            // write the index to a file
            Index index = indexCreator.finalizeIndex(p);
            // VERY important! either use write based on input file or pass the little endian a BGZF stream
            index.write(outputStream);
        }
    }

    private String toRealPath(final String relativePath) {
        return baseDirPath + relativePath;
    }

    @NotNull
    private File createGeneFileByType(GeneFileType type, Map<String, Object> params) {
        File file;
        switch (type) {
            case ORIGINAL:
                file = new File(toRealPath(substitute(GENE_FILE, params)));
                break;
            case LARGE_SCALE:
                file = new File(toRealPath(substitute(GENE_LARGE_SCALE_FILE, params)));
                break;
            case TRANSCRIPT:
                file = new File(toRealPath(substitute(GENE_TRANSCRIPT_FILE, params)));
                break;
            default:
                throw new UnsupportedGeneFileTypeException(type);
        }
        return file;
    }

    private void fillSimpleIndexFile(final BlockCompressedDataInputStream stream, final DataOutputStream indexStream)
            throws IOException {
        do {
            final long seekPos = stream.available();
            final long filePosition = stream.getFilePointer();
            indexStream.writeLong(filePosition);
            indexStream.writeLong(seekPos);
            stream.seek(filePosition + seekPos - 1);
            //it's need to get next block
            stream.read();
        } while (stream.available() != 0);
    }

    private File makeDir(final String relativePath) {
        final String realPath = toRealPath(relativePath);
        final File directory = new File(realPath);

        final boolean result = directory.exists() || directory.mkdirs();
        LOGGER.info(getMessage(MessagesConstants.INFO_FILES_STATUS_RESOURCE_AT_PATH, realPath, result));
        Assert.isTrue(result, getMessage(MessagesConstants.ERROR_FILES_MISSING_RESOURCE_AT_PATH, realPath));
        return directory;
    }

    private String substitute(final FilePathFormat fmt, final Map<String, Object> parameters) {
        return new StrSubstitutor(parameters).replace(fmt.getPath());
    }

    /**
     * Declares names of placeholders, that should be substitute by real values based on provided
     * metadata about file resource which should be read or written.
     */
    enum FilePathPlaceholder {
        ID,
        DIR_ID,
        USER_ID,
        PROJECT_ID,
        CHROMOSOME_NAME,
        FILE_NAME,
        GENE_EXTENSION,
        SAMPLE_NAME,
        FEATURE_FILE_DIR
    }

}
