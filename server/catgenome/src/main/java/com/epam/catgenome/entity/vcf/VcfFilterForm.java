/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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

package com.epam.catgenome.entity.vcf;

import com.epam.catgenome.dao.index.FeatureIndexDao.FeatureIndexFields;
import com.epam.catgenome.entity.AbstractFilterForm;
import com.epam.catgenome.entity.index.FeatureType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermsQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@code VcfFilterForm} represents a VO used to handle query parameters
 * that allow finding and filtering variations saved in the lucene index
 */

public class VcfFilterForm extends AbstractFilterForm {
    private String failedFilter;
    private FilterSection<List<VariationType>> variationTypes;
    private FilterSection<List<String>> genes;
    private FilterSection<List<VariationEffect>> effects;
    private FilterSection<List<VariationImpact>> impacts;
    private Map<String, Object> additionalFilters;
    private List<Float> quality;
    private Boolean isExon;
    private Integer startIndex;
    private Integer endIndex;
    private FilterSection<List<String>> sampleNames;

    private Integer page;
    /**
     * Additional fields to show in Variations table
     */
    private List<String> infoFields;

    private Map<Long, List<Long>> vcfFileIdsByProject;
    private List<Long> chromosomeIds;

    /**
     * Creates a {@code BooleanQuery} for loading all types of features ()
     * @return a {@code BooleanQuery} to a lucene index without filtering by a{@code FeatureType}
     */
    public BooleanQuery computeQuery() {
        return computeQuery(null);
    }

    /**
     * Creates a {@code BooleanQuery} for loading variations from a lucene index with specified filters
     * @param featureType type of features to find
     * @return a {@code BooleanQuery} to a lucene with specified filters
     */
    public BooleanQuery computeQuery(FeatureType featureType) {

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        addFeatureTypeFilter(featureType, builder);
        addVcfFileFilter(builder);
        addChromosomeFilter(builder);
        addGeneFilter(builder);
        addExonFilter(builder);
        addVariationTypeFilter(builder);
        addSampleNamesFilter(builder);
        addFailedFilter(builder);
        addQualityFilter(builder);
        addPositionFilter(builder);
        addAdditionalFilters(builder);
        return builder.build();
    }

    public boolean filterEmpty() {
        return isFilterEmpty(variationTypes) && isFilterEmpty(genes) && isFilterEmpty(effects)
                && isFilterEmpty(impacts) && MapUtils.isEmpty(additionalFilters)
                && CollectionUtils.isEmpty(quality) && (isExon == null || !isExon) && startIndex == null &&
                endIndex == null && CollectionUtils.isEmpty(chromosomeIds);
    }

    private <T> boolean isFilterEmpty(FilterSection<List<T>> filterSection) {
        return filterSection == null || CollectionUtils.isEmpty(filterSection.field);
    }

    private void addAdditionalFilters(BooleanQuery.Builder builder) {
        if (additionalFilters != null && !additionalFilters.isEmpty()) {
            for (Map.Entry<String, Object> entry : additionalFilters.entrySet()) {
                addAdditionalFilter(builder, entry);
            }
        }
    }

    /**
     * Filter variations by positions, using only variation's start index
     * @param builder
     */
    private void addPositionFilter(BooleanQuery.Builder builder) {
        if (startIndex != null && endIndex != null) {
            builder.add(IntPoint.newRangeQuery(FeatureIndexFields.START_INDEX.getFieldName(), startIndex, endIndex),
                    BooleanClause.Occur.MUST);
        } else {
            if (startIndex != null) {
                builder.add(IntPoint.newRangeQuery(FeatureIndexFields.START_INDEX.getFieldName(), startIndex, Integer
                        .MAX_VALUE), BooleanClause.Occur.MUST);
            } else if (endIndex != null) {
                builder.add(IntPoint.newRangeQuery(FeatureIndexFields.START_INDEX.getFieldName(), Integer.MIN_VALUE,
                        endIndex), BooleanClause.Occur.MUST);
            }
        }
    }

    private void addAdditionalFilter(BooleanQuery.Builder builder,
            Map.Entry<String, Object> entry) {
        String key = entry.getKey().toLowerCase();
        if (entry.getValue() instanceof List) {
            addFiltersFromList(builder, entry, key);
        } else if (entry.getValue() instanceof Integer || entry.getValue() instanceof Long) {
            builder.add(IntPoint.newExactQuery(key, (Integer) entry.getValue()), BooleanClause.Occur.MUST);
        } else if (entry.getValue() instanceof Float || entry.getValue() instanceof Double) {
            builder.add(FloatPoint.newExactQuery(key, (Float) entry.getValue()), BooleanClause.Occur.MUST);
        } else {
            builder.add(new TermQuery(new Term(key, entry.getValue().toString().toLowerCase())),
                    BooleanClause.Occur.MUST);
        }
    }

    private void addQualityFilter(BooleanQuery.Builder builder) {
        if (quality != null && !quality.isEmpty()) {
            if (quality.size() < 2) {
                builder.add(FloatPoint.newExactQuery(FeatureIndexFields.QUALITY.getFieldName(),
                                                     quality.get(0)), BooleanClause.Occur.MUST);
            } else {
                Assert.isTrue(quality.get(0) != null || quality.get(1) != null, "Incorrect filter parameter:" +
                        " quality:[null, null]");
                builder.add(FloatPoint.newRangeQuery(FeatureIndexFields.QUALITY.getFieldName(),
                        quality.get(0) != null ? quality.get(0) : Float.MIN_VALUE,
                        quality.get(1) != null ? quality.get(1) : Float.MAX_VALUE),
                        BooleanClause.Occur.MUST);
            }
        }
    }

    private void addFailedFilter(BooleanQuery.Builder builder) {
        if (StringUtils.isNotBlank(failedFilter)) {
            builder.add(new TermQuery(new Term(FeatureIndexFields.FAILED_FILTER.getFieldName(), failedFilter)),
                        BooleanClause.Occur.MUST);
        }
    }

    private void addVariationTypeFilter(BooleanQuery.Builder builder) {
        if (variationTypes != null && !variationTypes.field.isEmpty()) {
            BooleanQuery.Builder typesBuilder = new BooleanQuery.Builder();
            for (int i = 0; i < variationTypes.field.size(); i++) {
                TermQuery termQuery = new TermQuery(new Term(FeatureIndexFields.VARIATION_TYPE.getFieldName(),
                        variationTypes.field.get(i).toString().toLowerCase()));
                typesBuilder.add(termQuery, variationTypes.conjunction ? BooleanClause.Occur.MUST :
                        BooleanClause.Occur.SHOULD);
            }

            builder.add(typesBuilder.build(), BooleanClause.Occur.MUST);
        }
    }

    private void addSampleNamesFilter(final BooleanQuery.Builder builder) {
        if (sampleNames != null && !sampleNames.field.isEmpty()) {
            final BooleanQuery.Builder typesBuilder = new BooleanQuery.Builder();
            for (int i = 0; i < sampleNames.field.size(); i++) {
                TermQuery termQuery = new TermQuery(new Term(FeatureIndexFields.SAMPLE_NAMES.getFieldName(),
                        sampleNames.field.get(i)));
                typesBuilder.add(termQuery, sampleNames.conjunction ? BooleanClause.Occur.MUST :
                        BooleanClause.Occur.SHOULD);
            }
            builder.add(typesBuilder.build(), BooleanClause.Occur.MUST);
        }
    }

    private void addExonFilter(BooleanQuery.Builder builder) {
        if (isExon != null && isExon) {
            builder.add(new TermQuery(new Term(FeatureIndexFields.IS_EXON.getFieldName(), isExon.toString())),
                        BooleanClause.Occur.MUST);
        }
    }

    private void addGeneFilter(BooleanQuery.Builder builder) {
        if (genes != null && !genes.field.isEmpty()) {
            BooleanQuery.Builder genesBuilder = new BooleanQuery.Builder();
            for (int i = 0; i < genes.field.size(); i++) {
                PrefixQuery geneIdPrefixQuery = new PrefixQuery(new Term(FeatureIndexFields.GENE_ID.getFieldName(),
                        genes.field.get(i).toLowerCase()));
                PrefixQuery geneNamePrefixQuery = new PrefixQuery(
                    new Term(FeatureIndexFields.GENE_NAME.getFieldName(), genes.field.get(i).toLowerCase()));
                BooleanQuery.Builder geneIdOrNameQuery = new BooleanQuery.Builder();
                geneIdOrNameQuery.add(geneIdPrefixQuery, BooleanClause.Occur.SHOULD);
                geneIdOrNameQuery.add(geneNamePrefixQuery, BooleanClause.Occur.SHOULD);

                genesBuilder.add(geneIdOrNameQuery.build(), genes.conjunction ? BooleanClause.Occur.MUST :
                        BooleanClause.Occur.SHOULD);
            }

            builder.add(genesBuilder.build(), BooleanClause.Occur.MUST);
        }
    }

    private void addChromosomeFilter(BooleanQuery.Builder builder) {
        if (CollectionUtils.isNotEmpty(chromosomeIds)) {
            List<Term> chromosomeTerms = chromosomeIds.stream().map(id -> new Term(FeatureIndexFields.CHROMOSOME_ID
                            .getFieldName(), id.toString())).collect(Collectors.toList());
            builder.add(new TermsQuery(chromosomeTerms), BooleanClause.Occur.MUST);
        }
    }

    private void addVcfFileFilter(BooleanQuery.Builder builder) {
        if (vcfFileIdsByProject != null && !vcfFileIdsByProject.isEmpty()) {
            List<Term> terms = vcfFileIdsByProject.values().stream().flatMap(List::stream)
                    .map(vcfFileId -> new Term(FeatureIndexFields.FILE_ID.getFieldName(), vcfFileId.toString()))
                    .collect(Collectors.toList());
            TermsQuery termsQuery = new TermsQuery(terms);
            builder.add(termsQuery, BooleanClause.Occur.MUST);
        }
    }

    private void addFeatureTypeFilter(FeatureType featureType, BooleanQuery.Builder builder) {
        if (featureType != null) {
            builder.add(new TermQuery(new Term(FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                                               featureType.getFileValue())),
                    BooleanClause.Occur.MUST);
        }
    }

    private void addFiltersFromList(BooleanQuery.Builder builder, Map.Entry<String, Object> entry,
            String key) {
        List list = (List) entry.getValue();
        if (list.isEmpty()) {
            return;
        }
        Object val = list.get(0);
        if (list.size() < 2) {
            tryAddIntegeralFilter(builder, entry, key, val);
            tryAddFloatingFilter(builder, entry, key, val);
        } else {
            val = list.get(0) != null ? list.get(0) : list.get(1);
            Assert.notNull(val, "Incorrect filter parameter: " + key + ":[null, null]");
            tryAddIntegralKeyValueFilter(builder, entry, key, list, val);
            tryAddFloatingKeyFalueFilter(builder, entry, key, list, val);
        }
    }

    private void tryAddFloatingKeyFalueFilter(BooleanQuery.Builder builder,
            Map.Entry<String, Object> entry, String key, List list, Object val) {
        if (val instanceof Float || entry.getValue() instanceof Double) {
            builder.add(FloatPoint.newRangeQuery(key,
                    list.get(0) != null ? (Float) list.get(0) : Float.MIN_VALUE,
                    list.get(1) != null ? (Float) list.get(1) : Float.MAX_VALUE),
                    BooleanClause.Occur.MUST);
        }
    }

    private void tryAddIntegralKeyValueFilter(BooleanQuery.Builder builder,
            Map.Entry<String, Object> entry, String key, List list, Object val) {
        if (val instanceof Integer || entry.getValue() instanceof Long) {
            builder.add(IntPoint.newRangeQuery(key,
                    list.get(0) != null ? (Integer) list.get(0) : Integer.MIN_VALUE,
                    list.get(1) != null ? (Integer) list.get(1) : Integer.MAX_VALUE),
                    BooleanClause.Occur.MUST);
        }
    }

    private void tryAddFloatingFilter(BooleanQuery.Builder builder, Map.Entry<String, Object> entry,
            String key, Object val) {
        if (val instanceof Float || entry.getValue() instanceof Double) {
            builder.add(FloatPoint.newExactQuery(key, (Float) entry.getValue()),
                    BooleanClause.Occur.MUST);
        }
    }

    private void tryAddIntegeralFilter(BooleanQuery.Builder builder, Map.Entry<String, Object> entry,
            String key, Object val) {
        if (val instanceof Integer || entry.getValue() instanceof Long) {
            builder.add(IntPoint.newExactQuery(key, (Integer) entry.getValue()),
                    BooleanClause.Occur.MUST);
        }
    }

    public void setVariationTypes(FilterSection<List<VariationType>> variationTypes) {
        this.variationTypes = variationTypes;
    }

    public List<Long> getChromosomeIds() {
        return chromosomeIds;
    }

    public void setChromosomeIds(List<Long> chromosomeIds) {
        this.chromosomeIds = chromosomeIds;
    }

    public void setGenes(FilterSection<List<String>> genes) {
        this.genes = genes;
    }

    public FilterSection<List<VariationEffect>> getEffects() {
        return effects;
    }

    public void setEffects(FilterSection<List<VariationEffect>> effects) {
        this.effects = effects;
    }

    public FilterSection<List<VariationImpact>> getImpacts() {
        return impacts;
    }

    public void setImpacts(FilterSection<List<VariationImpact>> impacts) {
        this.impacts = impacts;
    }

    public String getFailedFilter() {
        return failedFilter;
    }

    public void setFailedFilter(String failedFilter) {
        this.failedFilter = failedFilter;
    }

    public FilterSection<List<VariationType>> getVariationTypes() {
        return variationTypes;
    }

    public FilterSection<List<String>> getGenes() {
        return genes;
    }

    public Map<Long, List<Long>> getVcfFileIdsByProject() {
        return vcfFileIdsByProject;
    }

    public void setVcfFileIdsByProject(Map<Long, List<Long>> vcfFileIdsByProject) {
        this.vcfFileIdsByProject = vcfFileIdsByProject;
    }

    public Map<String, Object> getAdditionalFilters() {
        return additionalFilters;
    }

    public void setAdditionalFilters(Map<String, Object> additionalFilters) {
        this.additionalFilters = additionalFilters;
    }

    public List<Float> getQuality() {
        return quality;
    }

    public void setQuality(List<Float> quality) {
        this.quality = quality;
    }

    public List<String> getAdditionalFields() {
        return infoFields;
    }

    public void setInfoFields(List<String> infoFields) {
        this.infoFields = infoFields;
    }

    public Boolean getExon() {
        return isExon;
    }

    public void setExon(Boolean exon) {
        isExon = exon;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    public Integer getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(Integer endIndex) {
        this.endIndex = endIndex;
    }

    public FilterSection<List<String>> getSampleNames() {
        return sampleNames;
    }

    public void setSampleNames(FilterSection<List<String>> sampleNames) {
        this.sampleNames = sampleNames;
    }

    public static class FilterSection<T> {

        private T field;
        private boolean conjunction = false;
        public FilterSection(T field, boolean conjunction) {
            this.field = field;
            this.conjunction = conjunction;
        }

        public FilterSection(T field) {
            this.field = field;
        }

        public FilterSection() {
            // no-op
        }

        public T getField() {
            return field;
        }

        public boolean isConjunction() {
            return conjunction;
        }

        public void setField(T field) {
            this.field = field;
        }

        public void setConjunction(boolean conjunction) {
            this.conjunction = conjunction;
        }

    }

    public List<Long> getVcfFileIds() {
        return vcfFileIdsByProject.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }
}
