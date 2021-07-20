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

import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.epam.catgenome.manager.gene.writer.Gff3FeatureImpl;
import com.epam.catgenome.manager.gene.writer.Gff3Writer;
import com.epam.catgenome.util.IOHelper;
import com.epam.catgenome.util.sort.SortableRecord;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;
import htsjdk.samtools.util.SortingCollection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.epam.catgenome.manager.gene.featurecounts.FeatureCountsParserUtils.*;

@Slf4j
public class FeatureCountsToGffConvertor {

    public void convert(final String featureCountsFilePath, final String gffFilePath, final File tmpDir,
                        final int maxMemory) {
        try (Gff3Writer gff3Writer = new Gff3Writer(Paths.get(gffFilePath));
             Reader reader = getReader(featureCountsFilePath)) {
            final Map<Integer, String> header = new HashMap<>();
            final LineProcessor<SortingCollection<SortableRecord>> processor =
                    new FeatureCountsLineProcessor(tmpDir, header, maxMemory);
            SortingCollection<SortableRecord> sortableRecords = null;
            try {
                sortableRecords = CharStreams.readLines(reader, processor);
                StreamSupport.stream(sortableRecords.spliterator(), false)
                        .map(record -> recordToGffFeature(record, header))
                        .filter(Objects::nonNull)
                        .forEach(feature -> writeGffFeature(feature, gff3Writer));
            } finally {
                if (Objects.nonNull(sortableRecords)) {
                    sortableRecords.cleanup();
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot convert data.", e);
        }
    }

    private static BufferedReader getReader(final String featureCountsFilePath) throws IOException {
        return new BufferedReader(new InputStreamReader(
                IOHelper.openStream(featureCountsFilePath), StandardCharsets.UTF_8));
    }

    private Gff3FeatureImpl recordToGffFeature(final SortableRecord record, final Map<Integer, String> header) {
        final String chromosome = record.getChromosome();
        final int start = record.getStart();
        final String line = record.getText();
        final String[] lineColumns = StringUtils.split(line, TAB_DELIMITER);

        final String geneId = lineColumns[GENE_ID_INDEX];
        final String[] starts = StringUtils.split(lineColumns[STARTS_INDEX], FIELD_DELIMITER);
        final String[] ends = StringUtils.split(lineColumns[ENDS_INDEX], FIELD_DELIMITER);
        final String[] strands = StringUtils.split(lineColumns[STRANDS_INDEX], FIELD_DELIMITER);

        if (Stream.of(starts.length, ends.length, strands.length).distinct().count() != 1) {
            log.debug("Failed to process '{}' feature counts record. The sizes for: 'Start', 'End', 'Strand' " +
                    "shall be the same", geneId);
            return null;
        }

        final Map<String, List<String>> attributes = parseAdditionalColumns(filterUtilTypeField(lineColumns), header);
        final String featureType = lineColumns[lineColumns.length - 1];

        switch (featureType) {
            case GENE_TYPE:
                return buildGeneGffFeature(chromosome, start, calculateGeneEnd(ends, geneId),
                        lineColumns[LENGTH_INDEX], geneId, strands, attributes);
            case TRANSCRIPT_TYPE:
                return buildTranscriptGffFeature(chromosome, start, calculateGeneEnd(ends, geneId),
                        lineColumns[LENGTH_INDEX], strands, geneId, attributes);
            case EXON_TYPE:
                return buildExonGffFeature(chromosome, start, geneId, starts, ends, strands, attributes);
            default:
                log.debug("Failed to determine feature type '{}'. Supported types: exon, gene.", featureType);
                return null;
        }
    }

    private Gff3FeatureImpl buildExonGffFeature(final String chromosome, final int start, final String geneId,
                                                final String[] starts, final String[] ends, final String[] strands,
                                                final Map<String, List<String>> attributes) {
        final List<String> exonIdAttribute = buildFeatureIdAttribute(EXON_TYPE, geneId);
        final List<String> transcriptIdAttribute = buildFeatureIdAttribute(TRANSCRIPT_TYPE, geneId);
        final int exonIndex = findIndex(starts, start);
        if (exonIndex == -1) {
            log.warn("Failed to parse exon for gene '{}'", geneId);
            return null;
        }
        final int end = Integer.parseInt(ends[exonIndex]);
        final StrandSerializable strand = StrandSerializable.forValue(strands[exonIndex]);
        final int length = end - start + 1;
        attributes.put(EXON_ID_ATTRIBUTE_NAME, exonIdAttribute);
        attributes.put(PARENT_ATTRIBUTE_NAME, transcriptIdAttribute);
        return buildGffFeature(attributes, length, chromosome, start, end, strand, EXON_TYPE);
    }

    private Gff3FeatureImpl buildTranscriptGffFeature(final String chromosome, final int start, final int end,
                                                      final String lengthColumn, final String[] strands,
                                                      final String geneId,
                                                      final Map<String, List<String>> attributes) {
        final List<String> transcriptIdAttribute = buildFeatureIdAttribute(TRANSCRIPT_TYPE, geneId);
        final List<String> geneIdAttribute = buildFeatureIdAttribute(GENE_TYPE, geneId);
        final StrandSerializable strand = determineGeneStrand(strands);
        final int length = Integer.parseInt(lengthColumn);
        attributes.put(TRANSCRIPT_ID_ATTRIBUTE_NAME, transcriptIdAttribute);
        attributes.put(ID_ATTRIBUTE_NAME, transcriptIdAttribute);
        attributes.put(PARENT_ATTRIBUTE_NAME, geneIdAttribute);
        return buildGffFeature(attributes, length, chromosome, start, end, strand, TRANSCRIPT_TYPE);
    }

    private Gff3FeatureImpl buildGeneGffFeature(final String chromosome, final int start, final int end,
                                                final String lengthColumn, final String geneId, final String[] strands,
                                                final Map<String, List<String>> attributes) {
        final List<String> geneIdAttribute = buildFeatureIdAttribute(GENE_TYPE, geneId);
        final StrandSerializable strand = determineGeneStrand(strands);
        final int length = Integer.parseInt(lengthColumn);
        attributes.put(GENE_ID_ATTRIBUTE_NAME, Collections.singletonList(geneId));
        attributes.put(ID_ATTRIBUTE_NAME, geneIdAttribute);
        return buildGffFeature(attributes, length, chromosome, start, end, strand, GENE_TYPE);
    }

    private String[] filterUtilTypeField(final String[] lineColumns) {
        return (String[]) ArrayUtils.remove(lineColumns, lineColumns.length - 1);
    }

    private void writeGffFeature(final Gff3FeatureImpl feature, final Gff3Writer gff3Writer) {
        try {
            gff3Writer.addFeature(feature);
        } catch (IOException e) {
            log.debug(e.getMessage());
        }
    }

    private Gff3FeatureImpl buildGffFeature(final Map<String, List<String>> attributes, final int length,
                                            final String contig, final int start, final int end,
                                            final StrandSerializable strand, final String type) {
        attributes.put(LENGTH_ATTRIBUTE_NAME, Collections.singletonList(String.valueOf(length)));
        return new Gff3FeatureImpl(contig,
                FEATURE_COUNTS_SOURCE,
                type,
                start,
                end,
                EMPTY_VALUE,
                strand,
                EMPTY_VALUE,
                attributes);
    }

    private int findIndex(final String[] starts, final int targetStart) {
        for (int i = 0; i < starts.length; i++) {
            if (targetStart == Integer.parseInt(starts[i])) {
                return i;
            }
        }
        return -1;
    }

    private List<String> buildFeatureIdAttribute(final String featureType, final String geneId) {
        return Collections.singletonList(featureType + "." + geneId);
    }

    private int calculateGeneEnd(final String[] ends, final String geneId) {
        return Stream.of(ends)
                .mapToInt(Integer::parseInt)
                .max()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Failed to calculate end for gene '%s'", geneId)));
    }

    private StrandSerializable determineGeneStrand(final String[] strands) {
        final Set<String> strandsSet = Stream.of(strands).collect(Collectors.toSet());
        if (strandsSet.size() != 1) {
            throw new IllegalArgumentException("Multiple strands is not supported for 'FEATURE_COUNTS' format");
        }
        return StrandSerializable.forValue(strands[0]);
    }

    private Map<String, List<String>> parseAdditionalColumns(final String[] allColumns,
                                                            final Map<Integer, String> header) {
        if (allColumns.length == MANDATORY_COLUMNS_COUNT) {
            return Collections.emptyMap();
        }

        return header.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, entry ->
                        Collections.singletonList(buildAdditionalColumnValue(allColumns, entry.getKey()))));
    }

    private String buildAdditionalColumnValue(final String[] allColumns, final Integer index) {
        return allColumns.length < index ? EMPTY_VALUE : allColumns[index];
    }
}
