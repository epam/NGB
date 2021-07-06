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

package com.epam.catgenome.manager.gene.featurecounts;

import com.epam.catgenome.entity.gene.FeatureCounts;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.epam.catgenome.manager.gene.writer.Gff3FeatureImpl;
import com.epam.catgenome.manager.gene.writer.Gff3Writer;
import com.google.common.io.LineProcessor;
import htsjdk.tribble.annotation.Strand;
import htsjdk.tribble.bed.SimpleBEDFeature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class FeatureCountsLineProcessor implements LineProcessor<FeatureCountsReaderStatus> {
    private static final String TAB_DELIMITER = "\t";
    private static final String FIELD_DELIMITER = ";";
    private static final String COMMENT_MARKER = "#";
    private static final String EMPTY_VALUE = ".";
    private static final int GENE_ID_INDEX = 0;
    private static final int CHROMOSOMES_INDEX = 1;
    private static final int STARTS_INDEX = 2;
    private static final int ENDS_INDEX = 3;
    private static final int STRANDS_INDEX = 4;
    private static final int LENGTH_INDEX = 5;
    private static final int MANDATORY_COLUMNS_COUNT = 6;
    private static final String FEATURE_COUNTS_SOURCE = "featureCounts";
    private static final String LENGTH_ATTRIBUTE_NAME = "Length";
    private static final String GENE_ID_ATTRIBUTE_NAME = "Geneid";
    private static final String GENE_TYPE = "gene";
    private static final String EXON_TYPE = "exon";

    private final Map<Integer, String> header;
    private final Gff3Writer gff3Writer;
    private boolean headerProcessed;
    private FeatureCountsReaderStatus result;

    public FeatureCountsLineProcessor(final Gff3Writer gff3Writer) {
        this.result = FeatureCountsReaderStatus.PROCESSING;
        this.header = new HashMap<>();
        this.gff3Writer = gff3Writer;
    }

    @Override
    public boolean processLine(final String line) {
        if (StringUtils.isBlank(line)) {
            result = FeatureCountsReaderStatus.SUCCESSFUL;
            return false;
        }

        if (line.startsWith(COMMENT_MARKER)) {
            return true;
        }

        if (!headerProcessed) {
            headerProcessed = true;
            processHeader(line);
            return true;
        }

        final String[] lineColumns = StringUtils.split(line, TAB_DELIMITER);

        if (notEnoughColumns(lineColumns)) {
            log.debug("Record contains not enough columns");
            return true;
        }

        final String geneId = lineColumns[GENE_ID_INDEX];
        final FeatureCounts.FeatureCountsBuilder featureBuilder = FeatureCounts.builder()
                .geneId(geneId)
                .length(Integer.parseInt(lineColumns[LENGTH_INDEX]))
                .additional(parseAdditionalColumns(lineColumns));
        parseFeatures(lineColumns[CHROMOSOMES_INDEX],
                lineColumns[STARTS_INDEX],
                lineColumns[ENDS_INDEX],
                lineColumns[STRANDS_INDEX],
                geneId,
                featureBuilder);

        featureCountToGff(featureBuilder.build()).stream()
                .sorted(Comparator.comparing(Gff3FeatureImpl::getContig)
                        .thenComparing(Gff3FeatureImpl::getStart))
                .forEach(this::writeGffFeature);

        return true;
    }

    @Override
    public FeatureCountsReaderStatus getResult() {
        return result;
    }

    private boolean notEnoughColumns(final String[] columns) {
        return columns.length < MANDATORY_COLUMNS_COUNT;
    }

    private void processHeader(final String line) {
        final String[] headerColumns = StringUtils.split(line, TAB_DELIMITER);
        if (notEnoughColumns(headerColumns)) {
            throw new IllegalStateException("Header contains not enough columns");
        }
        for (int i = MANDATORY_COLUMNS_COUNT; i < headerColumns.length; i++) {
            header.put(i, headerColumns[i]);
        }
    }

    private void parseFeatures(final String rawChromosomes, final String rawStarts,
                               final String rawEnds, final String rawStrands,
                               final String geneId, final FeatureCounts.FeatureCountsBuilder featureBuilder) {
        final String[] chromosomes = StringUtils.split(rawChromosomes, FIELD_DELIMITER);
        final String[] starts = StringUtils.split(rawStarts, FIELD_DELIMITER);
        final String[] ends = StringUtils.split(rawEnds, FIELD_DELIMITER);
        final String[] strands = StringUtils.split(rawStrands, FIELD_DELIMITER);

        if (Stream.of(chromosomes.length, starts.length, ends.length, strands.length).distinct().count() != 1) {
            log.debug("Failed to process '{}' feature counts record. The sizes for: 'Chr', 'Start', 'End', 'Strand' " +
                    "shall be the same", geneId);
            return;
        }

        final List<SimpleBEDFeature> features = new ArrayList<>();
        for (int i = 0; i < chromosomes.length; i++) {
            final SimpleBEDFeature feature = new SimpleBEDFeature(Integer.parseInt(starts[i]),
                    Integer.parseInt(ends[i]), chromosomes[i]);
            feature.setStrand(Strand.toStrand(strands[i]));
            features.add(feature);
        }

        featureBuilder
                .features(features)
                .chromosome(findChromosomeName(chromosomes))
                .start(calculateGeneStart(starts, geneId))
                .end(calculateGeneEnd(ends, geneId))
                .strand(determineGeneStrand(strands));
    }

    private Map<String, String> parseAdditionalColumns(final String[] allColumns) {
        if (allColumns.length == MANDATORY_COLUMNS_COUNT) {
            return null;
        }

        return header.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, entry ->
                        buildAdditionalColumnValue(allColumns, entry.getKey())));
    }

    private String buildAdditionalColumnValue(final String[] allColumns, final Integer index) {
        return allColumns.length < index ? EMPTY_VALUE : allColumns[index];
    }

    private String findChromosomeName(final String[] chromosomes) {
        final Set<String> chromosomesSet = Stream.of(chromosomes).collect(Collectors.toSet());
        if (chromosomesSet.size() != 1) {
            throw new IllegalArgumentException("Multiple chromosomes is not supported for 'FEATURE_COUNTS' format");
        }
        return chromosomes[0];
    }

    private int calculateGeneStart(final String[] starts, final String geneId) {
        return Stream.of(starts)
                .mapToInt(Integer::parseInt)
                .min()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Failed to calculate start for gene '%s'", geneId)));
    }

    private int calculateGeneEnd(final String[] ends, final String geneId) {
        return Stream.of(ends)
                .mapToInt(Integer::parseInt)
                .min()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Failed to calculate end for gene '%s'", geneId)));
    }

    private Strand determineGeneStrand(final String[] strands) {
        final Set<String> strandsSet = Stream.of(strands).collect(Collectors.toSet());
        if (strandsSet.size() != 1) {
            throw new IllegalArgumentException("Multiple strands is not supported for 'FEATURE_COUNTS' format");
        }
        return Strand.toStrand(strands[0]);
    }

    private List<Gff3FeatureImpl> featureCountToGff(final FeatureCounts featureCount) {
        final Map<String, List<String>> attributes = featureCount.getAdditional().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry ->
                        Collections.singletonList(entry.getValue())));
        attributes.put(GENE_ID_ATTRIBUTE_NAME, Collections.singletonList(featureCount.getGeneId()));

        final List<Gff3FeatureImpl> gffFeatures = featureCount.getFeatures().stream()
                .map(feature -> featureCountToExon(attributes, feature))
                .collect(Collectors.toList());
        gffFeatures.add(featureCountToGffGene(attributes, featureCount));
        return gffFeatures;
    }

    private Gff3FeatureImpl featureCountToGffGene(final Map<String, List<String>> attributes,
                                                  final FeatureCounts feature) {
        return buildGffFeature(attributes, feature.getLength(),
                feature.getChromosome(), feature.getStart(), feature.getEnd(),
                feature.getStrand(), GENE_TYPE);
    }

    private Gff3FeatureImpl featureCountToExon(final Map<String, List<String>> attributes,
                                               final SimpleBEDFeature feature) {
        return buildGffFeature(attributes,
                feature.getEnd() - feature.getStart() + 1,
                feature.getContig(), feature.getStart(), feature.getEnd(),
                feature.getStrand(), EXON_TYPE);
    }

    private Gff3FeatureImpl buildGffFeature(final Map<String, List<String>> attributes, final int length,
                                            final String contig, final int start, final int end,
                                            final Strand strand, final String type) {
        attributes.put(LENGTH_ATTRIBUTE_NAME, Collections.singletonList(String.valueOf(length)));
        return new Gff3FeatureImpl(contig,
                FEATURE_COUNTS_SOURCE,
                type,
                start,
                end,
                EMPTY_VALUE,
                StrandSerializable.valueOf(strand.name()),
                EMPTY_VALUE,
                attributes);
    }

    private void writeGffFeature(final Gff3FeatureImpl feature) {
        try {
            gff3Writer.addFeature(feature);
        } catch (IOException e) {
            log.debug(e.getMessage());
        }
    }
}
