package com.epam.catgenome.dao.index.indexer;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.dao.index.field.SortedIntPoint;
import com.epam.catgenome.dao.index.field.SortedStringField;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.BytesRef;

/**
 * Created by Mikhail_Miroliubov on 7/27/2017.
 */
public abstract class AbstractDocumentCreator<E extends FeatureIndexEntry> {
    public Document createIndexDocument(E entry, final Long featureFileId) {
        Document document = new Document();
        document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.FEATURE_ID.getFieldName(), entry.getFeatureId()));

        FieldType fieldType = new FieldType();
        fieldType.setOmitNorms(true);
        fieldType.setIndexOptions(IndexOptions.DOCS);
        fieldType.setStored(true);
        fieldType.setTokenized(false);
        fieldType.setDocValuesType(DocValuesType.SORTED);
        fieldType.freeze();
        Field field = new Field(FeatureIndexDao.FeatureIndexFields.CHROMOSOME_ID.getFieldName(), entry.getChromosome() != null ?
                new BytesRef(entry.getChromosome().getId().toString()) : new BytesRef(""), fieldType);
        document.add(field);
        document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.CHROMOSOME_NAME.getFieldName(),
                entry.getChromosome().getName(), true));

        document.add(new SortedIntPoint(FeatureIndexDao.FeatureIndexFields.START_INDEX.getFieldName(), entry.getStartIndex()));
        document.add(new StoredField(FeatureIndexDao.FeatureIndexFields.START_INDEX.getFieldName(), entry.getStartIndex()));
        document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.START_INDEX.getGroupName(),
                new BytesRef(entry.getStartIndex().toString())));

        document.add(new SortedIntPoint(FeatureIndexDao.FeatureIndexFields.END_INDEX.getFieldName(), entry.getEndIndex()));
        document.add(new StoredField(FeatureIndexDao.FeatureIndexFields.END_INDEX.getFieldName(), entry.getEndIndex()));
        document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.END_INDEX.getGroupName(),
                new BytesRef(entry.getStartIndex().toString())));

        document.add(new StringField(FeatureIndexDao.FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                entry.getFeatureType() != null ? entry.getFeatureType().getFileValue() : "", Field.Store.YES));
        document.add(new StringField(FeatureIndexDao.FeatureIndexFields.FILE_ID.getFieldName(), featureFileId.toString(),
                Field.Store.YES));

        document.add(new StringField(FeatureIndexDao.FeatureIndexFields.FEATURE_NAME.getFieldName(),
                entry.getFeatureName() != null ? entry.getFeatureName().toLowerCase() : "", Field.Store.YES));
        document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.FEATURE_NAME.getFieldName(),
                new BytesRef(entry.getFeatureName() != null ? entry.getFeatureName() : "")));

        document.add(new SortedSetDocValuesFacetField(FeatureIndexDao.FeatureIndexFields.CHR_ID.getFieldName(),
                entry.getChromosome().getId().toString()));

        document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.UID.getFieldName(), entry.getUuid().toString()));
        document.add(new SortedSetDocValuesFacetField(FeatureIndexDao.FeatureIndexFields.F_UID.getFieldName(),
                entry.getUuid().toString()));

        addExtraFeatureFields(document, entry);

        return document;
    }

    abstract void addExtraFeatureFields(Document document, E entry);

    public static AbstractDocumentCreator createDocumentCreator(FeatureIndexEntry entry) {
        if (entry instanceof VcfIndexEntry) {
            return new VcfDocumentCreator();
        } else {
            return new DefaultDocumentCreator();
        }
    }
}
