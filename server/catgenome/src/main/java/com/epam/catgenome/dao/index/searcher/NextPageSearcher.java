package com.epam.catgenome.dao.index.searcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.dao.index.indexer.AbstractDocumentBuilder;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.*;

public class NextPageSearcher extends AbstractIndexSearcher {

    private ScoreDoc pointer;
    private Integer pageSize;

    public NextPageSearcher(FeatureIndexDao featureIndexDao, FileManager fileManager,
            VcfManager vcfManager, VcfFilterForm filterForm) {
        super(featureIndexDao, fileManager, vcfManager, filterForm);
        this.pointer = filterForm.getPointer().toScoreDoc();
        this.pageSize = filterForm.getPageSize();
    }

    @Override
    protected IndexSearchResult<VcfIndexEntry> performSearch(IndexSearcher searcher,
            MultiReader reader, Query query, Sort sort, AbstractDocumentBuilder<VcfIndexEntry> documentCreator)
            throws IOException {
        final TopDocs docs = getNextPage(searcher, query, pointer, pageSize, sort);
        final ScoreDoc[] hits = docs.scoreDocs;
        List<VcfIndexEntry> entries = new ArrayList<>(pageSize);
        for (int i = 0; i < hits.length; i++) {
            VcfIndexEntry entry = documentCreator.buildEntry(searcher, hits[i].doc);
            entries.add(entry);
        }
        ScoreDoc lastEntry = hits[hits.length-1];
        return new IndexSearchResult<>(entries, false, docs.totalHits, lastEntry);
    }

    private TopDocs getNextPage(IndexSearcher searcher, Query query, ScoreDoc pointer,
            Integer pageSize, Sort sort) throws IOException {
        final TopDocs docs;
        Query constantQuery = new ConstantScoreQuery(query);
        if (sort == null) {
            docs = searcher.searchAfter(pointer, constantQuery, pageSize);
        } else {

            docs = searcher.searchAfter(pointer, constantQuery, pageSize, sort, false, false);
        }
        return docs;
    }
}
