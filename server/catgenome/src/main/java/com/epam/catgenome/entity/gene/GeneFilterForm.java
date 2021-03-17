/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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

package com.epam.catgenome.entity.gene;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.dao.index.field.IndexSortField;
import com.epam.catgenome.entity.AbstractFilterForm;
import com.epam.catgenome.entity.index.FeatureType;
import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@Getter
@Setter
public class GeneFilterForm extends AbstractFilterForm {
    private Integer startIndex;
    private Integer endIndex;
    private List<String> chromosomes;
    private String featureId;
    private List<FeatureType> featureTypes;

    /**
     * Create sorting from orderBy params
     *
     * @return sorting
     */
    public Sort createSorting() {
        if (isEmpty(orderBy)) {
            return new Sort();
        }
        final List<SortField> sortFields = new ArrayList<>();
        for (OrderBy order : orderBy) {
            final IndexSortField indexSortField = IndexSortField.getByName(order.getField());
            if (indexSortField != null) {
                sortFields.add(new SortField(indexSortField.getFieldName(), indexSortField.getType(), order.isDesc()));
            }
        }
        return sortFields.isEmpty() ? new Sort() : new Sort(sortFields.toArray(new SortField[sortFields.size()]));
    }

    /**
     * Creates a {@code BooleanQuery} for loading all types of features ()
     *
     * @return a {@code BooleanQuery} to a lucene index without filtering by a{@code FeatureType}
     */
    public BooleanQuery computeQuery() {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        addFeatureNameFilter(mainBuilder);
        addFeatureTypesFilter(mainBuilder);
        addChromosomeFilter(mainBuilder);
        addPositionFilter(mainBuilder);
        return mainBuilder.build();
    }

    /**
     * Filter variations by feature name
     *
     * @param builder
     */
    private void addFeatureNameFilter(final BooleanQuery.Builder builder) {
        final BooleanQuery.Builder prefixQueryBuilder = new BooleanQuery.Builder()
                .add(new PrefixQuery(new Term(FeatureIndexDao.FeatureIndexFields.FEATURE_ID.getFieldName(),
                featureId.toLowerCase())), BooleanClause.Occur.SHOULD)
                .add(new PrefixQuery(new Term(FeatureIndexDao.FeatureIndexFields.FEATURE_NAME.getFieldName(),
                featureId.toLowerCase())), BooleanClause.Occur.SHOULD);

        builder.add(prefixQueryBuilder.build(), BooleanClause.Occur.MUST);
    }

    /**
     * Filter variations by feature types
     *
     * @param builder
     */
    private void addFeatureTypesFilter(final BooleanQuery.Builder builder) {
        if (isNotEmpty(featureTypes)) {
            final BooleanQuery.Builder featureTypeBuilder = new BooleanQuery.Builder();
            for (FeatureType type : featureTypes) {
                featureTypeBuilder.add(new TermQuery(new Term(
                                FeatureIndexDao.FeatureIndexFields.FEATURE_TYPE.getFieldName(), type.getFileValue())),
                        BooleanClause.Occur.SHOULD);
            }
            builder.add(featureTypeBuilder.build(), BooleanClause.Occur.MUST);
        }
    }

    /**
     * Filter variations by specified chromosomes
     *
     * @param builder
     */
    private void addChromosomeFilter(final BooleanQuery.Builder builder) {
        if (isNotEmpty(chromosomes)) {
            final BooleanQuery.Builder chromosomeBuilder = new BooleanQuery.Builder();
            for (String chromosome : chromosomes) {
                chromosomeBuilder.add(new TermQuery(new Term(
                                FeatureIndexDao.FeatureIndexFields.CHROMOSOME_NAME.getFieldName(), chromosome)),
                        BooleanClause.Occur.SHOULD);
            }
            builder.add(chromosomeBuilder.build(), BooleanClause.Occur.MUST);
        }
    }

    /**
     * Filter variations by positions, using start and end indexes
     *
     * @param builder
     */
    private void addPositionFilter(final BooleanQuery.Builder builder) {
        if (startIndex != null && endIndex != null) {
            final BooleanQuery.Builder positionBuilder = new BooleanQuery.Builder()
                    .add(IntPoint.newRangeQuery(FeatureIndexDao.FeatureIndexFields.START_INDEX.getFieldName(),
                    startIndex, endIndex), BooleanClause.Occur.MUST)
                    .add(IntPoint.newRangeQuery(FeatureIndexDao.FeatureIndexFields.END_INDEX.getFieldName(),
                    startIndex, endIndex), BooleanClause.Occur.MUST);

            builder.add(positionBuilder.build(), BooleanClause.Occur.MUST);
        } else {
            if (startIndex != null) {
                builder.add(IntPoint.newRangeQuery(FeatureIndexDao.FeatureIndexFields.START_INDEX.getFieldName(),
                        startIndex, Integer.MAX_VALUE),
                        BooleanClause.Occur.MUST);
            } else if (endIndex != null) {
                builder.add(IntPoint.newRangeQuery(FeatureIndexDao.FeatureIndexFields.END_INDEX.getFieldName(),
                        Integer.MIN_VALUE, endIndex),
                        BooleanClause.Occur.MUST);
            }
        }
    }

    @Override
    public boolean filterEmpty() {
        return false;
    }

    @Override
    public List<String> getInfoFields() {
        return emptyList();
    }

    @Override
    public Integer getPage() {
        return 1;
    }
}
