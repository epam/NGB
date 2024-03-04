/*
 * MIT License
 *
 * Copyright (c) 2024 EPAM Systems
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
package com.epam.catgenome.manager.sequence;

import com.epam.catgenome.controller.vo.Query2TrackConverter;
import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.controller.vo.sequence.LocalSequenceRequest;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.externaldb.target.UrlEntity;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFilterForm;
import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.index.GeneIndexEntry;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.protein.ProteinSequenceEntry;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.sequence.LocalSequence;
import com.epam.catgenome.entity.target.*;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.exception.GeneReadingException;
import com.epam.catgenome.exception.ReferenceReadingException;
import com.epam.catgenome.exception.TargetGenesException;
import com.epam.catgenome.manager.FeatureIndexManager;
import com.epam.catgenome.manager.TrackHelper;
import com.epam.catgenome.manager.gene.GeneTrackManager;
import com.epam.catgenome.manager.protein.ProteinSequenceReconstructionManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.reference.ReferenceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SequencesManager {
    private static final int BATCH_SIZE = 100;
    private final List<String> geneKeys = Arrays.asList("gene_id", "geneId", "gene");
    private final ReferenceGenomeManager referenceGenomeManager;
    private final FeatureIndexManager featureIndexManager;
    private final TrackHelper trackHelper;
    private final GeneTrackManager geneTrackManager;
    private final ReferenceManager referenceManager;
    private final ProteinSequenceReconstructionManager psReconstructionManager;

    public String getSequence(final LocalSequenceRequest request) throws ParseException, IOException,
            TargetGenesException, ReferenceReadingException {
        final TrackQuery trackQuery = new TrackQuery();
        trackQuery.setScaleFactor(1D);
        trackQuery.setChromosomeId(request.getChromosomeId());
        trackQuery.setStartIndex(request.getBegin().intValue());
        trackQuery.setEndIndex(request.getEnd().intValue());

        switch (request.getDatabase()) {
            case NUCLEOTIDE:
                trackQuery.setId(request.getReferenceId());
                final Track<com.epam.catgenome.entity.reference.Sequence> sequenceTrack =
                        referenceManager.getNucleotidesResultFromNib(Query2TrackConverter.convertToTrack(trackQuery));
                return sequenceTrack.getBlocks().stream()
                        .map(com.epam.catgenome.entity.reference.Sequence::getText)
                        .collect(Collectors.joining());
            case PROTEIN:
                trackQuery.setId(request.getFeatureFileId());
                final Track<Gene> geneTrack = Query2TrackConverter.convertToTrack(trackQuery);
                final Chromosome chromosome = trackHelper.validateTrack(geneTrack);
                final Map<Gene, List<ProteinSequenceEntry>> proteinSequences = psReconstructionManager
                        .reconstructProteinSequence(geneTrackManager.loadGenes(geneTrack, false),
                                chromosome, request.getReferenceId(), false, true);
                return proteinSequences.values().stream()
                        .flatMap(List::stream)
                        .map(ProteinSequenceEntry::getText)
                        .collect(Collectors.joining());
            default:
                throw new TargetGenesException("Incorrect sequence id format. " +
                        "Sequence type should be PROTEIN or NUCLEOTIDE.");
        }
    }

    public List<GeneRefSection> getGeneSequencesTable(final Map<String, Long> targetGenes) throws IOException {
        final List<GeneRefSection> geneRefSections = new ArrayList<>();
        for (Map.Entry<String, Long> targetGene : targetGenes.entrySet()) {
            List<Reference> references = referenceGenomeManager.loadAllReferenceGenomesByTaxId(targetGene.getValue());
            for (Reference ref : references) {
                GeneRefSection geneRefSection = new GeneRefSection();
                geneRefSection.setGeneId(targetGene.getKey());
                UrlEntity refSectionReference = new UrlEntity();
                refSectionReference.setId(ref.getId().toString());
                refSectionReference.setName(ref.getName());
                geneRefSection.setReference(refSectionReference);
                List<GeneSequence> sequences = new ArrayList<>();

                List<? extends FeatureFile> geneFiles = featureIndexManager.getGeneFilesForReference(ref.getId(),
                        Collections.emptyList());

                for (String k : geneKeys) {
                    GeneFilterForm filterForm = getGeneFilterForm(k, targetGene.getKey());
                    IndexSearchResult<GeneIndexEntry> indexSearchResult =
                            featureIndexManager.getGeneSearchResult(filterForm, geneFiles);
                    processFeatures(ref.getId(), sequences, indexSearchResult.getEntries());
                    for (int i = 2; i <= indexSearchResult.getTotalPagesCount(); i++) {
                        filterForm.setPage(i);
                        indexSearchResult = featureIndexManager.getGeneSearchResult(filterForm, geneFiles);
                        processFeatures(ref.getId(), sequences, indexSearchResult.getEntries());
                    }
                }
                geneRefSection.setSequences(sequences);
                geneRefSections.add(geneRefSection);
            }
        }
        return geneRefSections;
    }

    public List<String> getGeneProteins(final String geneId, final long taxId) throws IOException {
        List<String> sequences = new ArrayList<>();
        List<Reference> references = referenceGenomeManager.loadAllReferenceGenomesByTaxId(taxId);
        for (Reference ref : references) {
            List<? extends FeatureFile> geneFiles = featureIndexManager.getGeneFilesForReference(ref.getId(),
                    Collections.emptyList());
            for (String k : geneKeys) {
                GeneFilterForm filterForm = getGeneFilterForm(k, geneId);
                IndexSearchResult<GeneIndexEntry> indexSearchResult =
                        featureIndexManager.getGeneSearchResult(filterForm, geneFiles);
                sequences.addAll(getSequences(ref.getId(), indexSearchResult.getEntries()));

                for (int i = 2; i <= indexSearchResult.getTotalPagesCount(); i++) {
                    filterForm.setPage(i);
                    indexSearchResult = featureIndexManager.getGeneSearchResult(filterForm, geneFiles);
                    sequences.addAll(getSequences(ref.getId(), indexSearchResult.getEntries()));
                }
            }
        }
        return sequences;
    }

    private static GeneFilterForm getGeneFilterForm(final String geneField, final String geneId) {
        final GeneFilterForm filterForm = new GeneFilterForm();
        filterForm.setPageSize(BATCH_SIZE);
        filterForm.setPage(1);
        final Map<String, String> geneFilters = new HashMap<>();
        geneFilters.put(geneField, geneId);
        filterForm.setAdditionalFilters(geneFilters);
        filterForm.setFeatureTypes(Arrays.asList(FeatureType.MRNA.getFileValue(), FeatureType.GENE.getFileValue()));
        return filterForm;
    }

    private static void processFeatures(final Long referenceId,
                                        final List<GeneSequence> sequences, final List<GeneIndexEntry> features) {
        features.forEach(f -> {
            GeneSequence geneSequence = new GeneSequence();
            LocalSequence mRNASequence = new LocalSequence();
            mRNASequence.setId(f.getFeatureId());
            mRNASequence.setName(f.getFeatureName());
            mRNASequence.setBegin(Long.valueOf(f.getStartIndex()));
            mRNASequence.setEnd(Long.valueOf(f.getEndIndex()));
            mRNASequence.setStrand(Sequence.parseStrand(f.getStrand()));
            mRNASequence.setReferenceId(referenceId);
            mRNASequence.setFeatureFileId(f.getFeatureFileId());
            mRNASequence.setChromosomeId(f.getChromosome().getId());
            geneSequence.setMRNA(mRNASequence);
            sequences.add(geneSequence);
        });
    }

    private List<String> getSequences(final Long referenceId, final List<GeneIndexEntry> features)
            throws GeneReadingException {
        final List<String> sequences = new ArrayList<>();
        for (GeneIndexEntry f : features) {
            final TrackQuery trackQuery = new TrackQuery();
            trackQuery.setScaleFactor(1D);
            trackQuery.setChromosomeId(f.getChromosome().getId());
            trackQuery.setStartIndex(f.getStartIndex());
            trackQuery.setEndIndex(f.getEndIndex());
            trackQuery.setId(f.getFeatureFileId());
            final Track<Gene> geneTrack = Query2TrackConverter.convertToTrack(trackQuery);
            final Chromosome chromosome = trackHelper.validateTrack(geneTrack);
            final Map<Gene, List<ProteinSequenceEntry>> proteinSequences = psReconstructionManager
                    .reconstructProteinSequence(geneTrackManager.loadGenes(geneTrack, false),
                            chromosome, referenceId, false, true);
            String sequence = proteinSequences.values().stream()
                    .flatMap(List::stream)
                    .map(ProteinSequenceEntry::getText)
                    .collect(Collectors.joining());
            sequences.add(sequence);
        }
        return sequences;
    }
}
