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

package com.epam.catgenome.manager.gene;

import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.manager.gene.parser.GeneFeature;
import com.epam.catgenome.util.Utils;

/**
 * Source:      GeneUtils
 * Created:     11.10.16, 14:03
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * An util class for various common operations, connected with Gene or GeneFeature classes
 * </p>
 *
 *
 */
public final class GeneUtils {
    public static final String GENE_SYMBOL_FILED = "gene_symbol";
    public static final String GENE_NAME_FILED = "gene_name";
    public static final String TRANSCRIPT_ID_FILED = "transcript_id"; // mRNA_id??
    public static final String TRANSCRIPT_NAME_FILED = "transcript_name";
    public static final String NAME_FIELD = "Name";

    private GeneUtils() {
        // no-op
    }

    private enum GeneFeatureType {
        GENE(new String[] {"gene"}),
        CHROMOSOME(new String[] {"chromosome"}),
        EXON(new String[] {"exon"}),
        TRANSCRIPT(new String[] {"transcript", "mRNA"});

        private String[] featureTypeNames;

        GeneFeatureType(String[] name) {
            featureTypeNames = name;
        }

        public String[] getFeatureTypeNames() {
            return featureTypeNames;
        }
    }

    /**
     * Tests if Gene block is a "gene" feature block
     * @param gene a gene to test
     * @return true if block is a "gene" feature block
     */
    public static boolean isGene(Gene gene) {
        return isType(GeneFeatureType.GENE, gene);
    }

    /**
     * Tests if GeneFeature is a "gene" feature
     * @param feature a feature to test
     * @return true if a feature is a "gene" feature
     */
    public static boolean isGene(GeneFeature feature) {
        return isType(GeneFeatureType.GENE, feature);
    }

    /**
     * Tests if Gene block is a "chromosome" feature block
     * @param gene a gene to test
     * @return true if block is a "chromosome" feature block
     */
    public static boolean isChromosome(Gene gene) {
        return isType(GeneFeatureType.CHROMOSOME, gene);
    }

    /**
     * Tests if GeneFeature is an "exon" feature
     * @param feature a feature to test
     * @return true if a feature is an "exon" feature
     */
    public static boolean isExon(GeneFeature feature) {
        return isType(GeneFeatureType.EXON, feature);
    }

    /**
     * Tests if Gene block is an "exon" feature block
     * @param gene a gene to test
     * @return true if block is an "exon" feature block
     */
    public static boolean isExon(Gene gene) {
        return isType(GeneFeatureType.EXON, gene);
    }

    /**
     * Tests if Gene block is a "transcript" feature block
     * @param gene a gene to test
     * @return true if block is a "transcript" feature block
     */
    public static boolean isTranscript(Gene gene) {
        return isType(GeneFeatureType.TRANSCRIPT, gene);
    }

    /**
     * Tests if GeneFeature is a "transcript" feature
     * @param feature a feature to test
     * @return true if a feature is a "transcript" feature
     */
    public static boolean isTranscript(GeneFeature feature) {
        return isType(GeneFeatureType.TRANSCRIPT, feature);
    }

    /**
     * Gets "gene_name" attribute of Gene block
     *
     * @param gene a {@code Gene} block
     * @return the value of "gene_name" attribute of a {@code Gene} block
     */
    public static String getGeneName(Gene gene) {
        if (gene.getAttributes() != null) {
            String geneName = gene.getAttributes().containsKey(GENE_NAME_FILED) ?
                              gene.getAttributes().get(GENE_NAME_FILED) : gene.getAttributes().get(GENE_SYMBOL_FILED);
            if (geneName == null && isGene(gene)) {
                geneName = gene.getAttributes().get(NAME_FIELD);
            }

            return geneName;
        }

        return null;
    }

    /**
     * Gets "transcript_id" attribute of a {@code GeneFeature}
     *
     * @param feature a {@code GeneFeature}
     * @return the value of "transcript_id" attribute of a {@code GeneFeature}
     */
    public static String getTranscriptId(GeneFeature feature) {
        if (feature.getAttributes() != null) {
            return feature.getAttributes().get(TRANSCRIPT_ID_FILED);
        }

        return null;
    }

    /**
     * Gets "transcrip_id" attribute of Gene block
     *
     * @param gene a {@code Gene} block
     * @return the value of "transcript_id" attribute of a {@code Gene} block
     */
    public static String getTranscriptId(Gene gene) {
        if (gene.getAttributes() != null) {
            return gene.getAttributes().get(TRANSCRIPT_ID_FILED);
        }

        return null;
    }

    /**
     *  Checks that Gene belongs to chromosome
     * @param gene a Gene to check
     * @param chromosome a chromosome to check
     * @return true is specified Gene belongsto specified Chromosome
     */
    public static boolean belongsToChromosome(Gene gene, Chromosome chromosome) {
        return gene.getSeqName().equalsIgnoreCase(chromosome.getName()) ||
                gene.getSeqName().equalsIgnoreCase(Utils.changeChromosomeName(chromosome.getName()));
    }

    private static boolean isType(GeneFeatureType type, Gene gene) {
        for (String featureTypeName : type.featureTypeNames) {
            if (featureTypeName.equalsIgnoreCase(gene.getFeature())) {
                return true;
            }
        }

        return false;
    }

    private static boolean isType(GeneFeatureType type, GeneFeature feature) {
        for (String featureTypeName : type.featureTypeNames) {
            if (featureTypeName.equalsIgnoreCase(feature.getFeature())) {
                return true;
            }
        }

        return false;
    }
}
