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
import com.epam.catgenome.entity.externaldb.opentarget.TargetDetails;
import com.epam.catgenome.util.IndexUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static com.epam.catgenome.util.IndexUtils.getByIdsQuery;
import static com.epam.catgenome.util.NgbFileUtils.getDirectory;

@Service
public class TargetDetailsManager {

    @Value("${opentargets.target.index.directory}")
    private String indexDirectory;

    public List<TargetDetails> search(final List<String> ids) throws ParseException, IOException {
        final List<TargetDetails> result = new ArrayList<>();
        final Query query = getByIdsQuery(ids, IndexFields.TARGET_ID.getFieldName());
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, ids.size());
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                TargetDetails entry = entryFromDoc(doc);
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
                for (TargetDetails entry: readData(f)) {
                    addDoc(writer, entry);
                }
            }
        }
    }

    private List<TargetDetails> readData(final File path) throws IOException {
        final List<TargetDetails> entries = new ArrayList<>();
        String line;
        final ObjectMapper objectMapper = new ObjectMapper();
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    JsonNode jsonNodes = objectMapper.readTree(line);
                    TargetDetails entry = entryFromJson(jsonNodes);
                    entries.add(entry);
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException(MessagesConstants.ERROR_INCORRECT_JSON_FORMAT);
                }
            }
        }
        return entries;
    }

    private static TargetDetails entryFromJson(final JsonNode jsonNodes) {
        final JsonNode descriptions = jsonNodes.at("/functionDescriptions");
        final StringBuilder description = new StringBuilder();
        if (descriptions.isArray()) {
            Iterator<JsonNode> node = descriptions.elements();
            while (node.hasNext()) {
                description.append(node.next().asText());
            }
        }
        return TargetDetails.builder()
                .id(jsonNodes.at("/id").asText())
                .description(description.toString())
                .build();
    }
    
    @Getter
    private enum IndexFields {
        TARGET_ID("targetId"),
        DESCRIPTION("description");
        private final String fieldName;

        IndexFields(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    private TargetDetails entryFromDoc(final Document doc) {
        return TargetDetails.builder()
                .id(IndexUtils.getField(doc, IndexFields.TARGET_ID.getFieldName()))
                .description(IndexUtils.getField(doc, IndexFields.DESCRIPTION.getFieldName()))
                .build();
    }

    private static void addDoc(final IndexWriter writer, final TargetDetails entry) throws IOException {
        final Document doc = new Document();
        doc.add(new TextField(IndexFields.TARGET_ID.getFieldName(), String.valueOf(entry.getId()), Field.Store.YES));
        doc.add(new TextField(IndexFields.DESCRIPTION.getFieldName(),
                String.valueOf(entry.getDescription()), Field.Store.YES));
        writer.addDocument(doc);
    }
}
