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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * <p>
 * Class for data from GA4GH (set)
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CallSet extends ReferenceGA4GH {

    /**
     * {@code String} The sample ID this call set corresponds to.
     */
    private String sampleId;

    /**
     * {@code List} The IDs of the variant sets this call set belongs to.
     * This field must have exactly length one, as a call set belongs to a single variant set.
     * This field is repeated for compatibility with the GA4GH 0.5.1 API.
     */
    private List<String> variantSetIds;

    /**
     * {@code String} The date this call set was created in milliseconds from the epoch.
     */
    private String created;

    /**
     * {@code Map} A map of additional call set information.
     */
    private Map<String, ArrayList<String>> info;

    public CallSet() {
        // no-op
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public List<String> getVariantSetIds() {
        return variantSetIds;
    }

    public void setVariantSetIds(List<String> variantSetIds) {
        this.variantSetIds = variantSetIds;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public Map<String, ArrayList<String>> getInfo() {
        return info;
    }

    public void setInfo(Map<String, ArrayList<String>> info) {
        this.info = info;
    }
}
