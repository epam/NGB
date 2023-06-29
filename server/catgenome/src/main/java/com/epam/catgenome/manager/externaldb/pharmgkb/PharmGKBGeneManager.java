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
import com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBGene;
import com.epam.catgenome.util.FileFormat;
import lombok.Getter;
import org.apache.http.util.TextUtils;
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
public class PharmGKBGeneManager {

    private static final int COLUMNS = 17;
    private final String indexDirectory;

    public PharmGKBGeneManager(final @Value("${targets.index.directory}") String indexDirectory) {
        this.indexDirectory = Paths.get(indexDirectory, "pharmgkb.gene").toString();
    }

    public List<PharmGKBGene> search(final List<String> ids)
            throws IOException, ParseException {
        final List<PharmGKBGene> entries = new ArrayList<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {

            final Query query = getByPharmIdsQuery(ids);
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, ids.size());
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            for (ScoreDoc scoreDoc : scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                PharmGKBGene entry = entryFromDoc(doc);
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
            for (PharmGKBGene entry: readEntries(path)) {
                addDoc(writer, entry);
            }
        }
    }

    private List<PharmGKBGene> readEntries(final String path) throws IOException {
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

    @Getter
    private enum IndexFields {
        PHARMGKB_ID("PharmGKB_Accession_Id"),
        GENE_ID("Ensembl_Id");
        private final String fieldName;

        IndexFields(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    private static void addDoc(final IndexWriter writer, final PharmGKBGene entry) throws IOException {
        final String geneId = entry.getGeneId();
        if (!TextUtils.isBlank(geneId)) {
            final Document doc = new Document();
            doc.add(new TextField(IndexFields.PHARMGKB_ID.getFieldName(), entry.getPharmGKBId(), Field.Store.YES));
            doc.add(new TextField(IndexFields.GENE_ID.getFieldName(), entry.getGeneId(), Field.Store.YES));
            writer.addDocument(doc);
        }
    }

    private static PharmGKBGene entryFromDoc(final Document doc) {
        return PharmGKBGene.builder()
                .pharmGKBId(doc.getField(IndexFields.PHARMGKB_ID.getFieldName()).stringValue())
                .geneId(doc.getField(IndexFields.GENE_ID.getFieldName()).stringValue())
                .build();
    }

    private static Query getByPharmIdsQuery(final List<String> ids)
            throws ParseException {
        return getByIdsQuery(ids, IndexFields.PHARMGKB_ID.getFieldName());
    }
}
