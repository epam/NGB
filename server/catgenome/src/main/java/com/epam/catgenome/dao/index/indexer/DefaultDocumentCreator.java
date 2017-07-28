package com.epam.catgenome.dao.index.indexer;

import com.epam.catgenome.entity.index.FeatureIndexEntry;
import org.apache.lucene.document.Document;

/**
 * Created by Mikhail_Miroliubov on 7/27/2017.
 */
public class DefaultDocumentCreator extends AbstractDocumentBuilder
{
    @Override
    FeatureIndexEntry createSpecificEntry(Document doc) {
        return new FeatureIndexEntry();
    }

    @Override
    void addExtraFeatureFields(Document document, FeatureIndexEntry entry) {
        // No-op
    }
}
