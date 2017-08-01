package com.epam.catgenome.dao.index.indexer;

import com.epam.catgenome.entity.index.FeatureIndexEntry;
import org.apache.lucene.document.Document;

/**
 * Default implementation of AbstractDocumentBuilder. No specific actions is taken for specific entries fields
 */
public class DefaultDocumentBuilder extends AbstractDocumentBuilder {
    @Override
    protected FeatureIndexEntry createSpecificEntry(Document doc) {
        return new FeatureIndexEntry();
    }

    @Override
    protected void addExtraFeatureFields(Document document, FeatureIndexEntry entry) {
        // No-op
    }
}
