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
package com.epam.catgenome.manager.externaldb.pharmgkb;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBDrug;
import com.epam.catgenome.util.FileFormat;
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
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.epam.catgenome.util.IndexUtils.getByIdsQuery;
import static com.epam.catgenome.util.NgbFileUtils.getFile;

@Service
public class PharmGKBDrugManager {

    public static final int COLUMNS = 14;
    private final String indexDirectory;

    public PharmGKBDrugManager(final @Value("${targets.index.directory}") String indexDirectory) {
        this.indexDirectory = Paths.get(indexDirectory, "pharmgkb.drug").toString();
    }

    public List<PharmGKBDrug> search(final List<String> ids)
            throws IOException, ParseException {
        final List<PharmGKBDrug> entries = new ArrayList<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {

            final Query query = getByDrugIdsQuery(ids);
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, ids.size());
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            for (ScoreDoc scoreDoc : scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                PharmGKBDrug entry = entryFromDoc(doc);
                entries.add(entry);
            }
        }
        return entries;
    }

    public void importData(final String path) throws IOException {
        getFile(path);
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            for (PharmGKBDrug entry: readEntries(path)) {
                addDoc(writer, entry);
            }
        }
    }

    private List<PharmGKBDrug> readEntries(final String path) throws IOException {
        final List<PharmGKBDrug> entries = new ArrayList<>();
        String line;
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            line = bufferedReader.readLine();
            String[] cells = line.split(FileFormat.TSV.getSeparator());
            Assert.isTrue(cells.length == COLUMNS, MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(FileFormat.TSV.getSeparator());
                PharmGKBDrug entry = PharmGKBDrug.builder()
                        .drugId(cells[0].trim())
                        .drugName(cells[1].trim())
                        .source(cells[2].trim())
                        .build();
                entries.add(entry);
            }
        }
        return entries;
    }

    @Getter
    private enum IndexFields {
        DRUG_ID("drug_id"),
        DRUG_NAME("drug_name"),
        SOURCE("source");
        private final String fieldName;

        IndexFields(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    private static void addDoc(final IndexWriter writer, final PharmGKBDrug entry) throws IOException {
        final Document doc = new Document();
        doc.add(new TextField(IndexFields.DRUG_ID.getFieldName(), entry.getDrugId(), Field.Store.YES));
        doc.add(new TextField(IndexFields.DRUG_NAME.getFieldName(), entry.getDrugName(), Field.Store.YES));
        doc.add(new TextField(IndexFields.SOURCE.getFieldName(), entry.getSource(), Field.Store.YES));
        writer.addDocument(doc);
    }

    private static PharmGKBDrug entryFromDoc(final Document doc) {
        return PharmGKBDrug.builder()
                .drugId(doc.getField(IndexFields.DRUG_ID.getFieldName()).stringValue())
                .drugName(doc.getField(IndexFields.DRUG_NAME.getFieldName()).stringValue())
                .source(doc.getField(IndexFields.SOURCE.getFieldName()).stringValue())
                .build();
    }

    private static Query getByDrugIdsQuery(final List<String> ids) throws ParseException {
        return getByIdsQuery(ids, IndexFields.DRUG_ID.getFieldName());
    }
}
