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

package com.epam.catgenome.entity.track;

/**
 * Source:      Block.java
 * Created:     10/8/15, 7:22 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code Block} represents a base building element of any particular tracks. By their nature
 * such elements are interval characteristics with additional data.
 * <p>
 * The extensions of {@code Block} can be used to handle sub-sequences from the reference, a
 * single genes, variants etc. In fact any such entities are interval with additional data as
 * GC-content value, gene's name or variant definitions etc.
 */
public class Block {

    /**
     * {@code Integer} represents the ending interval index, inclusive.
     */
    private Integer endIndex;

    /**
     * {@code Integer} represents the beginning interval index, inclusive.
     */
    private Integer startIndex;

    public Block() {
        //no operations by default
    }

    public Block(final Block block) {
        endIndex = block.getEndIndex();
        startIndex = block.getStartIndex();
    }

    public Block(final Integer startIndex) {
        this.startIndex = startIndex;
    }

    public Block(final Integer startIndex, final Integer endIndex) {
        this(startIndex);
        this.endIndex = endIndex;
    }

    public final Integer getEndIndex() {
        return endIndex;
    }

    public final void setEndIndex(final Integer endIndex) {
        this.endIndex = endIndex;
    }

    public final Integer getStartIndex() {
        return startIndex;
    }

    public final void setStartIndex(final Integer startIndex) {
        this.startIndex = startIndex;
    }

}
