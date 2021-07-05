/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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
package com.epam.catgenome.manager.gene;

public enum GeneType {

    UTR_3("3'UTR", "3'UTR", "3'UTR"),
    UTR_5("5'UTR", "5'UTR", "5'UTR"),
    ASSEMBLY_GAP("assembly_gap", "assembly_gap", "assembly_gap"),
    C_REGION("C_region", "C_region", "pseudoC_region"),
    CDS("CDS", "CDS", "pseudogenic_exon"),
    CENTROMERE("centromere", "centromere", "centromere"),
    D_SEGMENT("D_segment", "D_segment", "pseudoD_segment"),
    D_LOOP("D-loop", "D-loop", "D-loop"),
    EXON("exon", "exon", "pseudogenic_exon"),
    GAP("gap", "gap", "gap"),
    GENE("gene", "gene", "pseudogene"),
    INTRON("intron", "intron", "pseudogenic_region"),
    I_DNA("iDNA", "iDNA", "iDNA"),
    J_SEGMENT("J-segment", "J-segment", "pseudpseudoncRNAoJ_segment"),
    MOBILE_ELEMENT("mobile_element", "mobile_element", "mobile_element"),
    M_RNA("mRNA", "mRNA", "pseudogenic_transcript"),
    N_REGION("N_region", "N_region", "pseudoN_region"),
    NC_RNA("ncRNA", "ncRNA", "pseudoncRNA"),
    OPERON("operon", "operon", "pseudooperon"),
    POLYA_SITE("polyA_site", "polyA_site", "polyA_site"),
    PROPEPTIDE("propeptide", "propeptide", "pseudopropeptide"),
    REGULATORY("regulatory", "regulatory", "pseudoregulatory"),
    REPEAT_REGION("repeat_region", "repeat_region", "repeat_region"),
    R_RNA("rRNA", "rRNA", "pseudorRNA"),
    S_REGION("S_region", "S_region", "pseudoS_region"),
    STEM_LOOP("stem_loop", "stem_loop", "stem_loop"),
    STS("STS", "STS", "STS"),
    TELOMERE("telomere", "telomere", "telomere"),
    TM_RNA("tmRNA", "tmRNA", "pseudotmRNA"),
    TRANSIT_PEPTIDE("transit_peptide", "transit_peptide", "pseudotransit_peptide"),
    T_RNA("tRNA", "tRNA", "pseudotRNA"),
    V_REGION("V_region", "V_region", "pseudoV_region"),
    V_SEGMENT("V_segment", "V_segment", "pseudoV_segment"),
    //transform
    MAT_PEPTIDE("mat_peptide", "mature_protein_region", "pseudomat_peptide"),
    MISC_BINDING("misc_binding", "binding_site", "binding_site"),
    MISC_DIFFERENCE("misc_difference", "sequence_difference", "sequence_difference"),
    MISC_FEATURE("misc_feature", Constants.REGION, "pseudogenic_region"),
    MISC_RECOMB("misc_recomb", "recombination_feature", "recombination_feature"),
    MISC_RNA("misc_RNA", "mature_transcript", "pseudogenic_transcript"),
    MISC_STRUCTURE("misc_structure", "sequence_secondary_structure", "sequence_secondary_structure"),
    MODIFIED_BASE("modified_base", "modified_base_site", "modified_base_site"),
    OLD_SEQUENCE("old_sequence", Constants.REGION, Constants.REGION),
    ORI_T("oriT", "origin_of_transfer", "origin_of_transfer"),
    PRECURSOR_RNA("precursor_RNA", Constants.PRIMARY_TRANSCRIPT, Constants.PRIMARY_TRANSCRIPT),
    PRIM_TRANSCRIPT("prim_transcript", Constants.PRIMARY_TRANSCRIPT, Constants.PRIMARY_TRANSCRIPT),
    PRIMER_BIND("primer_bind", "primer_binding_site", "primer_binding_site"),
    PROTEIN_BIND("protein_bind", "protein_binding_site", "protein_binding_site"),
    REP_ORIGIN("rep_origin", "origin_of_replication", "origin_of_replication"),
    SIG_PEPTIDE("sig_peptide", "sig_peptide", "pseudosig_peptide"),
    UNSURE("unsure", Constants.REGION, Constants.REGION),
    VARIATION("variation", "sequence_variant", "sequence_variant"),
    //source
//    SOURCE("source", "", ""),
    //default;
    DEFAULT(Constants.REGION, Constants.REGION, Constants.REGION);

    private final String genbankName;
    private final String gffName;
    private final String gffPseudoName;

    public String getGenbankName() {
        return genbankName;
    }

    public String getGffName() {
        return gffName;
    }

    public String getGffPseudoName() {
        return gffPseudoName;
    }

    GeneType(String genbankName, String gffName, String gffPseudoName) {
        this.genbankName = genbankName;
        this.gffName = gffName;
        this.gffPseudoName = gffPseudoName;
    }

    public static String getType(final String genbankName, final boolean pseudo) {
        for (GeneType type: GeneType.values()) {
            if (type.getGenbankName().equals(genbankName)) {
                return pseudo ? type.getGffPseudoName() : type.getGffName();
            }
        }
        return DEFAULT.getGffName();
    }

    private static class Constants {
        public static final String REGION = "region";
        public static final String PRIMARY_TRANSCRIPT = "primary_transcript";
    }
}
