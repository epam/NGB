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

package com.epam.catgenome.manager.protein;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.protein.ProteinSequenceConstructRequest;
import com.epam.catgenome.manager.gene.GeneTrackManager;
import com.epam.catgenome.manager.gene.GeneUtils;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.gene.ProteinSequenceVariationQuery;
import com.epam.catgenome.controller.vo.Query2TrackConverter;
import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.protein.MrnaProteinSequenceVariants;
import com.epam.catgenome.entity.protein.ProteinSequence;
import com.epam.catgenome.entity.protein.ProteinSequenceEntry;
import com.epam.catgenome.entity.protein.ProteinSequenceInfo;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.exception.GeneReadingException;
import com.epam.catgenome.manager.TrackHelper;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;

/**
 * Created: 2/2/2016
 * Project: CATGenome Browser
 *
 * <p>
 * A service class, that manages ProteinSequence entities
 * </p>
 */
@Service
public class ProteinSequenceManager {

    private static final String TRANSCRIPT_ID_FILED = "transcript_id";

    @Autowired
    private GeneTrackManager geneTrackManager;

    @Autowired
    private TrackHelper trackHelper;

    @Autowired
    private ProteinSequenceReconstructionManager psReconstructionManager;

    @Value("${gene.translation.seq.tags:translation_seq}")
    private List<String> translationAttrTags;

    /**
     * Load protein sequence for specified track (start and end indexes, gene item id, reference genome).
     *
     * @param geneTrack track
     * @return track of protein sequences
     * @throws GeneReadingException if errors occurred during working with gene file
     */
    @Transactional(propagation = Propagation.REQUIRED)
    @Cacheable(cacheNames = "proteinTrack", key = "#geneTrack.proteinCacheKey(#referenceId)",
            unless = "#result == null") //TODO: remove?
    public Track<ProteinSequenceInfo> loadProteinSequence(final Track<Gene> geneTrack, final Long referenceId)
        throws GeneReadingException {
        Assert.notNull(referenceId, MessageHelper.getMessage(MessagesConstants.ERROR_REFERENCE_ID_NULL));
        Chromosome chromosome = trackHelper.validateTrack(geneTrack);

        Map<Gene, List<ProteinSequenceEntry>> proteinSequences = psReconstructionManager
                .reconstructProteinSequence(geneTrackManager.loadGenes(geneTrack, false),
                        chromosome, referenceId, false, true);

        Track<ProteinSequenceInfo> track = new Track<>(geneTrack);
        List<ProteinSequenceInfo> blocks = new ArrayList<>(proteinSequences.size());

        for (Map.Entry<Gene, List<ProteinSequenceEntry>> mrnaEntry : proteinSequences.entrySet()) {
            List<ProteinSequenceEntry> psEntryList = mrnaEntry.getValue();
            List<ProteinSequence> psList =
                    psEntryList.stream().map(ProteinSequence::new).collect(Collectors.toList());
            String transcriptId = mrnaEntry.getKey().getAttributes().get(TRANSCRIPT_ID_FILED);
            if (StringUtils.isNotEmpty(transcriptId)) {
                blocks.add(new ProteinSequenceInfo(geneTrack.getStartIndex(), geneTrack.getEndIndex(), transcriptId,
                        psList));
            }
        }

        track.setBlocks(blocks);
        return track;
    }

    /**
     * Load protein sequences for specified track (start and end indexes, gene item id, reference genome),
     * mapped to transcripts
     *
     * @param geneTrack a gene track to load protein sequences for
     * @param referenceId a reference ID to load protein sequences from
     * @param collapsedTrack indicates if a track is collapsed
     * @return a map of protein sequences to transcripts
     * @throws GeneReadingException
     */
    public Map<Gene, List<ProteinSequenceEntry>> loadProteinSequenceWithoutGrouping(
            final Track<Gene> geneTrack, final Long referenceId,
            final boolean collapsedTrack, final boolean extendCds)
        throws GeneReadingException {
        Assert.notNull(referenceId, MessageHelper.getMessage(MessagesConstants.ERROR_REFERENCE_ID_NULL));
        Chromosome chromosome = trackHelper.validateTrack(geneTrack);

        return psReconstructionManager.reconstructProteinSequence(geneTrack, chromosome, referenceId,
                collapsedTrack, extendCds);
    }

    public List<ProteinSequenceEntry> loadCdsProteinSequence(final Track<Gene> geneTrack,
                                                             final Gene cds,
                                                             final Long referenceId) {
        Assert.notNull(referenceId, MessageHelper.getMessage(MessagesConstants.ERROR_REFERENCE_ID_NULL));
        Chromosome chromosome = trackHelper.validateTrack(geneTrack);

        return psReconstructionManager.reconstructCdsProteinSequence(geneTrack, cds, chromosome, referenceId);
    }

    /**
     * Load protein sequence for gene track, taking into account variations.
     *
     * @param psVariationQuery query
     * @param referenceId      reference id
     * @return list of possible protein sequence tracks
     * @throws GeneReadingException   if error occurred during working with reference or gene files
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Track<MrnaProteinSequenceVariants> loadProteinSequenceWithVariations(final ProteinSequenceVariationQuery
                                                                                        psVariationQuery,
                                                                                final Long referenceId)
        throws GeneReadingException {
        TrackQuery trackQuery = psVariationQuery.getTrackQuery();
        Track<ProteinSequence> track = Query2TrackConverter.convertToTrack(trackQuery);
        Chromosome chromosome = trackHelper.validateTrack(track);
        Track<Gene> geneTrack = geneTrackManager.loadGenes(Query2TrackConverter.convertToTrack(trackQuery), false);

        // Check if variations changes CDS.
        Map<Gene, List<Gene>> mrnaToCdsMap = psReconstructionManager.loadCds(geneTrack, chromosome, false);
        Set<Gene> allCds = new HashSet<>();
        mrnaToCdsMap.values().stream().forEach(allCds::addAll);
        Map<Variation, List<Gene>> intersections = findIntersections(psVariationQuery.getVariations(), allCds);

        // Change mRNA according to variations and load new protein sequences.
        Map<Gene, List<List<Sequence>>> cdsToNucleotidesMap;
        try {
            cdsToNucleotidesMap =
                psReconstructionManager.loadNucleotidesForReferenceVariationCds(chromosome, referenceId, intersections);
        } catch (IOException e) {
            throw new GeneReadingException(geneTrack, e);
        }

        HashMap<Gene, List<Gene>> mrnaToVarCdsMap = makeMrnaToVarCdsMap(mrnaToCdsMap, allCds, cdsToNucleotidesMap);
        Map<Gene, List<List<ProteinSequenceEntry>>> mrnaToAminoAcidsMap = loadProteinSequencesByVarCds(mrnaToVarCdsMap,
                                                               cdsToNucleotidesMap, referenceId, geneTrack, chromosome);

        Map<String, List<List<ProteinSequence>>> blocks = new HashMap<>(mrnaToAminoAcidsMap.size());
        for (Map.Entry<Gene, List<List<ProteinSequenceEntry>>> mrnaToAminoAcidsEntry : mrnaToAminoAcidsMap.entrySet()) {
            List<List<ProteinSequenceEntry>> psEntryList = mrnaToAminoAcidsEntry.getValue();
            List<List<ProteinSequence>> lst = new ArrayList<>();
            for (List<ProteinSequenceEntry> psEntry : psEntryList) {
                lst.add(psEntry.stream().map(ProteinSequence::new).collect(Collectors.toList()));
            }
            String transcriptId = mrnaToAminoAcidsEntry.getKey().getAttributes().get(TRANSCRIPT_ID_FILED);
            if (StringUtils.isNotEmpty(transcriptId)) {
                blocks.put(transcriptId, lst);
            }

        }

        Track<MrnaProteinSequenceVariants> result = new Track<>();
        result.setBlocks(Collections
                .singletonList(new MrnaProteinSequenceVariants(track.getStartIndex(), track.getEndIndex(), blocks)));

        return result;
    }

    @SneakyThrows
    @Transactional
    public ProteinSequence loadProteinSequence(final ProteinSequenceConstructRequest request) {
        final Track<Gene> geneTrack = Query2TrackConverter.convertToTrack(request.getTrackQuery());
        final Track<Gene> genes = geneTrackManager.loadGenes(geneTrack, false);

        final Optional<Gene> geneToTranslate = Optional.ofNullable(
                lookForGene(genes.getBlocks(), request.getFeatureId(), request.getFeatureType())
        );
        final Optional<ProteinSequenceEntry> selfTranslation = geneToTranslate
                .flatMap(gene -> Optional.ofNullable(gene.getAttributes()))
                .flatMap(a ->
                        CollectionUtils.intersection(a.keySet(), translationAttrTags)
                                .stream()
                                .findFirst()
                                .map(a::get))
                .map(t -> new ProteinSequenceEntry(t, 0L,  0L, t.length() - 1L,
                        (long) request.getTrackQuery().getStartIndex(), (long) request.getTrackQuery().getEndIndex()));
        if (selfTranslation.isPresent()) {
            return GeneUtils.constructProteinString(Collections.singletonList(selfTranslation.get()),
                    isGeneOnReverseStrand(geneToTranslate));
        }

        if (request.getFeatureType() == FeatureType.CDS) {
            if (geneToTranslate.isPresent()) {
                return GeneUtils.constructProteinString(
                        loadCdsProteinSequence(geneTrack, geneToTranslate.get(), request.getReferenceId()),
                        isGeneOnReverseStrand(geneToTranslate));
            } else {
                throw new IllegalArgumentException(MessageHelper.getMessage(
                        MessagesConstants.ERROR_CANT_FIND_TRANSCRIPT, request.getFeatureId(),
                        request.getFeatureType()));
            }
        }

        final String transcript = resolveFeatureNameForTranslation(request, geneTrack);
        return loadProteinSequenceWithoutGrouping(genes, request.getReferenceId(), false, false)
                .entrySet().stream()
                .filter(e -> e.getKey().getFeatureName().equalsIgnoreCase(transcript))
                .findFirst()
                .map(e -> GeneUtils.constructProteinString(e.getValue(),
                        isGeneOnReverseStrand(Optional.ofNullable(e.getKey()))))
                .orElseThrow(() -> new IllegalArgumentException(MessageHelper.getMessage(
                        MessagesConstants.ERROR_CANT_FIND_TRANSCRIPT, request.getFeatureId(),
                        request.getFeatureType())));
    }

    private boolean isGeneOnReverseStrand(final Optional<Gene> geneToTranslate) {
        return geneToTranslate
                .map(Gene::getStrand)
                .map(s -> StrandSerializable.NEGATIVE == s)
                .orElse(false);
    }

    // If we got MRNA -> we just take featureId form request, or else if we gote GENE feature, we need to choose
    // canonical transcript, for any other types we can't build any aminoacid sequence
    private String resolveFeatureNameForTranslation(final ProteinSequenceConstructRequest request,
                                                    final Track<Gene> geneTrack) throws GeneReadingException {
        final String transcript;
        if (request.getFeatureType() == FeatureType.GENE) {
            transcript = geneTrackManager.loadGenesTranscript(geneTrack, null, null)
                    .getBlocks()
                    .stream()
                    .filter(geneTranscript -> geneTranscript.getFeatureName().equalsIgnoreCase(request.getFeatureId()))
                    .findFirst()
                    .map(GeneUtils::getCanonical)
                    .map(Gene::getFeatureName)
                    .orElseThrow(() -> new IllegalArgumentException(
                            MessageHelper.getMessage(
                                    MessagesConstants.ERROR_CANT_FIND_TRANSCRIPT, request.getFeatureId(),
                                    request.getFeatureType())
                    ));

        } else if (request.getFeatureType() == FeatureType.MRNA) {
            transcript = request.getFeatureId();
        } else {
            throw new IllegalArgumentException(
                    MessageHelper.getMessage(MessagesConstants.ERROR_WRONG_FEATURE_TYPE_FOR_PROTEIN_SEQ));
        }
        return transcript;
    }

    private Gene lookForGene(final List<Gene> genes, final String geneName, final FeatureType featureType) {
        for (Gene gene : ListUtils.emptyIfNull(genes)) {
            if (featureType.name().equalsIgnoreCase(gene.getFeature()) && geneName.equals(gene.getFeatureName())) {
                return gene;
            } else if (CollectionUtils.isNotEmpty(gene.getItems())) {
                final Gene childGene = lookForGene(gene.getItems(), geneName, featureType);
                if (childGene != null) {
                    return childGene;
                }
            }
        }
        return null;
    }

    private Map<Gene, List<List<ProteinSequenceEntry>>> loadProteinSequencesByVarCds(
                                                                            HashMap<Gene, List<Gene>> mrnaToVarCdsMap,
                                                 Map<Gene, List<List<Sequence>>> cdsToNucleotidesMap, long referenceId,
                                             Track<Gene> geneTrack, Chromosome chromosome) throws GeneReadingException {
        Map<Gene, List<List<ProteinSequenceEntry>>> mrnaToAminoAcidsMap = new HashMap<>();
        for (Map.Entry<Gene, List<Gene>> mrnaToVarCdsEntry : mrnaToVarCdsMap.entrySet()) {
            // Load protein sequences.
            try {
                reconstructProteinSequenceVariants(referenceId, geneTrack, chromosome, cdsToNucleotidesMap,
                                                   mrnaToAminoAcidsMap, mrnaToVarCdsEntry.getKey(),
                                                   mrnaToVarCdsEntry.getValue());
            } catch (IOException e) {
                throw new GeneReadingException(geneTrack, e);
            }
        }

        return mrnaToAminoAcidsMap;
    }

    private HashMap<Gene, List<Gene>> makeMrnaToVarCdsMap(Map<Gene, List<Gene>> mrnaToCdsMap, Set<Gene> allCds,
                                                          Map<Gene, List<List<Sequence>>> cdsToNucleotidesMap) {
        HashMap<Gene, List<Gene>> mrnaToVarCdsMap = new HashMap<>();
        List<Gene> cdsListNoDuplicates = removeCdsDuplicates(allCds, cdsToNucleotidesMap);

        for (Map.Entry<Gene, List<Gene>> mrnaCdsEntry : mrnaToCdsMap.entrySet()) {
            List<Gene> variationCdsList = new ArrayList<>();
            for (Gene cds : cdsListNoDuplicates) {
                List<Gene> cdsList = mrnaCdsEntry.getValue();
                List<Gene> collect = cdsList.stream().filter(c -> c.getStartIndex().equals(cds.getStartIndex()) &&
                                                                  c.getEndIndex().equals(cds.getEndIndex()))
                    .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(collect)) {
                    variationCdsList.add(cds);
                }
            }
            mrnaToVarCdsMap.put(mrnaCdsEntry.getKey(), variationCdsList);
        }

        return mrnaToVarCdsMap;
    }

    private void reconstructProteinSequenceVariants(final Long referenceId, final Track<Gene> geneTrack,
                                                    final Chromosome chromosome,
                                                    final Map<Gene, List<List<Sequence>>> cdsToNucleotidesMap,
                                                    final Map<Gene, List<List<ProteinSequenceEntry>>>
                                                            mrnaToAminoAcidsMap,
                                                    final Gene mrna, final List<Gene> cdses) throws IOException {
        Map<Gene, List<List<Sequence>>> cdsToPossibleNucleotideSeqs = new HashMap<>();
        List<Integer> frames = new ArrayList<>();
        for (Gene cds : cdses) {
            frames.add(cds.getFrame());
            List<List<Sequence>> variants = cdsToNucleotidesMap.get(cds);
            if (variants == null) {
                variants = psReconstructionManager.loadNucleotidesForReferenceCds(chromosome, referenceId,
                        Collections.singletonList(cds));
            }
            cdsToPossibleNucleotideSeqs.put(cds, variants);
        }

        List<List<ImmutablePair<Gene, List<Sequence>>>> nucleotideVariants = makeNucleatideVariantsList(cdses,
                                                                                        cdsToPossibleNucleotideSeqs);

        List<List<ProteinSequenceEntry>> variantList = new ArrayList<>();
        for (List<ImmutablePair<Gene, List<Sequence>>> nucleotideVariant : nucleotideVariants) {
            List<List<Sequence>> cdsNucleotides =
                    nucleotideVariant.stream().map(ImmutablePair::getValue).collect(Collectors.toList());


            Map<Gene, List<ProteinSequenceEntry>> cdsToAminoAcidsMap = psReconstructionManager.getAminoAcids(geneTrack,
                    cdses, cdsNucleotides, frames, true);
            List<ProteinSequenceEntry> aminoAcids = new ArrayList<>();
            cdsToAminoAcidsMap.values().stream().forEach(aminoAcids::addAll);
            variantList.add(aminoAcids);
        }
        mrnaToAminoAcidsMap.put(mrna, variantList);
    }

    private List<List<ImmutablePair<Gene, List<Sequence>>>> makeNucleatideVariantsList(final List<Gene> cdses,
                                                        Map<Gene, List<List<Sequence>>> cdsToPossibleNucleotideSeqs) {
        boolean isNegative = StrandSerializable.NEGATIVE.equals(cdses.get(0).getStrand());
        return combineData(cdsToPossibleNucleotideSeqs, (o1, o2) -> {
            if (o1.getStartIndex() < o2.getStartIndex()) {
                return isNegative ? 1 : -1;
            } else if (o1.getStartIndex() > o2.getStartIndex()) {
                return isNegative ? -1 : 1;
            } else {
                return 0;
            }
        });
    }

    private Map<Variation, List<Gene>> findIntersections(final Track<Variation> variations, final Set<Gene> allCds) {
        Map<Variation, List<Gene>> intersections = new HashMap<>();
        for (Variation variation : variations.getBlocks()) {
            List<Gene> currIntersections = allCds.stream().filter(geneFeature ->
                    variation.getStartIndex() >= geneFeature.getStartIndex() && variation.getStartIndex() <= geneFeature
                            .getEndIndex()).collect(
                    Collectors.toList());
            if (CollectionUtils.isNotEmpty(currIntersections)) {
                intersections.put(variation, currIntersections);
            }
        }
        return intersections;
    }

    private ArrayList<Gene> removeCdsDuplicates(final Set<Gene> allCdsList,
                                                final Map<Gene, List<List<Sequence>>> alternativeNucleotides) {
        ArrayList<Gene> variationCds = new ArrayList<>();
        variationCds.addAll(alternativeNucleotides.keySet());
        Set<Gene> helpAllCdsList = allCdsList;
        // Remove duplicates from all cds list.
        for (Gene cds : variationCds) {
            helpAllCdsList = allCdsList.stream()
                .filter(geneFeature -> !geneFeature.getStartIndex().equals(cds.getStartIndex()) && !geneFeature
                            .getEndIndex().equals(cds.getEndIndex())).collect(Collectors.toSet());
        }
        variationCds.addAll(helpAllCdsList);
        return variationCds;
    }

    private List<List<ImmutablePair<Gene, List<Sequence>>>> combineData(
            final Map<Gene, List<List<Sequence>>> data,
            final Comparator<Gene> comparator) {
        List<List<ImmutablePair<Gene, List<Sequence>>>> source =
                data.entrySet().stream().sorted((e1, e2) -> comparator.compare(e1.getKey(), e2.getKey()))
                        .map(e -> e.getValue().stream().map(s -> new ImmutablePair<>(e.getKey(), s))
                                .collect(Collectors.toList())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(source)) {
            return Collections.emptyList();
        }
        List<List<ImmutablePair<Gene, List<Sequence>>>> start = new ArrayList<>();
        for (ImmutablePair<Gene, List<Sequence>> p : source.remove(0)) {
            List<ImmutablePair<Gene, List<Sequence>>> ll = new ArrayList<>();
            ll.add(p);
            start.add(ll);
        }
        return recursiveCombine(start, source);
    }

    private List<List<ImmutablePair<Gene, List<Sequence>>>> recursiveCombine(
            List<List<ImmutablePair<Gene, List<Sequence>>>> acc,
            List<List<ImmutablePair<Gene, List<Sequence>>>> source) {
        if (source.isEmpty()) {
            return acc;
        }
        List<ImmutablePair<Gene, List<Sequence>>> nextLevel = source.remove(0);
        List<List<ImmutablePair<Gene, List<Sequence>>>> newAcc = new ArrayList<>();
        for (ImmutablePair<Gene, List<Sequence>> p : nextLevel) {
            for (List<ImmutablePair<Gene, List<Sequence>>> list : acc) {
                List<ImmutablePair<Gene, List<Sequence>>> newList = new ArrayList<>();
                newList.addAll(list);
                newList.add(p);
                newAcc.add(newList);
            }
        }
        return recursiveCombine(newAcc, source);
    }
}
