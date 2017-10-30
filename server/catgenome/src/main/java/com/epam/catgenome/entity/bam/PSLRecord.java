package com.epam.catgenome.entity.bam;

import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 *
 */
public class PSLRecord extends Block implements Serializable {

    private String chr;
    private int match;
    private int misMatch;
    private int repMatch;
    private int qGapCount;
    private int tGapCount;
    private int ns;
    private int qGapBases;
    private int tGapBases;
    private double score;
    private StrandSerializable strand;

    private String name;

    @JsonIgnore
    private int qSize;

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

    public String getChr() {
        return chr;
    }

    public void setChr(String chr) {
        this.chr = chr;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
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


    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }

        PSLRecord record = (PSLRecord) o;

        if (match != record.match) {
            return false;
        }
        if (misMatch != record.misMatch) {
            return false;
        }
        if (repMatch != record.repMatch) {
            return false;
        }
        if (qGapCount != record.qGapCount) {
            return false;
        }
        if (tGapCount != record.tGapCount) {
            return false;
        }
        if (ns != record.ns) {
            return false;
        }
        if (qGapBases != record.qGapBases) {
            return false;
        }
        if (tGapBases != record.tGapBases) {
            return false;
        }
        if (Double.compare(record.score, score) != 0) {
            return false;
        }
        if (qSize != record.qSize) {
            return false;
        }
        if (!chr.equals(record.chr)) {
            return false;
        }
        if (strand != record.strand) {
            return false;
        }
        return name.equals(record.name);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = chr.hashCode();
        result = 31 * result + match;
        result = 31 * result + misMatch;
        result = 31 * result + repMatch;
        result = 31 * result + qGapCount;
        result = 31 * result + tGapCount;
        result = 31 * result + ns;
        result = 31 * result + qGapBases;
        result = 31 * result + tGapBases;
        temp = Double.doubleToLongBits(score);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + strand.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + qSize;
        return result;
    }
}
