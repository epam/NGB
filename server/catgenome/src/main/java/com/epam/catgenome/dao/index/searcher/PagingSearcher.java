/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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


package com.epam.catgenome.dao.index.searcher;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;

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
            VcfManager vcfManager, VcfFilterForm filterForm, ExecutorService executorService) {
        super(featureIndexDao, fileManager, vcfManager, filterForm, executorService);
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
        return hits.length == 0 ? null : hits[to-1];
    }
}
