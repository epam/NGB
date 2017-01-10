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

package com.epam.catgenome.entity.vcf;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.epam.catgenome.entity.track.Block;

/**
 * Represents VCF track block, parsed from a VCF file, the structure of this class is
 * similar to the VCF file line.
 */
public class Variation extends Block {

    private String identifier;
    private String referenceAllele;
    private List<String> alternativeAlleles;
    private Double quality;
    private List<Filter> failedFilters;
    private VariationType type;
    private GenotypeData genotypeData;
    private Map<String, InfoField> info;
    private Map<String, Object> bindInfo;
    private boolean structural = false;
    private Set<String> geneNames;

    /**
     * On smaller scales defines that a certain number of variations is present in this pixel
     */
    private Integer variationsCount;

    public Variation() {
        // No-op
    }

    /**
     * @param startIndex border of a block
     * @param endIndex border of a block
     * @param referenceAllele reference sequence
     * @param alternativeAlleles {@code List} of variation sequences present at the block
     */
    public Variation(int startIndex, int endIndex, String referenceAllele, List<String> alternativeAlleles) {
        this.referenceAllele = referenceAllele;
        this.alternativeAlleles = alternativeAlleles;

        setStartIndex(startIndex);
        setEndIndex(endIndex);
    }

    public String getReferenceAllele() {
        return referenceAllele;
    }

    public void setReferenceAllele(String referenceAllele) {
        this.referenceAllele = referenceAllele;
    }

    public List<String> getAlternativeAlleles() {
        return alternativeAlleles;
    }

    public void setAlternativeAlleles(List<String> alternativeAlleles) {
        this.alternativeAlleles = alternativeAlleles;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Double getQuality() {
        return quality;
    }

    public void setQuality(Double quality) {
        this.quality = quality;
    }

    public List<Filter> getFailedFilters() {
        return failedFilters;
    }

    public void setFailedFilters(List<Filter> failedFilters) {
        this.failedFilters = failedFilters;
    }

    public VariationType getType() {
        return type;
    }

    public void setType(VariationType type) {
        this.type = type;
    }

    public Map<String, InfoField> getInfo() {
        return info;
    }

    public void setInfo(Map<String, InfoField> info) {
        this.info = info;
    }

    public Integer getVariationsCount() {
        return variationsCount;
    }

    public void setVariationsCount(Integer variationsCount) {
        this.variationsCount = variationsCount;
    }

    public GenotypeData getGenotypeData() {
        return genotypeData;
    }

    public void setGenotypeData(GenotypeData genotypeData) {
        this.genotypeData = genotypeData;
    }

    public boolean isStructural() {
        return structural;
    }

    public void setStructural(boolean structural) {
        this.structural = structural;
    }

    public Map<String, Object> getBindInfo() {
        return bindInfo;
    }

    public void setBindInfo(Map<String, Object> bindInfo) {
        this.bindInfo = bindInfo;
    }

    public Set<String> getGeneNames() {
        return geneNames;
    }

    public void setGeneNames(Set<String> geneNames) {
        this.geneNames = geneNames;
    }

    public static class InfoField {
        private String description;
        private Object value;

        public InfoField() {
            //no operations
        }

        public InfoField(String description, Object value) {
            this.description = description;
            this.value = value;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
