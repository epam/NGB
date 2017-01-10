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
 * class for NCBI Clin-Var DB data (short var)
 * </p>
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NCBIShortVarVO {

    @JsonProperty(value = "snp_id")
    private Long snpId;

    @JsonProperty(value = "organism")
    private String organism;

    @JsonProperty(value = "acc")
    private String accession;

    @JsonProperty(value = "chrpos")
    private String chromosomePosition;

    @JsonProperty(value = "chr")
    private String chr;

    @JsonProperty(value = "contigpos")
    private String contigPosition;

    @JsonProperty(value = "global_maf")
    private String globalMaf;

    @JsonProperty(value = "clinical_significance")
    private String clinicalSignificance;

    @JsonProperty(value = "snp_class")
    private String snpClass;

    @JsonProperty(value = "docsum")
    private String hgvsNames;

    @JsonProperty(value = "tax_id")
    private String taxId;

    @JsonProperty(value = "handle")
    private String handle;

    @JsonProperty(value = "genes")
    private List<NCBIGeneSummaryVO> genes;

    private String fasta;

    private String genomeLabel;

    private String contigLabel;

    private String assemblyNumber;

    public Long getSnpId() {
        return snpId;
    }

    public void setSnpId(Long snpId) {
        this.snpId = snpId;
    }

    public String getOrganism() {
        return organism;
    }
    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public String getAccession() {
        return accession;
    }
    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getChromosomePosition() {
        return chromosomePosition;
    }
    public void setChromosomePosition(String chromosomePosition) {
        this.chromosomePosition = chromosomePosition;
    }

    public String getContigPosition() {
        return contigPosition;
    }
    public void setContigPosition(String contigPosition) {
        this.contigPosition = contigPosition;
    }

    public String getFasta() {
        return fasta;
    }
    public void setFasta(String fasta) {
        this.fasta = fasta;
    }

    public String getGlobalMaf() {
        return globalMaf;
    }
    public void setGlobalMaf(String globalMaf) {
        this.globalMaf = globalMaf;
    }

    public String getClinicalSignificance() {
        return clinicalSignificance;
    }

    public void setClinicalSignificance(String clinicalSignificance) {
        this.clinicalSignificance = clinicalSignificance;
    }

    public String getChr() {
        return chr;
    }

    public void setChr(String chr) {
        this.chr = chr;
    }

    public String getSnpClass() {
        return snpClass;
    }

    public void setSnpClass(String snpClass) {
        this.snpClass = snpClass;
    }

    public String getHgvsNames() {
        return hgvsNames;
    }

    public void setHgvsNames(String hgvsNames) {
        this.hgvsNames = hgvsNames;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getGenomeLabel() {
        return genomeLabel;
    }

    public void setGenomeLabel(String genomeLabel) {
        this.genomeLabel = genomeLabel;
    }

    public String getContigLabel() {
        return contigLabel;
    }

    public void setContigLabel(String contigLabel) {
        this.contigLabel = contigLabel;
    }

    public String getAssemblyNumber() {
        return assemblyNumber;
    }

    public void setAssemblyNumber(String assemblyNumber) {
        this.assemblyNumber = assemblyNumber;
    }

    public List<NCBIGeneSummaryVO> getGenes() {
        return genes;
    }

    public void setGenes(List<NCBIGeneSummaryVO> genes) {
        this.genes = genes;
    }

    public static class NCBIGeneSummaryVO {
        @JsonProperty(value = "name")
        private String name;

        @JsonProperty(value = "gene_id")
        private String geneId;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGeneId() {
            return geneId;
        }

        public void setGeneId(String geneId) {
            this.geneId = geneId;
        }
    }


}