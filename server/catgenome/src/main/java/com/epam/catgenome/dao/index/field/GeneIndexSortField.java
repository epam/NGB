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

package com.epam.catgenome.dao.index.field;

import com.epam.catgenome.dao.index.FeatureIndexDao.FeatureIndexFields;
import org.apache.lucene.search.SortField;

import java.util.HashMap;
import java.util.Map;

public enum GeneIndexSortField {
    CHROMOSOME_NAME(FeatureIndexFields.CHROMOSOME_NAME, SortField.Type.STRING),
    FEATURE_NAME(FeatureIndexFields.FEATURE_NAME, SortField.Type.STRING),
    FEATURE_ID(FeatureIndexFields.GENE_IDS, SortField.Type.STRING),
    FEATURE_TYPE(FeatureIndexFields.FEATURE_TYPE, SortField.Type.STRING),
    START_INDEX(FeatureIndexFields.START_INDEX, SortField.Type.INT),
    END_INDEX(FeatureIndexFields.END_INDEX, SortField.Type.INT);

    private static Map<String, GeneIndexSortField> fieldMap = new HashMap<>();
    static {
        fieldMap.put(CHROMOSOME_NAME.name(), CHROMOSOME_NAME);
        fieldMap.put(FEATURE_NAME.name(), FEATURE_NAME);
        fieldMap.put(FEATURE_TYPE.name(), FEATURE_TYPE);
        fieldMap.put(START_INDEX.name(), START_INDEX);
        fieldMap.put(END_INDEX.name(), END_INDEX);
        fieldMap.put(FEATURE_ID.name(), FEATURE_ID);
    }

    private SortField.Type type;
    private FeatureIndexFields field;

    GeneIndexSortField(FeatureIndexFields field, SortField.Type type) {
        this.type = type;
        this.field = field;
    }

    public SortField.Type getType() {
        return type;
    }

    public FeatureIndexFields getField() {
        return field;
    }

    public String getFieldName() {
        return field.getFieldName();
    }

    public static GeneIndexSortField getByName(String name) {
        return fieldMap.get(name);
    }
}
