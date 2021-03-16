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
import com.epam.catgenome.dao.index.field.IndexSortField;
import com.epam.catgenome.dao.index.indexer.AbstractDocumentBuilder;
import com.epam.catgenome.entity.AbstractFilterForm;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.vcf.InfoItem;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedSetSortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.SimpleFSDirectory;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public abstract class AbstractIndexSearcher<T extends FeatureIndexEntry, R extends AbstractFilterForm>
        implements LuceneIndexSearcher<T> {
    private FeatureIndexDao featureIndexDao;
    private FileManager fileManager;
    private VcfManager vcfManager;
    private R filterForm;
    private ExecutorService executorService;

    public AbstractIndexSearcher(final FeatureIndexDao featureIndexDao, final FileManager fileManager,
                                 final VcfManager vcfManager, final R filterForm,
                                 final ExecutorService executorService) {
        this.featureIndexDao = featureIndexDao;
        this.fileManager = fileManager;
        this.vcfManager = vcfManager;
        this.filterForm = filterForm;
        this.executorService = executorService;
    }

    public static <T extends FeatureIndexEntry, R extends AbstractFilterForm> LuceneIndexSearcher<T> getIndexSearcher(
            final R filterForm, final FeatureIndexDao featureIndexDao, final FileManager fileManager,
            final VcfManager vcfManager, final ExecutorService executorService) {

        if (filterForm.getPointer() != null) {
            return new NextPageSearcher<T, R>(featureIndexDao, fileManager, vcfManager, filterForm, executorService);
        } else {
            return new PagingSearcher<T, R>(featureIndexDao, fileManager, vcfManager, filterForm, executorService);
        }
    }

    @Override
    public IndexSearchResult<T> getSearchResults(final List<? extends FeatureFile> files, final Query query)
            throws IOException {
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
                    .createDocumentCreator(files.get(0).getFormat(), filterForm.getInfoFields());
            final Sort sort = createSorting(filterForm.getOrderBy(), files);
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

    private Sort createSorting(final List<VcfFilterForm.OrderBy> orderBy,
                               final List<? extends FeatureFile> files) throws IOException {
        if (CollectionUtils.isNotEmpty(orderBy)) {
            final ArrayList<SortField> sortFields = new ArrayList<>();
            for (VcfFilterForm.OrderBy o : orderBy) {
                final IndexSortField sortField = IndexSortField.getByName(o.getField());
                if (sortField == null) {
                    final VcfFilterInfo info = vcfManager.getFiltersInfo(
                            files.stream().map(BaseEntity::getId).collect(Collectors.toList()));

                    final InfoItem infoItem = info.getInfoItemMap().get(o.getField());
                    Assert.notNull(infoItem, "Unknown sort field: " + o.getField());

                    final SortField.Type type = determineSortType(infoItem);
                    final SortField sf =
                            new SortedSetSortField(infoItem.getName().toLowerCase(), o.isDesc());

                    setMissingValuesOrder(sf, type, o.isDesc());

                    sortFields.add(sf);
                } else {
                    final SortField sf;
                    if (sortField.getType() == SortField.Type.STRING) {
                        sf = new SortedSetSortField(sortField.getField().getFieldName(), o.isDesc());
                    } else {
                        sf = new SortField(sortField.getField().getFieldName(), sortField.getType(),
                                o.isDesc());
                    }
                    setMissingValuesOrder(sf, sortField.getType(), o.isDesc());

                    sortFields.add(sf);
                }
            }

            return new Sort(sortFields.toArray(new SortField[sortFields.size()]));
        }

        return null;
    }

    private void setMissingValuesOrder(final SortField sf, final SortField.Type type, final boolean desc) {
        if (sf instanceof SortedSetSortField) {
            sf.setMissingValue(desc ? SortField.STRING_FIRST : SortField.STRING_LAST);
        } else {
            switch (type) {
                case STRING:
                    sf.setMissingValue(desc ? SortField.STRING_FIRST : SortField.STRING_LAST);
                    break;
                case FLOAT:
                    sf.setMissingValue(Float.MIN_VALUE);
                    break;
                case INT:
                    sf.setMissingValue(Integer.MIN_VALUE);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected sort type: " + type);
            }
        }
    }

    private SortField.Type determineSortType(final InfoItem item) {
        switch (item.getType()) {
            case Integer:
                return SortField.Type.INT;
            case Float:
                return SortField.Type.FLOAT;
            default:
                return SortField.Type.STRING;
        }
    }
}
