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
import com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBDisease;
import com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBGene;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.util.FileFormat;
import com.epam.catgenome.util.IndexUtils;
import lombok.Getter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBDisease.getUrl;
import static com.epam.catgenome.util.IndexUtils.buildTermQuery;
import static com.epam.catgenome.util.IndexUtils.getByIdsQuery;
import static com.epam.catgenome.util.NgbFileUtils.getFile;
import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;

@Service
public class PharmGKBDiseaseAssociationManager {

    private static final int COLUMNS = 11;
    private static final String GENE = "Gene";
    private static final String DISEASE = "Disease";
    private final int targetsTopHits;
    private final String indexDirectory;
    private final PharmGKBGeneManager pharmGKBGeneManager;

    public PharmGKBDiseaseAssociationManager(final @Value("${targets.index.directory}") String indexDirectory,
                                             final @Value("${targets.top.hits:10000}") int targetsTopHits,
                                             final PharmGKBGeneManager pharmGKBGeneManager) {
        this.targetsTopHits = targetsTopHits;
        this.pharmGKBGeneManager = pharmGKBGeneManager;
        this.indexDirectory = Paths.get(indexDirectory, "pharmgkb.disease").toString();
    }

    public SearchResult<PharmGKBDisease> search(final PharmGKBDiseaseSearchRequest request)
            throws IOException, ParseException {
        final List<PharmGKBDisease> entries = new ArrayList<>();
        final SearchResult<PharmGKBDisease> searchResult = new SearchResult<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {

            final int page = (request.getPage() == null || request.getPage() <= 0) ? 1 : request.getPage();
            final int pageSize = (request.getPageSize() == null || request.getPage() <= 0) ? DEFAULT_PAGE_SIZE
                    : request.getPageSize();
            final int hits = page * pageSize;

            final Query query = buildQuery(request);
            final Sort sort = getSort(request);
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, hits, sort);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            final int from = (page - 1) * pageSize;
            final int to = Math.min(from + pageSize, scoreDocs.length);
            for (int i = from; i < to; i++) {
                Document doc = searcher.doc(scoreDocs[i].doc);
                PharmGKBDisease entry = entryFromDoc(doc);
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
            final Query query = getByGeneIdsQuery(ids);
            final IndexSearcher searcher = new IndexSearcher(indexReader);
            final TopDocs topDocs = searcher.search(query, targetsTopHits);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            final List<PharmGKBDisease> entries = new ArrayList<>();
            for (ScoreDoc scoreDoc : scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                PharmGKBDisease entry = entryFromDoc(doc);
                entries.add(entry);
            }
            return entries.stream().map(PharmGKBDisease::getId).distinct().count();
        }
    }

    public void importData(final String path) throws IOException, ParseException {
        getFile(path);
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            final List<PharmGKBDisease> entries = readEntries(path);
            final Set<String> pharmGeneIds = entries.stream()
                    .map(PharmGKBDisease::getGeneId)
                    .collect(Collectors.toSet());
            final List<PharmGKBGene> geneIds = pharmGKBGeneManager.search(new ArrayList<>(pharmGeneIds));
            final Map<String, PharmGKBGene> geneIdsMap = geneIds.stream()
                    .collect(Collectors.toMap(PharmGKBGene::getPharmGKBId, Function.identity()));
            for (PharmGKBDisease entry: entries) {
                PharmGKBGene gene = geneIdsMap.getOrDefault(entry.getGeneId(), null);
                if (gene != null) {
                    entry.setGeneId(gene.getGeneId());
                    addDoc(writer, entry);
                }
            }
        }
    }

    public List<String> getFieldValues(final PharmGKBDiseaseField field) throws IOException {
        return IndexUtils.getFieldValues(field.getName(), indexDirectory);
    }

    private List<PharmGKBDisease> readEntries(final String path) throws IOException {
        final List<PharmGKBDisease> entries = new ArrayList<>();
        String line;
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            line = bufferedReader.readLine();
            String[] cells = line.split(FileFormat.TSV.getSeparator());
            Assert.isTrue(cells.length == COLUMNS, MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(FileFormat.TSV.getSeparator());
                if (cells[2].trim().equals(GENE) && cells[5].trim().equals(DISEASE)) {
                    PharmGKBDisease entry = PharmGKBDisease.builder()
                            .id(cells[3].trim())
                            .name(cells[4].trim())
                            .geneId(cells[0].trim())
                            .build();
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    @Getter
    private enum IndexFields {
        GENE_ID("geneId"),
        DISEASE_NAME("diseaseName"),
        DISEASE_ID("diseaseId");
        private final String name;

        IndexFields(String name) {
            this.name = name;
        }
    }

    private static void addDoc(final IndexWriter writer, final PharmGKBDisease entry) throws IOException {
        final Document doc = new Document();
        doc.add(new TextField(IndexFields.GENE_ID.getName(), entry.getGeneId(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexFields.GENE_ID.getName(), new BytesRef(entry.getGeneId())));

        doc.add(new TextField(IndexFields.DISEASE_ID.getName(), entry.getId(), Field.Store.YES));

        doc.add(new TextField(IndexFields.DISEASE_NAME.getName(), entry.getName(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexFields.DISEASE_NAME.getName(), new BytesRef(entry.getName())));
        writer.addDocument(doc);
    }

    private static PharmGKBDisease entryFromDoc(final Document doc) {
        final String id = doc.getField(IndexFields.DISEASE_ID.getName()).stringValue();
        return PharmGKBDisease.builder()
                .geneId(doc.getField(IndexFields.GENE_ID.getName()).stringValue())
                .id(id)
                .name(doc.getField(IndexFields.DISEASE_NAME.getName()).stringValue())
                .url(getUrl(id))
                .build();
    }

    private static Query getByGeneIdsQuery(final List<String> ids) throws ParseException {
        return getByIdsQuery(ids, IndexFields.GENE_ID.getName());
    }

    private static Query buildQuery(final PharmGKBDiseaseSearchRequest request) throws ParseException {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        mainBuilder.add(getByGeneIdsQuery(request.getGeneIds()), BooleanClause.Occur.MUST);
        if (request.getFilterBy() != null && request.getTerm() != null) {
            Query query;
            if (PharmGKBDiseaseField.DISEASE_NAME.equals(request.getFilterBy())) {
                final StandardAnalyzer analyzer = new StandardAnalyzer();
                query = new QueryParser(request.getFilterBy().getName(), analyzer).parse(request.getTerm());
            } else {
                query = buildTermQuery(request.getTerm(), request.getFilterBy().getName());
            }
            mainBuilder.add(query, BooleanClause.Occur.MUST);
        }
        return mainBuilder.build();
    }

    private static Sort getSort(final PharmGKBDiseaseSearchRequest request) {
        final SortField sortField = request.getOrderBy() == null ?
                new SortField(PharmGKBDiseaseField.getDefault(), SortField.Type.STRING, false) :
                new SortField(request.getOrderBy().getName(), SortField.Type.STRING, request.isReverse());
        return new Sort(sortField);
    }
}
