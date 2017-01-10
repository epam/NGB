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

import java.util.ArrayList;
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
 * class for NCBI Clin-Var DB data (gene)
 * </p>
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NCBIGeneVO {

    @JsonProperty(value = "gene_id")
    private String geneId;
    @JsonProperty(value = "gene_link")
    private String geneLink;
    @JsonProperty(value = "organism_common")
    private String organismCommon;
    @JsonProperty(value = "organism_scientific")
    private String organismScientific;
    @JsonProperty(value = "primary_source")
    private String primarySource;
    @JsonProperty(value = "primary_source_prefix")
    private String primarySourcePrefix;
    @JsonProperty(value = "gene_type")
    private String geneType;
    @JsonProperty(value = "ref_seq_status")
    private String refSeqStatus;
    @JsonProperty(value = "gene_summary")
    private String geneSummary;

    @JsonProperty(value = "official_symbol")
    private String officialSymbol;
    @JsonProperty(value = "official_full_name")
    private String officialFullName;
    @JsonProperty(value = "locus_tag")
    private String locusTag;
    @JsonProperty(value = "rna_name")
    private String rnaName;
    @JsonProperty(value = "lineage")
    private String lineage;
    @JsonProperty(value = "also_known_as")
    private List<String> alsoKnownAs;
    @JsonProperty(value = "link_to_citations")
    private String linkToCitations;
    @JsonProperty(value = "link_to_homologs_citations")
    private String linkToHomologsCitations;

    @JsonProperty(value = "interactions")
    private List<NCBIGeneInteractionVO> interactions;
    @JsonProperty(value = "number_of_publications")
    private int pubNumber;
    @JsonProperty(value = "pubmed_references")
    private List<NCBISummaryVO> pubmedReferences;
    @JsonProperty(value = "number_of_pathways")
    private int pathwaysNumber;
    @JsonProperty(value = "biosystems_references")
    private List<NCBISummaryVO> biosystemsReferences;

    public String getOrganismCommon() {
        return organismCommon;
    }

    public void setOrganismCommon(String organismCommon) {
        this.organismCommon = organismCommon;
    }

    public String getOrganismScientific() {
        return organismScientific;
    }

    public void setOrganismScientific(String organismScientific) {
        this.organismScientific = organismScientific;
    }

    public String getPrimarySource() {
        return primarySource;
    }

    public void setPrimarySource(String primarySource) {
        this.primarySource = primarySource;
    }

    public String getGeneType() {
        return geneType;
    }

    public void setGeneType(String geneType) {
        this.geneType = geneType;
    }

    public String getRefSeqStatus() {
        return refSeqStatus;
    }

    public void setRefSeqStatus(String refSeqStatus) {
        this.refSeqStatus = refSeqStatus;
    }

    public String getGeneSummary() {
        return geneSummary;
    }

    public void setGeneSummary(String geneSummary) {
        this.geneSummary = geneSummary;
    }

    public List<NCBIGeneInteractionVO> getInteractions() {
        return interactions;
    }

    public void setInteractions(List<NCBIGeneInteractionVO> interactions) {
        this.interactions = interactions;
    }

    public List<NCBISummaryVO> getPubmedReferences() {
        if(pubmedReferences == null) {
            pubmedReferences = new ArrayList<>();
        }

        return pubmedReferences;
    }

    public void setPubmedReferences(List<NCBISummaryVO> pubmedReferences) {
        this.pubmedReferences = pubmedReferences;
    }

    public List<NCBISummaryVO> getBiosystemsReferences() {
        if(biosystemsReferences == null) {
            biosystemsReferences = new ArrayList<>();
        }
        return biosystemsReferences;
    }

    public void setBiosystemsReferences(List<NCBISummaryVO> biosystemsReferences) {
        this.biosystemsReferences = biosystemsReferences;
    }

    public String getOfficialSymbol() {
        return officialSymbol;
    }

    public void setOfficialSymbol(String officialSymbol) {
        this.officialSymbol = officialSymbol;
    }

    public String getOfficialFullName() {
        return officialFullName;
    }

    public void setOfficialFullName(String officialFullName) {
        this.officialFullName = officialFullName;
    }

    public String getLocusTag() {
        return locusTag;
    }

    public void setLocusTag(String locusTag) {
        this.locusTag = locusTag;
    }

    public List<String> getAlsoKnownAs() {
        return alsoKnownAs;
    }

    public void setAlsoKnownAs(List<String> alsoKnownAs) {
        this.alsoKnownAs = alsoKnownAs;
    }

    public String getPrimarySourcePrefix() {
        return primarySourcePrefix;
    }

    public void setPrimarySourcePrefix(String primarySourcePrefix) {
        this.primarySourcePrefix = primarySourcePrefix;
    }

    public String getRnaName() {
        return rnaName;
    }

    public void setRnaName(String rnaName) {
        this.rnaName = rnaName;
    }

    public String getLineage() {
        return lineage;
    }

    public void setLineage(String lineage) {
        this.lineage = lineage;
    }

    public int getPubNumber() {
        return pubNumber;
    }

    public void setPubNumber(int pubNumber) {
        this.pubNumber = pubNumber;
    }

    public int getPathwaysNumber() {
        return pathwaysNumber;
    }

    public void setPathwaysNumber(int pathwaysNumber) {
        this.pathwaysNumber = pathwaysNumber;
    }

    public String getGeneId() {
        return geneId;
    }

    public void setGeneId(String geneId) {
        this.geneId = geneId;
    }

    public String getLinkToCitations() {
        return linkToCitations;
    }

    public void setLinkToCitations(String linkToCitations) {
        this.linkToCitations = linkToCitations;
    }

    public String getLinkToHomologsCitations() {
        return linkToHomologsCitations;
    }

    public void setLinkToHomologsCitations(String linkToHomologsCitations) {
        this.linkToHomologsCitations = linkToHomologsCitations;
    }

    public String getGeneLink() {
        return geneLink;
    }

    public void setGeneLink(String geneLink) {
        this.geneLink = geneLink;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NCBIGeneInteractionVO {

        @JsonProperty(value = "product_ref")
        private String productRef;
        @JsonProperty(value = "product_name")
        private String productName;
        @JsonProperty(value = "product_id")
        private String productId;

        @JsonProperty(value = "interactant_ref")
        private String interactantRef;
        @JsonProperty(value = "interactant_name")
        private String interactantName;
        @JsonProperty(value = "interactant_id")
        private String interactantId;

        @JsonProperty(value = "othergene_ref")
        private String otherGeneRef;
        @JsonProperty(value = "othergene_name")
        private String otherGeneName;
        @JsonProperty(value = "othergene_id")
        private String otherGeneId;

        @JsonProperty(value = "complex")
        private String complexLabel;

        @JsonProperty(value = "source_name")
        private String sourceName;
        @JsonProperty(value = "source_id")
        private String sourceId;

        @JsonProperty(value = "pubmed_ids")
        private List<Long> pubmedIdList;

        @JsonProperty(value = "description")
        private String description;

        public String getProductRef() {
            return productRef;
        }

        public void setProductRef(String productRef) {
            this.productRef = productRef;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public String getInteractantRef() {
            return interactantRef;
        }

        public void setInteractantRef(String interactantRef) {
            this.interactantRef = interactantRef;
        }

        public String getInteractantName() {
            return interactantName;
        }

        public void setInteractantName(String interactantName) {
            this.interactantName = interactantName;
        }

        public String getInteractantId() {
            return interactantId;
        }

        public void setInteractantId(String interactantId) {
            this.interactantId = interactantId;
        }

        public String getOtherGeneRef() {
            return otherGeneRef;
        }

        public void setOtherGeneRef(String otherGeneRef) {
            this.otherGeneRef = otherGeneRef;
        }

        public String getOtherGeneName() {
            return otherGeneName;
        }

        public void setOtherGeneName(String otherGeneName) {
            this.otherGeneName = otherGeneName;
        }

        public String getOtherGeneId() {
            return otherGeneId;
        }

        public void setOtherGeneId(String otherGeneId) {
            this.otherGeneId = otherGeneId;
        }

        public String getComplexLabel() {
            return complexLabel;
        }

        public void setComplexLabel(String complexLabel) {
            this.complexLabel = complexLabel;
        }

        public String getSourceName() {
            return sourceName;
        }

        public void setSourceName(String sourceName) {
            this.sourceName = sourceName;
        }

        public String getSourceId() {
            return sourceId;
        }

        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }

        public List<Long> getPubmedIdList() {
            return pubmedIdList;
        }

        public void setPubmedIdList(List<Long> pubmedIdList) {
            this.pubmedIdList = pubmedIdList;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }



}
