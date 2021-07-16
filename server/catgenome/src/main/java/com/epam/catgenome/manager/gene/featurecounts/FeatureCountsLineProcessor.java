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

import com.epam.catgenome.util.sort.AbstractFeatureSorter;
import com.epam.catgenome.util.sort.SortableRecord;
import com.epam.catgenome.util.sort.SortableRecordCodec;
import com.google.common.io.LineProcessor;
import htsjdk.samtools.util.SortingCollection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.catgenome.manager.gene.featurecounts.FeatureCountsParserUtils.*;

@Slf4j
public class FeatureCountsLineProcessor implements LineProcessor<SortingCollection<SortableRecord>> {

    private static final int ESTIMATED_RECORD_SIZE = 500;
    private static final int DEFAULT_MAX_MEMORY = 500;
    private static final String COMMENT_MARKER = "#";

    private final Map<Integer, String> header;
    private final SortingCollection<SortableRecord> sortingCollection;

    private boolean headerProcessed;

    public FeatureCountsLineProcessor(final File tmpDir, final Map<Integer, String> header, final int maxMemory) {
        this.sortingCollection = SortingCollection.newInstance(
                SortableRecord.class,
                new SortableRecordCodec(),
                AbstractFeatureSorter.getDefaultComparator(),
                getMemory(maxMemory) * 1024 * 1024 / ESTIMATED_RECORD_SIZE, tmpDir);
        this.header = header;
    }

    @Override
    public boolean processLine(final String line) {
        if (StringUtils.isBlank(line)) {
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
        final String[] chromosomes = StringUtils.split(lineColumns[CHROMOSOMES_INDEX], FIELD_DELIMITER);
        final String[] starts = StringUtils.split(lineColumns[STARTS_INDEX], FIELD_DELIMITER);

        if (Stream.of(chromosomes.length, starts.length).distinct().count() != 1) {
            log.debug("Failed to process '{}' feature counts record. The sizes for: 'Chr', 'Start'" +
                    "shall be the same", geneId);
            return true;
        }

        sortingCollection.add(new SortableRecord(findChromosomeName(chromosomes), calculateGeneStart(starts, geneId),
                line + TAB_DELIMITER + GENE_TYPE));
        sortingCollection.add(new SortableRecord(findChromosomeName(chromosomes), calculateGeneStart(starts, geneId),
                line + TAB_DELIMITER + TRANSCRIPT_TYPE));
        for (int i = 0; i < chromosomes.length; i++) {
            sortingCollection.add(new SortableRecord(chromosomes[i], Integer.parseInt(starts[i]),
                    line + TAB_DELIMITER + EXON_TYPE));
        }

        return true;
    }

    @Override
    public SortingCollection<SortableRecord> getResult() {
        return sortingCollection;
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

    private int getMemory(final int maxMemory) {
        return maxMemory > 0 ? maxMemory : DEFAULT_MAX_MEMORY;
    }
}
