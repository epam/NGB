/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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

package com.epam.catgenome.entity.blast.result;

import com.epam.catgenome.manager.blast.dto.Entry;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Alignment {
    private String queryAccessionVersion;
    private long queryStart;
    private long queryEnd;
    private long queryLength;
    private String querySequence;
    private double queryCoverageSubject;
    private double queryCoverageHsp;
    private double queryCoverageUniqueSubject;

    private String sequenceAccessionVersion;
    private String sequenceId;
    private long sequenceTaxId;
    private String sequenceScientificName;
    private String sequenceCommonName;
    private String sequenceStrand;
    private long sequenceLength;
    private long sequenceStart;
    private long sequenceEnd;
    private String sequence;

    private String btop;
    private double eValue;
    private double bitScore;
    private double score;
    private long length;
    private double percentIdentity;
    private long numIdentity;
    private long mismatch;
    private long positive;
    private long gapOpening;
    private long gaps;
    private double percentPositiveMatches;

    public static Alignment fromEntry(final Entry entry) {
        return Alignment.builder()
                .queryAccessionVersion(entry.getQueryAccVersion())
                .queryStart(entry.getQueryStart())
                .queryEnd(entry.getQueryEnd())
                .queryLength(entry.getQueryLen())
                .querySequence(entry.getQseq())
                .sequenceAccessionVersion(entry.getSeqAccVersion())
                .sequenceId(entry.getSeqSeqId())
                .sequenceLength(entry.getSeqLen())
                .sequenceStart(entry.getSeqStart())
                .sequenceEnd(entry.getSeqEnd())
                .sequence(entry.getSseq())
                .btop(entry.getBtop())
                .eValue(entry.getExpValue())
                .bitScore(entry.getBitScore())
                .score(entry.getScore())
                .length(entry.getLength())
                .percentIdentity(entry.getPercentIdent())
                .numIdentity(entry.getNumIdent())
                .mismatch(entry.getMismatch())
                .positive(entry.getPositive())
                .gapOpening(entry.getGapOpen())
                .gaps(entry.getGaps())
                .percentPositiveMatches(entry.getPercentPos())
                .sequenceTaxId(entry.getSeqTaxId())
                .sequenceScientificName(entry.getSeqSciName())
                .sequenceCommonName(entry.getSeqComName())
                .sequenceStrand(entry.getSeqStrand())
                .queryCoverageSubject(entry.getQueryCovS())
                .queryCoverageHsp(entry.getQueryCovHsp())
                .queryCoverageUniqueSubject(entry.getQueryCovUs())
                .build();
    }
}
