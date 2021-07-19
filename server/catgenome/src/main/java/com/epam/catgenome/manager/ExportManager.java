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
package com.epam.catgenome.manager;

import com.epam.catgenome.entity.gene.GeneFilterForm;
import com.epam.catgenome.entity.index.GeneIndexEntry;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.util.ExportFormat;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.util.Utils.NEW_LINE;

@Service
public class ExportManager {
    private static final List<String> GENE_FIELDS = Arrays.asList("Source", "Score", "Strand", "Frame", "Feature");

    @Autowired
    private FeatureIndexManager featureIndexManager;

    public byte[] exportGenesByReference(final GeneFilterForm filterForm, final long referenceId,
                                         final ExportFormat format, final boolean includeHeader) throws IOException {
        IndexSearchResult<GeneIndexEntry> indexSearchResult = featureIndexManager.getGeneSearchResult(filterForm,
                featureIndexManager.getGeneFilesForReference(referenceId, filterForm.getFileIds()));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final List<String> additionalFields = filterForm.getAdditionalFields();
        if (includeHeader) {
            String header = getHeader(format.getSeparator(), GENE_FIELDS, additionalFields);
            outputStream.write(header.getBytes());
        }

        for (GeneIndexEntry geneIndexEntry: indexSearchResult.getEntries()) {
            List<String> additionalValues = new ArrayList<>();
            if (additionalFields != null) {
                Map<String, String> attributes = geneIndexEntry.getAttributes();
                for (String field: additionalFields) {
                    if (attributes != null) {
                        additionalValues.add(attributes.getOrDefault(field, null));
                    }
                }
            }
            String line = geneIndexEntry.getSource() + format.getSeparator() +
                    geneIndexEntry.getScore() + format.getSeparator() +
                    geneIndexEntry.getStrand() + format.getSeparator() +
                    geneIndexEntry.getFrame() + format.getSeparator() +
                    geneIndexEntry.getFeature() +
                    (CollectionUtils.isEmpty(additionalFields) ? "" :
                            format.getSeparator() + String.join(format.getSeparator(), additionalValues)) + NEW_LINE;
            outputStream.write(line.getBytes());
        }
        return outputStream.toByteArray();
    }

    @NotNull
    private String getHeader(final String separator, final List<String> fields,
                             final List<String> additionalFields) {
        return String.join(separator, fields) +
                (CollectionUtils.isEmpty(additionalFields) ? "" :
                        separator + String.join(separator, additionalFields)) + NEW_LINE;
    }
}
