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
import com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBDrugAssociation;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.target.AssociationSearchRequest;
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
import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;

@Service
public class PharmGKBDrugAssociationManager {

    @Value("${pharmgkb.drug.association.index.directory}")
    private String indexDirectory;

    @Value("${targets.top.hits:10000}")
    private int targetsTopHits;

    public SearchResult<PharmGKBDrugAssociation> search(final AssociationSearchRequest request)
            throws IOException, ParseException {
        final List<PharmGKBDrugAssociation> entries = new ArrayList<>();
        final SearchResult<PharmGKBDrugAssociation> searchResult = new SearchResult<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {

            final int page = (request.getPage() == null || request.getPage() <= 0) ? 1 : request.getPage();
            final int pageSize = (request.getPageSize() == null || request.getPage() <= 0) ? DEFAULT_PAGE_SIZE
                    : request.getPageSize();
            final int hits = page * pageSize;

            final Query query = getByPharmGKBIdsQuery(request.getGeneIds());
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, hits);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            final int from = (page - 1) * pageSize;
            final int to = Math.min(from + pageSize, scoreDocs.length);
            for (int i = from; i < to; i++) {
                Document doc = searcher.doc(scoreDocs[i].doc);
                PharmGKBDrugAssociation entry = entryFromDoc(doc);
                entries.add(entry);
            }
            searchResult.setItems(entries);
            searchResult.setTotalCount(topDocs.totalHits);
        }
        return searchResult;
    }

    public long totalCount(final List<String> ids) throws ParseException, IOException {
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            final Query query = getByPharmGKBIdsQuery(ids);
            final IndexSearcher searcher = new IndexSearcher(indexReader);
            final TopDocs topDocs = searcher.search(query, targetsTopHits);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            final List<PharmGKBDrugAssociation> entries = new ArrayList<>();
            for (ScoreDoc scoreDoc : scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                PharmGKBDrugAssociation entry = entryFromDoc(doc);
                entries.add(entry);
            }
            return entries.stream().map(PharmGKBDrugAssociation::getDrugId).distinct().count();
        }
    }

    public void importData(final String path) throws IOException {
        getFile(path);
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            for (PharmGKBDrugAssociation entry: readEntries(path)) {
                addDoc(writer, entry);
            }
        }
    }

    private List<PharmGKBDrugAssociation> readEntries(final String path) throws IOException {
        final List<PharmGKBDrugAssociation> entries = new ArrayList<>();
        String line;
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            line = bufferedReader.readLine();
            String[] cells = line.split(FileFormat.TSV.getSeparator());
            String pharmGKBId;
            String[] drugIds;
            Assert.isTrue(cells.length == 4, MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(FileFormat.TSV.getSeparator());
                pharmGKBId = cells[0].trim();
                drugIds = cells[2].trim().split(";");
                for (String drug : drugIds) {
                    PharmGKBDrugAssociation entry = PharmGKBDrugAssociation.builder()
                            .pharmGKBGeneId(pharmGKBId)
                            .drugId(drug)
                            .build();
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    @Getter
    private enum IndexFields {
        PHARMGKB_ID("Gene_ID"),
        DRUG_ID("Label_ID");
        private final String fieldName;

        IndexFields(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    private static void addDoc(final IndexWriter writer, final PharmGKBDrugAssociation entry) throws IOException {
        final Document doc = new Document();
        doc.add(new TextField(IndexFields.PHARMGKB_ID.getFieldName(),
                String.valueOf(entry.getPharmGKBGeneId()), Field.Store.YES));
        doc.add(new TextField(IndexFields.DRUG_ID.getFieldName(),
                String.valueOf(entry.getDrugId()), Field.Store.YES));
        writer.addDocument(doc);
    }

    private static PharmGKBDrugAssociation entryFromDoc(final Document doc) {
        return PharmGKBDrugAssociation.builder()
                .pharmGKBGeneId(doc.getField(IndexFields.PHARMGKB_ID.getFieldName()).stringValue())
                .drugId(doc.getField(IndexFields.DRUG_ID.getFieldName()).stringValue())
                .build();
    }

    private static Query getByPharmGKBIdsQuery(final List<String> ids) throws ParseException {
        return getByIdsQuery(ids, IndexFields.PHARMGKB_ID.getFieldName());
    }
}
