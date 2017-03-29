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

package com.epam.catgenome.entity.bam;

import com.epam.catgenome.entity.track.Block;

/**
 * Source:
 * Created:     7/20/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.1, JDK 1.8
 * Represents different types of coverage for a base: A, C, G, T, N, insertions and deletions
 * coverage
 */
public class BaseCoverage extends Block {
    private Integer cCov;
    private Float value;
    private Integer aCov;
    private Integer tCov;
    private Integer gCov;
    private Integer nCov;
    private Integer delCov;
    private Integer insCov;

    public BaseCoverage() {
        //no-op
    }

    /**
     * @param startIndex coverage index
     * @param value of coverage
     */
    public BaseCoverage(final int startIndex, float value) {
        super(startIndex);
        this.value = value;
    }

    public BaseCoverage(int startIndex, int endIndex, float value) {
        super(startIndex, endIndex);
        this.value = value;
    }

    public Integer getcCov() {
        return cCov;
    }

    /**
     * Method for setting all kinds of coverage at once
     */
    public void setCoverage(final int cCov, final int aCov, final int tCov,
            final int gCov, final int nCov, final int delCov, final int insCov) {
        this.cCov = cCov > 0 ? cCov : null;
        this.aCov = aCov > 0 ? aCov : null;
        this.tCov = tCov > 0 ? tCov : null;
        this.gCov = gCov > 0 ? gCov : null;
        this.nCov = nCov > 0 ? nCov : null;
        this.delCov = delCov > 0 ? delCov : null;
        this.insCov = insCov > 0 ? insCov : null;
    }

    public void setcCov(Integer cCov) {
        this.cCov = cCov;
    }

    public Integer getaCov() {
        return aCov;
    }

    public void setaCov(Integer aCov) {
        this.aCov = aCov;
    }

    public Integer gettCov() {
        return tCov;
    }

    public void settCov(Integer tCov) {
        this.tCov = tCov;
    }

    public Integer getgCov() {
        return gCov;
    }

    public void setgCov(Integer gCov) {
        this.gCov = gCov;
    }

    public Integer getnCov() {
        return nCov;
    }

    public void setnCov(Integer nCov) {
        this.nCov = nCov;
    }

    public Integer getDelCov() {
        return delCov;
    }

    public void setDelCov(Integer delCov) {
        this.delCov = delCov;
    }

    public Integer getInsCov() {
        return insCov;
    }

    public void setInsCov(Integer insCov) {
        this.insCov = insCov;
    }

    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }
}
