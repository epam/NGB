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
package com.epam.catgenome.manager.externaldb.ncbi;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.util.FileFormat;
import com.google.common.collect.Lists;
import lombok.Getter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.util.IndexUtils.getByIdsQuery;
import static com.epam.catgenome.util.NgbFileUtils.getFile;

@Service
public class NCBIGeneIdsManager {

    private static final Integer BATCH_SIZE = 1000;
    private static final int COLUMNS = 7;
    private final String indexDirectory;

    public NCBIGeneIdsManager(final @Value("${ncbi.index.directory}") String indexDirectory) {
        this.indexDirectory = Paths.get(indexDirectory, "gene.ids").toString();
    }

    public Map<String, String> searchByEntrezIds(final List<String> ids) throws ParseException, IOException {
        return search(ids, true);
    }

    public Map<String, String> searchByEnsemblIds(final List<String> ids) throws ParseException, IOException {
        return search(ids, false);
    }

    private Map<String, String> search(final List<String> ids, final boolean byEntrez)
            throws ParseException, IOException {
        final Map<String, String> result = new HashMap<>();
        final List<List<String>> subSets = Lists.partition(ids, BATCH_SIZE);
        for (List<String> subIds : subSets) {
            Query query = getByIdsQuery(subIds, byEntrez ? IndexFields.ENTREZ_ID.getName() :
                    IndexFields.ENSEMBL_ID.getName());
            try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
                 IndexReader indexReader = DirectoryReader.open(index)) {
                IndexSearcher searcher = new IndexSearcher(indexReader);
                TopDocs topDocs = searcher.search(query, subIds.size());
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document doc = searcher.doc(scoreDoc.doc);
                    Pair<String, String> entry = entryFromDoc(doc);
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    public void importData(final String path) throws IOException {
        getFile(path);
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            for (Map.Entry<String, String> entry: readEntries(path).entrySet()) {
                addDoc(writer, entry);
            }
        }
    }

    @Getter
    private enum IndexFields {
        ENTREZ_ID("entrezId"),
        ENSEMBL_ID("ensemblId");
        private final String name;

        IndexFields(String name) {
            this.name = name;
        }
    }

    public Map<String, String> readEntries(final String path) throws IOException {
        final Map<String, String> entries = new HashMap<>();
        String line;
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            line = bufferedReader.readLine();
            String[] cells = line.split(FileFormat.TSV.getSeparator());
            Assert.isTrue(cells.length == COLUMNS, MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(FileFormat.TSV.getSeparator());
                entries.put(cells[1].trim(), cells[2].trim());
            }
        }
        return entries;
    }

    private static void addDoc(final IndexWriter writer, final Map.Entry<String, String> entry) throws IOException {
        final Document doc = new Document();
        doc.add(new StringField(IndexFields.ENTREZ_ID.getName(), entry.getKey(), Field.Store.YES));
        doc.add(new TextField(IndexFields.ENSEMBL_ID.getName(), entry.getValue(), Field.Store.YES));
        writer.addDocument(doc);
    }

    private static Pair<String, String> entryFromDoc(final Document doc) {
        return new ImmutablePair<>(doc.getField(IndexFields.ENTREZ_ID.getName()).stringValue(),
                doc.getField(IndexFields.ENSEMBL_ID.getName()).stringValue());
    }
}
