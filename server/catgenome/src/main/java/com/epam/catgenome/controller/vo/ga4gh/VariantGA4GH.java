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
 * Class for data from GA4GH (variant from GA4GH)
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VariantGA4GH extends VariantsGA4GH {

    /**
     * {@code String} The ID of the variant set this variant belongs to.
     */
    private String variantSetId;

    /**
     * {@code List} Names for the variant, for example a RefSNP ID.
     */
    private List<String> names;

    /**
     * {@code String} The reference on which this variant occurs.
     */
    private String referenceBases;

    /**
     * {@code List} The bases that appear instead of the reference bases.
     */
    private List<String> alternateBases;

    /**
     * {@code Double} A measure of how likely this variant is to be real. A higher value is better.
     */
    private Double quality;

    /**
     * {@code List} A list of filters (normally quality filters) this variant has failed.
     * PASS indicates this variant has passed all filters.
     */
    private List<String> filter;

    /**
     * {@code List} The variant calls for this particular variant.
     * Each one represents the determination of genotype with respect to this variant.
     */
    private List<VariantCall> calls;

    public VariantGA4GH() {
        // no-po
    }

    public String getVariantSetId() {
        return variantSetId;
    }

    public void setVariantSetId(String variantSetId) {
        this.variantSetId = variantSetId;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public String getReferenceBases() {
        return referenceBases;
    }

    public void setReferenceBases(String referenceBases) {
        this.referenceBases = referenceBases;
    }

    public List<String> getAlternateBases() {
        return alternateBases;
    }

    public void setAlternateBases(List<String> alternateBases) {
        this.alternateBases = alternateBases;
    }

    public Double getQuality() {
        return quality;
    }

    public void setQuality(Double quality) {
        this.quality = quality;
    }

    public List<String> getFilter() {
        return filter;
    }

    public void setFilter(List<String> filter) {
        this.filter = filter;
    }

    public List<VariantCall> getCalls() {
        return calls;
    }

    public void setCalls(List<VariantCall> calls) {
        this.calls = calls;
    }
}
