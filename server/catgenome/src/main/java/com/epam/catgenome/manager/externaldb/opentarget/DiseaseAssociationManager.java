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

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.externaldb.opentarget.DiseaseAssociation;
import com.epam.catgenome.entity.externaldb.opentarget.AssociationType;
import com.epam.catgenome.entity.externaldb.opentarget.DiseaseAssociationAggregated;
import com.epam.catgenome.entity.externaldb.opentarget.Disease;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.target.AssociationSearchRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.grouping.GroupingSearch;
import org.apache.lucene.search.grouping.TopGroups;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.util.IndexUtils.getByIdsQuery;
import static com.epam.catgenome.util.NgbFileUtils.getDirectory;
import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;

@Service
public class DiseaseAssociationManager {


    @Value("${opentargets.disease.association.index.directory}")
    private String indexDirectory;

    @Value("${targets.top.hits:10000}")
    private int targetsTopHits;

    public SearchResult<DiseaseAssociationAggregated> search(final AssociationSearchRequest request)
            throws IOException, ParseException {
        final List<DiseaseAssociationAggregated> records = new ArrayList<>();
        final SearchResult<DiseaseAssociationAggregated> searchResult = new SearchResult<>();
        final int page = (request.getPage() == null || request.getPage() <= 0) ? 1 : request.getPage();
        final int pageSize = (request.getPageSize() == null || request.getPage() <= 0) ? DEFAULT_PAGE_SIZE
                : request.getPageSize();
        final int offset = (page - 1) * pageSize;
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            final Query query = getByTargetIdsQuery(request.getGeneIds());
            final GroupingSearch groupingSearch = new GroupingSearch(IndexFields.TARGET_DISEASE.getFieldName());
            groupingSearch.setGroupDocsLimit(targetsTopHits);
            final IndexSearcher searcher = new IndexSearcher(indexReader);
            final TopGroups<DiseaseAssociation> groups = groupingSearch.search(searcher, query, offset, pageSize);
            for (int i = 0; i < groups.groups.length; i++) {
                ScoreDoc[] sdoc = groups.groups[i].scoreDocs;
                Document groupDoc = searcher.doc(sdoc[0].doc);
                Disease disease = Disease.builder()
                        .id(getDiseaseId(groupDoc))
                        .build();
                DiseaseAssociationAggregated record = DiseaseAssociationAggregated.builder()
                        .targetId(getTargetId(groupDoc))
                        .disease(disease)
                        .build();
                Map<AssociationType, Float> scores = new HashMap<>();
                for (ScoreDoc scoreDoc : sdoc) {
                    Document doc = searcher.doc(scoreDoc.doc);
                    scores.put(AssociationType.getByName(getDataTypeId(doc)), getScore(doc));
                }
                record.setScores(scores);
                records.add(record);
            }
            searchResult.setItems(records);
            searchResult.setTotalCount(totalCount(request.getGeneIds()));
        }
        return searchResult;
    }

    public int totalCount(final List<String> geneIds) throws ParseException, IOException {
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            final Query query = getByTargetIdsQuery(geneIds);
            final GroupingSearch groupingSearch = new GroupingSearch(IndexFields.TARGET_DISEASE.getFieldName());
            final IndexSearcher searcher = new IndexSearcher(indexReader);
            return groupingSearch.search(searcher, query, 0, targetsTopHits).groups.length;
        }
    }

    public void importData(final String path, final String overallPath) throws IOException {
        final File directory = getDirectory(path);
        final File overallDirectory = getDirectory(overallPath);
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            for (File f: ArrayUtils.addAll(directory.listFiles(), overallDirectory.listFiles())) {
                for (DiseaseAssociation entry: readEntries(f)) {
                    addDoc(writer, entry);
                }
            }
        }
    }

    private List<DiseaseAssociation> readEntries(final File path) throws IOException {
        final List<DiseaseAssociation> entries = new ArrayList<>();
        String line;
        final ObjectMapper objectMapper = new ObjectMapper();
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    JsonNode jsonNodes = objectMapper.readTree(line);
                    DiseaseAssociation entry = entryFromJson(jsonNodes);
                    entries.add(entry);
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException(MessagesConstants.ERROR_INCORRECT_JSON_FORMAT);
                }
            }
        }
        return entries;
    }

    private static DiseaseAssociation entryFromJson(final JsonNode jsonNodes) {
        return DiseaseAssociation.builder()
                .targetId(jsonNodes.at("/targetId").asText())
                .diseaseId(jsonNodes.at("/diseaseId").asText())
                .type(jsonNodes.has("datatypeId") ?
                        AssociationType.getByName(jsonNodes.at("/datatypeId").asText()) :
                        AssociationType.OVERALL)
                .score(Float.parseFloat(jsonNodes.at("/score").asText()))
                .build();
    }

    private static Query getByTargetIdsQuery(final List<String> ids)
            throws ParseException {
        return getByIdsQuery(ids, IndexFields.TARGET_ID.getFieldName());
    }

    private String getDiseaseId(final Document doc) {
        return doc.getField(IndexFields.DISEASE_ID.getFieldName()).stringValue();
    }

    private String getTargetId(final Document doc) {
        return doc.getField(IndexFields.TARGET_ID.getFieldName()).stringValue();
    }

    private String getDataTypeId(final Document doc) {
        return doc.getField(IndexFields.DATATYPE_ID.getFieldName()).stringValue();
    }

    private Float getScore(final Document doc) {
        return Float.parseFloat(doc.getField(IndexFields.SCORE.getFieldName()).stringValue());
    }

    @Getter
    private enum IndexFields {
        TARGET_DISEASE("target_disease"),
        DISEASE_ID("diseaseId"),
        TARGET_ID("targetId"),
        DATATYPE_ID("datatypeId"),
        SCORE("score");
        private final String fieldName;

        IndexFields(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    private static void addDoc(final IndexWriter writer, final DiseaseAssociation entry) throws IOException {
        final Document doc = new Document();
        doc.add(new TextField(IndexFields.DISEASE_ID.getFieldName(),
                String.valueOf(entry.getDiseaseId()), Field.Store.YES));
        doc.add(new TextField(IndexFields.TARGET_ID.getFieldName(),
                String.valueOf(entry.getTargetId()), Field.Store.YES));
        doc.add(new TextField(IndexFields.DATATYPE_ID.getFieldName(),
                String.valueOf(entry.getType().getName()), Field.Store.YES));
        doc.add(new StoredField(IndexFields.SCORE.getFieldName(), entry.getScore()));
        doc.add(new SortedDocValuesField(IndexFields.TARGET_DISEASE.getFieldName(),
                new BytesRef(entry.getDiseaseId() + entry.getTargetId())));
        writer.addDocument(doc);
    }
}
