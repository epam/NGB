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

package com.epam.catgenome.manager.bam;

import static com.epam.catgenome.component.MessageCode.RESOURCE_NOT_FOUND;
import static com.epam.catgenome.component.MessageHelper.getMessage;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.epam.catgenome.constant.Constants;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.bam.BamQueryOption;
import com.epam.catgenome.entity.bam.BamTrack;
import com.epam.catgenome.entity.bam.Read;
import com.epam.catgenome.entity.bucket.Bucket;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.manager.bam.handlers.Handler;
import com.epam.catgenome.manager.bucket.BucketManager;
import com.epam.catgenome.manager.reference.ReferenceManager;
import com.epam.catgenome.manager.reference.io.ChromosomeReferenceSequence;
import com.epam.catgenome.util.AuthUtils;
import com.epam.catgenome.util.BamUtil;
import com.epam.catgenome.util.ConsensusSequenceUtils;
import com.epam.catgenome.util.HdfsSeekableInputStream;
import com.epam.catgenome.util.Utils;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFlag;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.filter.AggregateFilter;
import htsjdk.samtools.filter.DuplicateReadFilter;
import htsjdk.samtools.filter.FailsVendorReadQualityFilter;
import htsjdk.samtools.filter.FilteringSamIterator;
import htsjdk.samtools.filter.NotPrimaryAlignmentFilter;
import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.filter.SecondaryOrSupplementaryFilter;
import htsjdk.samtools.util.CloseableIterator;

/**
 * Source:      BamHelper.java
 * Created:     12/4/2015
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code BamHelper} This component provides logic, connected with BAM file browsing and
 *  loading reads from the {@code BamFile}
 */
@Service
public class BamHelper {

    private static final Set<String> BAM_EXTENSIONS = new HashSet<>();

    static {
        BAM_EXTENSIONS.add(".bam");
        BAM_EXTENSIONS.add(".cram");
    }

    private static final Logger LOG = LoggerFactory.getLogger(BamHelper.class);

    @Autowired
    private BamFileManager bamFileManager;

    @Autowired
    private BucketManager bucketManager;

    @Autowired
    private ReferenceManager referenceManager;

    /**
     * Calculates the consensus sequence from the reads from a {@code BamFile}
     * @param track to load the consensus sequence
     * @param chromosome reference sequence
     * @return track with consensus sequence
     * @throws IOException if {@code BamFile} cannot be read
     */
    public Track<Sequence> calculateConsensusSequence(final Track<Sequence> track,
                                                      final Chromosome chromosome) throws IOException {
        Assert.notNull(track, getMessage(MessagesConstants.ERROR_INVALID_PARAM_TRACK_IS_NULL));
        Assert.notNull(chromosome, getMessage(MessagesConstants.ERROR_CHROMOSOME_ID_NOT_FOUND));
        final long start = System.currentTimeMillis();

        final BamFile bamFile = bamFileManager.loadBamFile(track.getId());
        Assert.notNull(bamFile, getMessage(MessagesConstants.ERROR_FILE_NOT_FOUND));
        ConsensusSequenceUtils.calculateConsensusSequence(track,
                getReadsFromFile(chromosome, track, bamFile, track.getBlocks()));

        final long end = System.currentTimeMillis();
        LOG.debug("Calculation of consensus sequence tooks " + (end - start) + "ms.");
        return track;
    }

    /**
     * Loads reads into the track from the file system
     * @param track to load reads
     * @param options track options
     * @return {@code BamTrack} filled with reads
     * @throws IOException if {@code BamFile} cannot be read
     */
    public BamTrack<Read> getReadsFromFile(final Track<Read> track, final BamQueryOption options)
            throws IOException {
        final BamTrack<Read> bamTrack = new BamTrack<>(track);
        final BamFile bamFile = bamFileManager.loadBamFile(bamTrack.getId());
        Assert.notNull(bamFile, getMessage(MessagesConstants.ERROR_FILE_NOT_FOUND));

        getReads(bamFile, bamTrack, options);
        return bamTrack;
    }

    /**
     * Loads reads into the track from the input URL
     * @param track to load reads
     * @param bamUrl {@code BamFile} URL
     * @param bamIndexUrl  {@code BamFile} index URL
     * @param options track options
     * @return {@code BamTrack} filled with reads
     * @throws IOException if {@code BamFile} cannot be read
     */
    public BamTrack<Read> getReadsFromUrl(final Track<Read> track, String bamUrl, String bamIndexUrl,
                                          final BamQueryOption options) throws IOException {
        final BamTrack<Read> bamTrack = new BamTrack<>(track);
        Assert.notNull(track.getChromosome().getReferenceId());
        final BamFile bamFile = makeUrlBamFile(bamUrl, bamIndexUrl, track.getChromosome().getReferenceId());

        getReads(bamFile, bamTrack, options);
        return bamTrack;
    }

    /**
     * Creates a temporary BamFile, not stored the database, and represented by it's file URL and index URL
     * @param bamUrl a URL string ,locating BAM file
     * @param bamIndexUrl a URL string ,locating BAI index file
     * @param referenceId - ID of the reference, for with this BAM is being browsed
     * @return
     */
    public BamFile makeUrlBamFile(String bamUrl, String bamIndexUrl, long referenceId) {
        final BamFile bamFile = new BamFile();

        bamFile.setReferenceId(referenceId);
        bamFile.setPath(bamUrl);
        bamFile.setType(BiologicalDataItemResourceType.getTypeFromPath(bamUrl));

        BiologicalDataItem index = new BiologicalDataItem();
        index.setPath(bamIndexUrl);
        index.setType(BiologicalDataItemResourceType.getTypeFromPath(bamIndexUrl));
        index.setFormat(BiologicalDataItemFormat.BAM_INDEX);
        bamFile.setIndex(index);

        return bamFile;
    }

    //for the case when the resource type is file
    protected void parseBamFile(final BamFile bamFile, List<Chromosome> chromosomes, Long id) throws IOException {
        try (SamReader reader = makeSamReader(bamFile, chromosomes, id)) {
            parseBam(reader);
        }
    }

    protected BamFile fillBamFile(final IndexedFileRegistrationRequest request) {
        final BiologicalDataItem indexItem = new BiologicalDataItem();
        final BamFile newBamFile = new BamFile();
        final String alternativeName = request.getName();

        newBamFile.setName(parseName(new File(request.getPath()).getName(), alternativeName, request.getType()));
        newBamFile.setType(request.getType());
        newBamFile.setFormat(BiologicalDataItemFormat.BAM);
        newBamFile.setCreatedBy(AuthUtils.getCurrentUserId());
        newBamFile.setReferenceId(request.getReferenceId());
        newBamFile.setCreatedDate(new Date());
        newBamFile.setPath(request.getPath());
        newBamFile.setBucketId(request.getS3BucketId());

        indexItem.setName("");
        indexItem.setType(request.getIndexType() == null ? request.getType() : request.getIndexType());
        indexItem.setFormat(BiologicalDataItemFormat.BAM_INDEX);
        indexItem.setCreatedBy(AuthUtils.getCurrentUserId());
        indexItem.setCreatedDate(new Date());
        indexItem.setBucketId(request.getIndexS3BucketId() == null ? request.getS3BucketId() : request.
                getIndexS3BucketId());
        indexItem.setPath(request.getIndexPath());
        newBamFile.setIndex(indexItem);

        return newBamFile;
    }

    protected String parseName(final String fileName, final String alternativeName,
            BiologicalDataItemResourceType type) {
        if (type == BiologicalDataItemResourceType.URL) {
            return defaultString(trimToNull(alternativeName), fileName);
        }

        boolean supported = false;
        for (final String ext : BAM_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                supported = true;
                break;
            }
        }
        if (!supported) {
            throw new IllegalArgumentException(getMessage("error.illegal.file.type", join(BAM_EXTENSIONS, ", ")));
        }
        return defaultString(trimToNull(alternativeName), fileName);
    }

    private void getReads(final BamFile bamFile, BamTrack<Read> bamTrack, final BamQueryOption options) throws
            IOException {
        Chromosome chromosome = bamTrack.getChromosome();
        try (SamReader reader = makeSamReader(bamFile, Collections.singletonList(chromosome),
                chromosome.getReferenceId())) {
            LOG.debug(getMessage(MessagesConstants.DEBUG_FILE_OPENING, bamFile.getPath()));
            String chromosomeName = options.getChromosomeName();

            if (reader.getFileHeader().getSequence(chromosomeName) == null) {
                chromosomeName = Utils.changeChromosomeName(chromosomeName);
            }

            CloseableIterator<SAMRecord> iterator = reader.query(chromosomeName, bamTrack.getStartIndex(),
                                                                       bamTrack.getEndIndex(), false);
            LOG.debug(getMessage(MessagesConstants.DEBUG_GET_ITERATOR_QUERY, iterator.toString()));

            iterator = filterIterator(iterator, options);

            final Handler<SAMRecord> filter = BamUtil.createSAMRecordHandler(bamTrack, options, referenceManager);

            while (iterator.hasNext()) {
                final SAMRecord samRecord = iterator.next();
                //if read unmapped
                filter.add(samRecord);
            }
            bamTrack.setBlocks(filter.getReadListResult());
            bamTrack.setMinPosition(filter.getMinPosition());
            bamTrack.setReferenceBuffer(filter.getReferenceBuff());
            bamTrack.setDownsampleCoverage(filter.getDownsampleCoverageResult());
            bamTrack.setBaseCoverage(filter.getBaseCoverage());
            bamTrack.setSpliceJunctions(filter.getSpliceJunctions());

        }
    }

    private CloseableIterator<SAMRecord> filterIterator(final CloseableIterator<SAMRecord> iterator,
                                                        final BamQueryOption options) {
        List<SamRecordFilter> filters = new ArrayList<>();
        if (options.isFilterDuplicate()) {
            filters.add(new DuplicateReadFilter());
        }
        if (options.isFilterNotPrimary()) {
            filters.add(new NotPrimaryAlignmentFilter());
        }
        if (options.isFilterVendorQualityFail()) {
            filters.add(new FailsVendorReadQualityFilter());
        }
        if (options.isFilterSupplementaryAlignment()) {
            filters.add(new SecondaryOrSupplementaryFilter());
        }

        if (!filters.isEmpty()) {
            return new FilteringSamIterator(iterator, new AggregateFilter(filters));
        }

        return iterator;
    }

    private Map<Sequence, List<SAMRecord>> getReadsFromFile(final Chromosome chromosome, final Track<Sequence> track,
                                                            final BamFile bamFile, final List<Sequence> blocks)
            throws IOException {
        final Map<Sequence, List<SAMRecord>> records = new HashMap<>();
        try (SamReader reader = makeSamReader(bamFile, Collections.singletonList(chromosome),
                chromosome.getReferenceId())) {
            LOG.debug(getMessage(MessagesConstants.DEBUG_FILE_OPENING, bamFile.getPath()));
            String chromosomeName = chromosome.getName();
            final int startIndex = track.getStartIndex();
            final int endIndex = track.getEndIndex();

            if (reader.getFileHeader().getSequence(chromosomeName) == null) {
                chromosomeName = Utils.changeChromosomeName(chromosomeName);
            }
            final SAMRecordIterator iterator = reader.queryOverlapping(chromosomeName, startIndex, endIndex);

            LOG.debug(getMessage(MessagesConstants.DEBUG_GET_ITERATOR_QUERY, iterator.toString()));
            while (iterator.hasNext()) {
                final SAMRecord samRecord = iterator.next();
                //if read unmapped
                if (!samRecord.getSAMFlags().contains(SAMFlag.READ_UNMAPPED) && !samRecord.getCigar().isEmpty() &&
                        samRecord.getEnd() > samRecord.getStart()) {
                    addRecordToBlock(blocks, records, samRecord);
                }
            }
        }
        return records;
    }

    private void addRecordToBlock(List<Sequence> blocks, Map<Sequence, List<SAMRecord>> records,
            SAMRecord samRecord) {
        blocks.forEach(block -> {
            if (!(samRecord.getStart() > block.getEndIndex()) && !(samRecord.getEnd() < block
                        .getStartIndex())) {
                List<SAMRecord> lst = records.get(block);
                if (lst == null) {
                    lst = new ArrayList<>();
                }
                lst.add(samRecord);
                records.remove(block);
                records.put(block, lst);
            }
        });
    }

    public SamReader makeSamReader(final BamFile bamFile, List<Chromosome> chromosomes, Long referenceId)
        throws IOException {
        return openSamReaderResource(loadIndex(loadFile(bamFile), bamFile.getIndex()), chromosomes, referenceId);
    }

    private SamInputResource loadIndex(final SamInputResource samInputResource, final BiologicalDataItem indexFile)
            throws IOException {
        SamInputResource resource;
        switch (indexFile.getType()) {
            case FILE:
                resource = samInputResource.index(new File(indexFile.getPath()));
                break;
            case URL:
                resource = samInputResource.index(new URL(indexFile.getPath()));
                break;
            case S3:
                resource = getS3Index(samInputResource, indexFile);
                break;
            case HDFS:
                resource = getHDFSIndex(samInputResource, indexFile);
                break;
            default:
                throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_INVALID_PARAM));
        }
        return resource;
    }

    private SamInputResource getHDFSIndex(SamInputResource samInputResource,
            BiologicalDataItem indexFile) throws IOException {
        URI uriIndex = URI.create(indexFile.getPath());
        Configuration conf = new Configuration();
        FileSystem fileBam = FileSystem.get(uriIndex, conf);
        FSDataInputStream indexStream = fileBam.open(new Path(uriIndex));
        return samInputResource.index(new HdfsSeekableInputStream(indexStream));
    }

    private SamInputResource getS3Index(SamInputResource samInputResource,
            BiologicalDataItem indexFile) {
        Assert.notNull(indexFile.getBucketId(), getMessage(MessagesConstants.ERROR_S3_BUCKET));
        final Bucket bucket = bucketManager.loadBucket(indexFile.getBucketId());
        Assert.notNull(bucket, getMessage(MessagesConstants.ERROR_S3_BUCKET));
        final AmazonS3 s3Client = new AmazonS3Client(new BasicAWSCredentials(bucket.getAccessKeyId(),
                bucket.getSecretAccessKey()));
        return samInputResource.index(s3Client.generatePresignedUrl(bucket.getBucketName(), indexFile.getPath(),
                Utils.getTimeForS3URL()));
    }

    private SamInputResource loadFile(final BamFile bamFile)
            throws IOException {
        SamInputResource resource;
        switch (bamFile.getType()) {
            case FILE:
            case URL:
                resource = SamInputResource.of(bamFile.getPath());
                break;
            case S3:
                resource = getS3SamInputResource(bamFile);
                break;
            case HDFS:
                resource= getHDFSSamInputResource(bamFile);
                break;
            default:
                throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_INVALID_PARAM));
        }
        return resource;
    }

    @NotNull private SamInputResource getHDFSSamInputResource(BamFile bamFile) throws IOException {
        final URI uriBam = URI.create(bamFile.getPath());
        final Configuration conf = new Configuration();
        final FileSystem fileBam = FileSystem.get(uriBam, conf);
        final FSDataInputStream inBam = fileBam.open(new Path(uriBam));
        return SamInputResource.of(new HdfsSeekableInputStream(inBam));
    }

    @NotNull private SamInputResource getS3SamInputResource(BamFile bamFile) {
        final Bucket bucket = bucketManager.loadBucket(bamFile.getBucketId());
        Assert.notNull(bucket, getMessage(MessagesConstants.ERROR_S3_BUCKET));
        final AmazonS3 s3Client = new AmazonS3Client(new BasicAWSCredentials(bucket.getAccessKeyId(),
                bucket.getSecretAccessKey()));
        return SamInputResource.of(s3Client.generatePresignedUrl(bucket.getBucketName(), bamFile.getPath(),
                Utils.getTimeForS3URL()));
    }

    private SamReader openSamReaderResource(final SamInputResource inputResource,
            List<Chromosome> chromosomes, Long referenceId) {
        Assert.notNull(inputResource, getMessage(RESOURCE_NOT_FOUND));
        return SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .referenceSource(new ReferenceSource(new ChromosomeReferenceSequence(chromosomes,
                        referenceId, referenceManager)))
                .open(inputResource);
    }

    private void parseBam(final SamReader reader) {
        Assert.notNull(reader, getMessage(RESOURCE_NOT_FOUND));
        final SAMFileHeader samFileHeader = reader.getFileHeader();
        //check we can read this Bam-file
        //bam-file is sorted
        Assert.notNull(samFileHeader.getAttribute(Constants.BAM_FILE_ATTRIBUTE_SORTED_NAME),
                getMessage(MessagesConstants.WRONG_HEADER_BAM_FILE));

        Assert.isTrue(samFileHeader.getAttribute(Constants.BAM_FILE_ATTRIBUTE_SORTED_NAME)
                        .equals(Constants.BAM_FILE_ATTRIBUTE_SORTED_VALUE),
                getMessage(MessagesConstants.WRONG_HEADER_BAM_FILE_NOT_SORT,
                        samFileHeader.getAttribute(Constants.BAM_FILE_ATTRIBUTE_SORTED_NAME)));
        //get list of chromosome
        final List<SAMSequenceRecord> list = samFileHeader.getSequenceDictionary().getSequences();
        Assert.notEmpty(list, getMessage(MessagesConstants.WRONG_HEADER_BAM_FILE_EMPTY_FILE));

        //get first chromosome and make a request to the file with this chromosome
        final SAMSequenceRecord samSequenceRecord = list.get(0);
        SAMRecordIterator iterator = reader.query(samSequenceRecord.getSequenceName(),
                Constants.BAM_START_INDEX_TEST, Math.min(Constants.MAX_BAM_END_INDEX_TEST,
                        samSequenceRecord.getSequenceLength()), false);
        Assert.notNull(iterator);
    }

}
