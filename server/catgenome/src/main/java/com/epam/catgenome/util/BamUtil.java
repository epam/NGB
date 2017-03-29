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

package com.epam.catgenome.util;

import java.io.IOException;
import java.util.List;

import org.springframework.util.Assert;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.Constants;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.bam.BamQueryOption;
import com.epam.catgenome.entity.bam.BasePosition;
import com.epam.catgenome.entity.bam.NgbSamTagAndValue;
import com.epam.catgenome.entity.bam.Read;
import com.epam.catgenome.entity.bam.TrackDirectionType;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.manager.bam.filters.LeftSAMRecordFilter;
import com.epam.catgenome.manager.bam.filters.MiddleSAMRecordFilter;
import com.epam.catgenome.manager.bam.filters.RightSAMRecordFilter;
import com.epam.catgenome.manager.bam.handlers.Handler;
import com.epam.catgenome.manager.bam.handlers.SAMRecordHandler;
import com.epam.catgenome.manager.bam.sifters.DownsamplingSifter;
import com.epam.catgenome.manager.bam.sifters.FullResultSifter;
import com.epam.catgenome.manager.bam.sifters.StandardSAMRecordSifter;
import com.epam.catgenome.manager.reference.ReferenceManager;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.SAMFlag;
import htsjdk.samtools.SAMRecord;

/**
 * Source:
 * Created:     7/4/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.1, JDK 1.8
 *
 * <p>
 * An Util class, containing common methods for opertaing BAM files
 * </p>
 *
 * @author Semen_Dmitriev
 */
public final class BamUtil {

    private BamUtil() {
        // no operations by default
    }

    /**
     * Create Read object from SAMRecord
     * @param samRecord a SAMRecord from BAM file
     * @param start new start of a record, without softClip
     * @param end new end of a record, without softClip
     * @param differentBase list of bases, where variation occurred
     * @param head head sequence of a Record
     * @param tail tail sequence of a Record
     * @return
     */
    public static Read createReadFromRecord(final SAMRecord samRecord, final int start, final int end,
                                            final List<BasePosition> differentBase,
                                            final String head, final String tail) {
        final Read read = new Read();
        read.setStartIndex(start);
        read.setEndIndex(end);
        read.setName(samRecord.getReadName());
        read.setCigarString(samRecord.getCigarString());
        read.setStand(!samRecord.getReadNegativeStrandFlag());
        read.setMappingQuality(samRecord.getMappingQuality());
        read.setFlagMask(samRecord.getFlags());
        read.setTLen(samRecord.getInferredInsertSize());
        read.setRNext(samRecord.getMateReferenceName());
        read.setPNext(samRecord.getMateAlignmentStart());
        read.setPairedReadName(samRecord.getPairedReadName());
        read.setRName(samRecord.getReferenceName());
        read.setDifferentBase(differentBase);
        read.setHeadSequence(head);
        read.setTailSequence(tail);
        return read;
    }

    public static Read createExtendedRead(final SAMRecord samRecord, final List<BasePosition> differentBase) {
        Read read = createReadFromRecord(samRecord, samRecord.getStart(), samRecord.getEnd(), differentBase,
                                         null, null);
        read.setSequence(samRecord.getReadString());
        read.setTags(NgbSamTagAndValue.fromSAMTags(samRecord.getAttributes()));
        read.setQualities(samRecord.getBaseQualityString());

        return read;
    }

    /**
     * Factory method to create a valid SAMRecordHandler for a track
     * @param track a track to create Handler
     * @param options options to determine, which Handler is neeeded
     * @param referenceManager ReferenceManager is required for Handler construction
     * @return a valid SAMRecordHandler for a track and options
     * @throws IOException
     */
    public static Handler<SAMRecord> createSAMRecordHandler(final Track<Read> track, final BamQueryOption options,
                                                            final ReferenceManager referenceManager, boolean
                                                                    coverageOnly)
    // TODO: int maxReadCount - decide reads or coverage by by read count
            throws IOException {
        Handler<SAMRecord> filter;
        final int startTrack = track.getStartIndex();
        final int endTrack = track.getEndIndex();
        switch (options.getTrackDirection()) {
            case LEFT:
                filter = new SAMRecordHandler(startTrack, endTrack, referenceManager, new LeftSAMRecordFilter(endTrack,
                        BamUtil.createSifter(endTrack, options, coverageOnly)), options); //maxReadCount
                break;
            case MIDDLE:
                filter = new SAMRecordHandler(startTrack, endTrack, referenceManager, new MiddleSAMRecordFilter(
                        BamUtil.createSifter(endTrack, options, coverageOnly)), options); //maxReadCount
                break;
            case RIGHT:
                filter = new SAMRecordHandler(startTrack, endTrack, referenceManager, new RightSAMRecordFilter(
                        startTrack, BamUtil.createSifter(endTrack, options, coverageOnly)), options); //maxReadCount
                break;
            default:
                throw new IllegalArgumentException("Unexpected track direction: " + options.getTrackDirection());
        }
        return filter;
    }

    /**
     * Checks if BamQueryOption is valid
     * @param option option to check
     * @param chromosome chromosome to be set into options
     */
    public static void validateOptions(final BamQueryOption option, final Chromosome chromosome) {
        option.setChromosomeName(chromosome.getName());
        option.setRefID(chromosome.getReferenceId());
        if (null == option.getTrackDirection()) {
            option.setTrackDirection(TrackDirectionType.MIDDLE);
        }
        if (null == option.getShowClipping()) {
            option.setShowClipping(false);
        }
        if (null == option.getShowSpliceJunction()) {
            option.setShowSpliceJunction(false);
        }
        if (validDownsempleParams(option.getFrame(), option.getCount())) {
            option.setDownSampling(true);
        }
        Assert.isTrue(null != option.getChromosomeName() || null != option.getRefID() ||
                null != option.getTrackDirection(), MessagesConstants.ERROR_NULL_PARAM);
        Assert.notNull(option.getTrackDirection(), MessageHelper.getMessage(MessagesConstants.ERROR_INVALID_PARAM));
    }

    /**
     * Checks if Read parameters are valid
     * @param flags flags to check
     * @param cigarList List of {@link CigarElement} to check
     * @param end end of the Read
     * @param start start of the Read
     * @return true if read parameters are valid
     */
    public static boolean validateReadParams(final int flags, final List<CigarElement> cigarList, final int end,
                                             final int start) {
        return !checkFlag(flags, SAMFlag.READ_UNMAPPED.intValue()) && !cigarList.isEmpty() && end > start;
    }

    /**
     * Creates a valid DownsamplingSifter
     * @param end track's end index
     * @param options options of a BAM query
     * @return
     */
    public static DownsamplingSifter<SAMRecord> createSifter(final int end, final BamQueryOption options,
                                                             boolean coverageOnly) {
        // TODO: int maxReadCount - decide reads or coverage by by read count
        return options.isDownSampling() ? new StandardSAMRecordSifter(options.getFrame(), options.getCount(), end,
                coverageOnly) : new FullResultSifter(coverageOnly);
    }

    public static boolean checkFlag(final int flagMasc, final int flag) {
        return ((flagMasc & flag) ^ flag) == 0;
    }

    private static boolean validDownsempleParams(final Integer frame, final Integer count) {
        return null != frame && null != count && frame > Constants.BAM_DOWNSAMPLING_MIN_FRAME_SIZE &&
               count > Constants.BAM_DOWNSAMPLING_MIN_COUNT && count < Constants.BAM_DOWNSAMPLING_MAX_COUNT;
    }
}
