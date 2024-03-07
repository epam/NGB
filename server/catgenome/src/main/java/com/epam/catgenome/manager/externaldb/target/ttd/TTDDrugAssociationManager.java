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

import com.epam.catgenome.entity.externaldb.target.ttd.TTDDrugAssociation;
import com.epam.catgenome.entity.index.FilterType;
import com.epam.catgenome.manager.externaldb.target.AbstractAssociationManager;
import com.epam.catgenome.manager.index.CaseInsensitiveWhitespaceAnalyzer;
import com.epam.catgenome.util.FileFormat;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TTDDrugAssociationManager extends AbstractAssociationManager<TTDDrugAssociation> {

    public TTDDrugAssociationManager(final @Value("${targets.index.directory}") String indexDirectory,
                                     final @Value("${targets.top.hits:10000}") int targetsTopHits) {
        super(Paths.get(indexDirectory, "ttd.drug.association").toString(), targetsTopHits);
    }

    public void importData(final String drugsPath, final String targetsPath) throws IOException, ParseException {
        final List<TTDDrugAssociation> entries = readEntries(targetsPath);
        processEntries(entries, drugsPath);
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new CaseInsensitiveWhitespaceAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            for (TTDDrugAssociation entry: entries) {
                addDoc(writer, entry);
            }
        }
    }

    public void processEntries(final List<TTDDrugAssociation> entries, final String drugsPath)
            throws IOException, ParseException {
        final List<TTDDrugAssociation> drugs = readDrugs(drugsPath);
        final Map<String, TTDDrugAssociation> drugsMap = drugs.stream()
                .collect(Collectors.toMap(TTDDrugAssociation::getId, Function.identity()));
        entries.forEach(e -> {
            if (drugsMap.containsKey(e.getId())) {
                TTDDrugAssociation drug = drugsMap.get(e.getId());
                e.setCompany(drug.getCompany());
                e.setType(drug.getType());
                e.setTherapeuticClass(drug.getTherapeuticClass());
                e.setInChI(drug.getInChI());
                e.setInChIKey(drug.getInChIKey());
                e.setCanonicalSmiles(drug.getCanonicalSmiles());
                e.setCompoundClass(drug.getCompoundClass());
            }
        });
    }

    @Override
    public List<TTDDrugAssociation> readEntries(final String path) throws IOException {
        final List<TTDDrugAssociation> entries = new ArrayList<>();
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
                if (groupedByField.containsKey("DRUGINFO")) {
                    List<TTDRecord> drugRecords = groupedByField.get("DRUGINFO");
                    drugRecords.forEach(r -> {
                        if (groupedByField.containsKey("GENENAME") && groupedByField.containsKey("TARGETID")) {
                            TTDDrugAssociation entry = TTDDrugAssociation.builder()
                                    .ttdGeneId(groupedByField.get("TARGETID").get(0).getValue())
                                    .ttdTarget(groupedByField.get("GENENAME").get(0).getValue())
                                    .build();
                            String[] drugProperties = r.getValue().split(FileFormat.TSV.getSeparator());
                            entry.setId(drugProperties[0]);
                            entry.setName(drugProperties[1]);
                            entry.setStatus(drugProperties[2]);
                            entries.add(entry);
                        }
                    });
                }
            });
        }
        return entries;
    }

    public List<TTDDrugAssociation> readDrugs(final String path) throws IOException {
        final List<TTDDrugAssociation> entries = new ArrayList<>();
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
            }
            Map<String, List<TTDRecord>> grouped = records.stream()
                    .collect(Collectors.groupingBy(TTDRecord::getId));
            grouped.forEach((k, v) -> {
                if (v.size() > 4) {
                    TTDDrugAssociation entry = TTDDrugAssociation.builder().build();
                    v.forEach(item -> {
                        switch (item.getFiled()) {
                            case "DRUG__ID":
                                entry.setId(item.getValue());
                                break;
                            case "TRADNAME":
                                entry.setName(item.getValue());
                                break;
                            case "DRUGCOMP":
                                entry.setCompany(item.getValue());
                                break;
                            case "THERCLAS":
                                entry.setTherapeuticClass(item.getValue());
                                break;
                            case "DRUGTYPE":
                                entry.setType(item.getValue());
                                break;
                            case "DRUGINCH":
                                entry.setInChI(item.getValue());
                                break;
                            case "DRUGINKE":
                                entry.setInChIKey(item.getValue());
                                break;
                            case "DRUGSMIL":
                                entry.setCanonicalSmiles(item.getValue());
                                break;
                            case "HIGHSTAT":
                                entry.setStatus(item.getValue());
                                break;
                            case "COMPCLAS":
                                entry.setCompoundClass(item.getValue());
                                break;
                            default:
                                break;
                        }
                    });
                    entries.add(entry);
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
    public List<TTDDrugAssociation> processEntries(final List<TTDDrugAssociation> entries)
            throws IOException, ParseException {
        return entries;
    }

    @Override
    public void addDoc(final IndexWriter writer, final TTDDrugAssociation entry) throws IOException {
        final Document doc = new Document();
        doc.add(new TextField(TTDDrugField.TTD_GENE_ID.name(), entry.getTtdGeneId(), Field.Store.YES));
        doc.add(new TextField(TTDDrugField.TTD_TARGET.name(), entry.getTtdTarget(), Field.Store.YES));
        doc.add(new StringField(TTDDrugField.DRUG_ID.name(), entry.getId(), Field.Store.YES));
        doc.add(new TextField(TTDDrugField.DRUG_NAME.name(), entry.getName(), Field.Store.YES));

        final String company = Optional.ofNullable(entry.getCompany()).orElse("");
        doc.add(new StringField(TTDDrugField.COMPANY.name(), company, Field.Store.YES));

        final String type = Optional.ofNullable(entry.getType()).orElse("");
        doc.add(new StringField(TTDDrugField.TYPE.name(), type, Field.Store.YES));

        final String therapeuticClass = Optional.ofNullable(entry.getTherapeuticClass()).orElse("");
        doc.add(new StringField(TTDDrugField.THERAPEUTIC_CLASS.name(), therapeuticClass, Field.Store.YES));

        final String inchi = Optional.ofNullable(entry.getInChI()).orElse("");
        doc.add(new TextField(TTDDrugField.INCHI.name(), inchi, Field.Store.YES));

        final String inchiKey = Optional.ofNullable(entry.getInChIKey()).orElse("");
        doc.add(new TextField(TTDDrugField.INCHI_KEY.name(), inchiKey, Field.Store.YES));

        final String canonicalSmiles = Optional.ofNullable(entry.getCanonicalSmiles()).orElse("");
        doc.add(new TextField(TTDDrugField.CANONICAL_SMILES.name(), canonicalSmiles, Field.Store.YES));

        final String status = Optional.ofNullable(entry.getStatus()).orElse("");
        doc.add(new StringField(TTDDrugField.STATUS.name(), status, Field.Store.YES));

        final String compoundClass = Optional.ofNullable(entry.getCompoundClass()).orElse("");
        doc.add(new StringField(TTDDrugField.COMPOUND_CLASS.name(), compoundClass, Field.Store.YES));

        writer.addDocument(doc);
    }

    @Override
    public TTDDrugAssociation entryFromDoc(final Document doc) {
        final String id = doc.getField(TTDDrugField.DRUG_ID.name()).stringValue();
        return TTDDrugAssociation.builder()
                .ttdGeneId(doc.getField(TTDDrugField.TTD_GENE_ID.name()).stringValue())
                .ttdTarget(doc.getField(TTDDrugField.TTD_TARGET.name()).stringValue())
                .id(id)
                .name(doc.getField(TTDDrugField.DRUG_NAME.name()).stringValue())
                .url(String.format(TTDDrugAssociation.URL_PATTERN, id))
                .company(doc.getField(TTDDrugField.COMPANY.name()).stringValue())
                .type(doc.getField(TTDDrugField.TYPE.name()).stringValue())
                .therapeuticClass(doc.getField(TTDDrugField.THERAPEUTIC_CLASS.name()).stringValue())
                .inChI(doc.getField(TTDDrugField.INCHI.name()).stringValue())
                .inChIKey(doc.getField(TTDDrugField.INCHI_KEY.name()).stringValue())
                .canonicalSmiles(doc.getField(TTDDrugField.CANONICAL_SMILES.name()).stringValue())
                .status(doc.getField(TTDDrugField.STATUS.name()).stringValue())
                .compoundClass(doc.getField(TTDDrugField.COMPOUND_CLASS.name()).stringValue())
                .build();
    }

    @Override
    public FilterType getFilterType(String fieldName) {
        return TTDDrugField.valueOf(fieldName).getType();
    }
}
