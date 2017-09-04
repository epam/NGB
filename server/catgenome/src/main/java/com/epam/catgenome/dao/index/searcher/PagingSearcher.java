package com.epam.catgenome.dao.index.searcher;

import java.io.IOException;
import java.util.*;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.dao.index.indexer.AbstractDocumentBuilder;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.*;

public class PagingSearcher extends AbstractIndexSearcher{

    private final Integer page;
    private final Integer pageSize;

    public PagingSearcher(FeatureIndexDao featureIndexDao, FileManager fileManager,
            VcfManager vcfManager, VcfFilterForm filterForm) {
        super(featureIndexDao, fileManager, vcfManager, filterForm);
        this.page = filterForm.getPage();
        this.pageSize = filterForm.getPageSize();
    }

    @Override
    protected IndexSearchResult<VcfIndexEntry> performSearch(IndexSearcher searcher,
            MultiReader reader, Query query, Sort sort, AbstractDocumentBuilder<VcfIndexEntry> documentCreator)
            throws IOException {
        int numDocs = page == null ? reader.numDocs() : page * pageSize;
        final TopDocs docs = performSearch(searcher, query, reader, numDocs, sort);

        int totalHits = docs.totalHits;

        final ScoreDoc[] hits = docs.scoreDocs;
        List<VcfIndexEntry> entries = new ArrayList<>(pageSize);
        ScoreDoc lastEntry = createIndexEntries(hits, entries, searcher, documentCreator, page, pageSize);

        return new IndexSearchResult<>(entries, false, totalHits, lastEntry);
    }

    private ScoreDoc createIndexEntries(final ScoreDoc[] hits, List<VcfIndexEntry> entries,
            IndexSearcher searcher, AbstractDocumentBuilder<VcfIndexEntry> documentCreator, Integer page,
            Integer pageSize) throws IOException {
        int from = page != null ? (page - 1) * pageSize : 0;
        int to = page != null ? Math.min(from + pageSize, hits.length) : hits.length;
        if (from > hits.length) {
            return null;
        }

        for (int i = from; i < to; i++) {
            VcfIndexEntry entry = documentCreator.buildEntry(searcher, hits[i].doc);
            entries.add(entry);
        }
        return hits[to-1];
    }
}
