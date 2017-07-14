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

package com.epam.catgenome.manager.wig.reader;

import htsjdk.tribble.Feature;

public class BedGraphFeature implements Feature {

    /**
     * represents the ending interval index, inclusive.
     */
    private int endIndex;

    /**
     * represents the beginning interval index, inclusive.
     */
    private int startIndex;

    private float value;


    public BedGraphFeature(String chr, final Integer startIndex, final Integer endIndex) {
        this.chr = chr;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.value = 0;
    }

    public BedGraphFeature(String chr, final Integer startIndex, final Integer endIndex, float value) {
        this.chr = chr;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.value = value;
    }

    private String chr;

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
        return startIndex;
    }

    @Override
    public int getEnd() {
        return endIndex;
    }

    public float getValue() {
        return value;
    }
}
