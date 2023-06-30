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
package com.epam.catgenome.manager.externaldb.dgidb;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.externaldb.dgidb.DGIDBDrugAssociation;
import com.epam.catgenome.entity.externaldb.opentarget.UrlEntity;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.util.FileFormat;
import com.epam.catgenome.util.IndexUtils;
import org.apache.http.util.TextUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.jetbrains.annotations.Nullable;
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

import static com.epam.catgenome.util.IndexUtils.buildTermQuery;
import static com.epam.catgenome.util.IndexUtils.getByIdsQuery;
import static com.epam.catgenome.util.NgbFileUtils.getFile;
import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;

@Service
public class DGIDBDrugAssociationManager {

    private static final String DRUG_URL_PATTERN = "https://www.dgidb.org/drugs/%s#_summary";
    private static final int COLUMNS = 11;
    private final String indexDirectory;
    private final int targetsTopHits;

    public DGIDBDrugAssociationManager(final @Value("${targets.index.directory}") String indexDirectory,
                                       final @Value("${targets.top.hits:10000}") int targetsTopHits) {
        this.indexDirectory = Paths.get(indexDirectory, "dgidb.drug.association").toString();
        this.targetsTopHits = targetsTopHits;
    }

    public SearchResult<DGIDBDrugAssociation> search(final DGIDBDrugSearchRequest request)
            throws IOException, ParseException {
        final List<DGIDBDrugAssociation> entries = new ArrayList<>();
        final SearchResult<DGIDBDrugAssociation> searchResult = new SearchResult<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {

            final int page = (request.getPage() == null || request.getPage() <= 0) ? 1 : request.getPage();
            final int pageSize = (request.getPageSize() == null || request.getPage() <= 0) ? DEFAULT_PAGE_SIZE
                    : request.getPageSize();
            final int hits = page * pageSize;

            final Query query = buildQuery(request);
            IndexSearcher searcher = new IndexSearcher(indexReader);
            final Sort sort = getSort(request);
            TopDocs topDocs = searcher.search(query, hits, sort);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            final int from = (page - 1) * pageSize;
            final int to = Math.min(from + pageSize, scoreDocs.length);
            for (int i = from; i < to; i++) {
                Document doc = searcher.doc(scoreDocs[i].doc);
                DGIDBDrugAssociation entry = entryFromDoc(doc);
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
            final Query query = getByEntrezIdsQuery(ids);
            final IndexSearcher searcher = new IndexSearcher(indexReader);
            final TopDocs topDocs = searcher.search(query, targetsTopHits);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            final List<DGIDBDrugAssociation> entries = new ArrayList<>();
            for (ScoreDoc scoreDoc : scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                DGIDBDrugAssociation entry = entryFromDoc(doc);
                entries.add(entry);
            }
            return entries.stream().map(e -> e.getDrug().getName()).distinct().count();
        }
    }

    public void importData(final String path) throws IOException {
        getFile(path);
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            for (DGIDBDrugAssociation entry: readEntries(path)) {
                addDoc(writer, entry);
            }
        }
    }

    public List<String> getFieldValues(final DGIDBDrugField field) throws IOException {
        return IndexUtils.getFieldValues(field.getName(), indexDirectory);
    }

    public List<DGIDBDrugAssociation> readEntries(final String path) throws IOException {
        final List<DGIDBDrugAssociation> entries = new ArrayList<>();
        String line;
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            line = bufferedReader.readLine();
            String[] cells = line.split(FileFormat.TSV.getSeparator());
            Assert.isTrue(cells.length == COLUMNS, MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(FileFormat.TSV.getSeparator());
                UrlEntity urlEntity = new UrlEntity();
                urlEntity.setName(getCellValue(cells, 7));
                DGIDBDrugAssociation entry = DGIDBDrugAssociation.builder()
                        .entrezId(getCellValue(cells, 2))
                        .interactionClaimSource(getCellValue(cells, 3))
                        .interactionTypes(getCellValue(cells, 4))
                        .drug(urlEntity)
                        .build();
                entries.add(entry);
            }
        }
        return entries;
    }

    @Nullable
    private static String getCellValue(final String[] cells, final int x) {
        return cells.length >= x + 1 ? cells[x].trim() : null;
    }

    private static void addDoc(final IndexWriter writer, final DGIDBDrugAssociation entry) throws IOException {
        if (!TextUtils.isBlank(entry.getEntrezId()) && !TextUtils.isBlank(entry.getDrug().getName())) {
            final Document doc = new Document();

            doc.add(new TextField(DGIDBDrugField.GENE_ID.getName(), entry.getEntrezId(), Field.Store.YES));

            doc.add(new TextField(DGIDBDrugField.DRUG_NAME.getName(), entry.getDrug().getName(), Field.Store.YES));
            doc.add(new SortedDocValuesField(DGIDBDrugField.DRUG_NAME.getName(),
                    new BytesRef(entry.getDrug().getName())));

            doc.add(new StringField(DGIDBDrugField.INTERACTION_TYPES.getName(),
                    entry.getInteractionTypes(), Field.Store.YES));
            doc.add(new SortedDocValuesField(DGIDBDrugField.INTERACTION_TYPES.getName(),
                    new BytesRef(entry.getInteractionTypes())));

            doc.add(new StringField(DGIDBDrugField.INTERACTION_CLAIM_SOURCE.getName(),
                    entry.getInteractionClaimSource(), Field.Store.YES));
            doc.add(new SortedDocValuesField(DGIDBDrugField.INTERACTION_CLAIM_SOURCE.getName(),
                    new BytesRef(entry.getInteractionClaimSource())));
            writer.addDocument(doc);
        }
    }

    private static DGIDBDrugAssociation entryFromDoc(final Document doc) {
        final String drugName = doc.getField(DGIDBDrugField.DRUG_NAME.getName()).stringValue();
        final UrlEntity urlEntity = new UrlEntity();
        urlEntity.setName(drugName);
        urlEntity.setUrl(getDrugUrl(drugName));
        return DGIDBDrugAssociation.builder()
                .entrezId(doc.getField(DGIDBDrugField.GENE_ID.getName()).stringValue())
                .drug(urlEntity)
                .interactionTypes(doc.getField(DGIDBDrugField.INTERACTION_TYPES.getName()).stringValue())
                .interactionClaimSource(doc.getField(DGIDBDrugField.INTERACTION_CLAIM_SOURCE.getName()).stringValue())
                .build();
    }

    private static Query getByEntrezIdsQuery(final List<String> ids)
            throws ParseException {
        return getByIdsQuery(ids, DGIDBDrugField.GENE_ID.getName());
    }

    private static Sort getSort(final DGIDBDrugSearchRequest request) {
        final SortField sortField = request.getOrderBy() == null ?
                new SortField(DGIDBDrugField.getDefault(), SortField.Type.STRING, false) :
                new SortField(request.getOrderBy().getName(), SortField.Type.STRING, request.isReverse());
        return new Sort(sortField);
    }

    private static Query buildQuery(final DGIDBDrugSearchRequest request) throws ParseException {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        mainBuilder.add(getByEntrezIdsQuery(request.getGeneIds()), BooleanClause.Occur.MUST);
        if (request.getFilterBy() != null && request.getTerm() != null) {
            Query query;
            if (DGIDBDrugField.DRUG_NAME.equals(request.getFilterBy())) {
                final StandardAnalyzer analyzer = new StandardAnalyzer();
                query = new QueryParser(request.getFilterBy().getName(), analyzer).parse(request.getTerm());
            } else {
                query = buildTermQuery(request.getTerm(), request.getFilterBy().getName());
            }
            mainBuilder.add(query, BooleanClause.Occur.MUST);
        }
        return mainBuilder.build();
    }

    private static String getDrugUrl(final String drugId) {
        return String.format(DRUG_URL_PATTERN, drugId);
    }
}
