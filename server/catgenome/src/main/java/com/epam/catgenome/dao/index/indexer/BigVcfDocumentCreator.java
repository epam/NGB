package com.epam.catgenome.dao.index.indexer;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.dao.index.field.SortedFloatPoint;
import com.epam.catgenome.dao.index.field.SortedIntPoint;
import com.epam.catgenome.dao.index.field.SortedStringField;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.vcf.VariationType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by Mikhail_Miroliubov on 7/27/2017.
 */
public class BigVcfDocumentCreator extends AbstractDocumentCreator<VcfIndexEntry> {
    private static Pattern viewFieldPattern = Pattern.compile("_.*_v$");

    private List<String> vcfInfoFields;

    @Override
    VcfIndexEntry createSpecificEntry(Document doc) {
        VcfIndexEntry vcfIndexEntry = new VcfIndexEntry();
        vcfIndexEntry.setGene(doc.get(FeatureIndexDao.FeatureIndexFields.GENE_ID.getFieldName()));

        BytesRef bytes = doc.getBinaryValue(FeatureIndexDao.FeatureIndexFields.GENE_IDS.getFieldName());
        if (bytes != null) {
            vcfIndexEntry.setGeneIds(bytes.utf8ToString());
        }

        vcfIndexEntry.setGeneName(doc.get(FeatureIndexDao.FeatureIndexFields.GENE_NAME.getFieldName()));

        bytes = doc.getBinaryValue(FeatureIndexDao.FeatureIndexFields.GENE_NAMES.getFieldName());
        if (bytes != null) {
            vcfIndexEntry.setGeneNames(bytes.utf8ToString());
        }

        vcfIndexEntry.setInfo(new HashMap<>());

        String isExonStr = doc.get(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName()); //TODO: remove, in future only binary
        // value will remain
        if (isExonStr == null) {
            bytes = doc.getBinaryValue(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName());
            if (bytes != null) {
                isExonStr = bytes.utf8ToString();
            }
        }
        boolean isExon = isExonStr != null && Boolean.parseBoolean(isExonStr);
        vcfIndexEntry.setExon(isExon);
        vcfIndexEntry.getInfo().put(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName(), isExon);

        BytesRef featureIdBytes = doc.getBinaryValue(FeatureIndexDao.FeatureIndexFields.VARIATION_TYPE.getFieldName());
        if (featureIdBytes != null) {
            vcfIndexEntry.setVariationType(VariationType.valueOf(featureIdBytes.utf8ToString().toUpperCase()));
        }
        vcfIndexEntry.setFailedFilter(doc.get(FeatureIndexDao.FeatureIndexFields.FAILED_FILTER.getFieldName()));

        IndexableField qualityField = doc.getField(FeatureIndexDao.FeatureIndexFields.QUALITY.getFieldName());
        if (qualityField != null) {
            vcfIndexEntry.setQuality(qualityField.numericValue().doubleValue());
        }

        if (vcfInfoFields != null) {
            for (String infoField : vcfInfoFields) {
                if (doc.getBinaryValue(infoField.toLowerCase()) != null) {
                    vcfIndexEntry.getInfo().put(infoField, doc.getBinaryValue(infoField.toLowerCase()).utf8ToString());
                } else {
                    vcfIndexEntry.getInfo().put(infoField, doc.get(infoField.toLowerCase()));
                }
            }
        }

        return vcfIndexEntry;
    }

    @Override
    void addExtraFeatureFields(Document document, VcfIndexEntry entry)
    {
        for (VariationType type : entry.getVariationTypes()) {
            document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.VARIATION_TYPE.getFieldName(),
                    type.name()));
        }

        if (CollectionUtils.isNotEmpty(entry.getFailedFilters())) {
            for (String failedFilter : entry.getFailedFilters()) {
                if (StringUtils.isNotBlank(failedFilter)) {
                    document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.FAILED_FILTER.getFieldName(),
                            failedFilter));
                }
            }
        }

        document.add(new SortedFloatPoint(FeatureIndexDao.FeatureIndexFields.QUALITY.getFieldName(), entry.getQuality()
                .floatValue()));
        document.add(new StoredField(FeatureIndexDao.FeatureIndexFields.QUALITY.getFieldName(), entry.getQuality()
                .floatValue()));
        document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.QUALITY.getGroupName(),
                new BytesRef(entry.getQuality().toString())));

        if (CollectionUtils.isNotEmpty(entry.getGeneIdList())) {
            for (String geneId : entry.getGeneIdList()) {
                if (StringUtils.isNotBlank(geneId)) {
                    document.add(new StringField(FeatureIndexDao.FeatureIndexFields.GENE_ID.getFieldName(),
                            geneId.toLowerCase(), Field.Store.YES));
                }
            }

            document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.GENE_IDS.getFieldName(),
                    entry.getGeneIds(), true));
        }

        if (CollectionUtils.isNotEmpty(entry.getGeneNameList())) {
            for (String geneName : entry.getGeneNameList()) {
                if (StringUtils.isNotBlank(geneName)) {
                    document.add(new StringField(FeatureIndexDao.FeatureIndexFields.GENE_NAME.getFieldName(),
                            geneName.toLowerCase(), Field.Store.YES));
                }
            }

            document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.GENE_NAMES.getFieldName(),
                    entry.getGeneNames(), true));
        }

        document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName(),
                entry.getExon().toString()));

        if (entry.getInfo() != null) {
            addVcfDocumentInfoFields(document, entry);
        }
    }

    private void addVcfDocumentInfoFields(Document document, VcfIndexEntry vcfIndexEntry) {
        for (Map.Entry<String, Object> info : vcfIndexEntry.getInfo().entrySet()) {
            if (viewFieldPattern.matcher(info.getKey()).matches()) { //view fields are for view purposes
                continue;
            }

            String viewKey = "_" + info.getKey() + "_v";
            if (info.getValue() instanceof Object[]) {
                for (Object value : (Object[]) info.getValue()) {
                    addInfoField(value, info.getKey(), document, viewKey, vcfIndexEntry);
                }
            } else {
                addInfoField(info.getValue(), info.getKey(), document, viewKey, vcfIndexEntry);
            }
        }
    }

    private void addInfoField(Object value, String key, Document document, String viewKey, VcfIndexEntry vcfIndexEntry) {
        if (value instanceof Integer) {
            document.add(new SortedIntPoint(key.toLowerCase(), (Integer) value));
            if (vcfIndexEntry.getInfo().containsKey(viewKey)) {
                document.add(new StoredField(key.toLowerCase(), vcfIndexEntry.getInfo().get(viewKey).toString()));
                document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.getGroupName(key.toLowerCase()),
                        new BytesRef(vcfIndexEntry.getInfo().get(viewKey).toString())));
            } else {
                document.add(new StoredField(key.toLowerCase(), (Integer) value));
                document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.getGroupName(key.toLowerCase()),
                        new BytesRef(value.toString())));
            }
        } else if (value instanceof Float) {
            document.add(new SortedFloatPoint(key.toLowerCase(), (Float) value));

            if (vcfIndexEntry.getInfo().containsKey(viewKey)) {
                document.add(new StoredField(key.toLowerCase(), vcfIndexEntry.getInfo().get(viewKey).toString()));
                document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.getGroupName(key.toLowerCase()),
                        new BytesRef(vcfIndexEntry.getInfo().get(viewKey).toString())));
            } else {
                document.add(new StoredField(key.toLowerCase(), (Float) value));
                document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.getGroupName(key.toLowerCase()),
                        new BytesRef(value.toString())));
            }
        } else {
            if (vcfIndexEntry.getInfo().containsKey(viewKey)) {
                document.add(new SortedStringField(key.toLowerCase(), vcfIndexEntry.getInfo().get(viewKey).toString()));
            } else {
                document.add(new SortedStringField(key.toLowerCase(), value.toString().trim()));
            }
        }
    }

    public void setVcfInfoFields(List<String> vcfInfoFields) {
        this.vcfInfoFields = vcfInfoFields;
    }
}
