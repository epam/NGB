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

/**
 * Source:
 * Created:     7/6/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.1, JDK 1.8
 * Represents an expansible from start and end reference sequence
 */
public class BamReferenceBuffer {

    private String buffer;

    /**
     * @param buffer initial reference sequence
     */
    public BamReferenceBuffer(String buffer) {
        this.buffer = buffer;
    }

    /**
     * Adds a string to the end of the current reference
     * @param tailString string to add to reference
     */
    public void addTail(final String tailString) {
        final StringBuilder builder = new StringBuilder(buffer);
        builder.append(tailString);
        buffer = builder.toString();
    }

    /**
     * Adds a string to the start of the current reference
     * @param headString string to add to reference
     */
    public void addHead(final String headString) {
        final StringBuilder builder = new StringBuilder(buffer);
        builder.insert(0, headString);
        buffer = builder.toString();
    }

    public String getBuffer() {
        return buffer;
    }

    public void setBuffer(String buffer) {
        this.buffer = buffer;
    }
}
