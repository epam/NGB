/*
 * MIT License
 *
 * Copyright (c) 2017-2021 EPAM Systems
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

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.dao.index.indexer.AbstractDocumentBuilder;
import com.epam.catgenome.entity.AbstractFilterForm;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class NextPageSearcher<T extends FeatureIndexEntry, R extends AbstractFilterForm>
        extends AbstractIndexSearcher<T, R> {

    private ScoreDoc pointer;
    private Integer pageSize;

    public NextPageSearcher(final FeatureIndexDao featureIndexDao, final FileManager fileManager,
            final VcfManager vcfManager, final R filterForm, final ExecutorService executorService) {
        super(featureIndexDao, fileManager, vcfManager, filterForm, executorService);
        this.pointer = filterForm.getPointer().toScoreDoc();
        this.pageSize = filterForm.getPageSize();
    }

    @Override
    protected IndexSearchResult<T> performSearch(final IndexSearcher searcher, final MultiReader reader,
                                                 final Query query, final Sort sort,
                                                 final AbstractDocumentBuilder<T> documentCreator)
            throws IOException {
        final TopDocs docs = getNextPage(searcher, query, pointer, pageSize, sort);
        final ScoreDoc[] hits = docs.scoreDocs;
        final List<T> entries = new ArrayList<>(pageSize);
        for (int i = 0; i < hits.length; i++) {
            final T entry = documentCreator.buildEntry(searcher, hits[i].doc);
            entries.add(entry);
        }

        final ScoreDoc lastEntry = hits.length == 0 ? null : hits[hits.length-1];
        return new IndexSearchResult<>(entries, false, docs.totalHits, lastEntry);
    }

    private TopDocs getNextPage(final IndexSearcher searcher, final Query query, final ScoreDoc pointer,
                                final Integer pageSize, final Sort sort) throws IOException {
        final TopDocs docs;
        final Query constantQuery = new ConstantScoreQuery(query);
        if (sort == null) {
            docs = searcher.searchAfter(pointer, constantQuery, pageSize);
        } else {

            docs = searcher.searchAfter(pointer, constantQuery, pageSize, sort, false, false);
        }
        return docs;
    }
}
