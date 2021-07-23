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
package com.epam.catgenome.manager.export;

import com.epam.catgenome.entity.index.GeneIndexEntry;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.manager.FeatureIndexManager;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.util.Utils.NEW_LINE;

@Service
public class ExportManager {

    private static final String EMPTY_FIELD_VALUE = ".";

    @Value("${export.page.size:100}")
    private int exportPageSize;

    @Autowired
    private FeatureIndexManager featureIndexManager;

    public byte[] exportGenesByReference(final GeneExportFilterForm filterForm,
                                         final long referenceId,
                                         final ExportFormat format,
                                         final boolean includeHeader)
            throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        List<String> exportFields = filterForm.getExportFields();
        if (includeHeader) {
            outputStream.write(getGeneFileHeader(exportFields, format.getSeparator()).getBytes());
        }
        filterForm.setPageSize(exportPageSize);
        filterForm.setPage(1);
        setGeneAttributes(filterForm);
        IndexSearchResult<GeneIndexEntry> indexSearchResult = featureIndexManager.getGeneSearchResult(filterForm,
                featureIndexManager.getGeneFilesForReference(referenceId, filterForm.getFileIds()));
        writeGenePage(format, exportFields, indexSearchResult, outputStream);
        for (int i = 2; i <= indexSearchResult.getTotalPagesCount(); i++) {
            filterForm.setPage(i);
            indexSearchResult = featureIndexManager.getGeneSearchResult(filterForm,
                    featureIndexManager.getGeneFilesForReference(referenceId, filterForm.getFileIds()));
            writeGenePage(format, exportFields, indexSearchResult, outputStream);
        }
        return outputStream.toByteArray();
    }

    public byte[] exportVariations(final VcfExportFilterForm filterForm,
                                   final ExportFormat format,
                                   final boolean includeHeader)
            throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        List<String> exportFields = filterForm.getExportFields();
        if (includeHeader) {
            outputStream.write(getVcfFileHeader(exportFields, format.getSeparator()).getBytes());
        }
        filterForm.setPageSize(exportPageSize);
        filterForm.setPage(1);
        setVcfAttributes(filterForm);
        IndexSearchResult<VcfIndexEntry> indexSearchResult = featureIndexManager.filterVariations(filterForm);
        writeVcfPage(format, exportFields, indexSearchResult, outputStream);
        for (int i = 2; i <= indexSearchResult.getTotalPagesCount(); i++) {
            filterForm.setPage(i);
            indexSearchResult = featureIndexManager.filterVariations(filterForm);
            writeVcfPage(format, exportFields, indexSearchResult, outputStream);
        }
        return outputStream.toByteArray();
    }

    private void writeGenePage(final ExportFormat format,
                               final List<String> exportFields,
                               final IndexSearchResult<GeneIndexEntry> indexSearchResult,
                               ByteArrayOutputStream outputStream) throws IOException {
        for (GeneIndexEntry indexEntry : indexSearchResult.getEntries()) {
            List<String> fieldValues = new ArrayList<>();
            Map<String, String> attributes = MapUtils.emptyIfNull(indexEntry.getAttributes());
            for (String exportField: exportFields) {
                String value = GeneField.getByField(exportField) != null ?
                        GeneField.getByField(exportField).getGetter().apply(indexEntry) :
                        attributes.getOrDefault(exportField, EMPTY_FIELD_VALUE);
                fieldValues.add(value != null ? value : EMPTY_FIELD_VALUE);
            }
            String line = String.join(format.getSeparator(), fieldValues) + NEW_LINE;
            outputStream.write(line.getBytes());
        }
    }

    private void writeVcfPage(final ExportFormat format,
                               final List<String> exportFields,
                               final IndexSearchResult<VcfIndexEntry> indexSearchResult,
                               ByteArrayOutputStream outputStream) throws IOException {
        for (VcfIndexEntry indexEntry: indexSearchResult.getEntries()) {
            List<String> fieldValues = new ArrayList<>();
            Map<String, Object> attributes = MapUtils.emptyIfNull(indexEntry.getInfo());
            for (String exportField: exportFields) {
                String value = VcfField.getByField(exportField) != null ?
                        VcfField.getByField(exportField).getGetter().apply(indexEntry) :
                        (String) attributes.getOrDefault(exportField, EMPTY_FIELD_VALUE);
                fieldValues.add(value != null ? value : EMPTY_FIELD_VALUE);
            }
            String line = String.join(format.getSeparator(), fieldValues) + NEW_LINE;
            outputStream.write(line.getBytes());
        }
    }

    private void setGeneAttributes(GeneExportFilterForm filterForm) {
        List<String> attributesFields = new ArrayList<>();
        for (String field: filterForm.getExportFields()) {
            if (GeneField.getByField(field) == null) {
                attributesFields.add(field);
            }
        }
        filterForm.setAttributesFields(attributesFields);
    }

    private void setVcfAttributes(VcfExportFilterForm filterForm) {
        List<String> attributesFields = new ArrayList<>();
        for (String field: filterForm.getExportFields()) {
            if (VcfField.getByField(field) == null) {
                attributesFields.add(field);
            }
        }
        filterForm.setInfoFields(attributesFields);
    }

    private String getGeneFileHeader(final List<String> exportFields, final String separator) {
        List<String> header = new LinkedList<>();
        for (String exportField: exportFields) {
            if (GeneField.getByField(exportField) != null) {
                header.add(GeneField.getByField(exportField).getName());
            } else {
                header.add(exportField);
            }
        }
        return String.join(separator, header) + NEW_LINE;
    }

    private String getVcfFileHeader(final List<String> exportFields, final String separator) {
        List<String> header = new LinkedList<>();
        for (String exportField: exportFields) {
            if (VcfField.getByField(exportField) != null) {
                header.add(VcfField.getByField(exportField).getName());
            } else {
                header.add(exportField);
            }
        }
        return String.join(separator, header) + NEW_LINE;
    }
}
