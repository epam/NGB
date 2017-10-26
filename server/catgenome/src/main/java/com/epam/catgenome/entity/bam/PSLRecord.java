package com.epam.catgenome.entity.bam;

import com.epam.catgenome.manager.gene.parser.StrandSerializable;

/**
 *
 */
public class PSLRecord {

    private int tSize;
    private int start;
    private int end;
    private String chr;
    private int match;
    private int misMatch;
    private int repMatch;
    private int qNumInsert;
    private int tNumInsert;
    private int qGapCount;
    private int tGapCount;
    private int ns;
    private int qGapBases;
    private int tGapBases;
    private long score;
    private StrandSerializable strand;
    private String name;

    private int qSize;
    private String text;

    public int gettSize() {
        return tSize;
    }

    public void settSize(int tSize) {
        this.tSize = tSize;
    }

    public int getMatch() {
        return match;
    }

    public void setMatch(int match) {
        this.match = match;
    }

    public int getMisMatch() {
        return misMatch;
    }

    public void setMisMatch(int misMatch) {
        this.misMatch = misMatch;
    }

    public int getRepMatch() {
        return repMatch;
    }

    public void setRepMatch(int repMatch) {
        this.repMatch = repMatch;
    }

    public int getqNumInsert() {
        return qNumInsert;
    }

    public void setqNumInsert(int qNumInsert) {
        this.qNumInsert = qNumInsert;
    }

    public int gettNumInsert() {
        return tNumInsert;
    }

    public void settNumInsert(int tNumInsert) {
        this.tNumInsert = tNumInsert;
    }

    public int getqGapCount() {
        return qGapCount;
    }

    public void setqGapCount(int qGapCount) {
        this.qGapCount = qGapCount;
    }

    public int gettGapCount() {
        return tGapCount;
    }

    public void settGapCount(int tGapCount) {
        this.tGapCount = tGapCount;
    }

    public int getqSize() {
        return qSize;
    }

    public void setqSize(int qSize) {
        this.qSize = qSize;
    }

    public int getNs() {
        return ns;
    }

    public void setNs(int ns) {
        this.ns = ns;
    }

    public int getqGapBases() {
        return qGapBases;
    }

    public void setqGapBases(int qGapBases) {
        this.qGapBases = qGapBases;
    }

    public int gettGapBases() {
        return tGapBases;
    }

    public void settGapBases(int tGapBases) {
        this.tGapBases = tGapBases;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getChr() {
        return chr;
    }

    public void setChr(String chr) {
        this.chr = chr;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public StrandSerializable getStrand() {
        return strand;
    }

    public void setStrand(StrandSerializable strand) {
        this.strand = strand;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
