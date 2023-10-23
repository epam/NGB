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
package com.epam.catgenome.manager.externaldb.target;

import com.epam.catgenome.entity.externaldb.target.Association;
import com.epam.catgenome.entity.index.FilterType;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.index.AbstractIndexManager;
import com.epam.catgenome.manager.index.Filter;
import com.epam.catgenome.manager.index.SearchRequest;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.util.IndexUtils.*;
import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;
import static java.util.stream.Collectors.groupingBy;

public abstract class AbstractAssociationManager<T extends Association> extends AbstractIndexManager<T> {

    public AbstractAssociationManager(String indexDirectory, int topHits) {
        super(indexDirectory, topHits);
    }

    public SearchResult<T> search(final AssociationSearchRequest request)
            throws IOException, ParseException {
        final Query query = buildQuery(request.getGeneIds(), request.getFilters());
        return search(request, query);
    }

    public SearchResult<T> search(final SearchRequest request, final String diseaseId)
            throws ParseException, IOException {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        mainBuilder.add(getByTermQuery(diseaseId, IndexCommonFields.DISEASE_ID.name()), BooleanClause.Occur.MUST);
        if (request.getFilters() != null) {
            for (Filter filter: request.getFilters()) {
                addFieldQuery(mainBuilder, filter);
            }
        }
        final Query query = mainBuilder.build();
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
                T entry = entryFromDocDiseaseView(doc);
                entries.add(entry);
            }
            searchResult.setItems(entries);
            searchResult.setTotalCount(topDocs.totalHits);
        }
        return searchResult;
    }

    public List<T> search(final String diseaseId)
            throws ParseException, IOException {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        mainBuilder.add(getByTermQuery(diseaseId, IndexCommonFields.DISEASE_ID.name()), BooleanClause.Occur.MUST);
        final List<T> entries = new ArrayList<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(mainBuilder.build(), topHits, getDefaultSort());
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            for (ScoreDoc scoreDoc : scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                T entry = entryFromDocDiseaseView(doc);
                entries.add(entry);
            }
        }
        return entries;
    }

    public List<T> search(final List<String> geneIds) throws ParseException, IOException {
        return search(getByGeneIdsQuery(geneIds), getDefaultSort());
    }

    public T entryFromDocDiseaseView(Document doc) {
        return null;
    };

    public static Query getByGeneIdsQuery(final List<String> ids) throws ParseException {
        return getByTermsQuery(ids, IndexCommonFields.GENE_ID.name());
    }

    public List<T> searchByGeneIds(final List<String> ids) throws ParseException, IOException {
        return search(ids, IndexCommonFields.GENE_ID.name());
    }

    public Query buildQuery(final List<String> geneIds, final List<Filter> filters) throws ParseException {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        mainBuilder.add(getByGeneIdsQuery(geneIds), BooleanClause.Occur.MUST);
        if (filters != null) {
            for (Filter filter: filters) {
                addFieldQuery(mainBuilder, filter);
            }
        }
        return mainBuilder.build();
    }

    public void addFieldQuery(BooleanQuery.Builder builder, Filter filter) throws ParseException {
        Query query;
        switch (getFilterType(filter.getField())) {
            case PHRASE:
                query = getByPhraseQuery(filter.getTerms().get(0), filter.getField());
                break;
            case TERM:
                query = getByTermsQuery(filter.getTerms(), filter.getField());
                break;
            case OPTIONS:
                query = getByOptionsQuery(filter.getTerms(), filter.getField());
                break;
            case RANGE:
                query = getByRangeQuery(filter.getRange(), filter.getField());
                break;
            default:
                return;
        }
        builder.add(query, BooleanClause.Occur.MUST);
    }

    public Pair<Long, Long> recordsCount(final List<String> geneIds) throws ParseException, IOException {
        final List<T> result = searchByGeneIds(geneIds);
        return Pair.of((long) result.size(), result.stream().map(T::getId).distinct().count());
    }

    public Long totalCount(final List<String> geneIds) throws ParseException, IOException {
        final List<T> result = searchByGeneIds(geneIds);
        return (long) result.size();
    }

    public Map<String, Pair<Long, Long>> recordsCountMap(final List<String> geneIds)
            throws ParseException, IOException {
        final List<T> result = searchByGeneIds(geneIds);
        final Map<String, Pair<Long, Long>> totalCounts = new HashMap<>();
        final Map<String, List<T>> grouped = result.stream().collect(groupingBy(T::getGeneId));
        grouped.forEach((k, v) ->
                totalCounts.put(k.toLowerCase(),
                        Pair.of((long) v.size(), v.stream().map(T::getId).distinct().count())));
        return totalCounts;
    }

    public Map<String, Long> totalCountMap(final List<String> geneIds) throws ParseException, IOException {
        final List<T> result = searchByGeneIds(geneIds);
        final Map<String, Long> totalCounts = new HashMap<>();
        final Map<String, List<T>> grouped = result.stream().collect(groupingBy(T::getGeneId));
        grouped.forEach((k, v) -> totalCounts.put(k.toLowerCase(), (long) v.size()));
        return totalCounts;
    }

    public abstract FilterType getFilterType(String fieldName);

    @Getter
    private enum IndexCommonFields {
        GENE_ID,
        DISEASE_ID;
    }
}
