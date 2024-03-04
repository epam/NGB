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
import com.epam.catgenome.manager.externaldb.target.AssociationSearchRequest;
import com.epam.catgenome.manager.index.CaseInsensitiveWhitespaceAnalyzer;
import com.epam.catgenome.manager.index.Filter;
import com.epam.catgenome.util.FileFormat;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.IndexUtils.getByTermsQuery;


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

    public List<TTDDrugAssociation> searchByGeneNames(final AssociationSearchRequest request,
                                                      final List<String> geneNames) throws IOException, ParseException {
        final Query byGeneNamesQuery = getByTermsQuery(geneNames, TTDDrugField.TARGET.name());
        final Query query = buildQuery(byGeneNamesQuery, request.getFilters());
        return search(query, null);
    }

    public List<TTDDrugAssociation> searchByGeneNames(final List<String> geneNames) throws IOException, ParseException {
        final Query byGeneNamesQuery = getByTermsQuery(geneNames, TTDDrugField.TARGET.name());
        return search(byGeneNamesQuery, null);
    }

    public List<TTDDrugAssociation> searchBySequences(final AssociationSearchRequest request,
                                                      final List<String> sequences) throws IOException, ParseException {
        final Query bySequencesQuery = getByTermsQuery(sequences, TTDDrugField.GENE_ID.name());
        final Query query = buildQuery(bySequencesQuery, request.getFilters());
        return search(query, null);
    }

    public List<TTDDrugAssociation> searchBySequences(final List<String> sequences) throws IOException, ParseException {
        final Query bySequencesQuery = getByTermsQuery(sequences, TTDDrugField.GENE_ID.name());
        return search(bySequencesQuery, null);
    }

    public Query buildQuery(final Query query, final List<Filter> filters) throws ParseException {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        mainBuilder.add(query, BooleanClause.Occur.MUST);
        if (filters != null) {
            for (Filter filter: filters) {
                addFieldQuery(mainBuilder, filter);
            }
        }
        return mainBuilder.build();
    }

    @Override
    public List<TTDDrugAssociation> readEntries(final String path) throws IOException {
        final List<TTDDrugAssociation> entries = new ArrayList<>();
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            final List<TTDDrugRecord> records = new ArrayList<>();
            String line;
            String[] cells;
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(FileFormat.TSV.getSeparator());
                if (cells.length == 3) {
                    TTDDrugRecord record = TTDDrugRecord.builder()
                            .id(cells[0].trim())
                            .filed(cells[1].trim())
                            .value(cells[2].trim())
                            .build();
                    records.add(record);
                }
                if (cells.length == 5) {
                    TTDDrugRecord record = TTDDrugRecord.builder()
                            .id(cells[0].trim())
                            .filed(cells[1].trim())
                            .value(String.join(FileFormat.TSV.getSeparator(),
                                    Arrays.asList(cells[2].trim(), cells[3].trim(), cells[4].trim())))
                            .build();
                    records.add(record);
                }
            }
            Map<String, List<TTDDrugRecord>> grouped = records.stream()
                    .collect(Collectors.groupingBy(TTDDrugRecord::getId));
            grouped.forEach((k, v) -> {
                Map<String, List<TTDDrugRecord>> groupedByField = v.stream()
                        .collect(Collectors.groupingBy(TTDDrugRecord::getFiled));
                if (groupedByField.containsKey("DRUGINFO")) {
                    List<TTDDrugRecord> drugRecords = groupedByField.get("DRUGINFO");
                    drugRecords.forEach(r -> {
                        if (groupedByField.containsKey("GENENAME") && groupedByField.containsKey("TARGETID")) {
                            TTDDrugAssociation entry = TTDDrugAssociation.builder()
                                    .target(groupedByField.get("GENENAME").get(0).getValue())
                                    .geneId(groupedByField.get("TARGETID").get(0).getValue())
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

    @Builder
    @Getter
    static class TTDDrugRecord {
        private String id;
        private String filed;
        private String value;
    }

    public List<TTDDrugAssociation> readDrugs(final String path) throws IOException {
        final List<TTDDrugAssociation> entries = new ArrayList<>();
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            final List<TTDDrugRecord> records = new ArrayList<>();
            String line;
            String[] cells;
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(FileFormat.TSV.getSeparator());
                if (cells.length == 3) {
                    TTDDrugRecord record = TTDDrugRecord.builder()
                            .id(cells[0].trim())
                            .filed(cells[1].trim())
                            .value(cells[2].trim())
                            .build();
                    records.add(record);
                }
            }
            Map<String, List<TTDDrugRecord>> grouped = records.stream()
                    .collect(Collectors.groupingBy(TTDDrugRecord::getId));
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
        doc.add(new TextField(TTDDrugField.GENE_ID.name(),  entry.getGeneId(), Field.Store.YES));
        doc.add(new TextField(TTDDrugField.TARGET.name(),  entry.getTarget(), Field.Store.YES));
        doc.add(new StringField(TTDDrugField.DRUG_ID.name(), entry.getId(), Field.Store.YES));
        doc.add(new TextField(TTDDrugField.DRUG_NAME.name(), entry.getName(), Field.Store.YES));
        if (StringUtils.isNotBlank(entry.getCompany())) {
            doc.add(new StringField(TTDDrugField.COMPANY.name(), entry.getCompany(), Field.Store.YES));
        }
        if (StringUtils.isNotBlank(entry.getTherapeuticClass())) {
            doc.add(new StringField(TTDDrugField.THERAPEUTIC_CLASS.name(),
                    entry.getTherapeuticClass(), Field.Store.YES));
        }
        if (StringUtils.isNotBlank(entry.getTherapeuticClass())) {
            doc.add(new StringField(TTDDrugField.THERAPEUTIC_CLASS.name(),
                    entry.getTherapeuticClass(), Field.Store.YES));
        }
        if (StringUtils.isNotBlank(entry.getInChI())) {
            doc.add(new TextField(TTDDrugField.INCHI.name(), entry.getInChI(), Field.Store.YES));
        }
        if (StringUtils.isNotBlank(entry.getInChIKey())) {
            doc.add(new TextField(TTDDrugField.INCHI_KEY.name(), entry.getInChIKey(), Field.Store.YES));
        }
        if (StringUtils.isNotBlank(entry.getCanonicalSmiles())) {
            doc.add(new TextField(TTDDrugField.CANONICAL_SMILES.name(), entry.getCanonicalSmiles(), Field.Store.YES));
        }
        if (StringUtils.isNotBlank(entry.getStatus())) {
            doc.add(new StringField(TTDDrugField.STATUS.name(), entry.getStatus(), Field.Store.YES));
        }
        if (StringUtils.isNotBlank(entry.getCompoundClass())) {
            doc.add(new StringField(TTDDrugField.COMPOUND_CLASS.name(), entry.getCompoundClass(), Field.Store.YES));
        }
        writer.addDocument(doc);
    }

    @Override
    public TTDDrugAssociation entryFromDoc(final Document doc) {
        final TTDDrugAssociation ttdDrugAssociation = TTDDrugAssociation.builder()
                .geneId(doc.getField(TTDDrugField.GENE_ID.name()).stringValue())
                .target(doc.getField(TTDDrugField.TARGET.name()).stringValue())
                .id(doc.getField(TTDDrugField.DRUG_ID.name()).stringValue())
                .name(doc.getField(TTDDrugField.DRUG_NAME.name()).stringValue())
                .build();
        if (doc.getField(TTDDrugField.COMPANY.name()) != null) {
            ttdDrugAssociation.setCompany(doc.getField(TTDDrugField.COMPANY.name()).stringValue());
        }
        if (doc.getField(TTDDrugField.THERAPEUTIC_CLASS.name()) != null) {
            ttdDrugAssociation.setTherapeuticClass(doc.getField(TTDDrugField.THERAPEUTIC_CLASS.name()).stringValue());
        }
        if (doc.getField(TTDDrugField.INCHI.name()) != null) {
            ttdDrugAssociation.setInChI(doc.getField(TTDDrugField.INCHI.name()).stringValue());
        }
        if (doc.getField(TTDDrugField.INCHI_KEY.name()) != null) {
            ttdDrugAssociation.setInChIKey(doc.getField(TTDDrugField.INCHI_KEY.name()).stringValue());
        }
        if (doc.getField(TTDDrugField.CANONICAL_SMILES.name()) != null) {
            ttdDrugAssociation.setCanonicalSmiles(doc.getField(TTDDrugField.CANONICAL_SMILES.name()).stringValue());
        }
        if (doc.getField(TTDDrugField.STATUS.name()) != null) {
            ttdDrugAssociation.setStatus(doc.getField(TTDDrugField.STATUS.name()).stringValue());
        }
        if (doc.getField(TTDDrugField.COMPOUND_CLASS.name()) != null) {
            ttdDrugAssociation.setCompoundClass(doc.getField(TTDDrugField.COMPOUND_CLASS.name()).stringValue());
        }
        return ttdDrugAssociation;
    }

    @Override
    public FilterType getFilterType(String fieldName) {
        return TTDDrugField.valueOf(fieldName).getType();
    }
}
