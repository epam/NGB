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

public enum IndexSortField {
    CHROMOSOME_NAME(FeatureIndexFields.CHROMOSOME_NAME, SortField.Type.STRING),
    FEATURE_NAME(FeatureIndexFields.FEATURE_NAME, SortField.Type.STRING),
    START_INDEX(FeatureIndexFields.START_INDEX, SortField.Type.INT),
    END_INDEX(FeatureIndexFields.END_INDEX, SortField.Type.INT),
    FILTER(FeatureIndexFields.FAILED_FILTER, SortField.Type.STRING),
    VARIATION_TYPE(FeatureIndexFields.VARIATION_TYPE, SortField.Type.STRING),
    QUALITY(FeatureIndexFields.QUALITY, SortField.Type.STRING),
    GENE_ID(FeatureIndexFields.GENE_IDS, SortField.Type.STRING),
    GENE_NAME(FeatureIndexFields.GENE_NAMES, SortField.Type.STRING);

    private static Map<String, IndexSortField> fieldMap = new HashMap<>();
    static {
        fieldMap.put(CHROMOSOME_NAME.name(), CHROMOSOME_NAME);
        fieldMap.put(FEATURE_NAME.name(), FEATURE_NAME);
        fieldMap.put(START_INDEX.name(), START_INDEX);
        fieldMap.put(END_INDEX.name(), END_INDEX);
        fieldMap.put(FILTER.name(), FILTER);
        fieldMap.put(QUALITY.name(), QUALITY);
        fieldMap.put(GENE_ID.name(), GENE_ID);
        fieldMap.put(GENE_NAME.name(), GENE_NAME);
        fieldMap.put(VARIATION_TYPE.name(), VARIATION_TYPE);
    }

    private SortField.Type type;
    private FeatureIndexFields field;

    IndexSortField(FeatureIndexFields field, SortField.Type type) {
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

    public static IndexSortField getByName(String name) {
        return fieldMap.get(name);
    }
}
