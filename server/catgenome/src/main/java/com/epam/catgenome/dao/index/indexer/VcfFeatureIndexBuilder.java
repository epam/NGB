/*
 * MIT License
 *
 * Copyright (c) 2017-2022 EPAM Systems
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
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.vcf.GenotypeData;
import com.epam.catgenome.entity.vcf.InfoItem;
import com.epam.catgenome.entity.vcf.OrganismType;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.manager.FeatureIndexManager;
import com.epam.catgenome.manager.GeneInfo;
import com.epam.catgenome.manager.vcf.VcfManager;
import com.epam.catgenome.manager.vcf.reader.VcfFileReader;
import com.epam.catgenome.util.NggbIntervalTreeMap;
import com.epam.catgenome.util.Utils;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCompoundHeaderLine;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLineCount;
import htsjdk.variant.vcf.VCFHeaderLineType;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.util.MathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.IndexUtils.fetchGeneIdsFromBatch;
import static com.epam.catgenome.dao.index.indexer.BigVcfFeatureIndexBuilder.getSampleNames;

/**
 * An implementation of {@link FeatureIndexBuilder}, that indexes VCF file entries: {@link VariantContext}
 */
public class VcfFeatureIndexBuilder implements FeatureIndexBuilder<VariantContext, VcfIndexEntry> {
    private static final Logger LOGGER = LoggerFactory.getLogger(VcfFeatureIndexBuilder.class);

    private VcfFilterInfo filterInfo;
    private VCFHeader vcfHeader;
    private FeatureIndexManager featureIndexManager;

    private List<VcfIndexEntry> allEntries;

    private Map<GeneFile, NggbIntervalTreeMap<List<Gene>>> intervalMapCache = new HashMap<>();

    public VcfFeatureIndexBuilder(VcfFilterInfo filterInfo,
                                  VCFHeader vcfHeader,
                                  FeatureIndexManager featureIndexManager) {
        this.filterInfo = filterInfo;
        this.vcfHeader = vcfHeader;
        this.featureIndexManager = featureIndexManager;
        this.allEntries = new ArrayList<>();
    }

    public VCFHeader getVcfHeader() {
        return vcfHeader;
    }

    @Override
    public void add(final VariantContext context, final Map<String, Chromosome> chromosomeMap) {
        if (chromosomeMap.containsKey(context.getContig()) || chromosomeMap
                .containsKey(Utils.changeChromosomeName(context.getContig()))) {
            final VcfIndexEntry masterEntry = new VcfIndexEntry();
            masterEntry.setUuid(UUID.randomUUID());
            masterEntry.setFeatureId(context.getID());
            masterEntry
                    .setChromosome(Utils.getFromChromosomeMap(chromosomeMap, context.getContig()));
            masterEntry.setStartIndex(context.getStart());
            masterEntry.setEndIndex(context.getEnd());
            masterEntry.setFeatureType(FeatureType.VARIATION);
            masterEntry.setInfo(filterInfoByWhiteList(context, filterInfo, vcfHeader));
            masterEntry.setVariantContext(context);
            masterEntry.setSampleNames(getSampleNames(context));

            final double qual = context.getPhredScaledQual();
            masterEntry.setQuality(
                    MathUtils.equals(qual, VcfManager.HTSJDK_WRONG_QUALITY) ? 0D : qual);

            final List<OrganismType> organismTypes = new ArrayList<>();
            for (int i = 0; i < context.getAlternateAlleles().size(); i++) {
                Variation variation = VcfFileReader.createVariation(context, vcfHeader, i);
                organismTypes.addAll(variation.getGenotypeData().values().stream()
                        .map(GenotypeData::getOrganismType).collect(Collectors.toList()));
            }

            if (!organismTypes.isEmpty() && organismTypes.stream()
                    .anyMatch(type -> type.equals(OrganismType.NO_VARIATION))) {
                return;
            }
            allEntries.add(masterEntry);
        }
    }

    protected Map<String, Object> filterInfoByWhiteList(final VariantContext context,
            final VcfFilterInfo vcfFilterInfo, final VCFHeader vcfHeader) {
        final Map<String, Object> permittedInfo = new HashMap<>();
        final Map<String, Object> info = context.getAttributes();

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

    private void addNumberInfo(final Map<String, Object> permittedInfo, final Map<String, Object> info,
            final int count, final VCFHeaderLineCount countType, final VCFHeaderLineType type, final InfoItem key) {
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
            final String numberString = info.get(key.getName()).toString();

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

    private Object parseNumberArray(final VCFHeaderLineType type, final Object infoObject) {
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

    private Object parseNumber(final VCFHeaderLineType type, final Object infoObject) {
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

    private Object parseFlagInfo(final Map<String, Object> permittedInfo, final Map<String, Object> info,
            final int count, final InfoItem key) {
        if (count > 1) {
            permittedInfo.put("_" + key.getName()+ "_v", info.get(key.getName()).toString());
            return Utils.parseBooleanArray(info.get(key.getName()).toString());
        } else {
            return Boolean.parseBoolean(info.get(key.getName()).toString());
        }
    }

    private boolean isVariableLength(final VCFHeaderLineCount countType) {
        return countType != VCFHeaderLineCount.INTEGER;
    }


    public VcfIndexEntry build(final VcfIndexEntry entry, final List<GeneFile> geneFiles, final Chromosome chromosome) {
        final int start = 0;
        final int end = chromosome.getSize();

        final List<VcfIndexEntry> indexEntries = fillEntryDetails(entry, geneFiles, chromosome, start, end);
        return indexEntries.get(0);
    }

    @Override
    public List<VcfIndexEntry> build(final List<GeneFile> geneFiles, final Chromosome chromosome) {
        final List<VcfIndexEntry> processedEntries = new ArrayList<>();
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

    protected List<VcfIndexEntry> simplify(final VcfIndexEntry indexEntry, final Set<GeneInfo> geneIds,
            final String geneIdsString, final String geneNamesString, final Set<VariationType> types) {
        final List<String> ambiguousInfoFields = vcfHeader.getInfoHeaderLines().stream()
                .filter(l -> l.getCount(indexEntry.getVariantContext()) > 1 && !isVariableLength(
                        l.getCountType())).map(VCFCompoundHeaderLine::getID)
                .collect(Collectors.toList());

        final List<VcfIndexEntry> simplifiedEntries =
                simplifyVcfIndexEntries(indexEntry, indexEntry.getVariantContext(), geneIds, types,
                        geneIdsString, geneNamesString);

        return splitAmbiguousInfoFields(simplifiedEntries, ambiguousInfoFields);
    }

    private List<VcfIndexEntry> fillEntryDetails(final VcfIndexEntry entry, final List<GeneFile> geneFiles,
            final Chromosome chromosome, final int start, final int end) {
        String geneIdsString = null;
        String geneNamesString = null;
        Set<GeneInfo> geneIds = Collections.emptySet();

        for (GeneFile geneFile : geneFiles) {
            if (!intervalMapCache.containsKey(geneFile)) {
                intervalMapCache.put(geneFile, featureIndexManager
                        .loadGenesIntervalMap(Collections.singletonList(geneFile), start, end, chromosome));
            }
            NggbIntervalTreeMap<List<Gene>> intervalMap = intervalMapCache.get(geneFile);

            geneIds = fetchGeneIdsFromBatch(intervalMap, entry.getStartIndex(), entry.getEndIndex(), chromosome);
            geneIdsString = geneIds.stream().map(GeneInfo::getGeneId).collect(Collectors.joining(", "));
            geneNamesString = geneIds.stream().map(GeneInfo::getGeneName).collect(Collectors.joining(", "));
            entry.setIsExon(geneIds.stream().anyMatch(GeneInfo::isExon));
        }

        final Set<VariationType> types = new HashSet<>();
        for (int i = 0; i < entry.getVariantContext().getAlternateAlleles().size(); i++) {
            Variation variation = VcfFileReader.createVariation(entry.getVariantContext(), vcfHeader, i);
            types.add(variation.getType());
        }

        return simplify(entry, geneIds, geneIdsString, geneNamesString, types);
    }

    private List<VcfIndexEntry> simplifyVcfIndexEntries(final VcfIndexEntry masterEntry, final VariantContext context,
                                                        final Set<GeneInfo> geneIds, final Set<VariationType> types,
                                                        final String geneIdsString, final String geneNamesString) {
        final List<VcfIndexEntry> simplifiedEntries = new ArrayList<>();
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
                simplifyGeneIds(masterEntry, context, geneIds, geneIdsString, geneNamesString, simplifiedEntries, type);
            }
        }

        return simplifiedEntries;
    }

    private void simplifyFilters(final VcfIndexEntry masterEntry, final VariantContext context,
            final List<VcfIndexEntry> simplifiedEntries, final VariationType type) {
        for (String filter : context.getFilters()) {
            VcfIndexEntry entry = new VcfIndexEntry(masterEntry);
            entry.setVariationType(type);
            entry.setFailedFilter(filter);

            simplifiedEntries.add(entry);
        }
    }

    private void simplifyGeneIds(final VcfIndexEntry masterEntry, final VariantContext context,
            final Set<GeneInfo> geneIds, final String geneIdsString, final String geneNamesString,
            final List<VcfIndexEntry> simplifiedEntries, final VariationType type) {
        for (GeneInfo geneInfo : geneIds) {
            if (context.getFilters().isEmpty()) {
                VcfIndexEntry entry = new VcfIndexEntry(masterEntry);
                entry.setVariationType(type);
                entry.setGene(geneInfo.getGeneId());
                entry.setGeneName(geneInfo.getGeneName());
                entry.setGeneIds(geneIdsString);
                entry.setGeneNames(geneNamesString);

                simplifiedEntries.add(entry);
            } else {
                for (String filter : context.getFilters()) {
                    VcfIndexEntry entry = new VcfIndexEntry(masterEntry);
                    entry.setVariationType(type);
                    entry.setGene(geneInfo.getGeneId());
                    entry.setGeneName(geneInfo.getGeneName());
                    entry.setGeneIds(geneIdsString);
                    entry.setGeneNames(geneNamesString);
                    entry.setFailedFilter(filter);

                    simplifiedEntries.add(entry);
                }
            }
        }
    }

    private List<VcfIndexEntry> splitAmbiguousInfoFields(final List<VcfIndexEntry> entries,
                                                         final List<String> ambigousInfoFields) {
        ArrayList<VcfIndexEntry> queue = new ArrayList<>(entries);
        final List<VcfIndexEntry> simplifiedEntries = new ArrayList<>();
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

    private void makeCopies(final VcfIndexEntry vcfIndexEntry, final Object[] infoArray,
            final ArrayList<VcfIndexEntry> nextIteration, final String key) {
        for (Object element : infoArray) {
            VcfIndexEntry copy = new VcfIndexEntry(vcfIndexEntry);
            copy.getInfo().put(key, element);
            nextIteration.add(copy);
        }
    }

    public VcfFilterInfo getFilterInfo() {
        return filterInfo;
    }

    @Override
    public void clear() {
        this.allEntries.clear();
        this.intervalMapCache.clear();
    }
}
