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


package com.epam.catgenome.dao.index.indexer;

import java.util.HashSet;
import java.util.Set;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import org.apache.lucene.document.Document;

/**
 * Default implementation of AbstractDocumentBuilder. No specific actions is taken for specific entries fields
 */
public class DefaultDocumentBuilder extends AbstractDocumentBuilder {
    @Override
    protected Set<String> getRequiredFields() {
        Set<String> requiredFields = new HashSet<>();
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.CHROMOSOME_NAME.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.CHROMOSOME_ID.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.FILE_ID.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.SOURCE_FILE.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.FEATURE_ID.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.FEATURE_NAME.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.FEATURE_TYPE.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.START_INDEX.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.END_INDEX.getFieldName());
        return requiredFields;
    }

    @Override protected FeatureIndexEntry createSpecificEntry(Document doc) {
        return new FeatureIndexEntry();
    }

    @Override protected void addExtraFeatureFields(Document document, FeatureIndexEntry entry) {
        // No-op
    }
}
