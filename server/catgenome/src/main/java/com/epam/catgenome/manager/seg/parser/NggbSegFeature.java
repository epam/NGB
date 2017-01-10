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

package com.epam.catgenome.manager.seg.parser;

/**
 * {@code NggbSegFeature} is an implementation of a {@code SegFeature} and represents an object
 * parsed from the Seg file
 */

public class NggbSegFeature implements SegFeature {
    private String chr;
    private int start = -1;
    private int end = -1;
    private Integer numMark = 0;
    private Float segMean = Float.NaN;
    private String id;

    public NggbSegFeature(String chr, int start, int end, String id) {
        this.chr = chr;
        this.start = start;
        this.end = end;
        this.id = id;
    }

    @Override
    public String getChr() {
        return chr;
    }

    @Override
    public String getContig() {
        return chr;
    }

    @Override
    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    @Override
    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Integer getNumMark() {
        return numMark;
    }

    public void setNumMark(Integer numMark) {
        this.numMark = numMark;
    }

    @Override
    public Float getSegMean() {
        return segMean;
    }

    public void setSegMean(Float segMean) {
        this.segMean = segMean;
    }

    @Override
    public String toString() {
        return new StringBuilder(id).append('\t')
                .append(chr).append('\t')
                .append(start - 1).append('\t')
                .append(end).append('\t')
                .append(numMark).append('\t')
                .append(segMean).toString();
    }
}
