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

package com.epam.catgenome.controller.vo.externaldb.ensemblevo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Source: EnsemblVariationEntryVO
 * Created: 10.02.16
 * Project: CATGenome Browser 
 *
 * <p>
 * class for extarnale DB data (variation)
 * </p>
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnsemblVariationEntryVO {

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "MAF")
    private Double maf;

    @JsonProperty(value = "ambiguity")
    private String ambiguity;

    @JsonProperty(value = "var_class")
    private String varClass;

    @JsonProperty(value = "synonyms")
    private String [] synonyms;

    @JsonProperty(value = "evidence")
    private String[] evidences;

    @JsonProperty(value = "ancestral_allele")
    private String ancestralAllele;

    @JsonProperty(value = "minor_allele")
    private String minorAllele;

    @JsonProperty(value = "most_severe_consequence")
    private String mostSevereConsequence;

    @JsonProperty(value = "mappings")
    private List<VariationMapping> mappings;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getMaf() {
        return maf;
    }

    public void setMaf(Double maf) {
        this.maf = maf;
    }

    public String getAmbiguity() {
        return ambiguity;
    }

    public void setAmbiguity(String ambiguity) {
        this.ambiguity = ambiguity;
    }

    public String getVarClass() {
        return varClass;
    }

    public void setVarClass(String varClass) {
        this.varClass = varClass;
    }

    public String[] getEvidences() {
        return evidences;
    }

    public void setEvidences(String[] evidences) {
        this.evidences = evidences;
    }

    public String getAncestralAllele() {
        return ancestralAllele;
    }

    public void setAncestralAllele(String ancestralAllele) {
        this.ancestralAllele = ancestralAllele;
    }

    public String getMinorAllele() {
        return minorAllele;
    }

    public void setMinorAllele(String minorAllele) {
        this.minorAllele = minorAllele;
    }

    public String getMostSevereConsequence() {
        return mostSevereConsequence;
    }

    public void setMostSevereConsequence(String mostSevereConsequence) {
        this.mostSevereConsequence = mostSevereConsequence;
    }

    public String[] getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(String[] synonyms) {
        this.synonyms = synonyms;
    }

    public List<VariationMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<VariationMapping> mappings) {
        this.mappings = mappings;
    }

    public static class VariationMapping extends EnsemblBaseVO {

        @JsonProperty(value = "location")
        private String location;
        @JsonProperty(value = "assembly_name")
        private String assemblyName;
        @JsonProperty(value = "seq_region_name")
        private String seqRegionName;
        @JsonProperty(value = "strand")
        private Integer strand;
        @JsonProperty(value = "coord_system")
        private String coordSystem;
        @JsonProperty(value = "allele_string")
        private String alleleString;

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getAssemblyName() {
            return assemblyName;
        }

        public void setAssemblyName(String assemblyName) {
            this.assemblyName = assemblyName;
        }

        public String getSeqRegionName() {
            return seqRegionName;
        }

        public void setSeqRegionName(String seqRegionName) {
            this.seqRegionName = seqRegionName;
        }

        public Integer getStrand() {
            return strand;
        }

        public void setStrand(Integer strand) {
            this.strand = strand;
        }

        public String getCoordSystem() {
            return coordSystem;
        }

        public void setCoordSystem(String coordSystem) {
            this.coordSystem = coordSystem;
        }

        public String getAlleleString() {
            return alleleString;
        }

        public void setAlleleString(String alleleString) {
            this.alleleString = alleleString;
        }
    }

}
