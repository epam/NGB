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
import com.epam.catgenome.entity.externaldb.opentarget.Disease;
import com.epam.catgenome.entity.externaldb.opentarget.UrlEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.Getter;
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
import java.util.stream.Collectors;

import static com.epam.catgenome.util.IndexUtils.deserialize;
import static com.epam.catgenome.util.IndexUtils.getByIdsQuery;
import static com.epam.catgenome.util.IndexUtils.getField;
import static com.epam.catgenome.util.IndexUtils.serialize;
import static com.epam.catgenome.util.NgbFileUtils.getDirectory;

@Service
public class DiseaseManager {

    private static final String OPEN_TARGETS_DISEASE_URL_PATTERN = "https://platform.opentargets.org/disease/%s";
    private static final Integer BATCH_SIZE = 1000;

    @Value("${opentargets.disease.index.directory}")
    private String indexDirectory;

    public List<Disease> search(final List<String> ids) throws ParseException, IOException {
        final List<Disease> result = new ArrayList<>();
        final List<List<String>> subSets = Lists.partition(ids, BATCH_SIZE);
        for (List<String> subIds : subSets) {
            Query query = getByIdsQuery(subIds, IndexFields.DISEASE_ID.getFieldName());
            try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
                 IndexReader indexReader = DirectoryReader.open(index)) {
                IndexSearcher searcher = new IndexSearcher(indexReader);
                TopDocs topDocs = searcher.search(query, subIds.size());
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document doc = searcher.doc(scoreDoc.doc);
                    Disease entry = entryFromDoc(doc);
                    result.add(entry);
                }
            }
        }
        return result;
    }

    public void importData(final String path) throws IOException {
        final File directory = getDirectory(path);
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            for (File f: directory.listFiles()) {
                for (Disease entry: readEntries(f)) {
                    addDoc(writer, entry);
                }
            }
        }
    }

    private List<Disease> readEntries(final File path) throws IOException {
        final List<Disease> entries = new ArrayList<>();
        String line;
        final ObjectMapper objectMapper = new ObjectMapper();
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    JsonNode jsonNodes = objectMapper.readTree(line);
                    Disease entry = entryFromJson(jsonNodes);
                    entries.add(entry);
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException(MessagesConstants.ERROR_INCORRECT_JSON_FORMAT);
                }
            }
        }
        return entries;
    }

    @Getter
    private enum IndexFields {
        DISEASE_ID("diseaseId"),
        NAME("name"),
        THERAPEUTIC_AREA_IDS("therapeuticAreaIds"),
        PARENTS("parents"),
        ANCESTORS("ancestors"),
        IS_THERAPEUTIC_AREA("isTherapeuticArea");
        private final String fieldName;

        IndexFields(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    private static Disease entryFromJson(final JsonNode jsonNodes) {
        final JsonNode therapeuticAreasNode = jsonNodes.at("/therapeuticAreas");
        final List<UrlEntity> therapeuticAreas = new ArrayList<>();
        UrlEntity urlEntity;
        if (therapeuticAreasNode.isArray()) {
            Iterator<JsonNode> node = therapeuticAreasNode.elements();
            while (node.hasNext()) {
                urlEntity = new UrlEntity(node.next().asText());
                therapeuticAreas.add(urlEntity);
            }
        }
        final JsonNode parentsNode = jsonNodes.at("/parents");
        final List<String> parents = new ArrayList<>();
        if (parentsNode.isArray()) {
            Iterator<JsonNode> node = parentsNode.elements();
            while (node.hasNext()) {
                parents.add(node.next().asText());
            }
        }
        final JsonNode ancestorsNode = jsonNodes.at("/ancestors");
        final List<String> ancestors = new ArrayList<>();
        if (ancestorsNode.isArray()) {
            Iterator<JsonNode> node = ancestorsNode.elements();
            while (node.hasNext()) {
                ancestors.add(node.next().asText());
            }
        }
        return Disease.builder()
                .id(jsonNodes.at("/id").asText())
                .name(jsonNodes.at("/name").asText())
                .isTherapeuticArea(jsonNodes.at("/ontology/isTherapeuticArea").asBoolean())
                .therapeuticAreas(therapeuticAreas)
                .parents(parents)
                .ancestors(ancestors)
                .build();
    }

    private Disease entryFromDoc(final Document doc) {
        final List<String> therapeuticAreasStr = deserialize(getField(doc,
                IndexFields.THERAPEUTIC_AREA_IDS.getFieldName()));
        final List<UrlEntity> therapeuticAreas = therapeuticAreasStr.stream()
                .map(UrlEntity::new)
                .collect(Collectors.toList());
        return Disease.builder()
                .id(getField(doc, IndexFields.DISEASE_ID.getFieldName()))
                .name(getField(doc, IndexFields.NAME.getFieldName()))
                .url(getDiseaseUrl(getField(doc, IndexFields.DISEASE_ID.getFieldName())))
                .parents(deserialize(getField(doc, IndexFields.PARENTS.getFieldName())))
                .ancestors(deserialize(getField(doc, IndexFields.ANCESTORS.getFieldName())))
                .isTherapeuticArea(Boolean.parseBoolean(doc.getField(IndexFields.IS_THERAPEUTIC_AREA.getFieldName())
                        .stringValue()))
                .therapeuticAreas(therapeuticAreas)
                .build();
    }

    public static String getDiseaseUrl(final String diseaseId) {
        return String.format(OPEN_TARGETS_DISEASE_URL_PATTERN, diseaseId);
    }

    private static void addDoc(final IndexWriter writer, final Disease entry) throws IOException {
        final Document doc = new Document();
        doc.add(new TextField(IndexFields.DISEASE_ID.getFieldName(), String.valueOf(entry.getId()), Field.Store.YES));
        doc.add(new TextField(IndexFields.NAME.getFieldName(), String.valueOf(entry.getName()), Field.Store.YES));
        doc.add(new TextField(IndexFields.IS_THERAPEUTIC_AREA.getFieldName(),
                String.valueOf(entry.isTherapeuticArea()), Field.Store.YES));
        doc.add(new TextField(IndexFields.PARENTS.getFieldName(), serialize(entry.getParents()), Field.Store.YES));
        doc.add(new TextField(IndexFields.ANCESTORS.getFieldName(), serialize(entry.getAncestors()), Field.Store.YES));
        final List<String> therapeuticAreaIds = entry.getTherapeuticAreas().stream()
                .map(UrlEntity::getId)
                .collect(Collectors.toList());
        doc.add(new TextField(IndexFields.THERAPEUTIC_AREA_IDS.getFieldName(),
                serialize(therapeuticAreaIds), Field.Store.YES));
        writer.addDocument(doc);
    }
}
