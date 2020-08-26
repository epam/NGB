/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

package com.epam.catgenome.manager.bam.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.epam.catgenome.constant.Constants;
import com.epam.catgenome.entity.bam.BamQueryOption;
import com.epam.catgenome.entity.bam.BamReferenceBuffer;
import com.epam.catgenome.entity.bam.BamTrackMode;
import com.epam.catgenome.entity.bam.BaseCoverage;
import com.epam.catgenome.entity.bam.BasePosition;
import com.epam.catgenome.entity.bam.SpliceJunctionsEntity;
import com.epam.catgenome.manager.bam.filters.Filter;
import com.epam.catgenome.manager.bam.sifters.DownsamplingSifter;
import com.epam.catgenome.manager.reference.ReferenceManager;
import com.epam.catgenome.util.BamUtil;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;


/**
 * Implementation for {@code SAMRecord} handling and getting data for a {@code BAMTrack}.
 * Provides data collecting, filtering and downsampling of the input reads according
 * to the {@code BamQueryOption}.
 */
public class SAMRecordHandler implements Handler<SAMRecord> {

    private static final String DELIMITER = "+";
    private static final String XS_TAG = "XS";

    private final Filter<SAMRecord> filter;
    //for coverage
    private final int[] coverageArray;
    private final int[] cCoverageArray;
    private final int[] aCoverageArray;
    private final int[] tCoverageArray;
    private final int[] gCoverageArray;
    private final int[] nCoverageArray;
    private final int[] insCoverageArray;
    private final int[] delCoverageArray;
    //track information
    private final int startTrack;
    private final int endTrack;
    // fof refBuffer
    private int min;
    private int max;
    private final long refID;
    private final String chromosomeName;
    private final ReferenceManager referenceManager;
    private BamReferenceBuffer referenceBuffer = null;
    //options
    private boolean showClipping;
    private boolean showSpliceJunction;
    private final HashMap<String, SpliceJunctionsEntity> spliceJunctionsHashMap = new HashMap<>();

    private BamTrackMode mode;

    /**
     * @param startTrack left track border
     * @param endTrack right track border
     * @param referenceManager for loading reference sequence
     * @param filter for filtering and downsampling
     * @param options track options
     * @throws IOException
     */
    public SAMRecordHandler(final int startTrack, final int endTrack, final ReferenceManager referenceManager,
                            final Filter<SAMRecord> filter, final BamQueryOption options) throws IOException {
        this.startTrack = startTrack;
        this.endTrack = endTrack;
        this.referenceManager = referenceManager;
        this.min = startTrack;
        this.max = endTrack + Constants.REFERENCE_STEP;

        this.refID = options.getRefID();
        this.showClipping = options.getShowClipping() != null && options.getShowClipping();
        this.chromosomeName = options.getChromosomeName();
        this.showSpliceJunction = options.getShowSpliceJunction() != null && options.getShowSpliceJunction();
        this.filter = filter;

        if (options.getMode() == BamTrackMode.FULL) {
            referenceBuffer = new BamReferenceBuffer(referenceManager.getSequenceString(min, max, refID, chromosomeName)
                    .toUpperCase());
        }

        this.coverageArray = new int[endTrack - startTrack + 1];
        this.cCoverageArray = new int[endTrack - startTrack + 1];
        this.aCoverageArray = new int[endTrack - startTrack + 1];
        this.tCoverageArray = new int[endTrack - startTrack + 1];
        this.gCoverageArray = new int[endTrack - startTrack + 1];
        this.nCoverageArray = new int[endTrack - startTrack + 1];
        this.insCoverageArray = new int[endTrack - startTrack + 1];
        this.delCoverageArray = new int[endTrack - startTrack + 1];
        this.mode = options.getMode();
    }

    /**
     * @param record for processing
     * @throws IOException
     */
    @Override
    public void add(final SAMRecord record) throws IOException {
        int start = record.getStart();
        int end = record.getEnd();
        final List<CigarElement> cigarList = record.getCigar().getCigarElements();
        final int flags = record.getFlags();
        String head = null;
        String tail = null;

        if (BamUtil.validateReadParams(flags, cigarList, end, start)) {
            coverageAdd(start, end, coverageArray, true);
            final String readString = record.getReadString();

            if (showClipping) {
                final CigarElement first = cigarList.get(0);
                final CigarElement last = cigarList.get(cigarList.size() - 1);

                if (first.getOperator().equals(CigarOperator.S)) {
                    start -= first.getLength();
                    head = readString.substring(0, first.getLength());
                }
                if (last.getOperator().equals(CigarOperator.S)) {
                    end += last.getLength();
                    tail = readString.substring(readString.length() - last.getLength(), readString.length());
                }
            }
            if (start < min) {
                refreshHeadReferenceBuffer(start);
            }
            if (end > max) {
                refreshTailReferenceBuffer(end);
            }

            List<BasePosition> differentBase = computeDifferentBase(readString,
                    referenceBuffer != null ? referenceBuffer.getBuffer() : null, start, min, cigarList, showClipping,
                    record);

            filter.add(record, start, end, mode == BamTrackMode.FULL ? differentBase : null, head, tail);
        }
    }

    /**
     * @return base coverage calculated from the added records
     */
    @Override
    public List<BaseCoverage> getBaseCoverage(double scaleFactor) {
        if (scaleFactor < 1 && mode != BamTrackMode.FULL) {
            return getSummarizedCoverage(scaleFactor);
        }

        int coverageValue = 0;
        int delCoverageValue = 0;
        final List<BaseCoverage> coverageList = new ArrayList<>();
        for (int i = 0; i < coverageArray.length; i++) {
            coverageValue += coverageArray[i];
            delCoverageValue += delCoverageArray[i];

            if (coverageValue - delCoverageValue > 0) {
                BaseCoverage baseCoverage =
                        new BaseCoverage(startTrack + i, coverageValue - delCoverageValue);
                if (mode == BamTrackMode.FULL) {
                    baseCoverage.setCoverage(cCoverageArray[i], aCoverageArray[i], tCoverageArray[i],
                            gCoverageArray[i], nCoverageArray[i], delCoverageValue,
                            insCoverageArray[i]);
                }
                coverageList.add(baseCoverage);
            }
        }
        return coverageList;
    }

    private List<BaseCoverage> getSummarizedCoverage(double scaleFactor) {
        int coverageValue = 0;
        int delCoverageValue = 0;
        final List<BaseCoverage> coverageList = new ArrayList<>();
        final int step = (int) Math.max(1, Math.round(1.0 / scaleFactor));
        int summ = 0;
        int denum = 0;
        for (int i = 0; i < coverageArray.length; i++) {
            coverageValue += coverageArray[i];
            delCoverageValue += delCoverageArray[i];

            summ = Math.max(summ, coverageValue - delCoverageValue);
            //summ += coverageValue - delCoverageValue;
            if (i != 0 && i % step == 0) { // end of step
                if (summ != 0) {
                    BaseCoverage baseCoverage =
                            new BaseCoverage(startTrack + i - denum, startTrack + i, summ); // / (float) denum
                    coverageList.add(baseCoverage);
                    summ = 0;
                }

                denum = 0;
            } else {
                denum++;
            }
        }
        return coverageList;
    }

    /**
     * @return minimum left coordinate from the added reads
     */
    @Override
    public Integer getMinPosition() {
        return min;
    }

    /**
     * @return list of {@SpliceJunctionsEntity} calculated from the added records
     */
    @Override
    public List<SpliceJunctionsEntity> getSpliceJunctions() {
        if (spliceJunctionsHashMap.isEmpty()) {
            return Collections.emptyList();
        } else {
            return getSpliceJunctionsList();
        }
    }

    /**
     * @return reference sequence for the interval covered by the added records
     */
    @Override
    public String getReferenceBuff() {
        return referenceBuffer != null ? referenceBuffer.getBuffer() : null;
    }

    @Override
    public DownsamplingSifter<SAMRecord> getSifter() {
        return filter.getSifter();
    }

    @Override
    public Filter<SAMRecord> getFilter() {
        return filter;
    }

    private void refreshHeadReferenceBuffer(final int start) throws IOException {
        if (mode == BamTrackMode.FULL) {
            final int helpMin = start <= 0 ? 1
                    : min - ((min - start) / Constants.REFERENCE_STEP + 1) * Constants.REFERENCE_STEP;
            referenceBuffer.addHead(referenceManager.getSequenceString(helpMin, min - 1, refID, chromosomeName)
                    .toUpperCase());
            min = helpMin;
        }
    }

    private void refreshTailReferenceBuffer(final int end) throws IOException {
        if (mode == BamTrackMode.FULL) {
            final int helpMax = max + ((end - max) / Constants.REFERENCE_STEP + 1) * Constants.REFERENCE_STEP;
            referenceBuffer.addTail(referenceManager.getSequenceString(max + 1, helpMax, refID, chromosomeName)
                    .toUpperCase());
            max = helpMax;
        }
    }

    private List<SpliceJunctionsEntity> getSpliceJunctionsList() {
        final List<SpliceJunctionsEntity> list = new ArrayList<>();
        list.addAll(spliceJunctionsHashMap.values());
        return list;
    }

    private void coverageAdd(final int start, final int end, final int[] coverage, final boolean increase) {
        final int k = increase ? 1 : -1;
        if (end >= startTrack && start <= endTrack) {
            if (start < startTrack) {
                coverage[0] += k;
            } else {
                coverage[start - startTrack] += k;
            }
            if (end < endTrack) {
                coverage[end - startTrack + 1] -= k;
            }
        }
    }

    // SAMRecord is needed because sometimes we need tags, but in other case record is useless, because
    // it doesn't cash some fields.
    private List<BasePosition> computeDifferentBase(final String readString, final String bufferBase,
                                                    final int startReadPosition, final int bufferStart,
                                                    final List<CigarElement> cigar, final boolean showClipping,
                                                    final SAMRecord record) {
        ReadBaseProcessor
                baseCounter = new ReadBaseProcessor(readString, bufferBase, startReadPosition, bufferStart,
                cigar, showClipping, record);
        return baseCounter.getMismatchBasePositions();
    }

    public List<BasePosition> computeDifferentBase(final SAMRecord record)
        throws IOException {
        if (record.getStart() < min) {
            refreshHeadReferenceBuffer(record.getStart());
        }
        if (record.getEnd() > max) {
            refreshTailReferenceBuffer(record.getEnd());
        }

        ReadBaseProcessor
            baseCounter = new ReadBaseProcessor(record.getReadString(), referenceBuffer.getBuffer(), record.getStart(),
                                                min, record.getCigar().getCigarElements(), showClipping, record);
        return baseCounter.getMismatchBasePositions();
    }

    private class ReadBaseProcessor {
        private final String upperReadString;
        private final String bufferBase;
        private final int startReadPosition;
        private final int bufferStart;
        private final List<CigarElement> cigar;
        private final boolean showClipping;
        private final SAMRecord record;

        private int position = 0;
        private int corrector = 0;

        //coordinate at reference
        private int bias;

        protected ReadBaseProcessor(String readString, String bufferBase, int startReadPosition,
                int bufferStart, List<CigarElement> cigar, boolean showClipping, SAMRecord record) {
            this.bufferBase = bufferBase;
            this.startReadPosition = startReadPosition;
            this.bufferStart = bufferStart;
            this.cigar = cigar;
            this.showClipping = showClipping;
            this.record = record;
            this.upperReadString = readString.toUpperCase();
            this.position = 0;
            this.corrector = 0;
            this.bias = startReadPosition - bufferStart;
        }

        protected List<BasePosition> getMismatchBasePositions() {
            position = 0;
            corrector = 0;
            final List<BasePosition> basePositions = new ArrayList<>();
            for (CigarElement cigarElement : cigar) {
                processCigarOperator(basePositions, cigarElement);
            }
            return basePositions;
        }

        private void processCigarOperator(List<BasePosition> basePositions, CigarElement cigarElement) {
            final int cigarLength = cigarElement.getLength();
            switch (cigarElement.getOperator()) {
                case M:
                case EQ:
                case X:
                    processMatch(basePositions, cigarLength);
                    break;
                case S:
                    processSoftClip(cigarLength);
                    break;
                case N:
                    processUnknown(cigarLength);
                    break;
                case D:
                    processDeletion(cigarLength);
                    break;
                case I:
                    processInsertion(cigarLength);
                    break;
                default:
                    break;
            }
        }

        private void processInsertion(int cigarLength) {
            //add to insCov, to the next base
            final int pos = startReadPosition + position + corrector - 1;
            if (pos >= startTrack && pos <= endTrack) {
                insCoverageArray[pos - startTrack]++;
            }
            position += cigarLength;
            corrector -= cigarLength;
        }

        private void processDeletion(int cigarLength) {
            //add to delCov
            coverageAdd(bufferStart + bias, bufferStart + bias + cigarLength - 1, delCoverageArray, true);
            bias += cigarLength;
            corrector += cigarLength;
        }

        private void processUnknown(int cigarLength) {
            if (showSpliceJunction) {
                final String strandString = getXSTag(record.getAttributes());
                final boolean strandSJ = strandString == null ? !record.getReadNegativeStrandFlag() :
                        "+".equals(strandString);
                addToSpliceJunctionsHashMap(bufferStart + bias - 1, bufferStart + bias + cigarLength - 1,
                        strandSJ);
            }
            coverageAdd(bufferStart + bias, bufferStart + bias + cigarLength - 1, coverageArray, false);
            bias += cigarLength;
            corrector += cigarLength;
        }

        private void processSoftClip(int cigarLength) {
            if (showClipping) {
                bias += cigarLength;
            } else {
                corrector -= cigarLength;
            }
            position += cigarLength;
        }

        private void processMatch(List<BasePosition> basePositions, int cigarLength) {
            for (int j = 0; j < cigarLength; j++) {
                if (checkIfBiasOutOfBound() && bufferBase != null && bufferBase.charAt(bias) != upperReadString.charAt(position)) {
                    basePositions.add(
                            new BasePosition(position + corrector, upperReadString.charAt(position))
                    );
                    addBaseCoverage(upperReadString.charAt(position), startReadPosition + position + corrector);
                    //add to the coverage array (c/a/t/g/n)
                }
                bias++;
                position++;
            }
        }

        private boolean checkIfBiasOutOfBound() {
            return 0 <= bias && bias < endTrack;
        }

        private String getXSTag(List<SAMRecord.SAMTagAndValue> list) {
            String tagValue = null;
            for (SAMRecord.SAMTagAndValue samTagAndValue : list) {
                if (samTagAndValue.tag.equalsIgnoreCase(XS_TAG)) {
                    tagValue = samTagAndValue.value.toString();
                }
            }
            return tagValue;
        }

        private void addBaseCoverage(final char ch, final int position) {
            if (position >= startTrack && position <= endTrack) {
                switch (ch) {
                    case 'C':
                        cCoverageArray[position - startTrack]++;
                        break;
                    case 'A':
                        aCoverageArray[position - startTrack]++;
                        break;
                    case 'T':
                        tCoverageArray[position - startTrack]++;
                        break;
                    case 'G':
                        gCoverageArray[position - startTrack]++;
                        break;
                    case 'N':
                        nCoverageArray[position - startTrack]++;
                        break;
                    default:
                        break;
                }
            }
        }

        private void addToSpliceJunctionsHashMap(final int start, final int end, final boolean strand) {
            final String key = start + DELIMITER + end + DELIMITER + strand;
            final SpliceJunctionsEntity value = spliceJunctionsHashMap.get(key);
            if (value != null) {
                value.inc();
            } else {
                spliceJunctionsHashMap.put(key, new SpliceJunctionsEntity(start, end, strand));
            }
        }

    }
}
