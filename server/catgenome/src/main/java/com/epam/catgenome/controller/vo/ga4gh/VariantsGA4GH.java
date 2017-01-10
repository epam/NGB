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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * <p>
 * Class for data from GA4GH (variants from GA4GH)
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VariantsGA4GH extends CallSet{

    /**
     * {@code String} Only return variants which have exactly this name.
     */
    private String variantName;

    /**
     * {@code List}  Only return variant calls which belong to call sets with these ids.
     * Leaving this blank returns all variant calls.
     * If a variant has no calls belonging to any of these call sets, it won't be returned at all.
     * Currently, variants with no calls from any call set will never be returned.
     */
    private List<String> callSetIds;

    /**
     *{@code String} Required. Only return variants in this reference sequence.
     */
    private String referenceName;

    /**
     * {@code String} The beginning of the window (0-based, inclusive) for which overlapping variants should be
     * returned. If unspecified, defaults to 0.
     */
    private String start;

    /**
     * {@code String} The end of the window, 0-based exclusive. If unspecified or 0, defaults to the length of the
     * reference.
     */
    private String end;

    /**
     * {@code String} The continuation token, which is used to page through large result sets.
     * To get the next page of results, set this parameter to the value of nextPageToken from the previous response.
     */
    private String pageToken;

    /**
     * {@code String} The maximum number of variants to return in a single page.
     * If unspecified, defaults to 5000. The maximum value is 10000.
     */
    private String pageSize;

    /**
     * {@code String} The maximum number of calls to return in a single page.
     * Note that this limit may be exceeded in the event that a matching variant contains more calls than the requested
     * maximum. If unspecified, defaults to 5000. The maximum value is 10000.
     */
    private String maxCalls;

    public VariantsGA4GH() {
        // no - op
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public List<String> getCallSetIds() {
        return callSetIds;
    }

    public void setCallSetIds(List<String> callSetIds) {
        this.callSetIds = callSetIds;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getPageToken() {
        return pageToken;
    }

    public void setPageToken(String pageToken) {
        this.pageToken = pageToken;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getMaxCalls() {
        return maxCalls;
    }

    public void setMaxCalls(String maxCalls) {
        this.maxCalls = maxCalls;
    }
}
