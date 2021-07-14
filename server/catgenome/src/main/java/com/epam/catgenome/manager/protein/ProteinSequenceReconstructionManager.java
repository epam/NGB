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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.protein.ProteinSequenceEntry;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.exception.GeneReadingException;
import com.epam.catgenome.manager.gene.GeneFileManager;
import com.epam.catgenome.manager.gene.GeneUtils;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.epam.catgenome.manager.reference.ReferenceManager;
import com.epam.catgenome.util.ProteinSequenceUtils;
import com.epam.catgenome.util.Utils;

/**
 * Created: 2/11/2016
 * Project: CATGenome Browser
 *
 * <p>
 * A service class, that contains logic for protein reconstruction tasks
 * </p>
 */
@Service
public class ProteinSequenceReconstructionManager {

    private static final int TRIPLE_LENGTH = 3;

    @Autowired
    private GffManager gffManager;

    @Autowired
    private ReferenceManager referenceManager;

    @Autowired
    private GeneFileManager geneFileManager;

    private static final List TRANSCRIPTS_NAMES = Arrays.asList("mrna", "transcript");
    private static final List CDS_NAMES = Arrays.asList("cds", "stop_codon");

    private static final Logger LOGGER = LoggerFactory.getLogger(ProteinSequenceReconstructionManager.class);

    /**
     * Reconstruct protein sequence for specified gene track.
     *
     * @param geneTrack   gene track
     * @param chromosome  chromosome
     * @param referenceId reference genome id
     * @return map of mRNA to corresponding reconstructed protein sequences
     * @throws IOException if error occurred while working with files
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Map<Gene, List<ProteinSequenceEntry>> reconstructProteinSequence(
            final Track<Gene> geneTrack, final Chromosome chromosome, final Long referenceId,
            final boolean collapsedTrack, final boolean extendCds) throws GeneReadingException {

        // Load CDS from gene file in specified interval [startIndex, endIndex].
        double time1 = Utils.getSystemTimeMilliseconds();
        Map<Gene, List<Gene>> mrnaToCdsMap = loadCds(geneTrack, chromosome, collapsedTrack);
        double time2 = Utils.getSystemTimeMilliseconds();
        LOGGER.info("loading CDS took {} ms", time2 - time1);

        time1 = Utils.getSystemTimeMilliseconds();
        ConcurrentMap<Gene, List<ProteinSequenceEntry>> mrnaToAminoAcidsMap = new ConcurrentHashMap<>();
        for (Map.Entry<Gene, List<Gene>> mrnaToCdsEntry : mrnaToCdsMap.entrySet()) {
            List<Gene> cdsList = mrnaToCdsEntry.getValue();
            List<Integer> frames = cdsList.stream().map(Gene::getFrame).collect(Collectors.toList());
            List<List<Sequence>> cdsNucleotides = loadCdsNucleatides(cdsList, referenceId, chromosome);
            if (cdsNucleotides == null) {
                continue;
            }

            // Convert nucleotide triple -> amino acid for all CDS.
            Map<Gene, List<ProteinSequenceEntry>> cdsToAminoAcidsMap = getAminoAcids(geneTrack, cdsList, cdsNucleotides,
                                                                                     frames, extendCds);

            List<ProteinSequenceEntry> aminoAcids = new ArrayList<>();
            cdsToAminoAcidsMap.values().stream().forEach(aminoAcids::addAll);
            mrnaToAminoAcidsMap.put(mrnaToCdsEntry.getKey(), aminoAcids);
        }
        time2 = Utils.getSystemTimeMilliseconds();
        LOGGER.info("protein sequence reconstruction took {} ms", time2 - time1);

        return mrnaToAminoAcidsMap;
    }

    public List<ProteinSequenceEntry> reconstructCdsProteinSequence(final Track<Gene> geneTrack, final Gene cds,
                                                                    final Chromosome chromosome,
                                                                    final Long referenceId) {
        final List<Gene> cdsList = Collections.singletonList(cds);
        final List<Integer> frames = cdsList.stream().map(Gene::getFrame).collect(Collectors.toList());
        final List<List<Sequence>> cdsNucleotides = loadCdsNucleatides(cdsList, referenceId, chromosome);
        return getAminoAcids(geneTrack, cdsList, cdsNucleotides, frames, false)
                .values().stream().flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * Load CDS from gene file in specified interval [startIndex, endIndex].
     *
     * @param geneTrack  gene track
     * @param chromosome chromosome
     * @return map of mRNA to list of its CDS
     * @throws IOException ir error occurred during working with gene file
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Map<Gene, List<Gene>> loadCds(final Track<Gene> geneTrack, final Chromosome chromosome, boolean
            collapsedTrack) throws GeneReadingException {
        List<Gene> blocks = geneTrack.getBlocks(); //chromosome
        blocks = blocks.stream()        // removed call to Utils.isFullyOnTrack to fix some bugs
                .filter(block -> !GeneUtils.isChromosome(block) && GeneUtils.belongsToChromosome(block, chromosome))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(blocks)) {
            return Collections.emptyMap();
        }

        GeneFile geneFile = geneFileManager.load(geneTrack.getId());

        return  fillCdsMap(blocks, geneTrack, geneFile, chromosome, collapsedTrack);
    }

    /**
     * Constructs amino acid sequences for specified gene track
     *
     * @param track a track to create sequences
     * @param cdsList a list of CDS blocks
     * @param cdsNucleotides a list od CDS sequences
     * @param frames a list of CDS's frames
     * @return a map of protein sequences to CDS
     */
    public Map<Gene, List<ProteinSequenceEntry>> getAminoAcids(final Track<Gene> track, final List<Gene> cdsList,
                                                               final List<List<Sequence>> cdsNucleotides,
                                                               final List<Integer> frames,
                                                               final boolean extendCDS) {
        if (CollectionUtils.isEmpty(cdsList) || CollectionUtils.isEmpty(cdsNucleotides)) {
            return Collections.emptyMap();
        }
        double time1 = Utils.getSystemTimeMilliseconds();
        Map<Gene, List<ProteinSequenceEntry>> proteinSequences = new HashMap<>();
        //if gene is on the negative strand, we should process it from the end
        checkAndRevert(cdsList, cdsNucleotides, frames);

        MutableInt aminoAcidCounter = new MutableInt(0);
        for (int i = 0; i < cdsNucleotides.size(); i++) {
            List<Sequence> nucleotides = cdsNucleotides.get(i);
            Gene cds = cdsList.get(i);

            int extendedStart = 0;
            int frame = frames.get(i);
            if (frame > 0 && i != 0) {
                //restore the part of the triplet from the previous nucleotides
                List<Sequence> prev = cdsNucleotides.get(i - 1);
                int prevNucleotides =  TRIPLE_LENGTH - frame;
                if (prev.size() >= prevNucleotides) {
                    List<Sequence> nucleotidesExtended = new ArrayList<>();
                    nucleotidesExtended.addAll(prev.subList(prev.size() - prevNucleotides, prev.size()));
                    nucleotidesExtended.addAll(nucleotides);
                    nucleotides = nucleotidesExtended;
                    extendedStart = -prevNucleotides;
                }
            } else {
                nucleotides = nucleotides.subList(frame, nucleotides.size());
            }

            // Construct amino acids from nucleotide triples.
            List<List<Sequence>> tripleList = ListUtils.partition(nucleotides, TRIPLE_LENGTH);
            List<ProteinSequenceEntry> value =
                    reconstructAminoAcidByTriples(track, cds, cdsNucleotides, i, tripleList,
                                                  extendedStart, aminoAcidCounter, extendCDS);

            proteinSequences.putIfAbsent(cds, value);

        }

        double time2 = Utils.getSystemTimeMilliseconds();
        LOGGER.debug("Get amino acids {}:{} ms", Thread.currentThread().getName(), time2 - time1);

        return proteinSequences;
    }

    /**
     * Load nucleotide sequence for reference CDS.
     *
     * @param chromosome  chromosome
     * @param referenceId reference id
     * @param cdsList     list of cds
     * @return list of nucleotide sequences
     * @throws IOException if errors occurred during working with files
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public List<List<Sequence>> loadNucleotidesForReferenceCds(final Chromosome chromosome, final Long referenceId,
                                                               final List<Gene> cdsList) throws IOException {
        if (CollectionUtils.isEmpty(cdsList)) {
            return Collections.emptyList();
        }
        double time1 = Utils.getSystemTimeMilliseconds();
        boolean isNegative = StrandSerializable.NEGATIVE.equals(cdsList.get(0).getStrand());
        List<List<Sequence>> cdsNucleotides = new ArrayList<>(cdsList.size());

        int startIndex = chromosome.getSize();
        int endIndex = 1;
        for (Gene cds : cdsList) {
            if (cds.getStartIndex() <= startIndex) {
                startIndex = cds.getStartIndex();
            }
            if (cds.getEndIndex() >= endIndex) {
                endIndex = cds.getEndIndex();
            }
        }

        String referenceString = referenceManager.getSequenceString(startIndex, endIndex, referenceId,
                chromosome.getName());

        for (Gene cds : cdsList) {
            String nucleotides = referenceString.substring(cds.getStartIndex() - startIndex, cds.getEndIndex() -
                    startIndex + 1);

            if (isNegative) {
                nucleotides = ProteinSequenceUtils.reverseComplement(nucleotides);
            }

            cdsNucleotides.add(ProteinSequenceUtils.breakSequenceString(nucleotides, cds.getStartIndex(), isNegative));
        }

        double time2 = Utils.getSystemTimeMilliseconds();
        LOGGER.debug("Loading nucleotides for reference cds {}:{} ms", Thread.currentThread().getName(), time2 - time1);
        return cdsNucleotides;
    }


    /**
     * Loads nucleotide sequences for specified CDS with variations.
     *
     * @param chromosome        chromosome
     * @param referenceId       reference id
     * @param variationToCdsMap map of pairs {Variation, List of CDS(-es), changed by this variation}
     * @return map of pairs {CDS, List of possible nucleotide sequences}
     * @throws IOException            if errors occurred during working with reference file
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Map<Gene, List<List<Sequence>>> loadNucleotidesForReferenceVariationCds(final Chromosome chromosome,
                       final Long referenceId, final Map<Variation, List<Gene>> variationToCdsMap)throws IOException {
        double time1 = Utils.getSystemTimeMilliseconds();
        Map<Gene, List<List<Sequence>>> cdsToAlternativeNucleotidesMap = new HashMap<>();

        for (Map.Entry<Variation, List<Gene>> variationToCdsEntry : variationToCdsMap.entrySet()) {
            List<Gene> cdsList = variationToCdsEntry.getValue();
            for (Gene cds : cdsList) {
                List<List<Sequence>> nucleotideSequences;

                if (cdsToAlternativeNucleotidesMap.containsKey(cds)) {
                    // If nucleotide sequence was already changed by another variation,
                    // work with changed nucleotide sequence.
                    nucleotideSequences = cdsToAlternativeNucleotidesMap.get(cds);
                } else {
                    // Load nucleotideSequences for CDS.
                    LOGGER.debug("Try read reference. Start:" + cds.getStartIndex() + ", end: " + cds.getEndIndex());
                    nucleotideSequences = Collections.singletonList(referenceManager.getNucleotidesFromNibFile(
                                cds.getStartIndex(), cds.getEndIndex(), referenceId, chromosome.getName()));
                }

                List<List<Sequence>> cdsNucleotides = new ArrayList<>(cdsList.size());
                for (List<Sequence> nucleotideSeq : nucleotideSequences) {
                    // Take into account variations on nucleotide sequences.
                    changeMRnaByVariations(variationToCdsEntry.getKey(), cdsNucleotides, nucleotideSeq);
                }
                if (!cdsToAlternativeNucleotidesMap.containsKey(cds) || CollectionUtils.isNotEmpty(cdsNucleotides)) {
                    cdsToAlternativeNucleotidesMap.put(cds, cdsNucleotides);
                }
            }
        }

        Map<Gene, List<List<Sequence>>> geneListMap = processStrand(cdsToAlternativeNucleotidesMap);
        double time2 = Utils.getSystemTimeMilliseconds();
        LOGGER.debug("Loading nucleotides for reference variation cds {} ms",
                     Thread.currentThread().getName(), time2 - time1);
        return geneListMap;
    }

    private Map<Gene, List<Gene>> fillCdsMap(List<Gene> blocksOnTrack, Track<Gene> geneTrack,
                                             GeneFile geneFile, Chromosome chromosome, boolean collapsedTrack)
        throws GeneReadingException {
        Map<Gene, List<Gene>> mRnaToCdsMap = new HashMap<>();

        int startIndex = chromosome.getSize();
        int endIndex = 1;
        Set<Gene> geneSet = new HashSet<>();
        for (Gene gene : blocksOnTrack) {
            if (gene.getStartIndex() < startIndex) {
                startIndex = gene.getStartIndex();
            }
            if (gene.getEndIndex() > endIndex) {
                endIndex = gene.getEndIndex();
            }
            geneSet.add(gene);
        }
        Track<Gene> newTrack = new Track<>(geneTrack);
        newTrack.setStartIndex(startIndex);
        newTrack.setEndIndex(endIndex);
        Track<Gene> loadedGenes = gffManager.loadGenes(newTrack, geneFile, chromosome, collapsedTrack);

        for (Gene gene : loadedGenes.getBlocks()) {
            if (gene.getItems() != null && !gene.getItems().isEmpty() && geneSet.contains(gene)) {
                Map<Gene, List<Gene>> map2 = new HashMap<>();
                readMrna(gene.getItems(), map2);
                map2.keySet().stream().forEach(f -> mRnaToCdsMap.put(f, map2.get(f)));
            }
        }

        return mRnaToCdsMap;
    }

    private void readMrna(final List<Gene> genes, final Map<Gene, List<Gene>> map) {
        if (CollectionUtils.isEmpty(genes) || map == null) {
            return;
        }
        List<Gene> distinctGenes = genes.stream().distinct().collect(Collectors.toList());
        for (Gene gene : distinctGenes) {
            if (TRANSCRIPTS_NAMES.contains(gene.getFeature().toLowerCase()) && gene.getItems() != null) {
                List<Gene> cds = gene.getItems().stream()
                    .filter(item -> CDS_NAMES.contains(item.getFeature().toLowerCase())).distinct()
                    .collect(Collectors.toList());
                map.put(gene, cds);
            }
        }
    }

    private List<List<Sequence>> loadCdsNucleatides(List<Gene> cdsList, long referenceId, Chromosome chromosome) {
        if (CollectionUtils.isEmpty(cdsList)) {
            return null;
        }

        // Load reference nucleotides for CDS.
        List<List<Sequence>> cdsNucleotides;
        try {
            cdsNucleotides = loadNucleotidesForReferenceCds(chromosome, referenceId, cdsList);
        } catch (IOException e) {
            LOGGER.error("Error during protein sequence reconstruction.", e);
            return null;
        }
        Assert.notNull(cdsNucleotides, "Cannot load nucleotides for cds list.");

        return cdsNucleotides;
    }

    private void checkAndRevert(final List<Gene> cdsList, final List<List<Sequence>> cdsNucleotides,
                                final List<Integer> frames) {
        if (cdsList.get(0).getStrand().equals(StrandSerializable.NEGATIVE)) {
            Collections.reverse(cdsList);
            Collections.reverse(cdsNucleotides);
            Collections.reverse(frames);
        }
    }

    /**
     * Change mRNA according to specified variation.
     *
     * @param variation          variation
     * @param cdsNucleotides     list of possible nucleotide sequences for specified cds
     * @param nucleotideSequence source nucleotide sequence
     */
    private void changeMRnaByVariations(final Variation variation,
                                        final List<List<Sequence>> cdsNucleotides,
                                        final List<Sequence> nucleotideSequence) {
        // Find source nucleotide sequence in mRNA.
        Integer startIndex = -1;
        Integer endIndex = -1;
        for (int j = 0; j < nucleotideSequence.size(); j++) {
            Sequence sequence = nucleotideSequence.get(j);
            if (sequence.getStartIndex().equals(variation.getStartIndex())) {
                startIndex = j;
            } else if (sequence.getEndIndex().equals(variation.getEndIndex())) {
                endIndex = j;
            }
        }

        if (startIndex == -1 || endIndex == -1) {
            return;
        }

        // Change mRNA according to variations.
        List<Sequence> sourceAllele = nucleotideSequence.subList(startIndex, endIndex + 1);
        replaceAlleles(variation, cdsNucleotides, nucleotideSequence, sourceAllele);
    }

    /**
     * Replace source allele with alternative alleles in CDS' nucleotide sequence.
     *
     * @param variation          variation
     * @param cdsNucleotides     list of alternative CDS nucleotide sequences
     * @param nucleotideSequence current alternative nucleotide sequence
     * @param sourceAllele       source allele
     */
    private void replaceAlleles(final Variation variation, final List<List<Sequence>> cdsNucleotides,
                                final List<Sequence> nucleotideSequence, final List<Sequence> sourceAllele) {
        for (String alternativeAllele : variation.getAlternativeAlleles()) {
            // Split alternative sourceAllele into nucleotides.
            List<String> alternativeNucleotides = Arrays.asList(alternativeAllele.split(""));
            List<Sequence> alternativeNucleotideSequence = new ArrayList<>(alternativeNucleotides.size());
            for (int m = 0; m < alternativeNucleotides.size(); m++) {
                // String -> Sequence
                Sequence sequence =
                        new Sequence(variation.getStartIndex() + m, variation.getStartIndex() + m,
                                alternativeNucleotides.get(m));
                alternativeNucleotideSequence.add(sequence);

                sourceAllele.clear();
                sourceAllele.addAll(alternativeNucleotideSequence);
            }

            cdsNucleotides.add(new ArrayList<>(nucleotideSequence));
        }
    }

    /**
     * If CDs has negative strand, construct reverse complement nucleotide sequence.
     *
     * @param cdsToNucleotidesMap map of CDS to it's possible nucleotide sequences.
     * @return map of CDS to it's possible nucleotide sequences with fixed strand
     */
    private Map<Gene, List<List<Sequence>>> processStrand(final Map<Gene, List<List<Sequence>>> cdsToNucleotidesMap) {
        Map<Gene, List<List<Sequence>>> result = new HashMap<>(cdsToNucleotidesMap.size());
        for (Map.Entry<Gene, List<List<Sequence>>> cdsToNucleotidesEntry : cdsToNucleotidesMap.entrySet()) {
            List<List<Sequence>> nucleotideSequences = cdsToNucleotidesEntry.getValue();
            boolean isNegative = StrandSerializable.NEGATIVE.equals(cdsToNucleotidesEntry.getKey().getStrand());

            List<List<Sequence>> correctNucleotideSequences = new ArrayList<>();
            for (List<Sequence> nucleotideSeq : nucleotideSequences) {
                // Construct reverse complement sequence if strand is "-".
                if (isNegative) {
                    nucleotideSeq = ProteinSequenceUtils.reverseComplement(nucleotideSeq);
                }
                // Remove frame from sequence.
                nucleotideSeq = nucleotideSeq.subList(cdsToNucleotidesEntry.getKey().getFrame(), nucleotideSeq.size());
                correctNucleotideSequences.add(new ArrayList<>(nucleotideSeq));
            }
            result.put(cdsToNucleotidesEntry.getKey(), correctNucleotideSequences);
        }
        return result;
    }

    private List<ProteinSequenceEntry> reconstructAminoAcidByTriples(final Track<Gene> track, final Gene cds,
                                                                     final List<List<Sequence>> cdsNucleotides,
                                                                     final int currCdsIndex,
                                                                     final List<List<Sequence>> tripleList,
                                                                     final Integer extendedStart,
                                                                     final MutableInt aminoAcidCounter,
                                                                     final boolean extendCDS) {
        List<ProteinSequenceEntry> proteinSequences = new ArrayList<>();
        int newExtendedStart = extendedStart;
        for (List<Sequence> aTripleList : tripleList) {
            List<Sequence> triple = new ArrayList<>(aTripleList);
            int length = triple.size();

            if (newExtendedStart == 0) {
                aminoAcidCounter.increment();
            }

            // Reconstruct boundary amino acids, using nucleotide sequences from nearby CDS.
            int lastCdsIndex = currCdsIndex;
            boolean isAdditionalCds = false;
            if (!extendCDS && length < TRIPLE_LENGTH) {
                continue;
            }
            while (length < TRIPLE_LENGTH) {
                lastCdsIndex = lastCdsIndex + 1;
                List<Sequence> utilNucleotides;
                if (lastCdsIndex < cdsNucleotides.size()) {
                    utilNucleotides = cdsNucleotides.get(lastCdsIndex);
                } else {
                    isAdditionalCds = true;
                    break;
                }

                LOGGER.info("Loaded additional sequences.");

                // Add nucleotides from next CDS.
                List<Sequence> subList = new ArrayList<>(utilNucleotides.subList(0, Math.min(utilNucleotides.size(),
                        TRIPLE_LENGTH - length)));
                triple.addAll(subList);
                newExtendedStart += subList.size();
                subList.clear();
                length = triple.size();

            }
            if (length < TRIPLE_LENGTH) {
                continue;
            }
            processCds(track, cds, proteinSequences, triple, isAdditionalCds, newExtendedStart, aminoAcidCounter);
            newExtendedStart = 0;
        }

        return proteinSequences;
    }

    private void addProteinSequenceEntry(Track<Gene> track, final Gene cds, List<ProteinSequenceEntry> proteinSequences,
                                         String aminoAcid, long tripleStartIndex, int newExtendedStart,
                                         final MutableInt aminoAcidCounter) {
        if (tripleStartIndex >= track.getStartIndex() && tripleStartIndex <= track.getEndIndex()) {
            long tripleEndIndex = tripleStartIndex + 2;
            if (newExtendedStart != 0) {
                tripleEndIndex = tripleEndIndex - Math.abs(newExtendedStart);
            }
            if (tripleEndIndex > cds.getEndIndex()) {
                tripleEndIndex = cds.getEndIndex();
            }
            if (!(tripleStartIndex > track.getStartIndex() && tripleEndIndex > track.getEndIndex()) &&
                !(tripleStartIndex < track.getStartIndex() && tripleEndIndex < track.getEndIndex())) {
                ProteinSequenceEntry protein = new ProteinSequenceEntry(aminoAcid, track.getId(),
                                                        cds.getStartIndex().longValue(), cds.getEndIndex().longValue(),
                                                                                    tripleStartIndex, tripleEndIndex);
                protein.setIndex(aminoAcidCounter.longValue() - 1);
                proteinSequences.add(protein);
            }
        }
    }

    private void processCds(Track<Gene> track, final Gene cds, List<ProteinSequenceEntry> proteinSequences,
                            List<Sequence> triple, boolean isAdditionalCds, int newExtendedStart,
                            final MutableInt aminoAcidCounter) {
        if (!isAdditionalCds) {
            boolean isNegative = StrandSerializable.NEGATIVE.equals(cds.getStrand());

            // Convert nucleotide triple to amino acid.
            String aminoAcid = ProteinSequenceUtils.tripletToAminoAcid(
                triple.stream().map(Sequence::getText).collect(Collectors.joining()));

            long tripleStartIndex = isNegative? triple.get(2).getStartIndex().longValue()
                                              : triple.get(0).getStartIndex().longValue();
            if (((newExtendedStart > 0) && isNegative) || ((newExtendedStart < 0) && !isNegative)) {
                tripleStartIndex = cds.getStartIndex();
            }
            addProteinSequenceEntry(track, cds, proteinSequences, aminoAcid, tripleStartIndex, newExtendedStart,
                                    aminoAcidCounter);
        }
    }
}
