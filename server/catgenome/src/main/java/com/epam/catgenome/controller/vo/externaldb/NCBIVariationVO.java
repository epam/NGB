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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Source:      NCBITaxonomyVO
 * Created:     16.03.16, 13:52
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.0.2, JDK 1.8
 *
 * <p>
 * class for NCBI Clin-Var DB data (variation)
 * </p>
 *
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class NCBIVariationVO {

    public static final String PATHOGENIC = "Pathogenic";

    @JsonProperty(value = "variation")
    private NCBIShortVarVO ncbiShortVar;

    @JsonProperty(value = "clinVar")
    private NCBIClinVarVO ncbiClinVar;

    @JsonProperty(value = "taxonomy")
    private NCBITaxonomyVO ncbiTaxonomy;

    public NCBIShortVarVO getNcbiShortVar() {
        return ncbiShortVar;
    }

    public void setNcbiShortVar(NCBIShortVarVO ncbiShortVar) {
        this.ncbiShortVar = ncbiShortVar;
    }

    public NCBIClinVarVO getNcbiClinVar() {
        return ncbiClinVar;
    }

    public void setNcbiClinVar(NCBIClinVarVO ncbiClinVar) {
        this.ncbiClinVar = ncbiClinVar;
    }

    public NCBITaxonomyVO getNcbiTaxonomy() {
        return ncbiTaxonomy;
    }

    public void setNcbiTaxonomy(NCBITaxonomyVO ncbiTaxonomy) {
        this.ncbiTaxonomy = ncbiTaxonomy;
    }

    public boolean isPathogenic() {
        return StringUtils.equals(ncbiShortVar.getClinicalSignificance(), PATHOGENIC);
    }

    @JsonProperty(value = "organism_summary")
    public String getOrganism() {
        return ncbiTaxonomy.getCommonname() + " (" + ncbiTaxonomy.getScientificname() + ")";
    }

    @JsonProperty(value = "gene_summary")
    public String getGene() {
        List<NCBIShortVarVO.NCBIGeneSummaryVO> genes = ncbiShortVar.getGenes();
        Optional<NCBIShortVarVO.NCBIGeneSummaryVO> first = genes.stream().findFirst();
        return first.isPresent() ? String.format("%s (%s)", first.get().getName(), first.get().getGeneId()) : null;
    }

    @JsonProperty(value = "refsnp_alleles")
    public String getAlleles(){
        String fasta = ncbiShortVar.getFasta();
        return StringUtils.isNotBlank(fasta) ? regexExtract(Pattern.compile(".*alleles=\"([\\w/]+)\".*"), fasta) : null;
    }

    @JsonProperty(value = "ref_seq_gene_mapping")
    public RefSeqGeneMapping getRefSeqGeneMapping() {
        RefSeqGeneMapping refSeqGeneMapping = new RefSeqGeneMapping();
        String docsum = ncbiShortVar.getHgvsNames();
        String info = StringUtils.isNotBlank(docsum)
                ? regexExtract(Pattern.compile(".*(NG_\\d+\\.\\d+:g\\.\\d+\\w+).*"), docsum)
                : null;
        if (info != null) {
            refSeqGeneMapping.setRefSeqGene(regexExtract(Pattern.compile(".*(NG_\\d+\\.\\d+).*"), info));
            refSeqGeneMapping.setPosition(regexExtract(Pattern.compile(".*:g\\.(\\d+).*"), info));
            refSeqGeneMapping.setAllele(regexExtract(Pattern.compile(".*:g\\.\\d+(\\w+).*"), info));
            refSeqGeneMapping.setGene(regexExtract(Pattern.compile(".*GENE=(\\w+:\\d+).*"), docsum));
        }
        return refSeqGeneMapping;
    }

    @JsonProperty(value = "maf_source")
    public String getMafSource(){
        String handle = ncbiShortVar.getHandle();
        return StringUtils.isNotBlank(handle) ? regexExtract(Pattern.compile("^([\\w-]*),?.*"), handle) : null;
    }

    @JsonProperty(value = "assembly_number")
    public String getAssemblyNumber(){
        return ncbiShortVar.getAssemblyNumber();
    }

    @JsonProperty(value = "assembly_name")
    public String getAssemblyName(){
        return ncbiShortVar.getGenomeLabel();
    }

    private static String regexExtract(Pattern p, String sourceString){
        String result = null;
        Matcher m = p.matcher(sourceString);

        if(m.matches()) {
            result = m.group(1);
        }

        return result;
    }

    public class RefSeqGeneMapping {

        @JsonProperty
        private String refSeqGene;

        @JsonProperty
        private String gene;

        @JsonProperty
        private String position;

        @JsonProperty
        private String allele;

        public String getRefSeqGene() {
            return refSeqGene;
        }

        public void setRefSeqGene(String refSeqGene) {
            this.refSeqGene = refSeqGene;
        }

        public String getGene() {
            return gene;
        }

        public void setGene(String gene) {
            this.gene = gene;
        }

        public String getPosition() {
            return position;
        }

        public void setPosition(String position) {
            this.position = position;
        }

        public String getAllele() {
            return allele;
        }

        public void setAllele(String allele) {
            this.allele = allele;
        }
    }
}

