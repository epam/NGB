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

package com.epam.catgenome.entity.protein;

import com.epam.catgenome.entity.track.Block;

/**
 * Created: 2/2/2016
 * Project: catgenome
 *
 * Represents protein sequence.
 *
 * @author Nina_Lukashina
 */
public class ProteinSequence extends Block {

    /**
     * String of protein sequence.
     */
    private String text;
    private Long index;

    public ProteinSequence() {
        // no-op
    }

    public ProteinSequence(final Integer startIndex, final Integer endIndex, final String text, final long index) {
        super(startIndex, endIndex);
        this.text = text;
        this.index = index;
    }

    public ProteinSequence(final ProteinSequenceEntry ps) {
        this(ps.getTripleStartIndex().intValue(), ps.getTripleEndIndex().intValue(), ps.getText(), ps.getIndex());
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public Long getIndex() {
        return index;
    }

    public void setIndex(Long index) {
        this.index = index;
    }
}
