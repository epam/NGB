/*
 * MIT License
 *
 * Copyright (c) 2017-2021 EPAM Systems
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

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.vcf.GenotypeData;
import com.epam.catgenome.entity.vcf.InfoItem;
import com.epam.catgenome.entity.vcf.OrganismType;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.manager.gene.GeneUtils;
import com.epam.catgenome.manager.vcf.VcfManager;
import com.epam.catgenome.manager.vcf.reader.VcfFileReader;
import com.epam.catgenome.util.NggbIntervalTreeMap;
import com.epam.catgenome.util.Utils;
import htsjdk.samtools.util.Interval;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCompoundHeaderLine;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLineCount;
import htsjdk.variant.vcf.VCFHeaderLineType;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.util.MathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.epam.catgenome.manager.vcf.reader.VcfFileReader.isEmptyStrain;

/**
 * An implementation of {@link FeatureIndexBuilder}, that indexes VCF file entries: {@link VariantContext}
 */
public class VcfFeatureIndexBuilder implements FeatureIndexBuilder<VariantContext, VcfIndexEntry> {
    private static final Logger LOGGER = LoggerFactory.getLogger(VcfFeatureIndexBuilder.class);

    private VcfFilterInfo filterInfo;
    private VCFHeader vcfHeader;
    private FeatureIndexDao featureIndexDao;

    private List<VcfIndexEntry> allEntries;

    private Map<GeneFile, NggbIntervalTreeMap<List<Gene>>> intervalMapCache = new HashMap<>();

    public VcfFeatureIndexBuilder(VcfFilterInfo filterInfo, VCFHeader vcfHeader, FeatureIndexDao featureIndexDao) {
        this.filterInfo = filterInfo;
        this.vcfHeader = vcfHeader;
        this.featureIndexDao = featureIndexDao;
        this.allEntries = new ArrayList<>();
    }

    public VCFHeader getVcfHeader() {
        return vcfHeader;
    }

    @Override public void add(VariantContext context, Map<String, Chromosome> chromosomeMap) {
        if (chromosomeMap.containsKey(context.getContig()) || chromosomeMap
                .containsKey(Utils.changeChromosomeName(context.getContig()))) {
            VcfIndexEntry masterEntry = new VcfIndexEntry();
            masterEntry.setUuid(UUID.randomUUID());
            masterEntry.setFeatureId(context.getID());
            masterEntry
                    .setChromosome(Utils.getFromChromosomeMap(chromosomeMap, context.getContig()));
            masterEntry.setStartIndex(context.getStart());
            masterEntry.setEndIndex(context.getEnd());
            masterEntry.setFeatureType(FeatureType.VARIATION);
            masterEntry.setInfo(filterInfoByWhiteList(context, filterInfo, vcfHeader));
            masterEntry.setVariantContext(context);

            double qual = context.getPhredScaledQual();
            masterEntry.setQuality(
                    MathUtils.equals(qual, VcfManager.HTSJDK_WRONG_QUALITY) ? 0D : qual);

            List<OrganismType> organismTypes = new ArrayList<>();
            for (int i = 0; i < context.getAlternateAlleles().size(); i++) {
                Variation variation = VcfFileReader.createVariation(context, vcfHeader, i);
                organismTypes.addAll(variation.getGenotypeData().values().stream()
                        .map(GenotypeData::getOrganismType).collect(Collectors.toList()));
            }

            if (!organismTypes.isEmpty() && organismTypes.stream()
                    .anyMatch(type -> type.equals(OrganismType.NO_VARIATION))) {
                return;
            }
            for (String sampleName: context.getSampleNames()) {
                if (!isEmptyStrain(context.getGenotype(sampleName))) {
                    masterEntry.setSampleName(sampleName);
                    allEntries.add(masterEntry);
                }
            }
        }
    }

    protected Map<String, Object> filterInfoByWhiteList(VariantContext context,
            VcfFilterInfo vcfFilterInfo, VCFHeader vcfHeader) {
        Map<String, Object> permittedInfo = new HashMap<>();
        Map<String, Object> info = context.getAttributes();

        vcfFilterInfo.getInfoItems().forEach(key -> {
            if (info.containsKey(key.getName())
                    && vcfHeader.getInfoHeaderLine(key.getName()) != null) {
                int count = vcfHeader.getInfoHeaderLine(key.getName()).getCount(context);
                VCFHeaderLineCount countType =
                        vcfHeader.getInfoHeaderLine(key.getName()).getCountType();
                switch (key.getType()) {
                    case Integer:
                        addNumberInfo(permittedInfo, info, count, countType,
                                VCFHeaderLineType.Integer, key);
                        break;
                    case Float:
                        addNumberInfo(permittedInfo, info, count, countType,
                                VCFHeaderLineType.Float, key);
                        break;
                    case Flag:
                        permittedInfo
                                .put(key.getName(), parseFlagInfo(permittedInfo, info, count, key));
                        break;
                    default:
                        permittedInfo.put(key.getName(), info.get(key.getName()));
                }
            }
        });

        return permittedInfo;
    }

    private void addNumberInfo(Map<String, Object> permittedInfo, Map<String, Object> info,
            int count, VCFHeaderLineCount countType, VCFHeaderLineType type, InfoItem key) {
        Object value;
        if (isVariableLength(countType)) {
            value = info.get(key.getName()).toString();
        } else if (count > 1) {
            value = parseNumberArray(type, info.get(key.getName()));

            if (value == null) {
                LOGGER.error(MessageHelper.getMessage(
                        MessagesConstants.ERROR_FEATURE_INDEX_WRITING_WRONG_PARAMETER_TYPE,
                        key.getName(), key.getType(), info.get(key.getName()).toString()));
                return;
            }

            permittedInfo.put("_" + key.getName() + "_v", info.get(key.getName()).toString());
        } else {
            String numberString = info.get(key.getName()).toString();

            if (NumberUtils.isNumber(numberString)) {
                value = parseNumber(type, info.get(key.getName()));
            } else {
                LOGGER.error(MessageHelper.getMessage(
                        MessagesConstants.ERROR_FEATURE_INDEX_WRITING_WRONG_PARAMETER_TYPE,
                        key.getName(), key.getType(), numberString));
                return;
            }
        }

        permittedInfo.put(key.getName(), value);
    }

    private Object parseNumberArray(VCFHeaderLineType type, Object infoObject) {
        switch (type) {
            case Integer:
                return Utils.parseIntArray(infoObject.toString());
            case Float:
                return Utils.parseFloatArray(infoObject.toString());
            default:
                throw new IllegalArgumentException(MessageHelper
                        .getMessage(MessagesConstants.ERROR_FEATURE_INDEX_INVALID_NUMBER_FORMAT,
                                type));
        }
    }

    private Object parseNumber(VCFHeaderLineType type, Object infoObject) {
        switch (type) {
            case Integer:
                return Integer.parseInt(infoObject.toString());
            case Float:
                return Float.parseFloat(infoObject.toString());
            default:
                throw new IllegalArgumentException(MessageHelper
                        .getMessage(MessagesConstants.ERROR_FEATURE_INDEX_INVALID_NUMBER_FORMAT,
                                type));
        }
    }

    private Object parseFlagInfo(Map<String, Object> permittedInfo, Map<String, Object> info,
            int count, InfoItem key) {
        if (count > 1) {
            permittedInfo.put("_" + key.getName()+ "_v", info.get(key.getName()).toString());
            return Utils.parseBooleanArray(info.get(key.getName()).toString());
        } else {
            return Boolean.parseBoolean(info.get(key.getName()).toString());
        }
    }

    private boolean isVariableLength(VCFHeaderLineCount countType) {
        return countType != VCFHeaderLineCount.INTEGER;
    }


    public VcfIndexEntry build(VcfIndexEntry entry, List<GeneFile> geneFiles, Chromosome chromosome) {
        int start = 0;
        int end = chromosome.getSize();

        List<VcfIndexEntry> indexEntries = fillEntryDetails(entry, geneFiles, chromosome, start, end);
        return indexEntries.get(0);
    }

    @Override public List<VcfIndexEntry> build(List<GeneFile> geneFiles, Chromosome chromosome) {
        List<VcfIndexEntry> processedEntries = new ArrayList<>();
        int start = chromosome.getSize();
        int end = 0;
        for (FeatureIndexEntry entry : allEntries) {
            start = Math.min(start, entry.getStartIndex());
            end = Math.max(end, entry.getEndIndex());
        }


        for (VcfIndexEntry indexEntry : allEntries) {
            List<VcfIndexEntry> filledEntries =
                    fillEntryDetails(indexEntry, geneFiles, chromosome, start, end);
            processedEntries
                    .addAll(filledEntries);
        }

        return processedEntries;
    }

    protected List<VcfIndexEntry> simplify(VcfIndexEntry indexEntry, Set<VariationGeneInfo> geneIds,
            String geneIdsString, String geneNamesString, Set<VariationType> types) {
        List<String> ambiguousInfoFields = vcfHeader.getInfoHeaderLines().stream()
                .filter(l -> l.getCount(indexEntry.getVariantContext()) > 1 && !isVariableLength(
                        l.getCountType())).map(VCFCompoundHeaderLine::getID)
                .collect(Collectors.toList());

        List<VcfIndexEntry> simplifiedEntries =
                simplifyVcfIndexEntries(indexEntry, indexEntry.getVariantContext(), geneIds, types,
                        geneIdsString, geneNamesString);

        return splitAmbiguousInfoFields(simplifiedEntries, ambiguousInfoFields);
    }

    private List<VcfIndexEntry> fillEntryDetails(VcfIndexEntry entry, List<GeneFile> geneFiles,
            Chromosome chromosome, int start, int end) {
        String geneIdsString = null;
        String geneNamesString = null;
        Set<VariationGeneInfo> geneIds = Collections.emptySet();

        for (GeneFile geneFile : geneFiles) {
            if (!intervalMapCache.containsKey(geneFile)) {
                intervalMapCache
                        .put(geneFile, loadGenesIntervalMap(geneFile, start, end, chromosome));
            }
            NggbIntervalTreeMap<List<Gene>> intervalMap = intervalMapCache.get(geneFile);

            geneIds = fetchGeneIdsFromBatch(intervalMap, entry.getStartIndex(),
                    entry.getEndIndex(), chromosome);
            geneIdsString =
                    geneIds.stream().map(i -> i.geneId).collect(Collectors.joining(", "));
            geneNamesString =
                    geneIds.stream().map(i -> i.geneName).collect(Collectors.joining(", "));
            entry.setExon(geneIds.stream().anyMatch(i -> i.isExon));
        }

        Set<VariationType> types = new HashSet<>();
        for (int i = 0; i < entry.getVariantContext().getAlternateAlleles().size(); i++) {
            Variation variation =
                    VcfFileReader.createVariation(entry.getVariantContext(), vcfHeader, i);
            types.add(variation.getType());
        }

        return simplify(entry, geneIds, geneIdsString, geneNamesString, types);
    }

    /**
     * Fetch gene IDs of genes, affected by variation. The variation is specified by it's start and end indexes
     *
     * @param intervalMap represents a batch loaded genes form gene file
     * @param start       a start index of the variation
     * @param end         an end index of the variation
     * @param chromosome  a {@code Chromosome}
     * @return a {@code Set} of IDs of genes, affected by the variation
     */
    private Set<VariationGeneInfo> fetchGeneIdsFromBatch(
            NggbIntervalTreeMap<List<Gene>> intervalMap, int start, int end,
            Chromosome chromosome) {
        Set<VariationGeneInfo> geneIds = getGeneIds(intervalMap, chromosome, start, start);
        if (end > start) {
            geneIds.addAll(getGeneIds(intervalMap, chromosome, end, end));
        }

        return geneIds;
    }

    private NggbIntervalTreeMap<List<Gene>> loadGenesIntervalMap(GeneFile geneFile, int start,
            int end, Chromosome chromosome) {
        final NggbIntervalTreeMap<List<Gene>> genesRangeMap = new NggbIntervalTreeMap<>();
        try {
            IndexSearchResult<FeatureIndexEntry> searchResult = featureIndexDao
                    .searchFeaturesInInterval(Collections.singletonList(geneFile), start, end,
                            chromosome);
            searchResult.getEntries().stream().filter(f -> f.getFeatureType() == FeatureType.EXON
                    || f.getFeatureType() == FeatureType.GENE).map(f -> {
                        Gene gene = new Gene();
                        gene.setFeature(f.getFeatureType().name());
                        gene.setStartIndex(f.getStartIndex());
                        gene.setEndIndex(f.getEndIndex());
                        gene.setGroupId(f.getFeatureId());
                        gene.setFeatureName(f.getFeatureName().toUpperCase());
                        return gene;
                    }).forEach(g -> {
                        Interval interval =
                                new Interval(chromosome.getName(), g.getStartIndex(), g.getEndIndex());
                        genesRangeMap.putIfAbsent(interval, new ArrayList<>());
                        genesRangeMap.get(interval).add(g);
                    });
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        genesRangeMap.setMaxEndIndex(start);
        genesRangeMap.setMinStartIndex(end);
        return genesRangeMap;
    }

    private Set<VariationGeneInfo> getGeneIds(NggbIntervalTreeMap<List<Gene>> intervalMap,
            Chromosome chromosome, int start, int end) {
        Collection<Gene> genes =
                intervalMap.getOverlapping(new Interval(chromosome.getName(), start, end)).stream()
                        .flatMap(l -> l.stream()).collect(Collectors.toList());
        Set<VariationGeneInfo> geneIds = new HashSet<>();
        if (genes != null) {
            boolean isExon = genes.stream().anyMatch(GeneUtils::isExon);
            geneIds = genes.stream().filter(GeneUtils::isGene)
                    .map(g -> new VariationGeneInfo(g.getGroupId(), g.getFeatureName(), isExon))
                    .collect(Collectors.toSet());
        }

        return geneIds;
    }

    private List<VcfIndexEntry> simplifyVcfIndexEntries(VcfIndexEntry masterEntry,
            VariantContext context, Set<VariationGeneInfo> geneIds, Set<VariationType> types,
            String geneIdsString, String geneNamesString) {
        List<VcfIndexEntry> simplifiedEntries = new ArrayList<>();
        for (VariationType type : types) {
            if (geneIds.isEmpty()) {
                if (context.getFilters().isEmpty()) {
                    VcfIndexEntry entry = new VcfIndexEntry(masterEntry);
                    entry.setVariationType(type);

                    simplifiedEntries.add(entry);
                } else {
                    simplifyFilters(masterEntry, context, simplifiedEntries, type);
                }
            } else {
                simplifyGeneIds(masterEntry, context, geneIds, geneIdsString, geneNamesString,
                        simplifiedEntries, type);
            }
        }

        return simplifiedEntries;
    }

    private void simplifyFilters(VcfIndexEntry masterEntry, VariantContext context,
            List<VcfIndexEntry> simplifiedEntries, VariationType type) {
        for (String filter : context.getFilters()) {
            VcfIndexEntry entry = new VcfIndexEntry(masterEntry);
            entry.setVariationType(type);
            entry.setFailedFilter(filter);

            simplifiedEntries.add(entry);
        }
    }

    private void simplifyGeneIds(VcfIndexEntry masterEntry, VariantContext context,
            Set<VariationGeneInfo> geneIds, String geneIdsString, String geneNamesString,
            List<VcfIndexEntry> simplifiedEntries, VariationType type) {
        for (VariationGeneInfo geneInfo : geneIds) {
            if (context.getFilters().isEmpty()) {
                VcfIndexEntry entry = new VcfIndexEntry(masterEntry);
                entry.setVariationType(type);
                entry.setGene(geneInfo.geneId);
                entry.setGeneName(geneInfo.geneName);
                entry.setGeneIds(geneIdsString);
                entry.setGeneNames(geneNamesString);

                simplifiedEntries.add(entry);
            } else {
                for (String filter : context.getFilters()) {
                    VcfIndexEntry entry = new VcfIndexEntry(masterEntry);
                    entry.setVariationType(type);
                    entry.setGene(geneInfo.geneId);
                    entry.setGeneName(geneInfo.geneName);
                    entry.setGeneIds(geneIdsString);
                    entry.setGeneNames(geneNamesString);
                    entry.setFailedFilter(filter);

                    simplifiedEntries.add(entry);
                }
            }
        }
    }

    private List<VcfIndexEntry> splitAmbiguousInfoFields(List<VcfIndexEntry> entries,
            List<String> ambigousInfoFields) {
        ArrayList<VcfIndexEntry> queue = new ArrayList<>(entries);
        List<VcfIndexEntry> simplifiedEntries = new ArrayList<>();
        for (String key : ambigousInfoFields) {
            ArrayList<VcfIndexEntry> nextIteration = new ArrayList<>();
            for (FeatureIndexEntry e : queue) {
                VcfIndexEntry vcfIndexEntry = (VcfIndexEntry) e;
                boolean found = false;

                if (vcfIndexEntry.getInfo().containsKey(key) && vcfIndexEntry.getInfo()
                        .get(key) instanceof Object[]) {
                    found = true;
                    Object[] arr = (Object[]) vcfIndexEntry.getInfo().get(key);
                    makeCopies(vcfIndexEntry, arr, nextIteration, key);
                }

                if (!found) {
                    simplifiedEntries.add(vcfIndexEntry);
                }
            }

            queue = nextIteration;
        }

        queue.addAll(simplifiedEntries);
        return queue;
    }

    private void makeCopies(VcfIndexEntry vcfIndexEntry, Object[] infoArray,
            ArrayList<VcfIndexEntry> nextIteration, String key) {
        for (Object element : infoArray) {
            VcfIndexEntry copy = new VcfIndexEntry(vcfIndexEntry);
            copy.getInfo().put(key, element);
            nextIteration.add(copy);
        }
    }

    public VcfFilterInfo getFilterInfo() {
        return filterInfo;
    }

    @Override public void clear() {
        this.allEntries.clear();
        this.intervalMapCache.clear();
    }

    protected static class VariationGeneInfo {
        protected String geneId;
        protected String geneName;
        protected boolean isExon;

        VariationGeneInfo(String geneId, String geneName, boolean isExon) {
            this.geneId = geneId;
            this.geneName = geneName;
            this.isExon = isExon;
        }

        @Override public int hashCode() {
            return geneId.hashCode();
        }

        @Override public boolean equals(Object obj) {
            return obj != null && obj.getClass() == this.getClass() && Objects
                    .equals(((VariationGeneInfo) obj).geneId, geneId);
        }
    }
}
