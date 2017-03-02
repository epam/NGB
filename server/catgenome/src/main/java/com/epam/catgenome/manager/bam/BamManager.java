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

import static com.epam.catgenome.component.MessageCode.NO_SUCH_REFERENCE;
import static com.epam.catgenome.component.MessageHelper.getMessage;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.epam.catgenome.constant.Constants;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.ReadQuery;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.bam.BamQueryOption;
import com.epam.catgenome.entity.bam.BamTrack;
import com.epam.catgenome.entity.bam.BasePosition;
import com.epam.catgenome.entity.bam.Read;
import com.epam.catgenome.entity.bam.TrackDirectionType;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.manager.TrackHelper;
import com.epam.catgenome.manager.bam.handlers.SAMRecordHandler;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.reference.ReferenceManager;
import com.epam.catgenome.util.BamUtil;
import com.epam.catgenome.util.Utils;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;

/**
 * Source:      BamManager
 * Created:     3/2/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code BamManager} represents a service class to handling BAM files and getting track results for BAM data.
 * <p>
 */
@Service
public class BamManager {

    @Autowired
    private TrackHelper trackHelper;

    @Autowired
    private BamHelper bamHelper;

    @Autowired
    private BamFileManager bamFileManager;

    @Autowired
    private BiologicalDataItemManager biologicalDataItemManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private ReferenceManager referenceManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(BamHelper.class);

    /**
     * Registers the Bam file in the system
     * @param request registration request from the client
     */
    public BamFile registerBam(final IndexedFileRegistrationRequest request) throws IOException {
        Assert.notNull(request, getMessage(MessagesConstants.ERROR_NULL_PARAM));
        Assert.notNull(request.getPath(), getMessage(MessagesConstants.ERROR_NULL_PARAM));
        Assert.notNull(request.getReferenceId(), getMessage(NO_SUCH_REFERENCE));
        Assert.notNull(request.getIndexPath(), getMessage(MessagesConstants.WRONG_BAM_INDEX_FILE));
        if (request.getType() == null) {
            request.setType(BiologicalDataItemResourceType.FILE);
        }
        final BamFile newBamFile = bamHelper.fillBamFile(request);
        try {
            biologicalDataItemManager.createBiologicalDataItem(newBamFile);
            Reference reference = referenceGenomeManager.loadReferenceGenome(request.getReferenceId());
            List<Chromosome> chromosomes = reference.getChromosomes();
            //we can read this file with this index file
            LOGGER.debug(getMessage(MessagesConstants.DEBUG_FILE_READING, request.getPath()));
            bamHelper.parseBamFile(newBamFile, chromosomes, reference.getId());


            biologicalDataItemManager.createBiologicalDataItem(newBamFile.getIndex());
            bamFileManager.save(newBamFile);
        } finally {
            if (newBamFile != null && newBamFile.getId() != null
                    && bamFileManager.loadBamFile(newBamFile.getId()) == null) {
                biologicalDataItemManager.deleteBiologicalDataItem(newBamFile.getId());
            }
        }

        return newBamFile;
    }

    /**
     * Removes bam file metadata from the system, deleting all additional files that were created
     * @param bamFileId {@code long} a bam file ID
     * @return deleted {@code BamFile} entity
     */
    public BamFile unregisterBamFile(final long bamFileId) throws IOException {
        BamFile fileToDelete = bamFileManager.loadBamFile(bamFileId);
        Assert.notNull(fileToDelete, getMessage(MessagesConstants.ERROR_FILE_NOT_FOUND));
        bamFileManager.deleteBamFile(fileToDelete);
        return fileToDelete;
    }

    /**
     * Returns {@code Track} filled with BAM data from a specified BAM file in the server's file system
     * @param track input track
     * @param option BAM track options
     * @return {@code Track} filled with BAM data
     * @throws IOException on resource reading errors
     */
    public BamTrack<Read> getBamTrack(final Track<Read> track, BamQueryOption option) throws IOException {
        final Chromosome chromosome = trackHelper.validateTrack(track);
        BamQueryOption currentOptions = option == null ? new BamQueryOption() : option;
        BamUtil.validateOptions(currentOptions, chromosome);
        return getFullResult(track, currentOptions);
    }

    /**
     * Returns {@code Track} filled with BAM data from a specified URL
     * @param track input track
     * @param option BAM track options
     * @param bamUrl path to BAM file
     * @param indexUrl path to Bam index file
     * @return{@code Track} filled with BAM data
     * @throws IOException on resource reading errors
     */
    public BamTrack<Read> getBamTrackFromUrl(final Track<Read> track, BamQueryOption option, String bamUrl,
                                             String indexUrl) throws IOException {
        final Chromosome chromosome = trackHelper.validateUrlTrack(track, bamUrl, indexUrl);
        BamQueryOption currentOptions = option == null ? new BamQueryOption() : option;
        BamUtil.validateOptions(currentOptions, chromosome);
        return getFullResultFromUrl(track, bamUrl, indexUrl, currentOptions);
    }

    /**
     * @deprecated remove this code when client stops using deprecated API
     */
    @Deprecated
    public BamTrack<Read> getFullMiddleReadsResult(final Track<Read> track, final Integer frame,
                                                   final Integer count, Boolean showClipping,
                                                   final Boolean showSpliceJunction) throws IOException {
        final Chromosome chromosome = trackHelper.validateTrack(track);
        final BamQueryOption option = new BamQueryOption(showClipping, showSpliceJunction, TrackDirectionType.MIDDLE,
                frame, count, chromosome);

        return getFullResult(track, option);
    }

    /**
     * @deprecated remove this code when client stops using deprecated API
     */
    @Deprecated
    public BamTrack<Read> getFullMiddleReadsResultFromUrl(final Track<Read> track, final Integer frame,
                                                   final Integer count, Boolean showClipping,
                                                   final Boolean showSpliceJunction, String bamUrl, String indexUrl)
            throws IOException {
        final Chromosome chromosome = trackHelper.validateUrlTrack(track, bamUrl, indexUrl);
        final BamQueryOption option = new BamQueryOption(showClipping, showSpliceJunction, TrackDirectionType.MIDDLE,
                frame, count, chromosome);

        return getFullResultFromUrl(track, bamUrl, indexUrl, option);
    }

    /**
     * @deprecated remove this code when client stops using deprecated API
     */
    @Deprecated
    public BamTrack<Read> getFullReadsResult(final Track<Read> track, final TrackDirectionType direction,
                                             final Integer frame,
                                             final Integer count, final Boolean showClipping,
                                             final Boolean showSpliceJunction) throws IOException {

        final Chromosome chromosome = trackHelper.validateTrack(track);
        Assert.notNull(chromosome, getMessage(MessagesConstants.ERROR_CHROMOSOME_ID_NOT_FOUND));
        final BamQueryOption option = new BamQueryOption(showClipping, showSpliceJunction,
                direction, frame, count, chromosome);

        return getFullResult(track, option);
    }

    /**
     * @deprecated remove this code when client stops using deprecated API
     */
    @Deprecated
    public BamTrack<Read> getFullReadsResultFromUrl(final Track<Read> track, final BamQueryOption option,
            String bamUrl, String indexUrl)
            throws IOException {
        final Chromosome chromosome = trackHelper.validateUrlTrack(track, bamUrl, indexUrl);
        Assert.notNull(chromosome, getMessage(MessagesConstants.ERROR_CHROMOSOME_ID_NOT_FOUND));
        final BamQueryOption currentOption = new BamQueryOption(option.getShowClipping(),
                option.getShowSpliceJunction(), option.getTrackDirection(),
                option.getFrame(), option.getCount(), chromosome);

        return getFullResultFromUrl(track, bamUrl, indexUrl, currentOption);
    }

    /**
     * Calculates consensus sequence for a specified by a track region track from the reads in the BAM file.
     * @param track input track
     * @return track filled with consensus sequence
     * @throws IOException
     */
    public Track<Sequence> calculateConsensusSequence(final Track<Sequence> track) throws IOException {
        if (track.getEndIndex() - track.getStartIndex() > Constants.MAX_BAM_INTERVAL) {
            return new Track<>(track);
        }
        final Chromosome chromosome = trackHelper.validateTrackWithBlockCount(track);
        TrackHelper.fillBlocks(track, indexes -> new Sequence(indexes.getLeft(), indexes.getRight()));
        return bamHelper.calculateConsensusSequence(track, chromosome);
    }

    /**
     * Loads a specific read form a BAM file, specified by ReadQuery object
     * @param query a {@link ReadQuery} object, that specifies a {@link Read} to load
     * @return a {@link Read} object
     * @throws IOException if somwthing goes wrong with the filesystem
     */
    public Read loadRead(final ReadQuery query, String fileUrl, String indexUrl) throws IOException {
        Assert.notNull(query, MessagesConstants.ERROR_NULL_PARAM);
        Assert.isTrue(query.getId() != null ||
                      (StringUtils.isNotBlank(fileUrl) && StringUtils.isNotBlank(indexUrl)),
                      MessagesConstants.ERROR_NULL_PARAM);

        Assert.notNull(query.getChromosomeId(), MessagesConstants.ERROR_NULL_PARAM);
        Assert.notNull(query.getStartIndex(), MessagesConstants.ERROR_NULL_PARAM);
        Assert.notNull(query.getEndIndex(), MessagesConstants.ERROR_NULL_PARAM);
        Assert.notNull(query.getName(), MessagesConstants.ERROR_NULL_PARAM);

        final Chromosome chromosome = referenceGenomeManager.loadChromosome(query.getChromosomeId());
        BamFile bamFile;
        if (query.getId() != null) {
            bamFile= bamFileManager.loadBamFile(query.getId());
        } else {
            bamFile = bamHelper.makeUrlBamFile(fileUrl, indexUrl, chromosome.getReferenceId());
        }
        return getReadFromBamFile(query, chromosome, bamFile);
    }

    @Nullable
    private Read getReadFromBamFile(ReadQuery query, Chromosome chromosome, BamFile bamFile) throws IOException {
        try (SamReader reader = bamHelper.makeSamReader(bamFile, Collections.singletonList(chromosome),
                                                        chromosome.getReferenceId())) {
            String chromosomeName = chromosome.getName();
            if (reader.getFileHeader().getSequence(chromosomeName) == null) {
                chromosomeName = Utils.changeChromosomeName(chromosomeName);
            }

            SAMRecordIterator iterator = reader.query(chromosomeName, query.getStartIndex(), query.getEndIndex(),
                                                      true);
            while (iterator.hasNext()) {
                final SAMRecord samRecord = iterator.next();
                if (samRecord.getReadName().equals(query.getName())) {
                    BamQueryOption option = new BamQueryOption();
                    option.setRefID(chromosome.getReferenceId());
                    option.setChromosomeName(chromosome.getName());
                    SAMRecordHandler recordHandler = new SAMRecordHandler(query.getStartIndex(), query.getEndIndex(),
                                                                          referenceManager, null, option);
                    List<BasePosition> diffBase = recordHandler.computeDifferentBase(samRecord);
                    return BamUtil.createExtendedRead(samRecord, diffBase);
                }
            }
        }
        return null;
    }

    private BamTrack<Read> getFullResult(final Track<Read> track, final BamQueryOption options)
            throws IOException {
        if (track.getEndIndex() - track.getStartIndex() > Constants.MAX_BAM_INTERVAL) {
            return new BamTrack<>(track);
        } else {
            return bamHelper.getReadsFromFile(track, options);
        }
    }

    private BamTrack<Read> getFullResultFromUrl(final Track<Read> track, String bamUrl, String indexUrl,
                                                final BamQueryOption options)
            throws IOException {
        if (track.getEndIndex() - track.getStartIndex() > Constants.MAX_BAM_INTERVAL) {
            return new BamTrack<>(track);
        } else {
            return bamHelper.getReadsFromUrl(track, bamUrl, indexUrl, options);
        }
    }
}
