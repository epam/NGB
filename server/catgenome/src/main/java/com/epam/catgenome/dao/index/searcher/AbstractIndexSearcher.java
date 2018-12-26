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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.dao.index.field.IndexSortField;
import com.epam.catgenome.dao.index.indexer.AbstractDocumentBuilder;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.index.VcfIndexEntry;
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

public abstract class AbstractIndexSearcher implements LuceneIndexSearcher<VcfIndexEntry> {
    private FeatureIndexDao featureIndexDao;
    private FileManager fileManager;
    private VcfManager vcfManager;
    private VcfFilterForm vcfFilterForm;
    private ExecutorService executorService;

    public AbstractIndexSearcher(FeatureIndexDao featureIndexDao, FileManager fileManager,
            VcfManager vcfManager, VcfFilterForm filterForm, ExecutorService executorService) {
        this.featureIndexDao = featureIndexDao;
        this.fileManager = fileManager;
        this.vcfManager = vcfManager;
        this.vcfFilterForm = filterForm;
        this.executorService = executorService;
    }

    public static LuceneIndexSearcher<VcfIndexEntry> getIndexSearcher(VcfFilterForm filterForm,
            FeatureIndexDao featureIndexDao, FileManager fileManager, VcfManager vcfManager,
            ExecutorService executorService) {
        if (filterForm.getPointer() != null) {
            return new NextPageSearcher(featureIndexDao, fileManager, vcfManager, filterForm, executorService);
        } else {
            return new PagingSearcher(featureIndexDao, fileManager, vcfManager, filterForm, executorService);
        }
    }

    @Override
    public IndexSearchResult<VcfIndexEntry> getSearchResults(List<? extends FeatureFile> files, Query query)
            throws IOException {
        if (CollectionUtils.isEmpty(files)) {
            return new IndexSearchResult<>(Collections.emptyList(), false, 0);
        }

        SimpleFSDirectory[] indexes = fileManager.getIndexesForFiles(files);
        long indexSize = featureIndexDao.getTotalIndexSize(indexes);
        if (indexSize > featureIndexDao.getLuceneIndexMaxSizeForGrouping() && vcfFilterForm.filterEmpty()) {
            throw new IllegalArgumentException("Variations filter shall be specified");
        }

        try (MultiReader reader = featureIndexDao.openMultiReader(indexes)) {
            if (reader.numDocs() == 0) {
                return new IndexSearchResult<>(Collections.emptyList(), false, 0);
            }
            IndexSearcher searcher = new IndexSearcher(reader, executorService);
            AbstractDocumentBuilder<VcfIndexEntry> documentCreator = AbstractDocumentBuilder
                    .createDocumentCreator(files.get(0).getFormat(), vcfFilterForm.getInfoFields());
            Sort sort = createSorting(vcfFilterForm.getOrderBy(), files);
            IndexSearchResult<VcfIndexEntry> searchResults = performSearch(searcher, reader, query,
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

    protected TopDocs performSearch(IndexSearcher searcher, Query query, MultiReader reader, int numDocs,
            Sort sort) throws IOException {
        return featureIndexDao.performSearch(searcher, query, reader, numDocs, sort);
    }

    protected abstract IndexSearchResult<VcfIndexEntry> performSearch(IndexSearcher searcher,
            MultiReader reader, Query query, Sort sort, AbstractDocumentBuilder<VcfIndexEntry> documentCreator)
            throws IOException;

    private Sort createSorting(List<VcfFilterForm.OrderBy> orderBy,
            List<? extends FeatureFile> files) throws IOException {
        if (CollectionUtils.isNotEmpty(orderBy)) {
            ArrayList<SortField> sortFields = new ArrayList<>();
            for (VcfFilterForm.OrderBy o : orderBy) {
                IndexSortField sortField = IndexSortField.getByName(o.getField());
                if (sortField == null) {
                    VcfFilterInfo info = vcfManager.getFiltersInfo(
                            files.stream().map(BaseEntity::getId).collect(Collectors.toList()));

                    InfoItem infoItem = info.getInfoItemMap().get(o.getField());
                    Assert.notNull(infoItem, "Unknown sort field: " + o.getField());

                    SortField.Type type = determineSortType(infoItem);
                    SortField sf =
                            new SortedSetSortField(infoItem.getName().toLowerCase(), o.isDesc());

                    setMissingValuesOrder(sf, type, o.isDesc());

                    sortFields.add(sf);
                } else {
                    SortField sf;
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

    private void setMissingValuesOrder(SortField sf, SortField.Type type, boolean desc) {
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

    private SortField.Type determineSortType(InfoItem item) {
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
