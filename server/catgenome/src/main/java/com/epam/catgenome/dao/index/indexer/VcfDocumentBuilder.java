/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.vcf.VariationType;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An extension of {@link AbstractDocumentBuilder}, that allows indexing and reading entries of VCF feature
 * indexes
 */
public class VcfDocumentBuilder extends AbstractDocumentBuilder<VcfIndexEntry> {
    private static final Pattern VIEW_FIELD_PATTERN = Pattern.compile("_.*_v$");

    private List<String> vcfInfoFields;

    @Override protected void addExtraFeatureFields(Document document, VcfIndexEntry entry) {
        document.add(new SortedStringField(
                FeatureIndexDao.FeatureIndexFields.VARIATION_TYPE.getFieldName(),
                entry.getVariationType().name()));

        if (StringUtils.isNotBlank(entry.getFailedFilter())) {
            document.add(new SortedStringField(
                    FeatureIndexDao.FeatureIndexFields.FAILED_FILTER.getFieldName(),
                    entry.getFailedFilter()));
        }

        document.add(new SortedFloatPoint(FeatureIndexDao.FeatureIndexFields.QUALITY.getFieldName(),
                entry.getQuality().floatValue()));
        document.add(new StoredField(FeatureIndexDao.FeatureIndexFields.QUALITY.getFieldName(),
                entry.getQuality().floatValue()));
        document.add(
                new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields.QUALITY.getGroupName(),
                        new BytesRef(entry.getQuality().toString())));

        if (StringUtils.isNotBlank(entry.getGene())) {
            document.add(new StringField(FeatureIndexDao.FeatureIndexFields.GENE_ID.getFieldName(),
                    entry.getGene().toLowerCase(), Field.Store.YES));
            document.add(new SortedStringField(
                    FeatureIndexDao.FeatureIndexFields.GENE_IDS.getFieldName(), entry.getGeneIds(),
                    true));
        }

        if (StringUtils.isNotBlank(entry.getGeneName())) {
            document.add(
                    new StringField(FeatureIndexDao.FeatureIndexFields.GENE_NAME.getFieldName(),
                            entry.getGeneName().toLowerCase(), Field.Store.YES));
            document.add(new SortedStringField(
                    FeatureIndexDao.FeatureIndexFields.GENE_NAMES.getFieldName(),
                    entry.getGeneNames(), true));
        }

        document.add(
                new SortedStringField(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName(),
                        entry.getExon().toString()));

        if (entry.getInfo() != null) {
            addVcfDocumentInfoFields(document, entry);
        }
    }

    @Override protected Set<String> getRequiredFields() {
        Set<String> requiredFields = new HashSet<>();
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.CHROMOSOME_NAME.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.CHROMOSOME_ID.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.FILE_ID.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.FEATURE_ID.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.START_INDEX.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.END_INDEX.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.GENE_NAME.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.GENE_ID.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.GENE_NAMES.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.GENE_IDS.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.QUALITY.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.VARIATION_TYPE.getFieldName());
        requiredFields.add(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName());
        return requiredFields;
    }

    @Override protected VcfIndexEntry createSpecificEntry(Document doc) {
        VcfIndexEntry vcfIndexEntry = new VcfIndexEntry();
        vcfIndexEntry.setGene(doc.get(FeatureIndexDao.FeatureIndexFields.GENE_ID.getFieldName()));

        BytesRef bytes =
                doc.getBinaryValue(FeatureIndexDao.FeatureIndexFields.GENE_IDS.getFieldName());
        if (bytes != null) {
            vcfIndexEntry.setGeneIds(bytes.utf8ToString());
        }

        vcfIndexEntry
                .setGeneName(doc.get(FeatureIndexDao.FeatureIndexFields.GENE_NAME.getFieldName()));

        bytes = doc.getBinaryValue(FeatureIndexDao.FeatureIndexFields.GENE_NAMES.getFieldName());
        if (bytes != null) {
            vcfIndexEntry.setGeneNames(bytes.utf8ToString());
        }

        vcfIndexEntry.setInfo(new HashMap<>());

        String isExonStr = doc.get(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName());
        //TODO: remove, in future only binary value will remain
        if (isExonStr == null) {
            bytes = doc.getBinaryValue(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName());
            if (bytes != null) {
                isExonStr = bytes.utf8ToString();
            }
        }
        boolean isExon = isExonStr != null && Boolean.parseBoolean(isExonStr);
        vcfIndexEntry.setExon(isExon);
        vcfIndexEntry.getInfo()
                .put(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName(), isExon);

        BytesRef featureIdBytes = doc.getBinaryValue(
                FeatureIndexDao.FeatureIndexFields.VARIATION_TYPE.getFieldName());
        if (featureIdBytes != null) {
            vcfIndexEntry.setVariationType(
                    VariationType.valueOf(featureIdBytes.utf8ToString().toUpperCase()));
        }
        vcfIndexEntry.setFailedFilter(
                doc.get(FeatureIndexDao.FeatureIndexFields.FAILED_FILTER.getFieldName()));

        IndexableField qualityField =
                doc.getField(FeatureIndexDao.FeatureIndexFields.QUALITY.getFieldName());
        if (qualityField != null) {
            vcfIndexEntry.setQuality(qualityField.numericValue().doubleValue());
        }

        if (vcfInfoFields != null) {
            for (String infoField : vcfInfoFields) {
                if (doc.getBinaryValue(infoField.toLowerCase()) != null) {
                    vcfIndexEntry.getInfo().put(infoField,
                            doc.getBinaryValue(infoField.toLowerCase()).utf8ToString());
                } else {
                    vcfIndexEntry.getInfo().put(infoField, doc.get(infoField.toLowerCase()));
                }
            }
        }

        return vcfIndexEntry;
    }

    private void addVcfDocumentInfoFields(Document document, VcfIndexEntry vcfIndexEntry) {
        for (Map.Entry<String, Object> info : vcfIndexEntry.getInfo().entrySet()) {
            if (VIEW_FIELD_PATTERN.matcher(info.getKey())
                    .matches()) { //view fields are for view purposes
                continue;
            }

            String viewKey = "_" + info.getKey() + "_v";
            if (info.getValue() instanceof Integer) {
                document.add(
                        new SortedIntPoint(info.getKey().toLowerCase(), (Integer) info.getValue()));
                if (vcfIndexEntry.getInfo().containsKey(viewKey)) {
                    document.add(new StoredField(info.getKey().toLowerCase(),
                            vcfIndexEntry.getInfo().get(viewKey).toString()));
                    document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields
                            .getGroupName(info.getKey().toLowerCase()),
                            new BytesRef(vcfIndexEntry.getInfo().get(viewKey).toString())));
                } else {
                    document.add(new StoredField(info.getKey().toLowerCase(),
                            (Integer) info.getValue()));
                    document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields
                            .getGroupName(info.getKey().toLowerCase()),
                            new BytesRef(info.getValue().toString())));
                }
            } else if (info.getValue() instanceof Float) {
                document.add(
                        new SortedFloatPoint(info.getKey().toLowerCase(), (Float) info.getValue()));

                if (vcfIndexEntry.getInfo().containsKey(viewKey)) {
                    document.add(new StoredField(info.getKey().toLowerCase(),
                            vcfIndexEntry.getInfo().get(viewKey).toString()));
                    document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields
                            .getGroupName(info.getKey().toLowerCase()),
                            new BytesRef(vcfIndexEntry.getInfo().get(viewKey).toString())));
                } else {
                    document.add(
                            new StoredField(info.getKey().toLowerCase(), (Float) info.getValue()));
                    document.add(new SortedDocValuesField(FeatureIndexDao.FeatureIndexFields
                            .getGroupName(info.getKey().toLowerCase()),
                            new BytesRef(info.getValue().toString())));
                }
            } else {
                if (vcfIndexEntry.getInfo().containsKey(viewKey)) {
                    document.add(new SortedStringField(info.getKey().toLowerCase(),
                            vcfIndexEntry.getInfo().get(viewKey).toString()));
                } else {
                    document.add(new SortedStringField(info.getKey().toLowerCase(),
                            info.getValue().toString().trim()));
                }
            }
        }
    }

    public void setVcfInfoFields(List<String> vcfInfoFields) {
        this.vcfInfoFields = vcfInfoFields;
    }
}
