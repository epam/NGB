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
import com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBDrugAssociation;
import com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBGene;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.util.FileFormat;
import com.epam.catgenome.util.IndexUtils;
import lombok.Getter;
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

import static com.epam.catgenome.util.IndexUtils.buildTermQuery;
import static com.epam.catgenome.util.IndexUtils.getByIdsQuery;
import static com.epam.catgenome.util.NgbFileUtils.getFile;
import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;

@Service
public class PharmGKBDrugAssociationManager {

    private static final String DRUG_URL_PATTERN = "https://www.pharmgkb.org/labelAnnotation/%s";
    private final String indexDirectory;
    private final int targetsTopHits;
    private final PharmGKBGeneManager pharmGKBGeneManager;
    private final PharmGKBDrugManager pharmGKBDrugManager;

    public PharmGKBDrugAssociationManager(final @Value("${targets.index.directory}") String indexDirectory,
                                          final @Value("${targets.top.hits:10000}") int targetsTopHits,
                                          final PharmGKBGeneManager pharmGKBGeneManager,
                                          final PharmGKBDrugManager pharmGKBDrugManager) {
        this.indexDirectory = Paths.get(indexDirectory, "pharmgkb.drug.association").toString();
        this.targetsTopHits = targetsTopHits;
        this.pharmGKBGeneManager = pharmGKBGeneManager;
        this.pharmGKBDrugManager = pharmGKBDrugManager;
    }

    public SearchResult<PharmGKBDrug> search(final PharmGKBDrugSearchRequest request)
            throws IOException, ParseException {
        final List<PharmGKBDrug> entries = new ArrayList<>();
        final SearchResult<PharmGKBDrug> searchResult = new SearchResult<>();
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
                PharmGKBDrug entry = entryFromDoc(doc);
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
            final List<PharmGKBDrug> entries = new ArrayList<>();
            for (ScoreDoc scoreDoc : scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                PharmGKBDrug entry = entryFromDoc(doc);
                entries.add(entry);
            }
            return entries.stream().map(PharmGKBDrug::getId).distinct().count();
        }
    }

    public void importData(final String path) throws IOException, ParseException {
        getFile(path);
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            final List<PharmGKBDrugAssociation> entries = readEntries(path);
            for (PharmGKBDrug entry: toPharmGKBDrugs(entries)) {
                addDoc(writer, entry);
            }
        }
    }

    public void importData(final String drugPath, final String drugAssociationPath)
            throws IOException, ParseException {
        pharmGKBDrugManager.importData(drugPath);
        importData(drugAssociationPath);
    }

    public List<String> getFieldValues(final PharmGKBDrugField field) throws IOException {
        return IndexUtils.getFieldValues(field.getName(), indexDirectory);
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
        GENE_ID("geneId"),
        DRUG_NAME("drugName"),
        DRUG_ID("drugId"),
        SOURCE("source");
        private final String name;

        IndexFields(String name) {
            this.name = name;
        }
    }

    private List<PharmGKBDrug> toPharmGKBDrugs(final List<PharmGKBDrugAssociation> entries)
            throws IOException, ParseException {
        final Set<String> pharmGeneIds = entries.stream()
                .map(PharmGKBDrugAssociation::getPharmGKBGeneId)
                .collect(Collectors.toSet());
        final List<PharmGKBGene> pharmGKBGenes = pharmGKBGeneManager.search(new ArrayList<>(pharmGeneIds));
        final Map<String, String> genesMap = pharmGKBGenes.stream()
                .collect(Collectors.toMap(PharmGKBGene::getPharmGKBId, PharmGKBGene::getGeneId));

        final Set<String> drugIds = entries.stream()
                .map(PharmGKBDrugAssociation::getDrugId)
                .collect(Collectors.toSet());
        final List<PharmGKBDrug> drugs = pharmGKBDrugManager.search(new ArrayList<>(drugIds));
        final Map<String, PharmGKBDrug> drugsMap = drugs.stream()
                .collect(Collectors.toMap(PharmGKBDrug::getId, Function.identity()));
        final List<PharmGKBDrug> result = new ArrayList<>();
        for (PharmGKBDrugAssociation entry: entries) {
            if (drugsMap.containsKey(entry.getDrugId())) {
                PharmGKBDrug drug = drugsMap.get(entry.getDrugId());
                PharmGKBDrug drugAssociation = PharmGKBDrug.builder()
                        .id(entry.getDrugId())
                        .name(drug.getName())
                        .geneId(genesMap.get(entry.getPharmGKBGeneId()))
                        .source(drug.getSource())
                        .build();
                result.add(drugAssociation);
            }
        }
        return result;
    }

    private static void addDoc(final IndexWriter writer, final PharmGKBDrug entry) throws IOException {
        final String geneId = entry.getGeneId();
        if (geneId != null) {
            final Document doc = new Document();
            doc.add(new TextField(IndexFields.GENE_ID.getName(), geneId, Field.Store.YES));
            doc.add(new SortedDocValuesField(IndexFields.GENE_ID.getName(), new BytesRef(geneId)));

            doc.add(new TextField(IndexFields.DRUG_ID.getName(), entry.getId(), Field.Store.YES));

            doc.add(new TextField(IndexFields.DRUG_NAME.getName(), entry.getName(), Field.Store.YES));
            doc.add(new SortedDocValuesField(IndexFields.DRUG_NAME.getName(), new BytesRef(entry.getName())));

            doc.add(new StringField(IndexFields.SOURCE.getName(), entry.getSource(), Field.Store.YES));
            doc.add(new SortedDocValuesField(IndexFields.SOURCE.getName(), new BytesRef(entry.getSource())));
            writer.addDocument(doc);
        }
    }

    private static PharmGKBDrug entryFromDoc(final Document doc) {
        final String id = doc.getField(IndexFields.DRUG_ID.getName()).stringValue();
        return PharmGKBDrug.builder()
                .id(id)
                .name(doc.getField(IndexFields.DRUG_NAME.getName()).stringValue())
                .url(getDrugUrl(id))
                .geneId(doc.getField(IndexFields.GENE_ID.getName()).stringValue())
                .source(doc.getField(IndexFields.SOURCE.getName()).stringValue())
                .build();
    }

    private static Query getByGeneIdsQuery(final List<String> ids) throws ParseException {
        return getByIdsQuery(ids, IndexFields.GENE_ID.getName());
    }

    private static Sort getSort(final PharmGKBDrugSearchRequest request) {
        final SortField sortField = request.getOrderBy() == null ?
                new SortField(PharmGKBDrugField.getDefault(), SortField.Type.STRING, false) :
                new SortField(request.getOrderBy().getName(), SortField.Type.STRING, request.isReverse());
        return new Sort(sortField);
    }

    private static Query buildQuery(final PharmGKBDrugSearchRequest request) throws ParseException {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        mainBuilder.add(getByGeneIdsQuery(request.getGeneIds()), BooleanClause.Occur.MUST);
        if (request.getFilterBy() != null && request.getTerm() != null) {
            Query query;
            if (PharmGKBDrugField.DRUG_NAME.equals(request.getFilterBy())) {
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
