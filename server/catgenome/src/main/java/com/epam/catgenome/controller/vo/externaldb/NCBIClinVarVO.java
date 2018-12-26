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

package com.epam.catgenome.controller.vo.externaldb;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Source:      NCBIEntryVO
 * Created:     09.02.16, 13:52
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.0.2, JDK 1.8
 *
 * <p>
 * class for NCBI Clin-Var DB data
 * </p>
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NCBIClinVarVO {

    @JsonProperty(value = "variation_set")
    private List<NCBIVariationSet> variationSet;

    @JsonProperty(value = "clinical_significance")
    private NCBIClinicalSignificance clinicalSignificance;

    @JsonProperty(value = "trait_set")
    private List<NCBITraitSet> traitSet;

    @JsonProperty(value = "clinvar_link")
    private String clinvarLink;

    public String getClinvarLink() {
        return clinvarLink;
    }

    public void setClinvarLink(String clinvarLink) {
        this.clinvarLink = clinvarLink;
    }

    public NCBIClinicalSignificance getClinicalSignificance() {
        return clinicalSignificance;
    }

    public void setClinicalSignificance(NCBIClinicalSignificance clinicalSignificance) {
        this.clinicalSignificance = clinicalSignificance;
    }

    public List<NCBIVariationSet> getVariationSet() {
        return variationSet;
    }

    public void setVariationSet(List<NCBIVariationSet> variationSet) {
        this.variationSet = variationSet;
    }

    public List<NCBITraitSet> getTraitSet() {
        return traitSet;
    }

    public void setTraitSet(List<NCBITraitSet> traitSet) {
        this.traitSet = traitSet;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NCBIClinicalSignificance {

        public static final String PATHOGENIC = "Pathogenic";

        @JsonProperty(value = "description")
        private String description;

        @JsonProperty(value = "last_evaluated")
        private String lastEvaluated;

        @JsonProperty(value = "review_status")
        private String reviewStatus;

        public boolean isPathogenic() {
            return description.equals(PATHOGENIC);
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getReviewStatus() {
            return reviewStatus;
        }

        public void setReviewStatus(String reviewStatus) {
            this.reviewStatus = reviewStatus;
        }

        public String getLastEvaluated() {
            return lastEvaluated;
        }

        public void setLastEvaluated(String lastEvaluated) {
            this.lastEvaluated = lastEvaluated;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NCBIVariationSet {
        @JsonProperty(value = "variation_xrefs")
        private List<NCBIXref> variationXrefs;

        public List<NCBIXref> getVariationXrefs() {
            return variationXrefs;
        }

        public void setVariationXrefs(List<NCBIXref> variationXrefs) {
            this.variationXrefs = variationXrefs;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NCBITraitSet {

        @JsonProperty(value = "trait_xrefs")
        private List<NCBIXref> traitXrefList;

        @JsonProperty(value = "trait_name")
        private String traitName;

        public List<NCBIXref> getTraitXrefList() {
            return traitXrefList;
        }

        public void setTraitXrefList(List<NCBIXref> traitXrefList) {
            this.traitXrefList = traitXrefList;
        }

        public String getTraitName() {
            return traitName;
        }

        public void setTraitName(String traitName) {
            this.traitName = traitName;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NCBIXref {

        @JsonProperty(value = "db_source")
        private String dbSource;

        @JsonProperty(value = "db_id")
        private String dbId;


        public String getDbSource() {
            return dbSource;
        }

        public void setDbSource(String dbSource) {
            this.dbSource = dbSource;
        }

        public String getDbId() {
            return dbId;
        }

        public void setDbId(String dbId) {
            this.dbId = dbId;
        }
    }

}
