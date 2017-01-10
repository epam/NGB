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
 * Class for data from GA4GH (variant)
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VariantCall extends CallSet {

    /**
     * {@code String} The ID of the call set this variant call belongs to.
     */
    private String callSetId;

    /**
     * {@code String} The name of the call set this variant call belongs to.
     */
    private String callSetName;

    /**
     * {@code List} The name of the call set this variant call belongs to.
     */
    private List<String> genotype;

    /**
     * {@code String}If this field is present, this variant call's genotype ordering implies the phase of the bases and
     * is consistent with any other variant calls in the same reference sequence which have the same phaseset value.
     * When importing data from VCF, if the genotype data was phased but no phase set was specified this field will be
     * set to *.
     */
    private String phaseset;
    /**
     * {@code List} The genotype likelihoods for this variant call. Each array entry represents how likely a specific
     * genotype is for this call. The value ordering is defined by the GL tag in the VCF spec. If Phred-scaled genotype
     * likelihood scores (PL) are available and log10(P) genotype likelihood scores (GL) are not, PL scores are
     * converted to GL scores. If both are available, PL scores are stored in info.
     */
    private List<Integer> genotypeLikelihood;

    public VariantCall() {
        // no-op
    }

    public String getCallSetId() {
        return callSetId;
    }

    public void setCallSetId(String callSetId) {
        this.callSetId = callSetId;
    }

    public String getCallSetName() {
        return callSetName;
    }

    public void setCallSetName(String callSetName) {
        this.callSetName = callSetName;
    }

    public List<String> getGenotype() {
        return genotype;
    }

    public void setGenotype(List<String> genotype) {
        this.genotype = genotype;
    }

    public String getPhaseset() {
        return phaseset;
    }

    public void setPhaseset(String phaseset) {
        this.phaseset = phaseset;
    }

    public List<Integer> getGenotypeLikelihood() {
        return genotypeLikelihood;
    }

    public void setGenotypeLikelihood(List<Integer> genotypeLikelihood) {
        this.genotypeLikelihood = genotypeLikelihood;
    }
}
