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
import org.apache.lucene.util.BytesRef;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by Mikhail_Miroliubov on 7/27/2017.
 */
public class BigVcfDocumentCreator extends AbstractDocumentCreator<VcfIndexEntry> {
    private static Pattern viewFieldPattern = Pattern.compile("_.*_v$");

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
}
