/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

package com.epam.catgenome.manager.gene;

import com.epam.catgenome.controller.vo.externaldb.ensemblevo.EnsemblEntryVO;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneTranscript;
import com.epam.catgenome.entity.gene.Transcript;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.exception.GeneReadingException;
import com.epam.catgenome.manager.FeatureIndexManager;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.TrackHelper;
import com.epam.catgenome.manager.externaldb.EnsemblDataManager;
import com.epam.catgenome.manager.externaldb.ExtenalDBUtils;
import com.epam.catgenome.manager.externaldb.UniprotDataManager;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.Uniprot;
import com.epam.catgenome.manager.gene.reader.AbstractGeneReader;
import com.epam.catgenome.manager.parallel.TaskExecutorService;
import com.epam.catgenome.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GeneTrackManager {
    private static final String PROTEIN_CODING = "protein_coding";

    private final TrackHelper trackHelper;
    private final GeneFileManager geneFileManager;
    private final FileManager fileManager;
    private final TaskExecutorService taskExecutorService;
    private final FeatureIndexManager featureIndexManager;
    private final EnsemblDataManager ensemblDataManager;
    private final UniprotDataManager uniprotDataManager;
    private final boolean loadFromIndex;

    public GeneTrackManager(final TrackHelper trackHelper, final GeneFileManager geneFileManager,
                            final FileManager fileManager, final TaskExecutorService taskExecutorService,
                            final FeatureIndexManager featureIndexManager,
                            final EnsemblDataManager ensemblDataManager, final UniprotDataManager uniprotDataManager,
                            @Value("${gene.track.index.load.enable:false}") final boolean loadFromIndex) {
        this.trackHelper = trackHelper;
        this.geneFileManager = geneFileManager;
        this.fileManager = fileManager;
        this.taskExecutorService = taskExecutorService;
        this.featureIndexManager = featureIndexManager;
        this.ensemblDataManager = ensemblDataManager;
        this.uniprotDataManager = uniprotDataManager;
        this.loadFromIndex = loadFromIndex;
    }

    /**
     * Loads gene track
     *
     * @param track {@code Track} a track, to load genes for
     * @param collapsed {@code boolean} flag, that determines if multiple transcript blocks in a gene block should be
     *                                collapsed
     * @return {@code Track} a track, filled with {@code Gene} blocks
     */
    public Track<Gene> loadGenes(final Track<Gene> track, boolean collapsed)
            throws GeneReadingException {
        return loadFromIndex ? loadGenesFromIndex(track, collapsed) : loadGenesFromFile(track, collapsed);
    }

    /**
     * Loads gene track for registered file from original file
     *
     * @param track {@code Track} a track, to load genes for
     * @param collapse {@code boolean} flag, that determines if multiple transcript blocks in a gene block should be
     *                                collapsed
     * @return {@code Track} a track, filled with {@code Gene} blocks
     */
    public Track<Gene> loadGenesFromFile(final Track<Gene> track, boolean collapse) throws GeneReadingException {
        final Chromosome chromosome = trackHelper.validateTrack(track);
        final GeneFile geneFile = geneFileManager.load(track.getId());

        return loadGenes(track, geneFile, chromosome, collapse, false);
    }

    /**
     * Loads gene track for registered file from lucene index file
     *
     * @param track {@code Track} a track, to load genes for
     * @param collapsed {@code boolean} flag, that determines if multiple transcript blocks in a gene block should be
     *                                 collapsed
     * @return {@code Track} a track, filled with {@code Gene} blocks
     */
    public Track<Gene> loadGenesFromIndex(final Track<Gene> track, final boolean collapsed)
            throws GeneReadingException {
        final Chromosome chromosome = trackHelper.validateTrack(track);
        final GeneFile geneFile = geneFileManager.load(track.getId());

        return loadGenes(track, geneFile, chromosome, collapsed, true);
    }

    /**
     * Loads gene track from an unregistered file
     *
     * @param track {@code Track} a track, to load genes for
     * @param collapse {@code boolean} flag, that determines if multiple transcript blocks in a gene block should be
     *                                collapsed
     * @return {@code Track} a track, filled with {@code Gene} blocks
     */
    public Track<Gene> loadGenes(final Track<Gene> track, boolean collapse, String fileUrl, String indexUrl)
            throws GeneReadingException {
        final Chromosome chromosome = trackHelper.validateUrlTrack(track, fileUrl, indexUrl);
        GeneFile geneFile;
        try {
            geneFile = Utils.createNonRegisteredFile(GeneFile.class, fileUrl, indexUrl, chromosome);
        } catch (InvocationTargetException e) {
            throw new GeneReadingException(track, e);
        }
        return loadGenes(track, geneFile, chromosome, collapse);
    }

    /**
     * Loads gene track from a specified {@code GeneFile}
     *
     * @param track a track, to load genes for
     * @param geneFile a {@code GeneFile} from which track should be loaded
     * @param chromosome a {@code Chromosome} for which track to load
     * @param collapsed {@code boolean} flag, that determines if multiple transcript blocks in a gene block should be
     *                                collapsed
     * @return a track, filled with {@code Gene} blocks
     * @throws GeneReadingException
     */
    public Track<Gene> loadGenes(final Track<Gene> track, GeneFile geneFile, Chromosome chromosome, boolean collapsed)
            throws GeneReadingException {
        return loadGenes(track, geneFile, chromosome, collapsed, loadFromIndex);
    }

    /**
     * Load transcripts from external databases for a desired interval, specified by track
     *
     * @param track a track, for which to load transcripts
     * @return a track, filled with gene features and transcripts
     * @throws GeneReadingException
     */
    public Track<GeneTranscript> loadGenesTranscript(final Track<Gene> track,
                                                     String fileUrl, String indexUrl) throws GeneReadingException {
        final Track<Gene> geneTrack;
        if (fileUrl == null) {
            geneTrack = loadGenes(track, false);
        } else {
            geneTrack = loadGenes(track, false, fileUrl, indexUrl);
        }

        final Track<GeneTranscript> geneTranscriptTrack = new Track<>(track);
        final List<GeneTranscript> geneTranscriptList = new ArrayList<>();

        for (Gene gene : geneTrack.getBlocks()) {
            try {
                gene.setTranscripts(getTranscriptFromDB(gene.getGroupId()));
                geneTranscriptList.add(new GeneTranscript(gene));
            } catch (ExternalDbUnavailableException e) {
                log.info("External DB Exception", e);
                geneTranscriptList.add(new GeneTranscript(gene, e.getMessage()));
            }
        }
        geneTranscriptTrack.setBlocks(geneTranscriptList);
        return geneTranscriptTrack;
    }

    /**
     * Fetch gene IDs of genes, affected by variation. The variation is specified by it's start and end indexes
     *
     * @param start a start index of the variation
     * @param end an end index of the variation
     * @param geneFiles a {@code List} of {@code GeneFile} to look for genes
     * @param chromosome a {@code Chromosome}
     * @return a {@code Set} of IDs of genes, affected by the variation
     * @throws GeneReadingException
     */
    public Set<String> fetchGeneIds(int start, int end, List<GeneFile> geneFiles, Chromosome chromosome)
            throws GeneReadingException {
        Set<String> geneIds = new HashSet<>();

        for (GeneFile geneFile : geneFiles) {
            List<Gene> genes = new ArrayList<>();
            Track<Gene> track = new Track<>();
            track.setStartIndex(start);
            track.setEndIndex(end);
            track.setId(geneFile.getId());
            track.setChromosome(chromosome);
            track.setScaleFactor(AbstractGeneReader.LARGE_SCALE_FACTOR_LIMIT);

            if (end > start) {
                track.setStartIndex(start);
                track.setEndIndex(start);
                track = loadGenes(track, geneFile, chromosome, false);
                genes.addAll(track.getBlocks());

                track.setStartIndex(end);
                track.setEndIndex(end);
                track = loadGenes(track, geneFile, chromosome, false);
                genes.addAll(track.getBlocks());

            } else {
                track.setStartIndex(start);
                track.setEndIndex(end);
                track = loadGenes(track, geneFile, chromosome, false);
                genes = track.getBlocks();
            }

            geneIds.addAll(genes.stream()
                    .filter(GeneUtils::isGene)
                    .map(Gene::getGroupId)
                    .collect(Collectors.toList()));
        }

        return geneIds;
    }

    private boolean setTrackBounds(final Track<Gene> track, final GeneFile geneFile, final Chromosome chromosome)
            throws GeneReadingException {
        final Pair<Integer, Integer> bounds;

        try {
            bounds = trackHelper.loadBounds(geneFile, chromosome);
        } catch (IOException e) {
            throw new GeneReadingException(geneFile, chromosome, track.getStartIndex(), track.getEndIndex(), e);
        }

        if (bounds == null) {
            track.setBlocks(Collections.emptyList());
            return false;
        }

        // If we are out of variation bounds, return empty list of variations
        if (track.getStartIndex() > bounds.getRight() || track.getEndIndex() < bounds.getLeft()) {
            track.setBlocks(Collections.emptyList());
            return false;
        }

        trackHelper.setBounds(track, bounds);
        return true;
    }

    private List<Transcript> getTranscriptFromDB(final String geneID) throws ExternalDbUnavailableException {
        final EnsemblEntryVO vo = ensemblDataManager.fetchEnsemblEntry(geneID);
        Assert.notNull(vo);
        final List<Transcript> transcriptList = ExtenalDBUtils.ensemblEntryVO2Transcript(vo);
        for (Transcript transcript : transcriptList) {
            if (transcript.getBioType().equals(PROTEIN_CODING)) {
                try {
                    final Uniprot un = uniprotDataManager.fetchUniprotEntry(transcript.getId());
                    ExtenalDBUtils.fillDomain(un, transcript);
                    ExtenalDBUtils.fillPBP(un, transcript);
                    ExtenalDBUtils.fillSecondaryStructure(un, transcript);
                } catch (ExternalDbUnavailableException e) {
                    log.debug(e.getMessage(), e);
                }
            }
        }
        return transcriptList;
    }

    private Track<Gene> loadGenes(final Track<Gene> track, final GeneFile geneFile, final Chromosome chromosome,
                                  final boolean collapsed, final boolean fromIndex)
            throws GeneReadingException {
        if (geneFile.getType() == BiologicalDataItemResourceType.FILE && geneFile.getCompressed() &&
                !setTrackBounds(track, geneFile, chromosome)) {
            return track;
        }

        final List<Gene> notSyncGenes;
        if (Objects.nonNull(geneFile.getId()) && fromIndex) {
            final AbstractGeneReader geneReader = AbstractGeneReader.createGeneReader(
                    taskExecutorService.getExecutorService(), fileManager, geneFile, featureIndexManager);
            notSyncGenes = geneReader.readGenesFromIndex(track, chromosome, collapsed,
                    taskExecutorService.getTaskNumberOfThreads());
        } else {
            final AbstractGeneReader gtfReader = AbstractGeneReader.createGeneReader(
                    taskExecutorService.getExecutorService(), fileManager, geneFile);
            notSyncGenes = gtfReader.readGenesFromGeneFile(track, chromosome, collapsed,
                    taskExecutorService.getTaskNumberOfThreads());
        }

        track.setBlocks(notSyncGenes);
        return track;
    }
}
