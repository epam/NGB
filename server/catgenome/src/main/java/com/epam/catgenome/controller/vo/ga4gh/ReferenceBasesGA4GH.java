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

package com.epam.catgenome.controller.vo.ga4gh;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Source:      ReferenceBasesGA4GH
 * Created:     30.05.16, 15:00
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0, JDK 1.8
 * <p>
 * Represents a reference obtained from google genomic server,
 * which comprises a substring of the bases that make up this reference.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReferenceBasesGA4GH {

    /**
     * {@Integer} the offset position (0-based) of the given sequence from the start of this Reference.
     * This value will differ for each page in a paginated request.
     */
    private Integer offset;

    /**
     * {@String} a substring of the bases that make up this reference.
     */
    private String sequence;

    /**
     * {@Integer} the continuation token, which is used to page through large result sets.
     * Provide this value in a subsequent request to return the next page of results.
     * This field will be empty if there aren't any additional results.
     */
    private Integer nextPageToken;

    public ReferenceBasesGA4GH() {
        // no-op
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public Integer getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(Integer nextPageToken) {
        this.nextPageToken = nextPageToken;
    }
}
