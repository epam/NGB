/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

package com.epam.catgenome.manager.vcf.reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.epam.catgenome.constant.Constants;
import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.controller.vo.ga4gh.CallSetSearch;
import com.epam.catgenome.controller.vo.ga4gh.GenotypeGA4GH;
import com.epam.catgenome.controller.vo.ga4gh.VariantGA4GH;
import com.epam.catgenome.controller.vo.ga4gh.VariantSet;
import com.epam.catgenome.controller.vo.ga4gh.VariantSetMetadata;
import com.epam.catgenome.controller.vo.ga4gh.VariantsSearch;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.vcf.Filter;
import com.epam.catgenome.entity.vcf.GenotypeData;
import com.epam.catgenome.entity.vcf.OrganismType;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.exception.Ga4ghResourceUnavailableException;
import com.epam.catgenome.exception.VcfReadingException;
import com.epam.catgenome.manager.externaldb.HttpDataManager;
import com.epam.catgenome.manager.externaldb.ParameterNameValue;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.util.Utils;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;

import static com.epam.catgenome.manager.vcf.reader.VcfFileReader.isVariation;


/**
 * Source:      VcfGa4ghReader
 * Created:     14.10.16, 17:25
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
public class VcfGa4ghReader extends AbstractVcfReader {
    private HttpDataManager httpDataManager;
    private JsonMapper objectMapper = new JsonMapper();

    public VcfGa4ghReader(HttpDataManager httpDataManager, ReferenceGenomeManager referenceGenomeManager) {
        this.httpDataManager = httpDataManager;
        this.referenceGenomeManager = referenceGenomeManager;
    }

    @Override
    public Track<Variation> readVariations(VcfFile vcfFile, Track<Variation> track, Chromosome chromosome,
                           Integer sampleIndex, boolean loadInfo, final boolean collapse,
                                           EhCacheBasedIndexCache indexCache) throws VcfReadingException {
        final String start = track.getStartIndex().toString();
        final String end = track.getEndIndex().toString();
        final List<VariantGA4GH> ghList;
        try {
            ghList = getVariantsGA4GH(vcfFile.getPath() + '-' + sampleIndex, start, end,
                    chromosome.getName());
        } catch (Ga4ghResourceUnavailableException e) {
            throw new VcfReadingException(vcfFile, e);
        }
        if (ghList.isEmpty()) {
            return null;
        }
        if ((track.getScaleFactor() >= 1 && track.getEndIndex() - track.getStartIndex() < Constants.GA4GH_MAX_SIZE)
            || !collapse) {
            ArrayList<Variation> variations = new ArrayList<>();
            final VariantSet metadata = variationMetadata(vcfFile.getPath());
            createVariationsFromGA4GH(vcfFile, loadInfo, ghList, variations, metadata);
            track.setBlocks(variations);
        } else {
            track.setBlocks(loadStatisticVariations(ghList, track, loadInfo, vcfFile));
        }
        return track;
    }

    private void createVariationsFromGA4GH(VcfFile vcfFile, boolean loadInfo,
            List<VariantGA4GH> ghList, ArrayList<Variation> variations, VariantSet metadata) {
        for (VariantGA4GH ghEntity : ghList) {
            Variation variation = createVariation(ghEntity, loadInfo, vcfFile, metadata.getMetadata());
            if (isVariation(variation)) {
                variations.add(variation);
            }
        }
    }

    @Override
    public Variation getNextOrPreviousVariation(final int fromPosition, final VcfFile vcfFile,
                                                final Integer sampleIndex, final Chromosome chromosome,
                                                final boolean forward, EhCacheBasedIndexCache indexCache)
            throws VcfReadingException {

        int end = forward ? chromosome.getSize() : 0;
        if ((forward && fromPosition + 1 >= end) || (!forward && fromPosition - 1 <= end)) { // no next features
            return null;
        }
        StringBuilder builder = new StringBuilder().append(vcfFile.getPath()).append('-').append(sampleIndex);
        return forward ? getNextVariation(fromPosition, vcfFile, chromosome, end, builder) :
                getPreviousVariation(fromPosition, vcfFile, chromosome, end, builder);
    }

    @Nullable
    private Variation getPreviousVariation(int fromPosition, VcfFile vcfFile, Chromosome chromosome,
            int end, StringBuilder builder) throws VcfReadingException {
        Variation lastFeature = null;
        int i = 0;
        boolean lastChunk = false;
        while (lastFeature == null) {
            if (lastChunk) {
                break;
            }
            int firstIndex = fromPosition - Constants.PREV_FEATURE_OFFSET * (i + 1);
            int lastIndex = fromPosition - 1 - Constants.PREV_FEATURE_OFFSET * i;
            if (firstIndex < end) {
                firstIndex = end;
                lastChunk = true; // this is the last chunk to be traversed
            }
            List<VariantGA4GH> varList;
            try {
                varList = getVariantsGA4GH(builder.toString(), String.valueOf(firstIndex),
                        String.valueOf(lastIndex), chromosome.getName());
            } catch (Ga4ghResourceUnavailableException e) {
                throw new VcfReadingException(vcfFile, e);
            }
            final VariantSet variantSet = variationMetadata(vcfFile.getPath());
            lastFeature = getLastVariation(vcfFile, varList, variantSet, fromPosition);
            i++;
        }
        return lastFeature;
    }

    private Variation getLastVariation(VcfFile vcfFile, List<VariantGA4GH> varList, VariantSet variantSet,
                                       int fromPosition) {
        Variation lastFeature = null;
        for (VariantGA4GH ghEntity : varList) {
            final Variation variation = createVariation(ghEntity, false, vcfFile,
                    variantSet.getMetadata());
            if (isVariation(variation) && variation.getEndIndex() < fromPosition) {
                lastFeature = variation;
            }
        }
        return lastFeature;
    }

    @Nullable
    private Variation getNextVariation(int fromPosition, VcfFile vcfFile, Chromosome chromosome,
            int end, StringBuilder builder) throws VcfReadingException {
        List<VariantGA4GH> varList;
        try {
            varList = getVariantsGA4GH(builder.toString(), String.valueOf(fromPosition),
                    String.valueOf(end), chromosome.getName());
        } catch (Ga4ghResourceUnavailableException e) {
            throw new VcfReadingException(vcfFile, e);
        }
        VariantSet variantSet = variationMetadata(vcfFile.getPath());
        for (VariantGA4GH ghEntity : varList) {
            Variation variation = createVariation(ghEntity, false, vcfFile, variantSet.getMetadata());
            if (isVariation(variation) && variation.getStartIndex() > fromPosition) {
                return variation;
            }
        }
        return null;
    }

    private VariantSet variationMetadata(final String path) throws VcfReadingException {
        final int index = path.indexOf('-');
        VariantSet variantSet;
        final String variantSetId;
        if (index >= 0) {
            variantSetId = path.substring(0, index - 1);
        } else {
            variantSetId = path;
        }
        final String location = Constants.URL_GOOGLE_GENOMIC_API +
                Constants.URL_VARIANT_SETS +
                variantSetId +
                Constants.GOOGLE_API_KEY;
        final ParameterNameValue[] params = new ParameterNameValue[]{};
        try {
            final String geneData = httpDataManager.fetchData(location, params);
            variantSet = objectMapper.readValue(geneData, VariantSet.class);
        } catch (IOException | ExternalDbUnavailableException e) {
            throw new VcfReadingException(path, e);
        }
        return variantSet;
    }

    /**
     * Translates {@code VariantGA4GH} object into our {@code Variant} entity
     *
     * @param ghEntity a {@code VariantGA4GH} object, that presents a variation from global alliance for genomics and
     *                 health.
     * @param file     a {@code VcfFile} object, VCF file.
     * @param loadInfo {@code boolean} defines if extended information for variation should be loaded
     * @return a {@code Variation} object, representing desired variation.
     */
    private Variation createVariation(final VariantGA4GH ghEntity, final boolean loadInfo, final VcfFile file,
                                      final List<VariantSetMetadata> metadata) {
        final Map<String, GenotypeData> genotypeData = new HashMap<>();

        final Variation variation = new Variation(Integer.parseInt(ghEntity.getStart()) + 1,
                Integer.parseInt(ghEntity.getEnd()), ghEntity.getReferenceBases(), ghEntity.getAlternateBases());
        variation.setGenotypeData(genotypeData);

        if (!isVariation(variation)) {
            return variation;
        }
        final Map<String, ArrayList<String>> infoVariation = ghEntity.getInfo();
        final Map<String, Variation.InfoField> mapInfo = getInfoMap(metadata, infoVariation);

        final int altIndex = Integer.parseInt(ghEntity.getCalls().get(0).getGenotype().get(0));
        final Map<String, ArrayList<String>> infoCallSet = ghEntity.getCalls().get(0).getInfo();
        final List<Allele> alleleList = getAllelesGA4GH(ghEntity.getReferenceBases(), ghEntity.getAlternateBases());
        final List<Allele> altAlleles = getAllelesGA4GH(ghEntity.getReferenceBases(), ghEntity.getAlternateBases(),
                ghEntity.getCalls().get(0).getGenotype());
        final List<Filter> filtersList = new ArrayList<>();
        final Genotype genotype =
                getGenotype(ghEntity, file, genotypeData, altIndex, infoCallSet,
                        alleleList, altAlleles, variation, filtersList);

        variation.setInfo(mapInfo);
        variation.setFailedFilters(filtersList);
        if (loadInfo) {
            parseInfo(variation, !altAlleles.isEmpty() ? altAlleles.get(0) : null, file.getReferenceId(), genotype);
        }

        variation.setIdentifier(ghEntity.getNames().isEmpty() ? "." : ghEntity.getNames().get(0));

        final Double qual = ghEntity.getQuality();
        variation.setQuality(qual > 0 ? qual : null);
        return variation;
    }

    @NotNull private Map<String, Variation.InfoField> getInfoMap(List<VariantSetMetadata> metadata,
            Map<String, ArrayList<String>> infoVariation) {
        final Map<String, Variation.InfoField> mapInfo = new HashMap<>();
        for (Map.Entry<String, ArrayList<String>> entry : infoVariation.entrySet()) {
            final Variation.InfoField infoField = new Variation.InfoField();
            String key = entry.getKey();
            for (VariantSetMetadata varMetadata : metadata) {
                if (key.equals(varMetadata.getId())) {
                    infoField.setDescription(varMetadata.getDescription());
                    infoField.setValue(entry.getValue());
                    mapInfo.put(key, infoField);
                    break;
                }
            }
        }
        return mapInfo;
    }

    @NotNull
    private Genotype getGenotype(VariantGA4GH ghEntity, VcfFile file, Map<String, GenotypeData> genotypeData,
            int altIndex, Map<String, ArrayList<String>> infoCallSet,
            List<Allele> alleleList, List<Allele> altAlleles, Variation variation,
            List<Filter> filtersList) {
        int gq = -1;
        int dp = -1;
        int[] ad = null;
        int[] pl = null;

        for (Map.Entry<String, ArrayList<String>> entry : infoCallSet.entrySet()) {
            switch (entry.getKey()) {
                case "GD":
                    gq = Integer.parseInt(entry.getValue().get(0));
                    break;
                case "DP":
                    dp = Integer.parseInt(entry.getValue().get(0));
                    break;
                case "AD":
                    ad = convertList(entry.getValue());
                    break;
                case "PL":
                    pl = convertList(entry.getValue());
                    break;
                case "FILTER":
                    final Filter filter = new Filter();
                    filter.setValue(entry.getValue().get(0));
                    filter.setDescription(entry.getKey());
                    filtersList.add(filter);
                    break;
                default:
                    break;
            }
        }
        determineVariationType(ghEntity, variation, alleleList);
        final Genotype genotype = new GenotypeGA4GH(file.getSamples().get(0).getName(), altAlleles, false, gq, dp, ad,
                pl, null, null);
        determineOrganismType(ghEntity, genotypeData, altIndex, alleleList, genotype);
        genotypeData.values().forEach(e -> e.setGenotypeString(ghEntity.getAlternateBases().get(0)));
        return genotype;
    }

    private void determineOrganismType(VariantGA4GH ghEntity, Map<String, GenotypeData> genotypeData,
            int altIndex, List<Allele> alleleList, Genotype genotype) {
        final OrganismType organismType;
        int[] genotypeArray = null;
        switch (genotype.getType()) {
            case HOM_VAR:
                organismType = OrganismType.HOMOZYGOUS;
                genotypeArray = new int[]{altIndex, altIndex};
                break;
            case MIXED:
            case HET:
                genotypeArray = new int[2];
                organismType = determineHeterozygousGenotypeGA4GH(ghEntity, alleleList, genotypeArray);
                break;
            case UNAVAILABLE:
            case NO_CALL:
                organismType = OrganismType.NOT_SPECIFIED;
                break;
            case HOM_REF:
                organismType = OrganismType.NO_VARIATION;
                genotypeArray = new int[]{0, 0};
                break;
            default:
                organismType = OrganismType.NOT_SPECIFIED;
        }
        genotypeData.values().forEach(e -> e.setOrganismType(organismType));
        for (GenotypeData e : genotypeData.values()) {
            e.setGenotype(genotypeArray);
        }
    }

    public List<VariantGA4GH> getVariantsGA4GH(final String callSetId, final String start, final String end,
                                               final String chrName) throws Ga4ghResourceUnavailableException {
        final ArrayList<String> listCallSetId = new ArrayList<>();
        VariantsSearch referenceBasesEntity = null;
        final List<VariantGA4GH> resultVariantSearch = new ArrayList<>();
        final JSONObject jsonObject = new JSONObject();
        final String variantSetId;
        final int index = callSetId.indexOf('-');
        if (index <= 0) {
            listCallSetId.add(callSetId + "-0");
            variantSetId = callSetId;
        } else {
            listCallSetId.add(callSetId);
            variantSetId = callSetId.substring(0, index);
        }
        try {
            jsonObject.put(Constants.CALL_SET_ID, listCallSetId);
            jsonObject.put(Constants.END_POSITION, end);
            jsonObject.put(Constants.START_POSITION, start);
            jsonObject.put(Constants.PAGE_SIZE, Constants.VARIANTS_PAGE_SIZE);
            jsonObject.put(Constants.VARIANT_SET_ID, variantSetId);
            jsonObject.put(Constants.REFERENCE_NAME, chrName);
            jsonObject.put(Constants.PAGE_TOKEN, "");
            final String location = Constants.URL_GOOGLE_GENOMIC_API +
                    Constants.URL_VARIANTS +
                    Constants.URL_SEARCH +
                    Constants.GOOGLE_API_KEY;
            do {
                if (referenceBasesEntity != null) {
                    jsonObject.remove(Constants.PAGE_TOKEN);
                    jsonObject.put(Constants.PAGE_TOKEN, referenceBasesEntity.getNextPageToken());
                }
                String geneData = httpDataManager.fetchData(location, jsonObject);
                referenceBasesEntity = objectMapper.readValue(geneData, VariantsSearch.class);
                resultVariantSearch.addAll(referenceBasesEntity.getVariants());
            }
            while (!referenceBasesEntity.getNextPageToken().isEmpty());

            referenceBasesEntity.setVariants(resultVariantSearch);
        } catch (IOException | JSONException | ExternalDbUnavailableException e) {
            throw new Ga4ghResourceUnavailableException(callSetId, e);
        }

        return referenceBasesEntity.getVariants();
    }

    private List<Variation> loadStatisticVariations(final List<VariantGA4GH> ghList, final Track<Variation> track,
                                                    final boolean loadInfo, final VcfFile file)
            throws VcfReadingException {
        ArrayList<Variation> variations = new ArrayList<>();
        int step = (int) Math.ceil((double) 1 / (double) track.getScaleFactor());
        int from = track.getStartIndex();
        boolean found = false;
        int to = from + step;
        int variationCount = 0; // On small scale we need to count overlapping variations
        List<Variation> extendingVariations = new ArrayList<>(); // variations, that extend one pixel region
        VariantGA4GH lastContext = null;
        VariantSet variantSet = variationMetadata(ghList.get(0).getVariantSetId());
        for (VariantGA4GH ghEntity : ghList) {
            Variation variation = createVariation(ghEntity, loadInfo, file, variantSet.getMetadata());

            if (Integer.parseInt(ghEntity.getStart()) > to) {
                found = false;
                to = to + step < Integer.parseInt(ghEntity.getStart()) ? (Integer.parseInt(ghEntity.getStart()) +
                        step) : (to + step);
                if (!variations.isEmpty()) {

                    Variation lastVariation = findLastNotBndVariation(variations);
                    if (lastVariation != null && lastContext != null) {
                        lastVariation.setVariationsCount(variationCount);
                        if (variationCount > 1) { // set overlapping variations type to VariationType.STATISTIC
                            lastVariation.setEndIndex(Integer.parseInt(lastContext.getEnd()));
                            lastVariation.setType(VariationType.STATISTIC);
                        }
                    }

                }

                if (variation.getType() == VariationType.BND) {
                    variations.add(variation);
                    continue;
                }
                variationCount = 0;
                variations.addAll(extendingVariations);
                extendingVariations.clear();
            }

            if (Integer.parseInt(ghEntity.getEnd()) > to && Integer.parseInt(ghEntity.getStart()) >= from) {
                if (isVariation(variation)) {
                    extendingVariations.add(variation);
                }
                continue;
            }

            if (!found && isVariation(variation)) {
                variations.add(variation);
                found = true;
            }

            if (isVariation(variation)) {
                variationCount++;
            }
            lastContext = ghEntity;
        }

        if (!variations.isEmpty()) {
            Variation lastVariation = findLastNotBndVariation(variations);

            if (lastVariation != null && lastContext != null) {
                lastVariation.setVariationsCount(variationCount);
                if (variationCount > 1) { // set overlapping variations type to VariationType.STATISTIC
                    lastVariation.setType(VariationType.STATISTIC);
                    lastVariation.setEndIndex(Integer.parseInt(lastContext.getEnd()));
                }
            }
        }
        variations.addAll(extendingVariations);
        return variations;
    }

    //all Alleles ref and alt
    private List<Allele> getAllelesGA4GH(final String ref, final List<String> alts) {
        List<Allele> alleleList = new ArrayList<>();
        alleleList.add(Allele.create(ref, true));
        for (String alt : alts) {
            alleleList.add(Allele.create(alt, false));
        }
        return alleleList;
    }

    /**
     * Get allele from variation
     *
     * @param ref      references bases
     * @param alts     list alternate reference
     * @param genotype
     * @return list allele
     */
    private List<Allele> getAllelesGA4GH(final String ref, final List<String> alts, final List<String> genotype) {
        final List<Allele> alleleList = new ArrayList<>();
        final int[] mass = convertList(genotype);
        List<String> listAllAllele = new ArrayList<>();
        listAllAllele.add(ref);
        listAllAllele.addAll(alts);
        for (int i = 0; i < mass.length; i++) {
            if (mass[i] == -1) {
                mass[i] = 0;
            }
            if (mass[i] == 0) {
                alleleList.add(Allele.create(listAllAllele.get(mass[i]), true));
            } else {
                alleleList.add(Allele.create(listAllAllele.get(mass[i]), false));
            }
        }
        return alleleList;
    }

    private static OrganismType determineHeterozygousGenotypeGA4GH(VariantGA4GH ga4GHEntity, List<Allele> alleles,
                                                                   int[] genotypeArray) {
        OrganismType organismType = null;
        for (int i = 0; i < genotypeArray.length; i++) {
            if (alleles.get(i).isReference()) {
                genotypeArray[i] = 0;
                organismType = OrganismType.HETEROZYGOUS;
            } else {
                if (organismType == null) {
                    organismType = OrganismType.HETERO_VAR;
                }
                genotypeArray[i] = ga4GHEntity.getAlternateBases().indexOf(alleles.get(i)) + 1;
            }
        }

        return organismType;
    }

    private void determineVariationType(final VariantGA4GH context, final Variation variation,
                                        final List<Allele> alleles) {
        final VariantContext.Type type = setTypeVCFforGA4GH(alleles); // Determine VariationType
        switch (type) {
            case SNP:
                variation.setType(VariationType.SNV);
                break;
            case INDEL:
            case MIXED:
                variation.setType(determineInDel(context, alleles));
                break;
            case SYMBOLIC:
                parseSymbolicVariation(variation, alleles);
                break;
            case MNP:
                variation.setType(VariationType.MNP);
                break;
            default:
                variation.setType(null);
                if (variation.getGenotypeData() == null) {
                    variation.setGenotypeData(Collections.emptyMap());
                }
                variation.getGenotypeData().values().forEach(e -> e.setOrganismType(OrganismType.NO_VARIATION));
        }
    }

    private static VariationType determineInDel(VariantGA4GH context, List<Allele> alleles) {

        if (alleles == null) { // No genotype information, trying to guess by first alt allele
            return context.getAlternateBases().get(0).length() > context.getReferenceBases().length() ?
                    VariationType.INS : VariationType.DEL;
        } else {
            if (alleles.size() > 1) { // Complex deletion/insertion
                if (alleles.get(0).isReference() ^ alleles.get(1).isReference()) { // if one is reference
                    if (alleles.get(0).length() == alleles.get(1).length()) { // if it is a change
                        return alleles.get(0).length() == 1 ? VariationType.SNV : VariationType.MNP;
                    }
                    if (alleles.get(0).isReference()) { // if it is an insertion/deletion
                        return alleles.get(0).length() > alleles.get(1).length() ?
                                VariationType.DEL : VariationType.INS;
                    } else if (alleles.get(1).isReference()) {
                        return alleles.get(1).length() > alleles.get(0).length() ?
                                VariationType.DEL : VariationType.INS;
                    } else {
                        return VariationType.MIXED;
                    }
                } else {
                    if (alleles.get(0).isNonReference()) {
                        // if both are alt
                        if (alleles.get(0).length() > context.getReferenceBases().length()
                                && alleles.get(1).length() > context.getReferenceBases().length()) {
                            return VariationType.INS;
                        } else if (alleles.get(0).length() < context.getReferenceBases().length() &&
                                alleles.get(1).length() < context.getReferenceBases().length()) {
                            return VariationType.DEL;
                        } else {
                            return VariationType.MIXED;
                        }
                    } else {
                        // if both are ref
                        return VariationType.MIXED;
                    }
                }
            } else {
                return alleles.get(0).isReference() ? VariationType.DEL : VariationType.INS;
            }
        }
    }

    private static void parseSymbolicVariation(final Variation variation, final List<Allele> alleles) {
        variation.setStructural(true);
        List<Allele> allelesAlt = new ArrayList<>();
        for (Allele allele : alleles) {
            if (!allele.isReference()) {
                allelesAlt.add(allele);
            }
        }
        for (Allele alt : allelesAlt) {
            if (STRUCT_DEL_TAG.equals(alt.getDisplayString())) {
                variation.setType(VariationType.DEL);
                return;
            }
            if (STRUCT_INS_TAG.equals(alt.getDisplayString())) {
                variation.setType(VariationType.INS);
                return;
            }
            if (INV_TAG.equals(alt.getDisplayString())) {
                variation.setType(VariationType.INV);
                return;
            }
            if (alt.getDisplayString() != null && DUP_TAG.matcher(alt.getDisplayString()).matches()) {
                variation.setType(VariationType.DUP);
                return;
            }
            for (Pattern pattern : BIND_PATTERNS) {
                Matcher matcher = pattern.matcher(alt.getDisplayString());
                if (matcher.matches()) {
                    variation.setType(VariationType.BND);
                    break;
                }
            }
        }
    }

    public CallSetSearch callSetSearch(String path)
            throws JSONException, InterruptedException, IOException, ExternalDbUnavailableException {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put(Constants.PAGE_SIZE, Constants.VARIANTS_PAGE_SIZE);
        jsonObject.put(Constants.VARIANT_SET_ID, path);
        final String location = Constants.URL_GOOGLE_GENOMIC_API +
                Constants.URL_CALL_SETS +
                Constants.URL_SEARCH +
                Constants.GOOGLE_API_KEY;
        String geneData = httpDataManager.fetchData(location, jsonObject);
        return objectMapper.readValue(geneData, CallSetSearch.class);
    }

    private void parseInfo(final Variation variation, final Allele alt, final Long refId, final Genotype genotype) {
        if (genotype != null) {
            Map<String, Object> genotypeInfo = new HashMap<>();
            if (genotype.getAD() != null) {
                genotypeInfo.put("AD", genotype.getAD());
            }
            genotypeInfo.put("GQ", genotype.getGQ());
            genotypeInfo.put("DP", genotype.getDP());
            if (genotype.getPL() != null) {
                genotypeInfo.put("PL", genotype.getPL());
            }
            genotypeInfo.put("GT", genotype.getGenotypeString());
        }

        if (variation.getType() == VariationType.BND && variation.getBindInfo() == null) {
            variation.setBindInfo(new HashMap<>());
            for (Pattern pattern : BIND_PATTERNS) {
                Matcher matcher = pattern.matcher(alt.getDisplayString());
                if (matcher.matches()) {
                    String chrName = matcher.group(1);
                    Optional<Chromosome> chromosome = referenceGenomeManager.loadChromosomes(refId)
                            .stream()
                            .filter(c -> c.getName().equals(chrName) ||
                                    c.getName().equals(Utils.changeChromosomeName(chrName)))
                            .findAny();

                    variation.getBindInfo().put(BIND_CHR_ATTRIBUTE, chromosome.isPresent() ?
                            chromosome.get().getId() : chrName);
                    variation.getBindInfo().put(BIND_POS_ATTRIBUTE, matcher.group(2));
                    break;
                }
            }
        }
    }

    //detects type by alternate and references
    private VariantContext.Type setTypeVCFforGA4GH(List<Allele> alleles) {
        VariantContext.Type type;
        switch (alleles.size()) {
            case 0:
                throw new IllegalStateException("Unexpected error: requested type of VariantContext with no alleles!" +
                        this);
            case 1:
                // note that this doesn't require a reference allele.  You can be monomorphic independent of having a
                // reference allele
                type = VariantContext.Type.NO_VARIATION;
                break;
            default:
                type = determinePolymorphicType(alleles);
        }
        return type;
    }

    private static VariantContext.Type determinePolymorphicType(List<Allele> alleles) {
        VariantContext.Type typePrevious = null;
        Allele ref = null;
        for (Allele allele : alleles) {
            if (allele.isReference()) {
                ref = allele;
                continue;
            }
            // find the type of this allele relative to the reference
            VariantContext.Type type = typeOfBiallelicVariant(ref, allele);

            // for the first alternate allele, set the type to be that one
            // if the type of this allele is different from that of a previous one, assign it the MIXED type and quit
            if (typePrevious == null) {
                typePrevious = type;
            } else if (type != typePrevious) {
                typePrevious = VariantContext.Type.MIXED;
                return typePrevious;
            }


        }
        return typePrevious;
    }

    private static VariantContext.Type typeOfBiallelicVariant(Allele ref, Allele allele) {
        if (ref.isSymbolic()) {
            throw new IllegalStateException("Unexpected error: encountered a record with a symbolic reference allele");
        }
        if (allele.isSymbolic()) {
            return VariantContext.Type.SYMBOLIC;
        }
        if (ref.length() == allele.length()) {
            if (allele.length() == 1) {
                return VariantContext.Type.SNP;
            } else {
                return VariantContext.Type.MNP;
            }
        }

        return VariantContext.Type.INDEL;
    }

    private int[] convertList(final List<String> strings) {

        final int[] mass = new int[strings.size()];
        for (int i = 0; i < mass.length; i++) {
            mass[i] = Integer.parseInt(strings.get(i));
        }
        return mass;
    }
}
