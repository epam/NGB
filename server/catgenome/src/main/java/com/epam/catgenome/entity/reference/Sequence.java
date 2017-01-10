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

package com.epam.catgenome.entity.reference;

import com.epam.catgenome.entity.track.Block;

/**
 * <p>
 * {@code Sequence} represents a business entity and is designed describe any region inside a
 * single chromosome.
 * <p>
 * In fact depending on <tt>startIndex</tt> and <tt>endIndex</tt> values it can be a single
 * nucleotide from the particular position in a chromosome or sequence of several nucleotides
 * between <tt>startIndex</tt> and <tt>endIndex</tt> inclusively.
 * <p>
 * Additionally, such intervals can be described by GC-content value, which is percentage of
 * nitrogenous bases on a sequence.
 */
public class Sequence extends Block {

    /**
     * {@code String} specifies nucleotides' sequence between <tt>startIndex</tt> and <tt>endIndex</tt>
     * positions in a single chromosome.
     */
    private String text;

    /**
     * {@code Float} generally specifies approximate percentage of nitrogenous bases on the given interval
     * of a chromosome that are either guanine or cytosine.
     */
    private Float contentGC;

    public Sequence() {
        // no operations by default
    }

    public Sequence(final Integer startIndex, final String text) {
        super(startIndex, startIndex);
        this.text = text;
    }

    public Sequence(final Integer startIndex, final Integer endIndex) {
        super(startIndex, endIndex);
    }

    public Sequence(final Integer startIndex, final Integer endIndex, final String text) {
        super(startIndex, endIndex);
        this.text = text;
    }

    public Sequence(final Integer startIndex, final Integer endIndex, final Float contentGC) {
        super(startIndex, endIndex);
        this.contentGC = contentGC;
    }

    public final String getText() {
        return text;
    }

    public final void setText(final String text) {
        this.text = text;
    }

    public final Float getContentGC() {
        return contentGC;
    }

    public final void setContentGC(final Float contentGC) {
        this.contentGC = contentGC;
    }

}
