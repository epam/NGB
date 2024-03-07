/*
 * MIT License
 *
 * Copyright (c) 2024 EPAM Systems
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
package com.epam.catgenome.manager.externaldb.target.ttd;

import com.epam.catgenome.entity.externaldb.target.ttd.TTDDiseaseAssociation;
import com.epam.catgenome.entity.index.FilterType;
import com.epam.catgenome.manager.externaldb.target.AbstractAssociationManager;
import com.epam.catgenome.util.FileFormat;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.SortField;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class TTDDiseaseAssociationManager extends AbstractAssociationManager<TTDDiseaseAssociation> {

    public TTDDiseaseAssociationManager(final @Value("${targets.index.directory}") String indexDirectory,
                                        final @Value("${targets.top.hits:10000}") int targetsTopHits) {
        super(Paths.get(indexDirectory, "ttd.disease.association").toString(), targetsTopHits);
    }

    @Override
    public List<TTDDiseaseAssociation> readEntries(final String path) throws IOException {
        final List<TTDDiseaseAssociation> entries = new ArrayList<>();
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            final List<TTDRecord> records = new ArrayList<>();
            String line;
            String[] cells;
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(FileFormat.TSV.getSeparator());
                if (cells.length == 3) {
                    TTDRecord record = TTDRecord.builder()
                            .id(cells[0].trim())
                            .filed(cells[1].trim())
                            .value(cells[2].trim())
                            .build();
                    records.add(record);
                }
                if (cells.length == 5) {
                    TTDRecord record = TTDRecord.builder()
                            .id(cells[0].trim())
                            .filed(cells[1].trim())
                            .value(String.join(FileFormat.TSV.getSeparator(),
                                    Arrays.asList(cells[2].trim(), cells[3].trim(), cells[4].trim())))
                            .build();
                    records.add(record);
                }
            }
            Map<String, List<TTDRecord>> grouped = records.stream()
                    .collect(Collectors.groupingBy(TTDRecord::getId));
            grouped.forEach((k, v) -> {
                Map<String, List<TTDRecord>> groupedByField = v.stream()
                        .collect(Collectors.groupingBy(TTDRecord::getFiled));
                if (groupedByField.containsKey("INDICATI")) {
                    List<TTDRecord> diseaseRecords = groupedByField.get("INDICATI");
                    diseaseRecords.forEach(r -> {
                        if (groupedByField.containsKey("TARGNAME") && groupedByField.containsKey("TARGETID")) {
                            String targetFullName = groupedByField.get("TARGNAME").get(0).getValue();
                            TTDDiseaseAssociation entry = TTDDiseaseAssociation.builder()
                                    .ttdGeneId(groupedByField.get("TARGETID").get(0).getValue())
                                    .ttdTarget(getTargetName(targetFullName))
                                    .build();
                            String[] diseaseProperties = r.getValue().split(FileFormat.TSV.getSeparator());
                            entry.setClinicalStatus(diseaseProperties[0]);
                            entry.setName(diseaseProperties[1]);
                            entries.add(entry);
                        }
                    });
                }
            });
        }
        return entries;
    }

    @Override
    public SortField getDefaultSortField() {
        return null;
    }

    @Override
    public List<TTDDiseaseAssociation> processEntries(final List<TTDDiseaseAssociation> entries)
            throws IOException, ParseException {
        return entries;
    }

    @Override
    public void addDoc(final IndexWriter writer, final TTDDiseaseAssociation entry) throws IOException {
        final Document doc = new Document();
        doc.add(new TextField(TTDDiseaseField.TTD_GENE_ID.name(), entry.getTtdGeneId(), Field.Store.YES));
        doc.add(new TextField(TTDDiseaseField.TTD_TARGET.name(), entry.getTtdTarget(), Field.Store.YES));
        doc.add(new TextField(TTDDiseaseField.DISEASE_NAME.name(), entry.getName(), Field.Store.YES));

        final String phase = Optional.ofNullable(entry.getClinicalStatus()).orElse("");
        doc.add(new StringField(TTDDiseaseField.CLINICAL_STATUS.name(), phase, Field.Store.YES));
        writer.addDocument(doc);
    }

    @Override
    public TTDDiseaseAssociation entryFromDoc(final Document doc) {
        return TTDDiseaseAssociation.builder()
                .ttdGeneId(doc.getField(TTDDiseaseField.TTD_GENE_ID.name()).stringValue())
                .ttdTarget(doc.getField(TTDDiseaseField.TTD_TARGET.name()).stringValue())
                .name(doc.getField(TTDDiseaseField.DISEASE_NAME.name()).stringValue())
                .clinicalStatus(doc.getField(TTDDiseaseField.CLINICAL_STATUS.name()).stringValue())
                .build();
    }

    @Override
    public FilterType getFilterType(String fieldName) {
        return TTDDiseaseField.valueOf(fieldName).getType();
    }

    private static String getTargetName(final String targetFullName) {
        String[] subs = targetFullName.split(" \\(");
        if (subs.length > 1) {
            return subs[1].replace(")", "");
        }
        return "";
    }
}
