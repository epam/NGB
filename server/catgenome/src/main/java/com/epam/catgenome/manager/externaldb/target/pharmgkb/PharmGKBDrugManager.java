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
package com.epam.catgenome.manager.externaldb.target.pharmgkb;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDrug;
import com.epam.catgenome.entity.index.FilterType;
import com.epam.catgenome.manager.index.AbstractIndexManager;
import com.epam.catgenome.util.FileFormat;
import lombok.Getter;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.SortField;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class PharmGKBDrugManager extends AbstractIndexManager<PharmGKBDrug> {

    public static final int COLUMNS = 14;

    public PharmGKBDrugManager(final @Value("${targets.index.directory}") String indexDirectory,
                               final @Value("${targets.top.hits:10000}") int targetsTopHits) {
        super(Paths.get(indexDirectory, "pharmgkb.drug").toString(), targetsTopHits);
    }

    public List<PharmGKBDrug> search(final List<String> ids) throws IOException, ParseException {
        return search(ids, IndexFields.DRUG_ID.name());
    }

    @Override
    public List<PharmGKBDrug> readEntries(final String path) throws IOException {
        final List<PharmGKBDrug> entries = new ArrayList<>();
        String line;
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            line = bufferedReader.readLine();
            String[] cells = line.split(FileFormat.TSV.getSeparator());
            Assert.isTrue(cells.length == COLUMNS, MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(FileFormat.TSV.getSeparator());
                PharmGKBDrug entry = PharmGKBDrug.builder()
                        .id(cells[0].trim())
                        .name(cells[1].trim())
                        .source(cells[2].trim())
                        .build();
                entries.add(entry);
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
    public List<PharmGKBDrug> processEntries(List<PharmGKBDrug> entries) {
        return entries;
    }

    @Override
    public void addDoc(final IndexWriter writer, final PharmGKBDrug entry) throws IOException {
        final Document doc = new Document();
        doc.add(new TextField(IndexFields.DRUG_ID.name(), entry.getId(), Field.Store.YES));
        doc.add(new TextField(IndexFields.DRUG_NAME.name(), entry.getName(), Field.Store.YES));
        doc.add(new TextField(IndexFields.SOURCE.name(), entry.getSource(), Field.Store.YES));
        writer.addDocument(doc);
    }

    @Override
    public PharmGKBDrug entryFromDoc(final Document doc) {
        return PharmGKBDrug.builder()
                .id(doc.getField(IndexFields.DRUG_ID.name()).stringValue())
                .name(doc.getField(IndexFields.DRUG_NAME.name()).stringValue())
                .source(doc.getField(IndexFields.SOURCE.name()).stringValue())
                .build();
    }

    @Getter
    private enum IndexFields {
        DRUG_ID,
        DRUG_NAME,
        SOURCE;
    }
}
