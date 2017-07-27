package com.epam.catgenome.dao.index.indexer;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.dao.index.field.SortedFloatPoint;
import com.epam.catgenome.dao.index.field.SortedIntPoint;
import com.epam.catgenome.dao.index.field.SortedStringField;
import com.epam.catgenome.entity.index.VcfIndexEntry;
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
public class VcfDocumentCreator extends AbstractDocumentCreator<VcfIndexEntry> {
    private static Pattern viewFieldPattern = Pattern.compile("_.*_v$");

    @Override
    void addExtraFeatureFields(Document document, VcfIndexEntry entry)
    {
        document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.VARIATION_TYPE.getFieldName(),
                entry.getVariationType().name()));

        if (StringUtils.isNotBlank(entry.getFailedFilter())) {
            document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.FAILED_FILTER.getFieldName(),
                    entry.getFailedFilter()));
        }

        document.add(new SortedFloatPoint(FeatureIndexDao.FeatureIndexFields.QUALITY.getFieldName(), entry.getQuality()
                .floatValue()));
        document.add(new StoredField(FeatureIndexDao.FeatureIndexFields.QUALITY.getFieldName(), entry.getQuality()
                .floatValue()));
        document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.QUALITY.getGroupName(),
                new BytesRef(entry.getQuality().toString())));

        if (StringUtils.isNotBlank(entry.getGene())) {
            document.add(new StringField(FeatureIndexDao.FeatureIndexFields.GENE_ID.getFieldName(),
                    entry.getGene().toLowerCase(), Field.Store.YES));
            document.add(new SortedStringField(FeatureIndexDao.FeatureIndexFields.GENE_IDS.getFieldName(),
                    entry.getGeneIds(), true));
        }

        if (StringUtils.isNotBlank(entry.getGeneName())) {
            document.add(new StringField(FeatureIndexDao.FeatureIndexFields.GENE_NAME.getFieldName(),
                    entry.getGeneName().toLowerCase(), Field.Store.YES));
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
            if (info.getValue() instanceof Integer) {
                document.add(new SortedIntPoint(info.getKey().toLowerCase(), (Integer) info.getValue()));
                if (vcfIndexEntry.getInfo().containsKey(viewKey)) {
                    document.add(new StoredField(info.getKey().toLowerCase(), vcfIndexEntry.getInfo()
                            .get(viewKey).toString()));
                    document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.getGroupName(info.getKey().toLowerCase()),
                            new BytesRef(vcfIndexEntry.getInfo().get(viewKey).toString())));
                } else {
                    document.add(new StoredField(info.getKey().toLowerCase(),
                            (Integer) info.getValue()));
                    document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.getGroupName(info.getKey().toLowerCase()),
                            new BytesRef(info.getValue().toString())));
                }
            } else if (info.getValue() instanceof Float) {
                document.add(new SortedFloatPoint(info.getKey().toLowerCase(), (Float) info.getValue()));

                if (vcfIndexEntry.getInfo().containsKey(viewKey)) {
                    document.add(new StoredField(info.getKey().toLowerCase(), vcfIndexEntry.getInfo()
                            .get(viewKey).toString()));
                    document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.getGroupName(info.getKey().toLowerCase()),
                            new BytesRef(vcfIndexEntry.getInfo().get(viewKey).toString())));
                } else {
                    document.add(new StoredField(info.getKey().toLowerCase(), (Float) info.getValue()));
                    document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.getGroupName(info.getKey().toLowerCase()),
                            new BytesRef(info.getValue().toString())));
                }
            } else {
                if (vcfIndexEntry.getInfo().containsKey(viewKey)) {
                    document.add(new SortedStringField(info.getKey().toLowerCase(), vcfIndexEntry.getInfo()
                            .get(viewKey).toString()));
                } else {
                    document.add(new SortedStringField(info.getKey().toLowerCase(), info.getValue().toString().trim()));
                }
            }
        }
    }
}
