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

import com.epam.catgenome.dao.index.FeatureIndexDao.FeatureIndexFields;
import com.epam.catgenome.dao.index.field.SortedIntPoint;
import com.epam.catgenome.dao.index.field.SortedStringField;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.index.GeneIndexEntry;
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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * An abstract class, whose extensions are responsible for building Lucene documents form
 * {@link FeatureIndexEntry}'es and backwards.
 */
public abstract class AbstractDocumentBuilder<E extends FeatureIndexEntry> {
    /**
     * Builds a Lucene {@link Document} from specified {@link FeatureIndexEntry}
     *
     * @param entry         an entry to index
     * @param featureFileId an ID of {@link com.epam.catgenome.entity.FeatureFile}, to which feature belongs
     * @return a document, ready to be written
     */
    public Document buildDocument(E entry, final Long featureFileId) {
        Document document = new Document();
        document.add(new SortedStringField(FeatureIndexFields.FEATURE_ID.getFieldName(),
                entry.getFeatureId()));

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
        document.add(new SortedStringField(FeatureIndexFields.CHROMOSOME_NAME.getFieldName(),
                entry.getChromosome().getName(), true));

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
                entry.getFeatureType() != null ? entry.getFeatureType().getFileValue() : entry.getCustomFeatureType(),
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

    /**
     * Creates an {@link FeatureIndexEntry} from fields of a specified Lucene {@link Document}
     * @param searcher to retrieve document from
     * @param docId a document ID to read entry from
     * @return an {@link FeatureIndexEntry}, represented by specified document
     */
    public E buildEntry(IndexSearcher searcher, int docId) throws IOException {
        Set<String> requiredFields = getRequiredFields();
        Document doc = searcher.doc(docId, requiredFields);

        FeatureType featureType =
                FeatureType.forValue(doc.get(FeatureIndexFields.FEATURE_TYPE.getFieldName()));
        E entry = createSpecificEntry(doc);

        entry.setFeatureType(featureType);
        BytesRef featureIdBytes = doc.getBinaryValue(FeatureIndexFields.FEATURE_ID.getFieldName());
        if (featureIdBytes == null) {
            entry.setFeatureId(doc.get(FeatureIndexFields.FEATURE_ID.getFieldName()));
        } else {
            entry.setFeatureId(featureIdBytes.utf8ToString());
        }

        entry.setStartIndex(
                doc.getField(FeatureIndexFields.START_INDEX.getFieldName()).numericValue()
                        .intValue());
        entry.setEndIndex(doc.getField(FeatureIndexFields.END_INDEX.getFieldName()).numericValue()
                .intValue());
        entry.setFeatureFileId(Long.parseLong(doc.get(FeatureIndexFields.FILE_ID.getFieldName())));
        entry.setFeatureName(doc.get(FeatureIndexFields.FEATURE_NAME.getFieldName()));

        String chromosomeId =
                doc.getBinaryValue(FeatureIndexFields.CHROMOSOME_ID.getFieldName()).utf8ToString();
        if (!chromosomeId.isEmpty()) {
            entry.setChromosome(new Chromosome(Long.parseLong(chromosomeId)));
            entry.getChromosome().setName(
                    doc.getBinaryValue(FeatureIndexFields.CHROMOSOME_NAME.getFieldName())
                            .utf8ToString());
        }

        return entry;
    }

    protected abstract Set<String> getRequiredFields();

    /**
     * Creates {@link FacetsConfig} for Lucene {@link org.apache.lucene.index.IndexWriter} to index fields with facets
     *
     * @param info is required only for {@link VcfDocumentBuilder} or {@link BigVcfDocumentBuilder}, can be null
     *             otherwise
     * @return a {@link FacetsConfig} for Lucene IndexWriter
     */
    public FacetsConfig createFacetsConfig(VcfFilterInfo info) {
        FacetsConfig facetsConfig = new FacetsConfig();
        facetsConfig.setIndexFieldName(FeatureIndexFields.CHR_ID.getFieldName(),
                FeatureIndexFields.FACET_CHR_ID.getFieldName());
        facetsConfig.setIndexFieldName(FeatureIndexFields.F_UID.getFieldName(),
                FeatureIndexFields.FACET_UID.getFieldName());

        return facetsConfig;
    }

    /**
     * Creates a specific extension of {@link FeatureIndexEntry}. Override this method to allow custom fields
     * to be loaded
     *
     * @param doc a Lucene {@link Document}, to load data from
     * @return a specific extension of {@link FeatureIndexEntry}
     */
    protected abstract E createSpecificEntry(Document doc);

    /**
     * Adds extra fields of specific {@link FeatureIndexEntry} extension. Override this method to allow custom fields
     * to be indexed
     *
     * @param document a document to add extra fields
     * @param entry    a specific {@link FeatureIndexEntry}
     */
    protected abstract void addExtraFeatureFields(Document document, E entry);

    /**
     * Creates a AbstractDocumentBuilder's extension, based on passed {@link FeatureIndexEntry}
     *
     * @param entry an {@link FeatureIndexEntry} to guess the type of {@link AbstractDocumentBuilder} extension
     * @return relevant {@link AbstractDocumentBuilder} extension
     */
    public static AbstractDocumentBuilder createDocumentCreator(FeatureIndexEntry entry) {
        if (entry instanceof VcfIndexEntry) {
            return new BigVcfDocumentBuilder();
        } else if (entry instanceof GeneIndexEntry) {
            return new GeneDocumentBuilder();
        } else {
            return new DefaultDocumentBuilder();
        }
    }

    /**
     * Creates a AbstractDocumentBuilder's extension, based on passed {@link FeatureIndexEntry}
     *
     * @param format {@link BiologicalDataItemFormat} to guess the type of {@link AbstractDocumentBuilder} extension
     * @param info   is required only for {@code BiologicalDataItemFormat.VCF}, can be null
     *               otherwise
     * @return relevant {@link AbstractDocumentBuilder} extension
     */
    public static AbstractDocumentBuilder createDocumentCreator(BiologicalDataItemFormat format,
            List<String> info) {
        switch (format) {
            case VCF:
                BigVcfDocumentBuilder vcfCreator = new BigVcfDocumentBuilder();
                vcfCreator.setVcfInfoFields(info);
                return vcfCreator;
            case GENE:
                GeneDocumentBuilder geneCreator = new GeneDocumentBuilder();
                geneCreator.setGeneAttributes(info);
                return geneCreator;
            default:
                return new DefaultDocumentBuilder();
        }
    }
}
