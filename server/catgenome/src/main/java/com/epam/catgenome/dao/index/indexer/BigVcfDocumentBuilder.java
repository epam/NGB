package com.epam.catgenome.dao.index.indexer;

import com.epam.catgenome.dao.index.FeatureIndexDao.FeatureIndexFields;
import com.epam.catgenome.dao.index.field.SortedIntPoint;
import com.epam.catgenome.dao.index.field.SortedStringField;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * An extension of {@link AbstractDocumentBuilder}, that allows indexing and reading entries of large VCF feature
 * indexes
 */
public class BigVcfDocumentBuilder extends AbstractDocumentBuilder<VcfIndexEntry> {
    private static final Pattern VIEW_FIELD_PATTERN = Pattern.compile("_.*_v$");
    private static final Logger LOGGER = LoggerFactory.getLogger(BigVcfDocumentBuilder.class);
    private static final int MAX_FACET_LABEL_LEGTH = 8190;

    private List<String> vcfInfoFields;

    @Override public FacetsConfig createFacetsConfig(VcfFilterInfo info) {
        FacetsConfig config = super.createFacetsConfig(info);

        config.setIndexFieldName(FeatureIndexFields.CHROMOSOME_NAME.getFieldName(),
                FeatureIndexFields.CHROMOSOME_NAME.getFacetName());
        config.setIndexFieldName(FeatureIndexFields.VARIATION_TYPE.getFieldName(),
                FeatureIndexFields.VARIATION_TYPE.getFacetName());
        config.setMultiValued(FeatureIndexFields.VARIATION_TYPE.getFieldName(), true);

        config.setIndexFieldName(FeatureIndexFields.FAILED_FILTER.getFieldName(),
                FeatureIndexFields.FAILED_FILTER.getFacetName());
        config.setMultiValued(FeatureIndexFields.FAILED_FILTER.getFieldName(), true);

        config.setIndexFieldName(FeatureIndexFields.GENE_IDS.getFieldName(),
                FeatureIndexFields.GENE_IDS.getFacetName());
        config.setIndexFieldName(FeatureIndexFields.GENE_NAMES.getFieldName(),
                FeatureIndexFields.GENE_NAMES.getFacetName());
        config.setIndexFieldName(FeatureIndexFields.QUALITY.getFieldName(),
                FeatureIndexFields.QUALITY.getFacetName());
        config.setIndexFieldName(FeatureIndexFields.IS_EXON.getFieldName(),
                FeatureIndexFields.IS_EXON.getFacetName());

        info.getInfoItems().forEach(i -> config.setIndexFieldName(i.getName().toLowerCase(),
                FeatureIndexFields.getFacetName(i.getName().toLowerCase())));

        return config;
    }

    /**
     * Creates a Lucene {@link Document} from specified {@link VcfIndexEntry}
     *
     * @param entry         an entry to index
     * @param featureFileId an ID of {@link com.epam.catgenome.entity.FeatureFile}, to which feature belongs
     * @return a Lucene {@link Document}
     */
    @Override public Document buildDocument(VcfIndexEntry entry, final Long featureFileId) {
        Document document = new Document();
        document.add(
                new StringField(FeatureIndexFields.FEATURE_ID.getFieldName(), entry.getFeatureId(),
                        Field.Store.YES));

        FieldType fieldType = new FieldType();
        fieldType.setOmitNorms(true);
        fieldType.setIndexOptions(IndexOptions.DOCS);
        fieldType.setStored(true);
        fieldType.setTokenized(false);
        fieldType.setDocValuesType(DocValuesType.SORTED);
        fieldType.freeze();
        Field field = new Field(FeatureIndexFields.CHROMOSOME_ID.getFieldName(),
                entry.getChromosome() != null ?
                        new BytesRef(entry.getChromosome().getId().toString()) :
                        new BytesRef(""), fieldType);
        document.add(field);

        document.add(new SortedSetDocValuesField(FeatureIndexFields.CHROMOSOME_NAME.getFieldName(),
                new BytesRef(entry.getChromosome().getName())));
        document.add(
                new SortedSetDocValuesFacetField(FeatureIndexFields.CHROMOSOME_NAME.getFieldName(),
                        entry.getChromosome().getName()));
        document.add(new StringField(FeatureIndexFields.CHROMOSOME_NAME.getFieldName(),
                new BytesRef(entry.getChromosome().getName()),
                Field.Store.YES)); // TODO: change to string?

        document.add(new SortedIntPoint(FeatureIndexFields.START_INDEX.getFieldName(),
                entry.getStartIndex()));
        document.add(new StoredField(FeatureIndexFields.START_INDEX.getFieldName(),
                entry.getStartIndex()));
        document.add(new SortedDocValuesField(FeatureIndexFields.START_INDEX.getGroupName(),
                new BytesRef(entry.getStartIndex().toString())));

        document.add(new SortedIntPoint(FeatureIndexFields.END_INDEX.getFieldName(),
                entry.getEndIndex()));
        document.add(
                new StoredField(FeatureIndexFields.END_INDEX.getFieldName(), entry.getEndIndex()));
        document.add(new SortedDocValuesField(FeatureIndexFields.END_INDEX.getGroupName(),
                new BytesRef(entry.getStartIndex().toString())));

        document.add(new StringField(FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                entry.getFeatureType() != null ? entry.getFeatureType().getFileValue() : "",
                Field.Store.YES));
        document.add(
                new StringField(FeatureIndexFields.FILE_ID.getFieldName(), featureFileId.toString(),
                        Field.Store.YES));

        document.add(new StringField(FeatureIndexFields.FEATURE_NAME.getFieldName(),
                entry.getFeatureName() != null ? entry.getFeatureName().toLowerCase() : "",
                Field.Store.YES));
        document.add(new SortedDocValuesField(FeatureIndexFields.FEATURE_NAME.getFieldName(),
                new BytesRef(entry.getFeatureName() != null ? entry.getFeatureName() : "")));

        document.add(new SortedSetDocValuesFacetField(FeatureIndexFields.CHR_ID.getFieldName(),
                entry.getChromosome().getId().toString()));

        document.add(new SortedStringField(FeatureIndexFields.UID.getFieldName(),
                entry.getUuid().toString()));
        document.add(new SortedSetDocValuesFacetField(FeatureIndexFields.F_UID.getFieldName(),
                entry.getUuid().toString()));

        addExtraFeatureFields(document, entry);

        return document;
    }

    @Override protected VcfIndexEntry createSpecificEntry(Document doc) {
        VcfIndexEntry vcfIndexEntry = new VcfIndexEntry();
        vcfIndexEntry.setGene(doc.get(FeatureIndexFields.GENE_ID.getFieldName()));

        BytesRef bytes = doc.getBinaryValue(FeatureIndexFields.GENE_IDS.getFieldName());
        if (bytes != null) {
            vcfIndexEntry.setGeneIds(bytes.utf8ToString());
        }

        vcfIndexEntry.setGeneName(doc.get(FeatureIndexFields.GENE_NAME.getFieldName()));

        bytes = doc.getBinaryValue(FeatureIndexFields.GENE_NAMES.getFieldName());
        if (bytes != null) {
            vcfIndexEntry.setGeneNames(bytes.utf8ToString());
        }

        vcfIndexEntry.setInfo(new HashMap<>());

        String isExonStr = doc.get(FeatureIndexFields.IS_EXON
                .getFieldName()); //TODO: remove, in future only binary
        // value will remain
        if (isExonStr == null) {
            bytes = doc.getBinaryValue(FeatureIndexFields.IS_EXON.getFieldName());
            if (bytes != null) {
                isExonStr = bytes.utf8ToString();
            }
        }
        boolean isExon = isExonStr != null && Boolean.parseBoolean(isExonStr);
        vcfIndexEntry.setExon(isExon);
        vcfIndexEntry.getInfo().put(FeatureIndexFields.IS_EXON.getFieldName(), isExon);

        String[] variationTypes = doc.getValues(FeatureIndexFields.VARIATION_TYPE.getFieldName());
        if (variationTypes.length > 0) {
            vcfIndexEntry.setVariationTypes(new HashSet<>());
            for (String variationType : variationTypes) {
                vcfIndexEntry.getVariationTypes()
                        .add(VariationType.valueOf(variationType.toUpperCase()));
            }

            vcfIndexEntry.setVariationType(VariationType.valueOf(
                    doc.get(FeatureIndexFields.VARIATION_TYPE.getFieldName()).toUpperCase()));
        }
        vcfIndexEntry.setFailedFilter(doc.get(FeatureIndexFields.FAILED_FILTER.getFieldName()));

        IndexableField qualityField = doc.getField(FeatureIndexFields.QUALITY.getFieldName());
        if (qualityField != null) {
            vcfIndexEntry.setQuality(qualityField.numericValue().doubleValue());
        }

        if (vcfInfoFields != null) {
            for (String infoField : vcfInfoFields) {
                if (doc.getBinaryValue(infoField.toLowerCase()) != null) {
                    vcfIndexEntry.getInfo().put(infoField,
                            doc.getBinaryValue(infoField.toLowerCase()).utf8ToString());
                } else {
                    String[] values = doc.getValues(infoField.toLowerCase());
                    if (values.length > 0) {
                        vcfIndexEntry.getInfo().put(infoField, values[values.length - 1]);
                    }
                }
            }
        }

        return vcfIndexEntry;
    }

    @Override protected void addExtraFeatureFields(Document document, VcfIndexEntry entry) {
        for (VariationType type : entry.getVariationTypes()) {
            document.add(new StringField(FeatureIndexFields.VARIATION_TYPE.getFieldName(),
                    type.name().toLowerCase(), Field.Store.YES));
            document.add(new SortedSetDocValuesFacetField(
                    FeatureIndexFields.VARIATION_TYPE.getFieldName(), type.name()));
            document.add(
                    new SortedSetDocValuesField(FeatureIndexFields.VARIATION_TYPE.getFieldName(),
                            new BytesRef(type.name())));
        }

        if (CollectionUtils.isNotEmpty(entry.getFailedFilters())) {
            for (String failedFilter : entry.getFailedFilters()) {
                if (StringUtils.isNotBlank(failedFilter)) {
                    document.add(new StringField(FeatureIndexFields.FAILED_FILTER.getFieldName(),
                            failedFilter.toLowerCase(), Field.Store.YES));
                    document.add(new SortedSetDocValuesField(
                            FeatureIndexFields.FAILED_FILTER.getFieldName(),
                            new BytesRef(failedFilter)));
                    document.add(new SortedSetDocValuesFacetField(
                            FeatureIndexFields.FAILED_FILTER.getFieldName(), failedFilter));
                }
            }
        }

        document.add(new SortedSetDocValuesFacetField(FeatureIndexFields.QUALITY.getFieldName(),
                entry.getQuality().toString()));
        document.add(new SortedSetDocValuesField(FeatureIndexFields.QUALITY.getFieldName(),
                new BytesRef(entry.getQuality().toString())));
        document.add(new FloatPoint(FeatureIndexFields.QUALITY.getFieldName(),
                entry.getQuality().floatValue()));
        document.add(new StoredField(FeatureIndexFields.QUALITY.getFieldName(),
                entry.getQuality().floatValue()));
                /*document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.QUALITY.getGroupName(),
                new BytesRef(entry.getQuality().toString())));*/

        if (CollectionUtils.isNotEmpty(entry.getGeneIdList())) {
            for (String geneId : entry.getGeneIdList()) {
                if (StringUtils.isNotBlank(geneId)) {
                    document.add(new StringField(FeatureIndexFields.GENE_ID.getFieldName(),
                            geneId.toLowerCase(), Field.Store.YES));
                }
            }
            
                                /*document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.GENE_IDS.getFieldName(),
                    entry.getGeneIds(), true));*/
            document.add(
                    new SortedSetDocValuesFacetField(FeatureIndexFields.GENE_IDS.getFieldName(),
                            entry.getGeneIds()));
            document.add(new SortedSetDocValuesField(FeatureIndexFields.GENE_IDS.getFieldName(),
                    new BytesRef(entry.getGeneIds())));
            document.add(new StoredField(FeatureIndexFields.GENE_IDS.getFieldName(),
                    entry.getGeneIds()));
        }

        if (CollectionUtils.isNotEmpty(entry.getGeneNameList())) {
            for (String geneName : entry.getGeneNameList()) {
                if (StringUtils.isNotBlank(geneName)) {
                    document.add(new StringField(FeatureIndexFields.GENE_NAME.getFieldName(),
                            geneName.toLowerCase(), Field.Store.YES));
                }
            }
            
                                /*document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.GENE_NAMES.getFieldName(),
                    entry.getGeneNames(), true));*/
            document.add(
                    new SortedSetDocValuesFacetField(FeatureIndexFields.GENE_NAMES.getFieldName(),
                            entry.getGeneNames()));
            document.add(new SortedSetDocValuesField(FeatureIndexFields.GENE_NAMES.getFieldName(),
                    new BytesRef(entry.getGeneNames())));
            document.add(new StoredField(FeatureIndexFields.GENE_NAMES.getFieldName(),
                    entry.getGeneNames()));
        }

        document.add(new SortedSetDocValuesFacetField(FeatureIndexFields.IS_EXON.getFieldName(),
                entry.getExon().toString()));
        document.add(new SortedSetDocValuesField(FeatureIndexFields.IS_EXON.getFieldName(),
                new BytesRef(entry.getExon().toString())));
        document.add(new StringField(FeatureIndexFields.IS_EXON.getFieldName(),
                entry.getExon().toString().toLowerCase(), Field.Store.YES));

        if (entry.getInfo() != null) {
            addVcfDocumentInfoFields(document, entry);
        }
    }

    private void addVcfDocumentInfoFields(Document document, VcfIndexEntry vcfIndexEntry) {
        for (Map.Entry<String, Object> info : vcfIndexEntry.getInfo().entrySet()) {
            if (VIEW_FIELD_PATTERN.matcher(info.getKey())
                    .matches()) { //view fields are for view purposes
                continue;
            }

            String viewKey = "_" + info.getKey() + "_v";
            if (info.getValue() instanceof Object[]) {
                for (Object value : (Object[]) info.getValue()) {
                    addInfoField(value, info.getKey(), document);
                }
                addViewField(vcfIndexEntry, document, info.getKey(), viewKey);
            } else {
                addInfoField(info.getValue(), info.getKey(), document);

                String strValue = info.getValue().toString();
                if (strValue.length() + info.getKey().length() > MAX_FACET_LABEL_LEGTH){
                    LOGGER.warn("{} field value is too long ({}), truncating: {}", info.getKey(),
                            strValue.length(), strValue);
                    strValue =
                            strValue.substring(0, MAX_FACET_LABEL_LEGTH - info.getKey().length());
                }

                document.add(new SortedSetDocValuesField(info.getKey().toLowerCase(),
                        new BytesRef(strValue)));
                document.add(
                        new SortedSetDocValuesFacetField(info.getKey().toLowerCase(), strValue));
            }
        }
    }

    private void addInfoField(Object value, String key, Document document) {
        if (value instanceof Integer) {
            document.add(new IntPoint(key.toLowerCase(), (Integer) value));
            document.add(new StoredField(key.toLowerCase(), (Integer) value));
        } else if (value instanceof Float) {
            document.add(new FloatPoint(key.toLowerCase(), (Float) value));
            document.add(new StoredField(key.toLowerCase(), (Float) value));
        } else {
            document.add(new StringField(key.toLowerCase(), value.toString().toLowerCase(),
                    Field.Store.YES));
        }
    }

    private void addViewField(VcfIndexEntry vcfIndexEntry, Document document, String key,
            String viewKey) {
        if (vcfIndexEntry.getInfo().containsKey(viewKey)) {
            document.add(new StoredField(key.toLowerCase(),
                    vcfIndexEntry.getInfo().get(viewKey).toString()));

            String strValue = vcfIndexEntry.getInfo().get(viewKey).toString();
            if (strValue.length() + key.length() > MAX_FACET_LABEL_LEGTH){
                LOGGER.warn("{} field value is too long ({}), truncating: {}", key,
                        strValue.length(), strValue);
                strValue = strValue.substring(0, MAX_FACET_LABEL_LEGTH - key.length());
            }
            document.add(new SortedSetDocValuesField(key.toLowerCase(), new BytesRef(strValue)));
            document.add(new SortedSetDocValuesFacetField(key.toLowerCase(), strValue));
        }
    }

    public void setVcfInfoFields(List<String> vcfInfoFields) {
        this.vcfInfoFields = vcfInfoFields;
    }
}
