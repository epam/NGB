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
package com.epam.catgenome.manager.target;

import com.epam.catgenome.entity.index.FilterType;
import com.epam.catgenome.entity.index.SortType;
import com.epam.catgenome.entity.target.TargetGeneField;
import com.epam.catgenome.manager.index.AbstractIndexManager;
import com.epam.catgenome.manager.index.CaseInsensitiveWhitespaceAnalyzer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;


@Service
public class TargetGeneFieldManager extends AbstractIndexManager<TargetGeneField> {

    public TargetGeneFieldManager(final @Value("${targets.index.directory}") String indexDirectory,
                                  final @Value("${targets.top.hits:10000}") int targetsTopHits) {
        super(Paths.get(indexDirectory, "gene.fields").toString(), targetsTopHits);
    }

    @Override
    public FilterType getFilterType(String fieldName) {
        return null;
    }

    public List<TargetGeneField> load(final Long targetId) throws ParseException, IOException {
        return search(Collections.singletonList(targetId.toString()), IndexField.TARGET_ID.name());
    }

    public void delete(final Long targetId) throws ParseException, IOException {
        final Query query = getByTermQuery(targetId.toString(), IndexField.TARGET_ID.name());
        delete(query);
    }

    public void create(final List<TargetGeneField> targetGeneFields)
            throws IOException, ParseException {
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new CaseInsensitiveWhitespaceAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            for (TargetGeneField g : targetGeneFields) {
                addDoc(writer, g);
            }
        }
    }

    @Override
    public List<TargetGeneField> readEntries(String path) throws IOException {
        return null;
    }

    @Override
    public SortField getDefaultSortField() {
        return new SortField(IndexField.FIELD.name(), SortField.Type.STRING, false);
    }

    @Override
    public List<TargetGeneField> processEntries(List<TargetGeneField> entries) {
        return entries;
    }

    @Override
    public void addDoc(IndexWriter writer, TargetGeneField entry) throws IOException {
        final Document doc = new Document();
        doc.add(new StringField(IndexField.TARGET_ID.name(), entry.getTargetId().toString(), Field.Store.YES));

        doc.add(new StringField(IndexField.FIELD.name(), entry.getField(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexField.FIELD.name(), new BytesRef(entry.getField())));

        doc.add(new StringField(IndexField.FILTER_TYPE.name(), entry.getFilterType().name(), Field.Store.YES));
        doc.add(new StringField(IndexField.SORT_TYPE.name(), entry.getSortType().name(), Field.Store.YES));
        writer.addDocument(doc);
    }

    @Override
    public TargetGeneField entryFromDoc(final Document doc) {
        return TargetGeneField.builder()
                .field(doc.getField(IndexField.FIELD.name()).stringValue())
                .filterType(FilterType.valueOf(doc.getField(IndexField.FILTER_TYPE.name()).stringValue()))
                .sortType(SortType.valueOf(doc.getField(IndexField.SORT_TYPE.name()).stringValue()))
                .build();
    }

    @AllArgsConstructor
    @Getter
    private enum IndexField {
        TARGET_ID,
        FIELD,
        FILTER_TYPE,
        SORT_TYPE;
    }
}
