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
package com.epam.catgenome.manager.index;

import com.epam.catgenome.manager.externaldb.SearchResult;
import com.google.common.collect.Lists;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.epam.catgenome.util.IndexUtils.getByTermsQuery;
import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;

public abstract class AbstractIndexManager<T> {

    private static final Integer BATCH_SIZE = 1000;
    public final String indexDirectory;
    public final int topHits;

    public AbstractIndexManager(String indexDirectory, int topHits) {
        this.indexDirectory = indexDirectory;
        this.topHits = topHits;
    }

    public SearchResult<T> search(final SearchRequest request, final Query query)
            throws IOException, ParseException {
        final List<T> entries = new ArrayList<>();
        final SearchResult<T> searchResult = new SearchResult<>();
        final int page = (request.getPage() == null || request.getPage() <= 0) ? 1 : request.getPage();
        final int pageSize = (request.getPageSize() == null || request.getPage() <= 0) ? DEFAULT_PAGE_SIZE
                : request.getPageSize();
        final int hits = page * pageSize;

        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, hits, getSort(request.getOrderInfos()));
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            final int from = (page - 1) * pageSize;
            final int to = Math.min(from + pageSize, scoreDocs.length);
            for (int i = from; i < to; i++) {
                Document doc = searcher.doc(scoreDocs[i].doc);
                T entry = entryFromDoc(doc);
                entries.add(entry);
            }
            searchResult.setItems(entries);
            searchResult.setTotalCount(topDocs.totalHits);
        }
        return searchResult;
    }

    public List<T> search(final Query query, final Sort sort) throws IOException, ParseException {
        final List<T> entries = new ArrayList<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = sort == null ? searcher.search(query, topHits) :
                    searcher.search(query, topHits, sort);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            for (ScoreDoc scoreDoc : scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                T entry = entryFromDoc(doc);
                entries.add(entry);
            }
        }
        return entries;
    }

    public List<T> search(final List<String> ids, final String fieldName)
            throws ParseException, IOException {
        final List<T> result = new ArrayList<>();
        final List<List<String>> subSets = Lists.partition(ids, BATCH_SIZE);
        for (List<String> subIds : subSets) {
            Query query = getByTermsQuery(subIds, fieldName);
            try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
                 IndexReader indexReader = DirectoryReader.open(index)) {
                IndexSearcher searcher = new IndexSearcher(indexReader);
                TopDocs topDocs = searcher.search(query, topHits);
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document doc = searcher.doc(scoreDoc.doc);
                    T entry = entryFromDoc(doc);
                    result.add(entry);
                }
            }
        }
        return result;
    }

    public void importData(final String path) throws IOException, ParseException {
        final List<T> entries = readEntries(path);
        final List<T> processedEntries = processEntries(entries);
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            for (T entry: processedEntries) {
                addDoc(writer, entry);
            }
        }
    }

    public Sort getSort(final List<OrderInfo> orderInfos) {
        final List<SortField> sortFields = new ArrayList<>();
        if (orderInfos == null) {
            sortFields.add(new SortField(getDefaultSortField(), SortField.Type.STRING, false));
        } else {
            for (OrderInfo orderInfo : orderInfos) {
                final SortField sortField = new SortField(orderInfo.getOrderBy(),
                        SortField.Type.STRING, orderInfo.isReverse());
                sortFields.add(sortField);
            }
        }
        return new Sort(sortFields.toArray(new SortField[sortFields.size()]));
    }

    public abstract List<T> readEntries(String path) throws IOException;
    public abstract String getDefaultSortField();
    public abstract List<T> processEntries(List<T> entries) throws IOException, ParseException;
    public abstract void addDoc(IndexWriter writer, T entry) throws IOException;
    public abstract T entryFromDoc(Document doc);
}
