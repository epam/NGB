/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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
package com.epam.catgenome.manager.externaldb.opentarget;

import com.epam.catgenome.entity.externaldb.opentarget.AssociatedDrug;
import com.epam.catgenome.entity.externaldb.opentarget.Disease;
import com.epam.catgenome.entity.externaldb.opentarget.Drug;
import com.epam.catgenome.entity.externaldb.opentarget.Source;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.IndexUtils.getByIdsQuery;
import static com.epam.catgenome.util.NgbFileUtils.getDirectory;
import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;

@Service
@Slf4j
public class AssociatedDrugManager {

    private static final String INCORRECT_JSON_FORMAT = "Incorrect JSON format";
    private static final String OPEN_TARGETS_DRUG_URL_PATTERN = "https://platform.opentargets.org/drug/%s";

    @Value("${drugs.index.directory}")
    private String indexDirectory;

    @Value("${targets.top.hits:10000}")
    private int targetsTopHits;

    @Autowired
    private DiseaseManager diseaseManager;

    public SearchResult<AssociatedDrug> search(final AssociationSearchRequest request)
            throws IOException, ParseException {
        final List<AssociatedDrug> entries = new ArrayList<>();
        final SearchResult<AssociatedDrug> searchResult = new SearchResult<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {

            final int page = (request.getPage() == null || request.getPage() <= 0) ? 1 : request.getPage();
            final int pageSize = (request.getPageSize() == null || request.getPage() <= 0) ? DEFAULT_PAGE_SIZE
                    : request.getPageSize();
            final int hits = page * pageSize;

            final Query query = getByTargetIdsQuery(request.getTargetIds());
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, hits);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            final int from = (page - 1) * pageSize;
            final int to = Math.min(from + pageSize, scoreDocs.length);
            for (int i = from; i < to; i++) {
                Document doc = searcher.doc(scoreDocs[i].doc);
                AssociatedDrug entry = entryFromDoc(doc);
                entries.add(entry);
            }
            fillDiseaseNames(entries);
            searchResult.setItems(entries);
            searchResult.setTotalCount(topDocs.totalHits);
        }
        return searchResult;
    }

    public int totalCount(final List<String> targetIds) throws ParseException, IOException {
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            final Query query = getByTargetIdsQuery(targetIds);
            final IndexSearcher searcher = new IndexSearcher(indexReader);
            final TopDocs topDocs = searcher.search(query, targetsTopHits);
            return topDocs.totalHits;
        }
    }

    public void importData(final String path) throws IOException {
        final File directory = getDirectory(path);
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            for (File f: directory.listFiles()) {
                for (AssociatedDrug entry: readEntries(f)) {
                    addDoc(writer, entry);
                }
            }
        }
    }

    public List<AssociatedDrug> readEntries(final File path) throws IOException {
        final List<AssociatedDrug> entries = new ArrayList<>();
        String line;
        final ObjectMapper objectMapper = new ObjectMapper();
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    JsonNode jsonNodes = objectMapper.readTree(line);
                    AssociatedDrug entry = entryFromJson(jsonNodes);
                    entries.add(entry);
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException(INCORRECT_JSON_FORMAT);
                }
            }
        }
        return entries;
    }

    private void fillDiseaseNames(final List<AssociatedDrug> entries) throws ParseException, IOException {
        final List<String> diseaseIds = entries.stream()
                .map(r -> r.getDisease().getId())
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(diseaseIds)) {
            final List<Disease> diseases = diseaseManager.search(diseaseIds);
            final Map<String, Disease> diseasesMap = diseases.stream()
                    .collect(Collectors.toMap(Disease::getId, Function.identity()));
            for (AssociatedDrug r : entries) {
                r.setDisease(diseasesMap.get(r.getDisease().getId()));
            }
        }
    }

    private static Query getByTargetIdsQuery(final List<String> ids)
            throws ParseException {
        return getByIdsQuery(ids, IndexFields.TARGET_ID.getFieldName());
    }

    @Getter
    private enum IndexFields {
        DRUG_ID("drugId"),
        DRUG_NAME("prefName"),
        DISEASE_ID("diseaseId"),
        TARGET_ID("targetId"),
        DRUG_TYPE("drugType"),
        MECHANISM_OF_ACTION("mechanismOfAction"),
        ACTION_TYPE("actionType"),
        PHASE("phase"),
        STATUS("status"),
        SOURCE("source"),
        SOURCE_URL("source_url");
        private final String fieldName;

        IndexFields(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    private static void addDoc(final IndexWriter writer, final AssociatedDrug entry) throws IOException {
        final Document doc = new Document();
        doc.add(new TextField(IndexFields.DRUG_ID.getFieldName(),
                String.valueOf(entry.getDrug().getId()), Field.Store.YES));
        doc.add(new TextField(IndexFields.DRUG_NAME.getFieldName(),
                String.valueOf(entry.getDrug().getName()), Field.Store.YES));
        doc.add(new TextField(IndexFields.DISEASE_ID.getFieldName(),
                String.valueOf(entry.getDisease().getId()), Field.Store.YES));
        doc.add(new TextField(IndexFields.TARGET_ID.getFieldName(),
                String.valueOf(entry.getTargetId()), Field.Store.YES));
        doc.add(new TextField(IndexFields.DRUG_TYPE.getFieldName(),
                String.valueOf(entry.getDrugType()), Field.Store.YES));
        doc.add(new TextField(IndexFields.MECHANISM_OF_ACTION.getFieldName(),
                String.valueOf(entry.getMechanismOfAction()), Field.Store.YES));
        doc.add(new TextField(IndexFields.ACTION_TYPE.getFieldName(),
                String.valueOf(entry.getActionType()), Field.Store.YES));
        doc.add(new TextField(IndexFields.PHASE.getFieldName(),
                String.valueOf(entry.getPhase()), Field.Store.YES));
        doc.add(new TextField(IndexFields.STATUS.getFieldName(),
                String.valueOf(entry.getStatus()), Field.Store.YES));
        doc.add(new TextField(IndexFields.SOURCE.getFieldName(),
                String.valueOf(entry.getSource().getName()), Field.Store.YES));
        doc.add(new TextField(IndexFields.SOURCE_URL.getFieldName(),
                String.valueOf(entry.getSource().getUrl()), Field.Store.YES));
        writer.addDocument(doc);
    }

    private static AssociatedDrug entryFromJson(final JsonNode jsonNodes) {
        final JsonNode urls = jsonNodes.at("/urls");
        final Source source = new Source();
        if (urls.isArray()) {
            Iterator<JsonNode> elements = urls.elements();
            if (elements.hasNext()) {
                JsonNode node = elements.next();
                source.setName(node.at("/niceName").asText());
                source.setUrl(node.at("/url").asText());
            }
        }
        final Disease disease = Disease.builder()
                .id(jsonNodes.at("/diseaseId").asText())
                .build();
        final Drug drug = Drug.builder()
                .id(jsonNodes.at("/drugId").asText())
                .name(jsonNodes.at("/prefName").asText())
                .build();
        return AssociatedDrug.builder()
                .drug(drug)
                .targetId(jsonNodes.at("/targetId").asText())
                .disease(disease)
                .drugType(jsonNodes.at("/drugType").asText())
                .mechanismOfAction(jsonNodes.at("/mechanismOfAction").asText())
                .actionType(jsonNodes.at("/mechanismOfAction").asText())
                .phase(jsonNodes.at("/phase").asText())
                .status(jsonNodes.at("/status").asText())
                .source(source)
                .build();
    }

    private static AssociatedDrug entryFromDoc(final Document doc) {
        final Source source = Source.builder()
                .name(doc.getField(IndexFields.SOURCE.getFieldName()).stringValue())
                .url(doc.getField(IndexFields.SOURCE_URL.getFieldName()).stringValue())
                .build();
        final Disease disease = Disease.builder()
                .id(doc.getField(IndexFields.DISEASE_ID.getFieldName()).stringValue())
                .build();
        final Drug drug = Drug.builder()
                .id(doc.getField(IndexFields.DRUG_ID.getFieldName()).stringValue())
                .name(doc.getField(IndexFields.DRUG_NAME.getFieldName()).stringValue())
                .url(String.format(OPEN_TARGETS_DRUG_URL_PATTERN,
                        doc.getField(IndexFields.DRUG_ID.getFieldName()).stringValue()))
                .build();
        return AssociatedDrug.builder()
                .drug(drug)
                .targetId(doc.getField(IndexFields.TARGET_ID.getFieldName()).stringValue())
                .disease(disease)
                .drugType(doc.getField(IndexFields.DRUG_TYPE.getFieldName()).stringValue())
                .mechanismOfAction(doc.getField(IndexFields.MECHANISM_OF_ACTION.getFieldName()).stringValue())
                .actionType(doc.getField(IndexFields.ACTION_TYPE.getFieldName()).stringValue())
                .phase(doc.getField(IndexFields.PHASE.getFieldName()).stringValue())
                .status(doc.getField(IndexFields.STATUS.getFieldName()).stringValue())
                .source(source)
                .build();
    }
}
