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
package com.epam.catgenome.manager.blast.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Entry {
    private String queryAccVersion;
    private long queryStart;
    private long queryEnd;
    private long queryLen;
    private String qseq;
    private String seqAccVersion;
    private String seqSeqId;
    private long seqLen;
    private long seqStart;
    private long seqEnd;
    private String sseq;
    private String btop;
    private double expValue;
    private double bitScore;
    private double score;
    private long length;
    private double percentIdent;
    private long numIdent;
    private long mismatch;
    private long positive;
    private long gapOpen;
    private long gaps;
    private double percentPos;
    private long seqTaxId;
    private String seqSciName;
    private String seqComName;
    private String seqStrand;
    private double queryCovS;
    private double queryCovHsp;
    private double queryCovUs;
}
