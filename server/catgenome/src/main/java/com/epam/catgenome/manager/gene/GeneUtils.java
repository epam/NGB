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
import com.epam.catgenome.entity.gene.GeneTranscript;
import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.protein.ProteinSequence;
import com.epam.catgenome.entity.protein.ProteinSequenceEntry;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.manager.gene.parser.GeneFeature;
import com.epam.catgenome.util.Utils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.epam.catgenome.manager.gene.parser.GffFeature.*;

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
    public static final String PROTEIN_CODING = "protein_coding";
    public static final ProteinSequence EMPTY_PROTEIN_SEQUENCE = new ProteinSequence(0, 0, "", 0);
    public static final String GENE_BIOTYPE = "gene_biotype";
    public static final String GENE_NAME_KEY = "gene_name";
    public static final String GENE_SYMBOL_KEY = "gene_symbol";
    public static final String MRNA_NAME_KEY = "mRNA_name";
    public static final String MRNA_SYMBOL_KEY = "mRNA_symbol";
    public static final String TRANSCRIPT_NAME_KEY = "transcript_name";
    public static final String TRANSCRIPT_SYMBOL_KEY = "transcript_symbol";

    private GeneUtils() {
        // no-op
    }

    public enum GeneFeatureType {
        GENE(new String[] {"gene"}),
        CHROMOSOME(new String[] {"chromosome"}),
        EXON(new String[] {"exon"}),
        CDS(new String[] {"CDS", "cds"}),
        UTR5(new String[] {"UTR5", "5utr", "utr5", "5UTR", "five_prime_utr"}),
        UTR3(new String[] {"UTR3", "3utr", "utr3", "3UTR", "three_prime_utr"}),
        RRNA(new String[] {"RRNA", "rrna"}),
        NCRNA(new String[] {"NCRNA", "ncrna"}),
        TRNA(new String[] {"TRNA", "trna"}),
        TMRNA(new String[] {"TMRNA", "tmrna"}),
        OPERON(new String[] {"OPERON", "operon"}),
        REGULATORY(new String[] {"REGULATORY", "regulatory"}),
        TRANSCRIPT(new String[] {"transcript", "mRNA", "mrna"});

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

    public static FeatureType fetchType(final String feature) {
        for (GeneFeatureType type : GeneFeatureType.values()) {
            for (String featureTypeName : type.featureTypeNames) {
                if (featureTypeName.equalsIgnoreCase(feature)) {
                    switch (type) {
                        case GENE:
                        case CHROMOSOME:
                            return FeatureType.GENE;
                        case OPERON:
                            return FeatureType.OPERON;
                        case TRNA:
                            return FeatureType.TRNA;
                        case TMRNA:
                            return FeatureType.TMRNA;
                        case REGULATORY:
                            return FeatureType.REGULATORY;
                        case TRANSCRIPT:
                            return FeatureType.MRNA;
                        case EXON:
                            return FeatureType.EXON;
                        case CDS:
                            return FeatureType.CDS;
                        case UTR5:
                            return FeatureType.UTR5;
                        case UTR3:
                            return FeatureType.UTR3;
                        case RRNA:
                            return FeatureType.RRNA;
                        case NCRNA:
                            return FeatureType.NCRNA;
                        default:
                            return FeatureType.GENERIC_GENE_FEATURE;
                    }
                }
            }
        }
        return null;
    }

    public static FeatureType fetchType(final GeneFeature feature) {
        return fetchType(feature.getFeature());
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

    /**
     *  Construct protein string from list of ProteinSequenceEntry
     * @return ProteinSequence
     */
    public static ProteinSequence constructProteinString(final List<ProteinSequenceEntry> proteinSequenceEntries,
                                                         final boolean reverseStrand) {
        if (ListUtils.emptyIfNull(proteinSequenceEntries).isEmpty()) {
            return EMPTY_PROTEIN_SEQUENCE;
        }
        proteinSequenceEntries.sort(Comparator.comparing(ProteinSequenceEntry::getTripleStartIndex));
        final String aminoAcidSequence = ListUtils.emptyIfNull(proteinSequenceEntries).stream()
                .map(ProteinSequenceEntry::getText)
                .filter(s -> !s.equalsIgnoreCase("stop") && !s.equalsIgnoreCase("start"))
                .collect(Collectors.joining());
        return new ProteinSequence(
                proteinSequenceEntries.get(0).getCdsStartIndex().intValue(),
                proteinSequenceEntries.get(proteinSequenceEntries.size() - 1).getCdsEndIndex().intValue(),
                reverseStrand ? StringUtils.reverse(aminoAcidSequence) : aminoAcidSequence,
                proteinSequenceEntries.stream().findFirst()
                        .map(ProteinSequenceEntry::getIndex).orElse(0L)
        );
    }

    /**
     * Returns canonical transcript for gene
     * */
    public static Gene getCanonical(final GeneTranscript gene) {
        return ListUtils.emptyIfNull(gene.getItems())
                .stream().max(canonicalTranscriptComparator()).orElse(null);
    }

    public static String getFeatureId(final String feature, final Map<String, String> attributes) {
        if (attributes == null) {
            return null;
        }

        if (GENE_FEATURE_NAME.equalsIgnoreCase(feature)) {
            return attributes.get(GENE_ID_KEY);
        }

        final String transcriptId = getTranscriptId(feature, attributes);
        if (transcriptId != null) {
            return transcriptId;
        }

        if (EXON_FEATURE_NAME.equalsIgnoreCase(feature)) {
            return attributes.get(EXON_ID_KEY);
        }
        return null;
    }

    private static String getTranscriptId(final String feature, final Map<String, String> attributes) {
        if (TRANSCRIPT_FEATURE_NAME.equalsIgnoreCase(feature) ||
                MRNA_FEATURE_NAME.equalsIgnoreCase(feature)) {
            if (attributes.containsKey(TRANSCRIPT_ID_KEY)) {
                return attributes.get(TRANSCRIPT_ID_KEY);
            }
            if (attributes.containsKey(MRNA_ID_KEY)) {
                return attributes.get(MRNA_ID_KEY);
            }
        }

        return null;
    }

    public static String getFeatureName(final String feature, final Map<String, String> attributes) {
        if (GENE_FEATURE_NAME.equalsIgnoreCase(feature)) {
            if (attributes.containsKey(GENE_NAME_KEY)) {
                return attributes.get(GENE_NAME_KEY);
            }
            return attributes.get(GENE_SYMBOL_KEY);
        }

        final String transcriptName = getTranscriptName(feature, attributes);
        if (transcriptName != null) {
            return transcriptName;
        }

        if (EXON_FEATURE_NAME.equalsIgnoreCase(feature)) {
            return getFeatureId(feature, attributes);
        }
        return null;
    }

    public static String findAttribute(final String key, final Map<String, String> attributes) {
        if (attributes == null) {
            return null;
        }

        final String possibleAttribute = attributes.get(key);
        return StringUtils.isBlank(possibleAttribute)
                ? attributes.get(StringUtils.lowerCase(key))
                : possibleAttribute;
    }

    private static String getTranscriptName(final String feature, final Map<String, String> attributes) {
        if (TRANSCRIPT_FEATURE_NAME.equalsIgnoreCase(feature) ||
                MRNA_FEATURE_NAME.equalsIgnoreCase(feature)) {
            if (attributes.containsKey(MRNA_NAME_KEY)) {
                return attributes.get(MRNA_NAME_KEY);
            }
            if (attributes.containsKey(TRANSCRIPT_NAME_KEY)) {
                return attributes.get(TRANSCRIPT_NAME_KEY);
            }
            if (attributes.containsKey(TRANSCRIPT_SYMBOL_KEY)) {
                return attributes.get(TRANSCRIPT_SYMBOL_KEY);
            }
            if (attributes.containsKey(MRNA_SYMBOL_KEY)) {
                return attributes.get(MRNA_SYMBOL_KEY);
            }
        }

        return null;
    }

    private static Comparator<Gene> canonicalTranscriptComparator() {
        return (left, right) -> {
            if (PROTEIN_CODING.equals(left.getAttributes().get(GENE_BIOTYPE)) &&
                    !PROTEIN_CODING.equals(right.getAttributes().get(GENE_BIOTYPE))) {
                return 1;
            } else if (!PROTEIN_CODING.equals(left.getAttributes().get(GENE_BIOTYPE)) &&
                    PROTEIN_CODING.equals(right.getAttributes().get(GENE_BIOTYPE))) {
                return -1;
            } else {
                long leftLen = left.getItems().stream().filter(GeneUtils::isExon)
                        .map(exon -> Math.abs(exon.getEndIndex() - exon.getStartIndex())).mapToLong(v -> v).sum();
                long rightLen = right.getItems().stream().filter(GeneUtils::isExon)
                        .map(exon -> Math.abs(exon.getEndIndex() - exon.getStartIndex())).mapToLong(v -> v).sum();
                return leftLen - rightLen >= 0 ? 1 : -1;
            }
        };
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
