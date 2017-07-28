package com.epam.catgenome.dao.index.indexer;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.dao.index.field.SortedSetFloatPoint;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.vcf.VariationType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Mikhail_Miroliubov on 7/27/2017.
 */
public class BigVcfDocumentBuilder extends AbstractDocumentBuilder<VcfIndexEntry>
{
    private static Pattern viewFieldPattern = Pattern.compile("_.*_v$");

    private List<String> vcfInfoFields;

    @Override
    protected VcfIndexEntry createSpecificEntry(Document doc) {
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
                    String[] values = doc.getValues(infoField.toLowerCase());
                    if (values.length > 0) {
                        vcfIndexEntry.getInfo().put(infoField, values[values.length - 1]);
                    }
                }
            }
        }

        return vcfIndexEntry;
    }

    @Override
    protected void addExtraFeatureFields(Document document, VcfIndexEntry entry)
    {
        document.add(new SortedSetDocValuesField(FeatureIndexDao.FeatureIndexFields.VARIATION_TYPE.getFieldName(),
                new BytesRef(entry.getVariationType().name())));

        for (VariationType type : entry.getVariationTypes()) {
            document.add(new StringField(FeatureIndexDao.FeatureIndexFields.VARIATION_TYPE.getFieldName(),
                    type.name().toLowerCase(), Field.Store.YES));
        }

        if (CollectionUtils.isNotEmpty(entry.getFailedFilters())) {
            for (String failedFilter : entry.getFailedFilters()) {
                if (StringUtils.isNotBlank(failedFilter)) {
                    document.add(new StringField(FeatureIndexDao.FeatureIndexFields.FAILED_FILTER.getFieldName(),
                            failedFilter.toLowerCase(), Field.Store.YES));
                }
            }

            document.add(new SortedSetDocValuesField(FeatureIndexDao.FeatureIndexFields.FAILED_FILTER.getFieldName(),
                    new BytesRef(entry.getFailedFilters().stream().collect(Collectors.joining(", ")))));
        }

        document.add(new SortedSetFloatPoint(FeatureIndexDao.FeatureIndexFields.QUALITY.getFieldName(), entry.getQuality()
                .floatValue()));
        document.add(new StoredField(FeatureIndexDao.FeatureIndexFields.QUALITY.getFieldName(), entry.getQuality()
                .floatValue()));
        /*document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.QUALITY.getGroupName(),
                new BytesRef(entry.getQuality().toString())));*/

        if (CollectionUtils.isNotEmpty(entry.getGeneIdList())) {
            for (String geneId : entry.getGeneIdList()) {
                if (StringUtils.isNotBlank(geneId)) {
                    document.add(new StringField(FeatureIndexDao.FeatureIndexFields.GENE_ID.getFieldName(),
                            geneId.toLowerCase(), Field.Store.YES));
                }
            }

            /*document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.GENE_IDS.getFieldName(),
                    entry.getGeneIds(), true));*/
            document.add(new SortedSetDocValuesField(FeatureIndexDao.FeatureIndexFields.GENE_IDS.getFieldName(),
                    new BytesRef(entry.getGeneIds())));
            document.add(new StoredField(FeatureIndexDao.FeatureIndexFields.GENE_IDS.getFieldName(), entry.getGeneIds()));
        }

        if (CollectionUtils.isNotEmpty(entry.getGeneNameList())) {
            for (String geneName : entry.getGeneNameList()) {
                if (StringUtils.isNotBlank(geneName)) {
                    document.add(new StringField(FeatureIndexDao.FeatureIndexFields.GENE_NAME.getFieldName(),
                            geneName.toLowerCase(), Field.Store.YES));
                }
            }

            /*document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.GENE_NAMES.getFieldName(),
                    entry.getGeneNames(), true));*/
            document.add(new SortedSetDocValuesField(FeatureIndexDao.FeatureIndexFields.GENE_NAMES.getFieldName(),
                    new BytesRef(entry.getGeneNames())));
            document.add(new StoredField(FeatureIndexDao.FeatureIndexFields.GENE_NAMES.getFieldName(),
                    entry.getGeneNames()));
        }

        document.add(new SortedSetDocValuesField(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName(),
                new BytesRef(entry.getExon().toString())));
        document.add(new StringField(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName(),
                entry.getExon().toString().toLowerCase(), Field.Store.YES));

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
                addViewField(vcfIndexEntry, document, info.getKey(), viewKey);
            } else {
                addInfoField(info.getValue(), info.getKey(), document, viewKey, vcfIndexEntry);
                document.add(new SortedSetDocValuesField(info.getKey().toLowerCase(), //FeatureIndexDao.FeatureIndexFields.getGroupName(
                        new BytesRef(info.getValue().toString())));
            }
        }
    }

    private void addInfoField(Object value, String key, Document document, String viewKey, VcfIndexEntry vcfIndexEntry) {
        if (value instanceof Integer) {
            document.add(new IntPoint(key.toLowerCase(), (Integer) value));
            document.add(new StoredField(key.toLowerCase(), (Integer) value));
        } else if (value instanceof Float) {
            document.add(new FloatPoint(key.toLowerCase(), (Float) value));
            document.add(new StoredField(key.toLowerCase(), (Float) value));
        } else {
            document.add(new StringField(key.toLowerCase(), value.toString().toLowerCase(), Field.Store.YES));
        }
    }

    private void addViewField(VcfIndexEntry vcfIndexEntry, Document document, String key, String viewKey) {
        if (vcfIndexEntry.getInfo().containsKey(viewKey)) {
            document.add(new StoredField(key.toLowerCase(), vcfIndexEntry.getInfo().get(viewKey).toString()));
            document.add(new SortedSetDocValuesField(key.toLowerCase(), //FeatureIndexDao.FeatureIndexFields.getGroupName(
                    new BytesRef(vcfIndexEntry.getInfo().get(viewKey).toString())));
        }
    }

    public void setVcfInfoFields(List<String> vcfInfoFields) {
        this.vcfInfoFields = vcfInfoFields;
    }
}
