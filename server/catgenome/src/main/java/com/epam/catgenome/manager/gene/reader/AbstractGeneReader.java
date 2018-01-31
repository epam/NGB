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

package com.epam.catgenome.manager.gene.reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.manager.gene.parser.GffFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneFileType;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.exception.GeneReadingException;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.gene.GeneUtils;
import com.epam.catgenome.manager.gene.parser.GeneFeature;
import com.epam.catgenome.manager.gene.parser.GffCodec;
import com.epam.catgenome.manager.parallel.ParallelTaskExecutionUtils;
import com.epam.catgenome.manager.parallel.TreeListMultiset;
import com.epam.catgenome.util.Utils;
import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.Interval;
import htsjdk.samtools.util.IntervalTreeMap;
import com.epam.catgenome.util.feature.reader.AbstractFeatureReader;
import htsjdk.tribble.readers.LineIterator;

/**
 * Source:      GeneReader
 * Created:     13.10.16, 12:59
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * A class that contains logic, connected with reading of gene files
 * </p>
 *
 *
 */
public abstract class AbstractGeneReader {
    public static final Double LARGE_SCALE_FACTOR_LIMIT = 0.001;
    private static final String TRANSCRIPT_FEATURE_NAME = "transcript";
    private static final int CODON_LENGTH = 3;
    private static final  Logger LOGGER = LoggerFactory.getLogger(AbstractGeneReader.class);

    private ExecutorService executorService;
    private FileManager fileManager;
    private GeneFile geneFile;

    protected AbstractGeneReader(ExecutorService executorService, FileManager fileManager, GeneFile geneFile) {
        this.executorService = executorService;
        this.fileManager = fileManager;
        this.geneFile = geneFile;
    }

    /**
     * Creates a proper implementation of AbstractGeneReader for a specified GeneFile
     *
     * @param executorService an ExecutorService is required for multithreading reading
     * @param fileManager     a FileManager is required for file access
     * @param geneFile        a GeneFile object, from which to read
     * @return a proper implementation of AbstractGeneReader for a specified GeneFile
     */
    public static AbstractGeneReader createGeneReader(ExecutorService executorService, FileManager fileManager,
                                                      GeneFile geneFile) {
        if (getOrigin(geneFile) == Gene.Origin.GTF) {
            return new GtfReader(executorService, fileManager, geneFile);
        } else {
            return new GffReader(executorService, fileManager, geneFile);
        }
    }

    /**
     * Reads genes from gene file in a interval, specified by a track
     *
     * @param track      a track, specifying interval to load genes from
     * @param chromosome a chromosome to load genes from
     * @param collapse   flag, determining if all transcripts of a single gene should be collapsed to one
     * @return a list of Gene features
     * @throws GeneReadingException
     */
    public List<Gene> readGenesFromGeneFile(final Track<Gene> track, final Chromosome chromosome, boolean collapse,
                                            int maxTaskCount)
            throws GeneReadingException {
        // Try to paralleling of reading from file.
        double time1 = Utils.getSystemTimeMilliseconds();
        int numOfSubIntervals = ParallelTaskExecutionUtils.splitFileReadingInterval(track, LOGGER, maxTaskCount);

        final ReaderState state = new ReaderState();

        final List<Callable<Throwable>> callables = new ArrayList<>(numOfSubIntervals);

        for (int i = 0; i < numOfSubIntervals; i++) {
            final int factor = i;
            final int num = numOfSubIntervals;
            callables.add(() -> readPartOfGeneFile(chromosome, track.getStartIndex(), factor, num,
                    track.getEndIndex(), state, track.getScaleFactor()));
        }

        List<Future<Throwable>> futures;
        try {
            futures = executorService.invokeAll(callables);
        } catch (InterruptedException | AssertionError e) {
            throw new GeneReadingException(geneFile, chromosome, track.getStartIndex(), track.getEndIndex(), e);
        }

        List<Throwable> errors = futures.stream().map(future -> {
            try {
                return future != null ? future.get() : null;
            } catch (InterruptedException | ExecutionException e) {
                return e;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        if (!errors.isEmpty()) {
            throw new GeneReadingException(track, errors.get(0));
        }

        double time2 = Utils.getSystemTimeMilliseconds();
        LOGGER.debug("Reading in {} threads, took {} ms", numOfSubIntervals, time2 - time1);

        time1 = Utils.getSystemTimeMilliseconds();
        List<Gene> passedGenes = processAssembly(state, track, collapse);
        time2 = Utils.getSystemTimeMilliseconds();
        LOGGER.debug("Assembly took {} ms", time2 - time1);
        // Replace synchronized list with simple list.
        return new ArrayList<>(passedGenes);
    }

    private List<Gene> processAssembly(ReaderState readerState,
                                       Track<Gene> track, boolean collapse) {
        List<Gene> passedGenes = Collections.synchronizedList(new ArrayList<>());
        if (!readerState.seenGenes.isEmpty()) {
            if (!readerState.genes.isEmpty()) {
                if (collapse) {
                    readerState.genes.parallelStream().forEach(gene -> collapseFeatures(readerState.mRnaStuffMap, gene,
                                                            readerState.mRnaMap, track.getScaleFactor(), passedGenes));
                } else {
                    readerState.genes.parallelStream().forEach(gene -> assembleFeatures(readerState.mRnaStuffMap, gene,
                                                            readerState.mRnaMap, track.getScaleFactor(), passedGenes));
                }
            }

            final int step = (int) Math.ceil(1 / track.getScaleFactor());
            readerState.unmappedFeatures.forEach(g -> {
                if (passesScaleFactor(g, track.getScaleFactor())) {
                    passedGenes.add(g);
                } else {
                    if (!passedGenes.isEmpty()) {
                        Gene lastGene = passedGenes.get(passedGenes.size() - 1);
                        int lastGeneStepNumber = lastGene.getEndIndex() / step;
                        int stepNumber = g.getEndIndex() / step;
                        if (lastGeneStepNumber == stepNumber) {
                            makeStatisticFeature(lastGene, g);
                        } else {
                            passedGenes.add(g);
                        }
                    } else {
                        passedGenes.add(g);
                    }
                }
            });

            addUnmappedFeatures(passedGenes, readerState.mRnaMap, readerState.mRnaStuffMap, step,
                                track.getScaleFactor());
        }

        return passedGenes;
    }

    private Throwable readPartOfGeneFile(final Chromosome chromosome,
                                       final Integer startIndex, final Integer factor, final Integer num,
                                       final Integer endIndex,
                                       final ReaderState state, Double scaleFactor)
        throws IOException {
        double time0 = Utils.getSystemTimeMilliseconds();
        try (AbstractFeatureReader<GeneFeature, LineIterator> featureReader = fileManager.makeGeneReader(
                geneFile, determineGeneFileType(scaleFactor))) {
            LOGGER.debug("Thread {} starts", Thread.currentThread().getName());
            double time11 = Utils.getSystemTimeMilliseconds();
            LOGGER.debug("Thread {} Reader creation {} ms", Thread.currentThread().getName(), time11 - time0);

            int start = startIndex + factor * ParallelTaskExecutionUtils.MAX_BLOCK_SIZE;
            int end;
            if (factor != num - 1) {
                end = startIndex + (factor + 1) * ParallelTaskExecutionUtils.MAX_BLOCK_SIZE;
            } else {
                end = endIndex;
            }
            LOGGER.debug("Thread {} Interval: {} - {}", Thread.currentThread().getName(), start, end);

            try (CloseableIterator<GeneFeature> iterator = Utils.query(featureReader, chromosome, start, end)) {
                double time21 = Utils.getSystemTimeMilliseconds();
                LOGGER.debug("Thread {} Query took {} ms", Thread.currentThread().getName(), time21 - time11);

                Map<String, Gene> overlappedMrnas = new HashMap<>();
                time11 = Utils.getSystemTimeMilliseconds();

                if (iterator.hasNext()) {
                    GeneFeature firstFeature = iterator.next();
                    processFeature(state, firstFeature, overlappedMrnas, start, end);
                }

                iterator.forEachRemaining(feature -> processFeature(state, feature, overlappedMrnas, start, end));
                time21 = Utils.getSystemTimeMilliseconds();
                LOGGER.debug("Thread {} Walkthrough took {} ms",
                        Thread.currentThread().getName(), time21 - time11);

                fillExonsCountForOverlapping(overlappedMrnas, featureReader, chromosome);
            } catch (SAMFormatException e) {
                LOGGER.error("", e);
                return e;
            }
            LOGGER.debug("Thread {} ends", Thread.currentThread().getName());
            return null;
        }
    }

    /**
     * Processes a GeneFeature, filling data structures, required for genes hierarchy assembly
     *
     * @param readerState      a ReaderState object, containing necessary feature collections
     * @param feature          a feature to process
     * @param overlappedMrnas  a List of transcripts, that are partially fitted it the track, to properly set their
     *                         exonsCount and aminoacidLength
     * @param start            start position of the track
     * @param end              end position of the track
     */
    private void processFeature(ReaderState readerState, GeneFeature feature, Map<String, Gene> overlappedMrnas,
                                  int start, int end) {
        Gene currGene = new Gene(feature);

        // Populate maps
        if (readerState.seenGenes.add(currGene)) {
            if (GeneUtils.isGene(currGene)) {
                readerState.genes.add(currGene);
            } else {
                if (currGene.getParentId() != null) {
                    mapFeature(currGene, readerState, overlappedMrnas, start, end);
                } else {
                    currGene.setMapped(false);
                    readerState.unmappedFeatures.add(currGene);
                }
            }
        }
    }

    /**
     * Maps gene to its parent features by putting it in a map of hierarchy
     *
     * @param currGene Gene to map
     * @param readerState a ReaderState object, containing necessary feature collections
     * @param overlappedMrnas  a List of transcripts, that are partially fitted it the track, to properly set their
     *                         exonsCount and aminoacidLength
     * @param start            start position of the track
     * @param end              end position of the track
     */
    protected abstract void mapFeature(Gene currGene, ReaderState readerState, Map<String, Gene> overlappedMrnas,
                                       int start, int end);

    /**
     * Assembles gene feature hierarchy in the way, that all gene's transcripts are merged into one, so that every
     * gene contains only one single transcript, containing all internal stuff: exons, CDS, etc. Overlapping
     * internal features are merged.
     *
     * @param mRnaStuffMap a map of mRNA internal features
     * @param gene         gene to fit in hierarchy
     * @param mRnaMap      map of mRNA features
     * @param scaleFactor  a client's scale factor
     * @param passedGenes  a list, filled with resulting assembled genes
     */
    protected abstract void collapseFeatures(ConcurrentMap<String, ConcurrentMap<String, List<Gene>>>
                                                     mRnaStuffMap, Gene gene,
                                             ConcurrentMap<String, ConcurrentMap<String, Gene>> mRnaMap,
                                             Double scaleFactor, List<Gene> passedGenes);

    /**
     * Assemble gene features hierarchy in the following way:
     * Gene features contain several mRNA features, mRNA features contain various internal features.
     *
     * @param mRnaStuffMap a map of mRNA internal features
     * @param gene         a gene to fit in hierarchy
     * @param mRnaMap      a map of mRNA features
     * @param passedGenes  a list, filled with resulting assembled genes
     */
    protected abstract void assembleFeatures(ConcurrentMap<String, ConcurrentMap<String, List<Gene>>>
                                                     mRnaStuffMap, Gene gene,
                                             ConcurrentMap<String, ConcurrentMap<String, Gene>> mRnaMap,
                                             Double scaleFactor, List<Gene> passedGenes);

    /**
     * Adds unmapped features to the list of resulting genes as first-level features
     *
     * @param genes        a resulting list of genes
     * @param mRnaMap      a map of mRNA features
     * @param mRnaStuffMap a map of mRNA internal features
     * @param step         size of a viewport's pixel in bps
     * @param scaleFactor  client's scale factor
     */
    protected abstract void addUnmappedFeatures(List<Gene> genes,
                                                ConcurrentMap<String, ConcurrentMap<String, Gene>> mRnaMap,
                                                ConcurrentMap<String, ConcurrentMap<String,
                                                        List<Gene>>> mRnaStuffMap, int step, Double scaleFactor);

    private void fillExonsCountForOverlapping(final Map<String, Gene> overlappedMrnas,
                                              final AbstractFeatureReader<GeneFeature, LineIterator> featureReader,
                                              final Chromosome chromosome) throws IOException {
        if (overlappedMrnas.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Gene> e : overlappedMrnas.entrySet()) {
            CloseableIterator<GeneFeature> iterator = Utils.query(featureReader, chromosome,
                                                              e.getValue().getStartIndex(), e.getValue().getEndIndex());
            long count = 0;
            long basesCount = 0;
            while (iterator.hasNext()) {
                GeneFeature feature = iterator.next();
                if (GeneUtils.isExon(feature) && Objects.equals(GeneUtils.getTranscriptId(feature), e.getKey())) {
                    count++;
                    basesCount += feature.getEnd() - feature.getStart();
                }
            }

            e.getValue().setExonsCount(count);
            e.getValue().setAminoacidLength(basesCount / CODON_LENGTH);
        }
    }

    protected boolean passesScaleFactor(Gene gene, Double scaleFactor) {
        return (gene.getEndIndex() - gene.getStartIndex()) * scaleFactor >= 1;
    }

    private void makeStatisticFeature(Gene statisticFeature, Gene featureToAdd) {
        statisticFeature.setFeatureCount(statisticFeature.getFeatureCount() + 1);
        statisticFeature.setGroupId(statisticFeature.getFeatureCount() + " Features");
        statisticFeature.setAttributes(Collections.singletonMap("name", statisticFeature.getGroupId()));
        statisticFeature.setFeature("statistic");
        statisticFeature.setScore(null);
        statisticFeature.setAminoacidLength(null);
        statisticFeature.setFrame(null);
        statisticFeature.setExonsCount(null);
        statisticFeature.setParentId(null);
        statisticFeature.setStrand(null);

        if (statisticFeature.getEndIndex() < featureToAdd.getEndIndex()) {
            statisticFeature.setEndIndex(featureToAdd.getEndIndex());
        }
    }

    protected void makeStatisticUnmappedFeature(Gene unmappedFeature, Double scaleFactor,
                                                TreeListMultiset<Integer, Gene> multiset, int step) {
        unmappedFeature.setMapped(false);
        if ((double) (unmappedFeature.getEndIndex() - unmappedFeature.getStartIndex()) * (double) scaleFactor >= 1) {
            multiset.add(unmappedFeature);
        } else {
            Gene lastGene = multiset.floor(unmappedFeature);
            if (lastGene != null) {
                int lastGeneStepNumber = lastGene.getEndIndex() / step;
                int stepNumber = unmappedFeature.getEndIndex() / step;
                if (lastGeneStepNumber == stepNumber) {
                    makeStatisticFeature(lastGene, unmappedFeature);
                } else {
                    multiset.add(unmappedFeature);
                }
            } else {
                multiset.add(unmappedFeature);
            }
        }
    }

    protected Gene createCanonicalTranscript(Gene gene) {
        Gene canonicalTranscript = new Gene();
        canonicalTranscript.setFeature(TRANSCRIPT_FEATURE_NAME);
        canonicalTranscript.setGroupId(gene.getGroupId());
        canonicalTranscript.setParentId(gene.getParentId());
        canonicalTranscript.setFeatureId(gene.getFeatureId());
        Map<String, String> attributes = new HashMap<>();
        attributes.put(GeneUtils.TRANSCRIPT_NAME_FILED, gene.getFeatureName());
        attributes.put(GffFeature.GENE_ID_KEY, gene.getFeatureId());
        canonicalTranscript.setAttributes(attributes);
        canonicalTranscript.setStrand(gene.getStrand());
        canonicalTranscript.setCanonical(true);

        return canonicalTranscript;
    }

    protected void setCanonicalTranscriptIndexes(Gene canonicalTranscript, Gene transcript) {
        if (canonicalTranscript.getStartIndex() == null) {
            canonicalTranscript.setStartIndex(transcript.getStartIndex());
        }
        if (canonicalTranscript.getEndIndex() == null) {
            canonicalTranscript.setEndIndex(transcript.getEndIndex());
        }

        canonicalTranscript.setStartIndex(Math.min(transcript.getStartIndex(), canonicalTranscript
                .getStartIndex()));
        canonicalTranscript.setEndIndex(Math.max(transcript.getEndIndex(), canonicalTranscript
                .getEndIndex()));
    }

    protected void groupMrnaStuff(List<Gene> mRnaStuff, IntervalTreeMap<Gene> stuffIntervalMap) {
        for (Gene s : mRnaStuff) {
            Interval interval = new Interval(s.getFeature(), s.getStartIndex(), s.getEndIndex());
            if (stuffIntervalMap.containsKey(interval)) {
                Gene merged = s;
                List<Interval> toRemove = new ArrayList<>();
                for (Gene g : stuffIntervalMap.getOverlapping(interval)) {
                    merged.setStartIndex(Math.min(merged.getStartIndex(), g.getStartIndex()));
                    merged.setEndIndex(Math.max(merged.getEndIndex(), g.getEndIndex()));
                    merged.setGroupId(null);
                    merged.setAttributes(Collections.emptyMap());
                    toRemove.add(new Interval(s.getFeature(), g.getStartIndex(), g.getEndIndex()));
                }
                merged.setFeature(FeatureType.CDS.name());
                toRemove.forEach(stuffIntervalMap::remove);
                stuffIntervalMap.put(new Interval(s.getFeature(), merged.getStartIndex(), merged.getEndIndex()),
                        merged);
            } else {
                s.setFeature(FeatureType.CDS.name());
                stuffIntervalMap.put(interval, s);
            }
        }
    }

    /**
     * Sets exonsCount and aminoacidLength for a Gene that is a transcript
     * @param mrna transcript Gene to set exonsCount and aminoacidLength
     * @param mRnaStuff stuff map to count exonsCount and aminoacidLength
     */
    protected void calculateExonsCountAndLength(Gene mrna, List<Gene> mRnaStuff) {
        long count = 0;
        long basesCount = 0;
        for (Gene g : mRnaStuff) {
            if (GeneUtils.isExon(g)) {
                count++;
                basesCount += g.getEndIndex() - g.getStartIndex();
            }
        }

        mrna.setExonsCount(count);
        mrna.setAminoacidLength(basesCount / CODON_LENGTH);
    }

    /**
     * Sets exonsCount and aminoacidLength for a Gene that is a transcript only if they are null
     * @param mrna transcript Gene to set exonsCount and aminoacidLength
     * @param mRnaStuff stuff map to count exonsCount and aminoacidLength
     */
    protected void setExonsCountAndLength(Gene mrna, List<Gene> mRnaStuff) {
        if (mrna.getExonsCount() == null) {
            calculateExonsCountAndLength(mrna, mRnaStuff);
        }
    }

    protected  <K, V> void removeIfEmpty(ConcurrentMap<String, ConcurrentMap<K, V>> collectionMap, String key) {
        if (collectionMap.get(key).isEmpty()) {
            collectionMap.remove(key);
        }
    }

    private static Gene.Origin getOrigin(GeneFile geneFile) {
        GffCodec.GffType type = GffCodec.GffType.forExt(FileManager.getGeneFileExtension(geneFile.getPath()));
        if (GffCodec.GffType.GTF.equals(type) || GffCodec.GffType.COMPRESSED_GTF.equals(type)) {
            return Gene.Origin.GTF;
        } else {
            return Gene.Origin.GFF;
        }
    }

    private GeneFileType determineGeneFileType(Double scaleFactor) {
        if (scaleFactor < LARGE_SCALE_FACTOR_LIMIT) {
            return GeneFileType.LARGE_SCALE;
        } else {
            return GeneFileType.ORIGINAL;
        }
    }

    /**
     * A private class for holding various collections, needed for query processing
     */
    protected static class ReaderState {

        /**
         * List, containing gene features.
         * Used to build gene hierarchy: genes -> transcripts -> etc
         */
        protected List<Gene> genes = Collections.synchronizedList(new ArrayList<>());

        /**
         * Map, containing transcript features with their transcript IDs, mapped to gene IDs.
         * Used to build gene hierarchy: genes -> transcripts -> etc
         */
        protected ConcurrentMap<String, ConcurrentMap<String, Gene>> mRnaMap = new ConcurrentHashMap<>();

        /**
         * Map, containing various transcript internal features, e.g. CDS, exon, 3UTR, mapped to gene IDs and
         * transcript IDs.
         * Used to build gene hierarchy: genes -> transcripts -> etc
         */
        protected ConcurrentMap<String, ConcurrentMap<String, List<Gene>>> mRnaStuffMap = new ConcurrentHashMap<>();

        /**
         * Set, to ensure that gene features are unique
         */
        protected Set<Gene> seenGenes = Collections.synchronizedSet(new HashSet<>());

        /**
         * List, containing features, that are not mopped in the hierarchy
         */
        protected List<Gene> unmappedFeatures = Collections.synchronizedList(new ArrayList<>());
    }
}
