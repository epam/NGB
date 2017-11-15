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
    private int mismatch;
    private int repMatch;
    private int qGapCount;
    private int tGapCount;
    private int ns;
    private int qGapBases;
    private int tGapBases;
    private int score;
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

    public int getMismatch() {
        return mismatch;
    }

    public void setMismatch(int mismatch) {
        this.mismatch = mismatch;
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

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
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
        if (mismatch != record.mismatch) {
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
        result = 31 * result + mismatch;
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
