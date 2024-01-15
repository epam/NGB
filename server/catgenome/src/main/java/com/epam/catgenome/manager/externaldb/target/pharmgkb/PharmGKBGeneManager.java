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
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBGene;
import com.epam.catgenome.entity.index.FilterType;
import com.epam.catgenome.manager.index.AbstractIndexManager;
import com.epam.catgenome.util.FileFormat;
import lombok.Getter;
import org.apache.http.util.TextUtils;
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
public class PharmGKBGeneManager extends AbstractIndexManager<PharmGKBGene> {

    private static final int COLUMNS = 17;

    public PharmGKBGeneManager(final @Value("${targets.index.directory}") String indexDirectory,
                               final @Value("${targets.top.hits:10000}") int targetsTopHits) {
        super(Paths.get(indexDirectory, "pharmgkb.gene").toString(), targetsTopHits);
    }

    public List<PharmGKBGene> search(final List<String> ids)
            throws IOException, ParseException {
        return search(ids, IndexFields.PHARMGKB_ID.name());
    }

    @Override
    public List<PharmGKBGene> readEntries(final String path) throws IOException {
        final List<PharmGKBGene> entries = new ArrayList<>();
        String line;
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            line = bufferedReader.readLine();
            String[] cells = line.split(FileFormat.TSV.getSeparator());
            Assert.isTrue(cells.length == COLUMNS, MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(FileFormat.TSV.getSeparator());
                PharmGKBGene entry = PharmGKBGene.builder()
                        .pharmGKBId(cells[0].trim())
                        .geneId(cells[3].trim())
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
    public List<PharmGKBGene> processEntries(List<PharmGKBGene> entries) {
        return entries;
    }

    @Override
    public void addDoc(final IndexWriter writer, final PharmGKBGene entry) throws IOException {
        final String geneId = entry.getGeneId();
        if (!TextUtils.isBlank(geneId)) {
            final Document doc = new Document();
            doc.add(new TextField(IndexFields.PHARMGKB_ID.name(), entry.getPharmGKBId(), Field.Store.YES));
            doc.add(new TextField(IndexFields.GENE_ID.name(), entry.getGeneId(), Field.Store.YES));
            writer.addDocument(doc);
        }
    }

    @Override
    public PharmGKBGene entryFromDoc(final Document doc) {
        return PharmGKBGene.builder()
                .pharmGKBId(doc.getField(IndexFields.PHARMGKB_ID.name()).stringValue())
                .geneId(doc.getField(IndexFields.GENE_ID.name()).stringValue())
                .build();
    }

    @Getter
    private enum IndexFields {
        PHARMGKB_ID,
        GENE_ID;
    }
}
