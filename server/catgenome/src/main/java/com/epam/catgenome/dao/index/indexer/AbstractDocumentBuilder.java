package com.epam.catgenome.dao.index.indexer;

import com.epam.catgenome.dao.index.FeatureIndexDao.FeatureIndexFields;
import com.epam.catgenome.dao.index.field.SortedIntPoint;
import com.epam.catgenome.dao.index.field.SortedStringField;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.BytesRef;

import java.util.List;

/**
 * Created by Mikhail_Miroliubov on 7/27/2017.
 */
public abstract class AbstractDocumentBuilder<E extends FeatureIndexEntry> {
    public Document createIndexDocument(E entry, final Long featureFileId) {
        Document document = new Document();
        document.add(new SortedStringField(FeatureIndexFields.FEATURE_ID.getFieldName(), entry.getFeatureId()));

        FieldType fieldType = new FieldType();
        fieldType.setOmitNorms(true);
        fieldType.setIndexOptions(IndexOptions.DOCS);
        fieldType.setStored(true);
        fieldType.setTokenized(false);
        fieldType.setDocValuesType(DocValuesType.SORTED);
        fieldType.freeze();
        Field field = new Field(FeatureIndexFields.CHROMOSOME_ID.getFieldName(), entry.getChromosome() != null ?
                new BytesRef(entry.getChromosome().getId().toString()) : new BytesRef(""), fieldType);
        document.add(field);
        document.add(new SortedStringField(FeatureIndexFields.CHROMOSOME_NAME.getFieldName(),
                entry.getChromosome().getName(), true));

        document.add(new SortedIntPoint(FeatureIndexFields.START_INDEX.getFieldName(), entry.getStartIndex()));
        document.add(new StoredField(FeatureIndexFields.START_INDEX.getFieldName(), entry.getStartIndex()));
        document.add(new SortedDocValuesField(FeatureIndexFields.START_INDEX.getGroupName(),
                new BytesRef(entry.getStartIndex().toString())));

        document.add(new SortedIntPoint(FeatureIndexFields.END_INDEX.getFieldName(), entry.getEndIndex()));
        document.add(new StoredField(FeatureIndexFields.END_INDEX.getFieldName(), entry.getEndIndex()));
        document.add(new SortedDocValuesField(FeatureIndexFields.END_INDEX.getGroupName(),
                new BytesRef(entry.getStartIndex().toString())));

        document.add(new StringField(FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                entry.getFeatureType() != null ? entry.getFeatureType().getFileValue() : "", Field.Store.YES));
        document.add(new StringField(FeatureIndexFields.FILE_ID.getFieldName(), featureFileId.toString(),
                Field.Store.YES));

        document.add(new StringField(FeatureIndexFields.FEATURE_NAME.getFieldName(),
                entry.getFeatureName() != null ? entry.getFeatureName().toLowerCase() : "", Field.Store.YES));
        document.add(new SortedDocValuesField(FeatureIndexFields.FEATURE_NAME.getFieldName(),
                new BytesRef(entry.getFeatureName() != null ? entry.getFeatureName() : "")));

        document.add(new SortedSetDocValuesFacetField(FeatureIndexFields.CHR_ID.getFieldName(),
                entry.getChromosome().getId().toString()));

        document.add(new SortedStringField(FeatureIndexFields.UID.getFieldName(), entry.getUuid().toString()));
        document.add(new SortedSetDocValuesFacetField(FeatureIndexFields.F_UID.getFieldName(),
                entry.getUuid().toString()));

        addExtraFeatureFields(document, entry);

        return document;
    }

    public E createEntryFromDocument(Document doc) {
        FeatureType featureType = FeatureType.forValue(doc.get(FeatureIndexFields.FEATURE_TYPE.getFieldName()));
        E entry = createSpecificEntry(doc);

        entry.setFeatureType(featureType);
        BytesRef featureIdBytes = doc.getBinaryValue(FeatureIndexFields.FEATURE_ID.getFieldName());
        if (featureIdBytes == null) {
            entry.setFeatureId(doc.get(FeatureIndexFields.FEATURE_ID.getFieldName()));
        } else {
            entry.setFeatureId(featureIdBytes.utf8ToString());
        }

        entry.setStartIndex(doc.getField(FeatureIndexFields.START_INDEX.getFieldName()).numericValue().intValue());
        entry.setEndIndex(doc.getField(FeatureIndexFields.END_INDEX.getFieldName()).numericValue().intValue());
        entry.setFeatureFileId(Long.parseLong(doc.get(FeatureIndexFields.FILE_ID.getFieldName())));
        entry.setFeatureName(doc.get(FeatureIndexFields.FEATURE_NAME.getFieldName()));

        String chromosomeId = doc.getBinaryValue(FeatureIndexFields.CHROMOSOME_ID.getFieldName()).utf8ToString();
        if (!chromosomeId.isEmpty()) {
            entry.setChromosome(new Chromosome(Long.parseLong(chromosomeId)));
            entry.getChromosome().setName(doc.getBinaryValue(FeatureIndexFields.CHROMOSOME_NAME.getFieldName())
                    .utf8ToString());
        }

        return entry;
    }

    public FacetsConfig createFacetsConfig(VcfFilterInfo info) {
        FacetsConfig facetsConfig = new FacetsConfig();
        facetsConfig.setIndexFieldName(FeatureIndexFields.CHR_ID.getFieldName(),
                FeatureIndexFields.FACET_CHR_ID.getFieldName());
        facetsConfig.setIndexFieldName(FeatureIndexFields.F_UID.getFieldName(),
                FeatureIndexFields.FACET_UID.getFieldName());

        return facetsConfig;
    }

    protected abstract E createSpecificEntry(Document doc);

    protected abstract void addExtraFeatureFields(Document document, E entry);

    public static AbstractDocumentBuilder createDocumentCreator(FeatureIndexEntry entry) {
        if (entry instanceof VcfIndexEntry) {
            return new BigVcfDocumentBuilder();
        } else {
            return new DefaultDocumentCreator();
        }
    }

    public static AbstractDocumentBuilder createDocumentCreator(BiologicalDataItemFormat format,
                                                                List<String> vcfInfoFields) {
        switch (format) {
            case VCF:
                BigVcfDocumentBuilder creator = new BigVcfDocumentBuilder();
                creator.setVcfInfoFields(vcfInfoFields);
                return creator;
            default:
                return new DefaultDocumentCreator();
        }
    }
}
