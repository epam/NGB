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

import com.epam.catgenome.entity.externaldb.opentarget.Disease;
import com.epam.catgenome.util.IndexUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

import static com.epam.catgenome.util.IndexUtils.getByIdsQuery;
import static com.epam.catgenome.util.NgbFileUtils.getDirectory;

@Service
@Slf4j
public class DiseasesManager {

    private static final String INCORRECT_JSON_FORMAT = "Incorrect JSON format";
    private static final String OPEN_TARGETS_DISEASE_URL_PATTERN = "https://platform.opentargets.org/disease/%s";

    @Value("${diseases.index.directory}")
    private String indexDirectory;

    public List<Disease> search(final List<String> ids) throws ParseException, IOException {
        final List<Disease> result = new ArrayList<>();
        final Query query = getByIdsQuery(ids, IndexFields.DISEASE_ID.getFieldName());
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, ids.size());
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                Disease entry = entryFromDoc(doc);
                result.add(entry);
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

    public List<Disease> readEntries(final File path) throws IOException {
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
                    throw new IllegalStateException(INCORRECT_JSON_FORMAT);
                }
            }
        }
        return entries;
    }

    @Getter
    private enum IndexFields {
        DISEASE_ID("diseaseId"),
        NAME("name");
        private final String fieldName;

        IndexFields(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    private static Disease entryFromJson(final JsonNode jsonNodes) {
        return Disease.builder()
                .id(jsonNodes.at("/id").asText())
                .name(jsonNodes.at("/name").asText())
                .build();
    }

    private Disease entryFromDoc(final Document doc) {
        return Disease.builder()
                .id(IndexUtils.getField(doc, IndexFields.DISEASE_ID.getFieldName()))
                .name(IndexUtils.getField(doc, IndexFields.NAME.getFieldName()))
                .url(String.format(OPEN_TARGETS_DISEASE_URL_PATTERN,
                        IndexUtils.getField(doc, IndexFields.DISEASE_ID.getFieldName())))
                .build();
    }

    private static void addDoc(final IndexWriter writer, final Disease entry) throws IOException {
        final Document doc = new Document();
        doc.add(new TextField(IndexFields.DISEASE_ID.getFieldName(), String.valueOf(entry.getId()), Field.Store.YES));
        doc.add(new TextField(IndexFields.NAME.getFieldName(), String.valueOf(entry.getName()), Field.Store.YES));
        writer.addDocument(doc);
    }
}
