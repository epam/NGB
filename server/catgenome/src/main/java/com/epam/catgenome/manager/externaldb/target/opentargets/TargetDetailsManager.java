/*
 * MIT License
 *
 * Copyright (c) 2023-2024 EPAM Systems
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
package com.epam.catgenome.manager.externaldb.target.opentargets;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.externaldb.target.opentargets.TargetDetails;
import com.epam.catgenome.entity.index.FilterType;
import com.epam.catgenome.manager.index.AbstractIndexManager;
import com.epam.catgenome.util.IndexUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.epam.catgenome.util.NgbFileUtils.getDirectory;

@Service
public class TargetDetailsManager extends AbstractIndexManager<TargetDetails> {

    @Value("${targets.opentargets.targetsDir:targets}")
    private String targetsDir;

    public TargetDetailsManager(final @Value("${targets.index.directory}") String indexDirectory,
                                final @Value("${targets.top.hits:10000}") int targetsTopHits) {
        super(Paths.get(indexDirectory, "opentargets.target").toString(), targetsTopHits);
    }

    public List<TargetDetails> search(final List<String> ids) throws ParseException, IOException {
        return search(ids, IndexFields.TARGET_ID.name());
    }

    @Override
    public List<TargetDetails> readEntries(final String path) throws IOException {
        final Path targetsPath = Paths.get(path, targetsDir);
        final File directory = getDirectory(targetsPath.toString());
        final List<TargetDetails> entries = new ArrayList<>();
        String line;
        final ObjectMapper objectMapper = new ObjectMapper();
        for (File f: directory.listFiles()) {
            try (Reader reader = new FileReader(f); BufferedReader bufferedReader = new BufferedReader(reader)) {
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
        }
        return entries;
    }

    @Override
    public SortField getDefaultSortField() {
        return null;
    }

    @Override
    public FilterType getFilterType(String fieldName) {
        return null;
    }

    @Override
    public List<TargetDetails> processEntries(List<TargetDetails> entries) throws IOException, ParseException {
        return entries;
    }

    @Override
    public TargetDetails entryFromDoc(final Document doc) {
        return TargetDetails.builder()
                .id(IndexUtils.getField(doc, IndexFields.TARGET_ID.name()))
                .symbol(IndexUtils.getField(doc, IndexFields.TARGET_SYMBOL.name()))
                .name(IndexUtils.getField(doc, IndexFields.TARGET_NAME.name()))
                .description(IndexUtils.getField(doc, IndexFields.DESCRIPTION.name()))
                .build();
    }

    @Override
    public void addDoc(final IndexWriter writer, final TargetDetails entry) throws IOException {
        final Document doc = new Document();
        doc.add(new TextField(IndexFields.TARGET_ID.name(), entry.getId(), Field.Store.YES));
        doc.add(new TextField(IndexFields.TARGET_SYMBOL.name(), entry.getSymbol(), Field.Store.YES));
        doc.add(new StringField(IndexFields.TARGET_NAME.name(), entry.getName(), Field.Store.YES));
        doc.add(new StringField(IndexFields.DESCRIPTION.name(), entry.getDescription(), Field.Store.YES));
        writer.addDocument(doc);
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
                .symbol(jsonNodes.at("/approvedSymbol").asText())
                .name(jsonNodes.at("/approvedName").asText())
                .description(description.toString())
                .build();
    }

    @Getter
    private enum IndexFields {
        TARGET_ID,
        TARGET_SYMBOL,
        TARGET_NAME,
        DESCRIPTION;
    }
}
