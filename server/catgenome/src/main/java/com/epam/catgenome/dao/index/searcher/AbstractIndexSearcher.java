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
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.manager.FileManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.SimpleFSDirectory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

public abstract class AbstractIndexSearcher<T extends FeatureIndexEntry, R extends AbstractFilterForm>
        implements LuceneIndexSearcher<T> {
    private FeatureIndexDao featureIndexDao;
    private FileManager fileManager;
    private R filterForm;
    private ExecutorService executorService;

    public AbstractIndexSearcher(final FeatureIndexDao featureIndexDao, final FileManager fileManager,
                                 final R filterForm, final ExecutorService executorService) {
        this.featureIndexDao = featureIndexDao;
        this.fileManager = fileManager;
        this.filterForm = filterForm;
        this.executorService = executorService;
    }

    public static <T extends FeatureIndexEntry, R extends AbstractFilterForm> LuceneIndexSearcher<T> getIndexSearcher(
            final R filterForm, final FeatureIndexDao featureIndexDao, final FileManager fileManager,
            final ExecutorService executorService) {

        if (filterForm.getPointer() != null) {
            return new NextPageSearcher<T, R>(featureIndexDao, fileManager, filterForm, executorService);
        } else {
            return new PagingSearcher<T, R>(featureIndexDao, fileManager, filterForm, executorService);
        }
    }

    @Override
    public IndexSearchResult<T> getSearchResults(final List<? extends FeatureFile> files, final Query query,
                                                 Sort sort) throws IOException {
        if (CollectionUtils.isEmpty(files)) {
            return new IndexSearchResult<>(Collections.emptyList(), false, 0);
        }

        final SimpleFSDirectory[] indexes = fileManager.getIndexesForFiles(files);
        long indexSize = featureIndexDao.getTotalIndexSize(indexes);
        if (indexSize > featureIndexDao.getLuceneIndexMaxSizeForGrouping() && filterForm.filterEmpty()) {
            throw new IllegalArgumentException("Variations filter shall be specified");
        }

        try (MultiReader reader = featureIndexDao.openMultiReader(indexes)) {
            if (reader.numDocs() == 0) {
                return new IndexSearchResult<>(Collections.emptyList(), false, 0);
            }
            final IndexSearcher searcher = new IndexSearcher(reader, executorService);
            final AbstractDocumentBuilder<T> documentCreator = AbstractDocumentBuilder
                    .createDocumentCreator(files.get(0).getFormat(), filterForm.getAdditionalFields());
            final IndexSearchResult<T> searchResults = performSearch(searcher, reader, query,
                    sort, documentCreator);
            //return 0 to prevent random access in UI
            if (indexSize > featureIndexDao.getLuceneIndexMaxSizeForGrouping()) {
                searchResults.setTotalResultsCount(0);
            }
            return searchResults;
        } finally {
            for (SimpleFSDirectory index : indexes) {
                IOUtils.closeQuietly(index);
            }
        }
    }

    protected TopDocs performSearch(final IndexSearcher searcher, final Query query, final MultiReader reader,
                                    int numDocs, final Sort sort) throws IOException {
        return featureIndexDao.performSearch(searcher, query, reader, numDocs, sort);
    }

    protected abstract IndexSearchResult<T> performSearch(IndexSearcher searcher, MultiReader reader, Query query,
                                                          Sort sort, AbstractDocumentBuilder<T> documentCreator)
            throws IOException;
}
