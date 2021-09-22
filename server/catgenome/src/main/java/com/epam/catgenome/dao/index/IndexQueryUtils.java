/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

package com.epam.catgenome.dao.index;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

import java.util.List;

public final class IndexQueryUtils {

    private IndexQueryUtils() {
        // no-op
    }

    public static Query intervalQuery(final String chrId, final int start, final int end,
                                      final List<String> featureTypes) {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        final Query chrQuery = new TermQuery(new Term(FeatureIndexDao.FeatureIndexFields.CHROMOSOME_ID.getFieldName(),
                new BytesRef(chrId)));
        mainBuilder.add(chrQuery, BooleanClause.Occur.MUST);

        if (CollectionUtils.isNotEmpty(featureTypes)) {
            final BooleanQuery.Builder featureTypeBuilder = new BooleanQuery.Builder();
            featureTypes.forEach(featureType -> featureTypeBuilder.add(new TermQuery(
                    new Term(FeatureIndexDao.FeatureIndexFields.FEATURE_TYPE.getFieldName(), featureType)),
                    BooleanClause.Occur.SHOULD));

            mainBuilder.add(featureTypeBuilder.build(), BooleanClause.Occur.MUST);
        }

        final BooleanQuery.Builder rangeBuilder = new BooleanQuery.Builder();
        //start in interval
        final Query startQuery =
                IntPoint.newRangeQuery(FeatureIndexDao.FeatureIndexFields.START_INDEX.getFieldName(), start, end);
        rangeBuilder.add(startQuery, BooleanClause.Occur.SHOULD);
        //end in interval
        final Query endQuery =
                IntPoint.newRangeQuery(FeatureIndexDao.FeatureIndexFields.END_INDEX.getFieldName(), start, end);
        rangeBuilder.add(endQuery, BooleanClause.Occur.SHOULD);

        //feature lasts along all the interval (start < range and end > range)
        final BooleanQuery.Builder spanQueryBuilder = new BooleanQuery.Builder();
        final Query startExtQuery =
                IntPoint.newRangeQuery(FeatureIndexDao.FeatureIndexFields.START_INDEX.getFieldName(),
                        0, start - 1);
        spanQueryBuilder.add(startExtQuery, BooleanClause.Occur.MUST);

        final Query endExtQuery =
                IntPoint.newRangeQuery(FeatureIndexDao.FeatureIndexFields.END_INDEX.getFieldName(),
                        end + 1, Integer.MAX_VALUE);
        spanQueryBuilder.add(endExtQuery, BooleanClause.Occur.MUST);
        rangeBuilder.add(spanQueryBuilder.build(), BooleanClause.Occur.SHOULD);

        mainBuilder.add(rangeBuilder.build(), BooleanClause.Occur.MUST);

        return mainBuilder.build();
    }
}
