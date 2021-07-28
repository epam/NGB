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

package com.epam.catgenome.dao.index.indexer;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.dao.index.field.SortedFloatPoint;
import com.epam.catgenome.dao.index.field.SortedIntPoint;
import com.epam.catgenome.dao.index.field.SortedStringField;
import com.epam.catgenome.entity.gene.GeneHighLevel;
import com.epam.catgenome.entity.index.GeneIndexEntry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.util.BytesRef;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An extension of {@link AbstractDocumentBuilder}, that allows indexing and reading entries of Gene feature
 * indexes
 */
public class GeneDocumentBuilder extends AbstractDocumentBuilder<GeneIndexEntry> {

    private List<String> attributesFields;

    @Override
    protected void addExtraFeatureFields(final Document document, final GeneIndexEntry entry) {
        document.add(new SortedIntPoint(FeatureIndexDao.FeatureIndexFields.FRAME.getFieldName(),
                entry.getFrame()));
        document.add(new StoredField(FeatureIndexDao.FeatureIndexFields.FRAME.getFieldName(),
                entry.getFrame()));
        document.add(new SortedFloatPoint(FeatureIndexDao.FeatureIndexFields.SCORE.getFieldName(),
                entry.getScore()));
        document.add(new StoredField(FeatureIndexDao.FeatureIndexFields.SCORE.getFieldName(),
                entry.getScore()));
        document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.SOURCE.getFieldName(),
                entry.getSource()));
        document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.STRAND.getFieldName(),
                entry.getStrand()));
        MapUtils.emptyIfNull(entry.getAttributes()).forEach((k, v) -> {
            document.add(new SortedStringField(k.toLowerCase(), v));
            document.add(new StoredField(k.toLowerCase(), v));
        });
    }

    @Override
    protected Set<String> getRequiredFields() {
        final Set<String> requiredFields = new HashSet<>();
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.CHROMOSOME_NAME.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.CHROMOSOME_ID.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.FILE_ID.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.FEATURE_NAME.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.FEATURE_TYPE.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.FEATURE_ID.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.START_INDEX.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.END_INDEX.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.SOURCE.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.STRAND.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.SCORE.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.FRAME.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.UID.getFieldName());
        if (!CollectionUtils.isEmpty(attributesFields)) {
            for (String infoField : attributesFields) {
                requiredFields.add(infoField.toLowerCase());
            }
        }
        return requiredFields;
    }

    @Override
    protected GeneIndexEntry createSpecificEntry(final Document doc) {
        final GeneIndexEntry geneIndexEntry = new GeneIndexEntry();

        geneIndexEntry.setStrand(extractField(doc, FeatureIndexDao.FeatureIndexFields.STRAND));
        geneIndexEntry.setSource(extractField(doc, FeatureIndexDao.FeatureIndexFields.SOURCE));
        geneIndexEntry.setScore(
                doc.getField(FeatureIndexDao.FeatureIndexFields.SCORE.getFieldName())
                        .numericValue().floatValue());
        geneIndexEntry.setFrame(
                doc.getField(FeatureIndexDao.FeatureIndexFields.FRAME.getFieldName())
                        .numericValue().intValue());

        // NOTE: hack to be able to have real feature type name as string
        // if FeatureType == GENERIC_GENE_FEATURE here we set the real one from index
        geneIndexEntry.setFeature(doc.get(FeatureIndexDao.FeatureIndexFields.FEATURE_TYPE.getFieldName()));

        if (CollectionUtils.isNotEmpty(attributesFields)) {
            geneIndexEntry.setAttributes(new HashMap<>());
            for (String infoField : attributesFields) {
                if (doc.getBinaryValue(infoField.toLowerCase()) != null) {
                    geneIndexEntry.getAttributes().put(infoField,
                            doc.getBinaryValue(infoField.toLowerCase()).utf8ToString());
                } else {
                    geneIndexEntry.getAttributes().put(infoField, doc.get(infoField.toLowerCase()));
                }
            }
        }
        return geneIndexEntry;
    }

    public Document copyGeneDocument(final GeneHighLevel geneContent, final Document oldDocument,
                                     final String documentUid, final String featureId, final String featureName) {
        final Document newDocument = copyDocument(oldDocument);

        newDocument.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.UID.getFieldName(), documentUid));
        newDocument.add(new SortedSetDocValuesFacetField(FeatureIndexDao.FeatureIndexFields.F_UID.getFieldName(),
                documentUid));

        final String featureType = geneContent.getFeature();
        newDocument.add(new StringField(FeatureIndexDao.FeatureIndexFields.FEATURE_TYPE.getFieldName(), featureType,
                Field.Store.YES));
        newDocument.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                featureType));

        newDocument.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.FEATURE_ID.getFieldName(), featureId));
        newDocument.add(new StringField(FeatureIndexDao.FeatureIndexFields.FEATURE_NAME.getFieldName(), featureName,
                Field.Store.YES));
        newDocument.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.FEATURE_NAME.getFieldName(),
                new BytesRef(featureName)));

        final Integer frame = geneContent.getFrame();
        newDocument.add(new SortedIntPoint(FeatureIndexDao.FeatureIndexFields.FRAME.getFieldName(), frame));
        newDocument.add(new StoredField(FeatureIndexDao.FeatureIndexFields.FRAME.getFieldName(), frame));

        final Float score = geneContent.getScore();
        newDocument.add(new SortedFloatPoint(FeatureIndexDao.FeatureIndexFields.SCORE.getFieldName(), score));
        newDocument.add(new StoredField(FeatureIndexDao.FeatureIndexFields.SCORE.getFieldName(), score));

        newDocument.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.SOURCE.getFieldName(),
                geneContent.getSource()));
        newDocument.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.STRAND.getFieldName(),
                geneContent.getStrand().getFileValue()));

        MapUtils.emptyIfNull(geneContent.getAttributes()).forEach((k, v) -> {
            newDocument.add(new SortedStringField(k.toLowerCase(), v));
            newDocument.add(new StoredField(k.toLowerCase(), v));
        });

        return newDocument;
    }

    private String extractField(final Document doc, final FeatureIndexDao.FeatureIndexFields fieldName) {
        final BytesRef field = doc.getBinaryValue(fieldName.getFieldName());
        if (field == null) {
            return doc.get(fieldName.getFieldName());
        } else {
            return field.utf8ToString();
        }
    }

    public void setGeneAttributes(List<String> attributes) {
        this.attributesFields = attributes;
    }
}
