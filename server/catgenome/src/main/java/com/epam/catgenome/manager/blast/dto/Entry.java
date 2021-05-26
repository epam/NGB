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
