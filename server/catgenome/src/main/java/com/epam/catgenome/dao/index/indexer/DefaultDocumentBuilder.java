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
