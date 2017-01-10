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

/**
 * Created: 2/2/2016
 * Project: catgenome
 *
 * <p>
 * Represents entry of protein sequence
 * </p>
 */
public class ProteinSequenceEntry {

    private String text;
    private Long geneId;
    private Long cdsStartIndex;
    private Long cdsEndIndex;
    private Long tripleStartIndex;
    private Long tripleEndIndex;
    private Long index;

    public ProteinSequenceEntry() {
        // no-op
    }

    public ProteinSequenceEntry(final String text, final Long geneId, final Long cdsStartIndex,
            final Long cdsEndIndex,
            final Long tripleStartIndex, final Long tripleEndIndex) {
        this.text = text;
        this.geneId = geneId;
        this.cdsStartIndex = cdsStartIndex;
        this.cdsEndIndex = cdsEndIndex;
        this.tripleStartIndex = tripleStartIndex;
        this.tripleEndIndex = tripleEndIndex;
    }

    public ProteinSequenceEntry(final ProteinSequenceEntry ps) {
        this(ps.getText(), ps.getGeneId(), ps.getCdsStartIndex(), ps.getCdsEndIndex(),
             ps.getTripleStartIndex(), ps.getTripleEndIndex());
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public Long getGeneId() {
        return geneId;
    }

    public void setGeneId(final Long geneId) {
        this.geneId = geneId;
    }

    public Long getCdsStartIndex() {
        return cdsStartIndex;
    }

    public void setCdsStartIndex(final Long cdsStartIndex) {
        this.cdsStartIndex = cdsStartIndex;
    }

    public Long getCdsEndIndex() {
        return cdsEndIndex;
    }

    public void setCdsEndIndex(final Long cdsEndIndex) {
        this.cdsEndIndex = cdsEndIndex;
    }

    public Long getTripleStartIndex() {
        return tripleStartIndex;
    }

    public void setTripleStartIndex(final Long tripleStartIndex) {
        this.tripleStartIndex = tripleStartIndex;
    }

    public Long getTripleEndIndex() {
        return tripleEndIndex;
    }

    public void setTripleEndIndex(final Long tripleEndIndex) {
        this.tripleEndIndex = tripleEndIndex;
    }

    public Long getIndex() {
        return index;
    }

    public void setIndex(Long index) {
        this.index = index;
    }
}
