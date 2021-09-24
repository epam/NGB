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

package com.epam.catgenome.manager.gene;

import com.epam.catgenome.component.MessageCode;
import com.epam.catgenome.constant.Constants;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.activity.Activity;
import com.epam.catgenome.entity.externaldb.ChainMinMax;
import com.epam.catgenome.entity.externaldb.DimEntity;
import com.epam.catgenome.entity.externaldb.DimStructure;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneFileType;
import com.epam.catgenome.entity.gene.GeneHighLevel;
import com.epam.catgenome.entity.gene.GeneLowLevel;
import com.epam.catgenome.entity.protein.ProteinSequence;
import com.epam.catgenome.entity.protein.ProteinSequenceEntry;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.exception.GeneReadingException;
import com.epam.catgenome.exception.HistogramReadingException;
import com.epam.catgenome.exception.HistogramWritingException;
import com.epam.catgenome.exception.RegistrationException;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.manager.DownloadFileManager;
import com.epam.catgenome.manager.FeatureIndexManager;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.TrackHelper;
import com.epam.catgenome.manager.activity.ActivityService;
import com.epam.catgenome.manager.externaldb.PdbDataManager;
import com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap.Alignment;
import com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap.PdbBlock;
import com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap.Segment;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.Record;
import com.epam.catgenome.manager.genbank.GenbankManager;
import com.epam.catgenome.manager.gene.parser.GeneFeature;
import com.epam.catgenome.manager.gene.parser.GffCodec;
import com.epam.catgenome.manager.gene.featurecounts.FeatureCountsToGffConvertor;
import com.epam.catgenome.manager.parallel.ParallelTaskExecutionUtils;
import com.epam.catgenome.manager.parallel.TaskExecutorService;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.genbank.GenbankUtils;
import com.epam.catgenome.util.HistogramUtils;
import com.epam.catgenome.util.IOHelper;
import com.epam.catgenome.util.NggbIntervalTreeMap;
import com.epam.catgenome.util.Utils;
import com.epam.catgenome.util.feature.reader.AbstractEnhancedFeatureReader;
import com.epam.catgenome.util.feature.reader.AbstractFeatureReader;
import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.Interval;
import htsjdk.samtools.util.IntervalTree;
import htsjdk.tribble.AsciiFeatureCodec;
import htsjdk.tribble.FeatureReader;
import htsjdk.tribble.readers.LineIterator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.constant.MessagesConstants.ERROR_REGISTER_FILE;

/**
 * Source:      GeneManager
 * Created:     02.12.15, 15:13
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * This component provides logic, associated with gene files (GFF, GTF) and tracks: registering files, reading records,
 * histograms and etc.
 * </p>
 *
 */
@Service
@Slf4j
public class GffManager {

    @Autowired
    private FileManager fileManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private GeneFileManager geneFileManager;

    @Autowired
    private BiologicalDataItemManager biologicalDataItemManager;

    @Autowired
    private TrackHelper trackHelper;

    @Autowired
    private PdbDataManager pBDataManager;

    @Autowired
    private DownloadFileManager downloadFileManager;

    @Autowired
    private FeatureIndexManager featureIndexManager;

    @Autowired
    private TaskExecutorService taskExecutorService;

    @Autowired(required = false)
    private EhCacheBasedIndexCache indexCache;

    @Autowired
    private GenbankManager genbankManager;

    @Autowired
    private ActivityService activityService;

    @Value("#{'${feature.counts.extensions}'.split(',')}")
    private List<String> featureCountsExtensions;

    @Value("${feature.counts.max.memory:500}")
    private int featureCountsMaxMemory;

    @Value("${gene.register.helper.files.enable:true}")
    private boolean createHelperFiles;

    private static final String EXON_FEATURE_NAME = "exon";

    private static final int EXON_SEARCH_CHUNK_SIZE = 100001;

    /**
     * Registers a gene file (GFF/GTF) in the system to make it available to browse. Creates Tabix index if absent
     * and a feature index to allow fast search for features
     *
     * @param request a request to register a file
     * @return a {@code GeneFile} object, representing gene file metadata in the system
     * @throws IOException
     */
    public GeneFile registerGeneFile(final FeatureIndexedFileRegistrationRequest request) {
        final String requestPath = request.getPath();
        Assert.isTrue(StringUtils.isNotBlank(requestPath), getMessage(
                MessagesConstants.ERROR_NULL_PARAM, "path"));
        Assert.notNull(request.getReferenceId(), getMessage(MessagesConstants.ERROR_NULL_PARAM, "referenceId"));
        if (request.getType() == null) {
            request.setType(BiologicalDataItemResourceType.FILE);
        }

        GeneFile geneFile;

        switch (request.getType()) {
            case FILE:
            case S3:
            case AZ:
                geneFile = registerGeneFileFromFile(request);
                break;
            case URL:
                request.setDoIndex(false);
                geneFile = registerGeneFileFromUrl(request);
                break;
            case DOWNLOAD:
                downloadFileForRegistration(requestPath, request);
                geneFile = registerGeneFileFromFile(request);
                break;
            default:
                throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_INVALID_PARAM));
        }

        return geneFile;
    }

    /**
     * Creates a feature index for {@link GeneFile}. If an index already exists, it will be deleted and created
     * from scratch
     * @param geneFileId an ID of gene file to reindex.
     * @param full
     * @param createTabixIndex
     * @return a {@link GeneFile}, for which index was created
     * @throws IOException if an error occurred while writing index
     */
    public GeneFile reindexGeneFile(long geneFileId, boolean full, boolean createTabixIndex) throws IOException {
        GeneFile geneFile = geneFileManager.load(geneFileId);
        fileManager.deleteFileFeatureIndex(geneFile);
        if (createTabixIndex) {
            File index = new File(geneFile.getIndex().getPath());
            if (index.exists()) {
                index.delete();
            }
        }
        GeneRegisterer geneRegisterer = new GeneRegisterer(referenceGenomeManager, fileManager, featureIndexManager,
                geneFile, createHelperFiles);
        geneRegisterer.reIndexFile(createTabixIndex);
        return geneFile;
    }

    private void downloadFileForRegistration(String requestPath, IndexedFileRegistrationRequest request) {
        final File newFile;

        try {
            newFile = downloadFileManager.downloadFromURL(requestPath);
        } catch (IOException e) {
            throw new RegistrationException("Error while downloading Gene file: " + requestPath, e);
        }

        request.setIndexPath(null);
        request.setName(request.getName() != null ? request.getName() : FilenameUtils.getBaseName(requestPath));
        request.setPath(newFile.getPath());
    }

    private GeneFile registerGeneFileFromFile(final FeatureIndexedFileRegistrationRequest request) {
        final GeneFile geneFile = new GeneFile();
        geneFile.setSource(request.getPath());
        final Long geneFileId = geneFileManager.createGeneFileId();

        if (isFeatureCounts(request.getPath())) {
            request.setName(request.getName() != null
                    ? request.getName()
                    : Paths.get(request.getPath()).getFileName().toString());
            final String gffFilePath = buildGffFileNameFromFeatureCounts(request.getPath(), geneFileId);
            new FeatureCountsToGffConvertor().convert(request.getPath(), gffFilePath, fileManager.getTempDir(),
                    featureCountsMaxMemory);
            request.setPath(gffFilePath);
            geneFile.setFormat(BiologicalDataItemFormat.FEATURE_COUNTS);
            request.setType(BiologicalDataItemResourceType.FILE);
        }

        String path = request.getPath();
        final File file = new File(path);
        geneFile.setId(geneFileId);
        geneFile.setCompressed(IOHelper.isGZIPFile(file.getName()));
        geneFile.setPath(path);
        geneFile.setName(request.getName() != null ? request.getName() : file.getName());
        geneFile.setType(request.getType());
        geneFile.setCreatedDate(new Date());
        geneFile.setReferenceId(request.getReferenceId());
        geneFile.setPrettyName(request.getPrettyName());

        fileManager.makeGeneDir(geneFile.getId());

        if (GenbankUtils.isGenbank(path)) {
            String geneDir = fileManager.getGeneDir(geneFile.getId());
            Assert.notNull(geneDir, getMessage(MessageCode.RESOURCE_NOT_FOUND));
            Path gffFilePath = Paths.get(geneDir,
                    FilenameUtils.removeExtension(geneFile.getName()) + GffCodec.GFF_EXTENSION);
            genbankManager.genbankToGff(path, gffFilePath);
            path = gffFilePath.toString();
            geneFile.setPath(path);
            request.setPath(path);
        }

        if (StringUtils.isNotBlank(request.getIndexPath())) {
            BiologicalDataItem indexItem = new BiologicalDataItem();
            indexItem.setCreatedDate(new Date());
            indexItem.setPath(request.getIndexPath());
            indexItem.setSource(request.getIndexPath());
            indexItem.setFormat(BiologicalDataItemFormat.GENE_INDEX);
            indexItem.setType(request.getIndexType());
            indexItem.setName("");

            geneFile.setIndex(indexItem);
        }

        try {
            long geneId = geneFile.getId();
            biologicalDataItemManager.createBiologicalDataItem(geneFile);
            geneFile.setBioDataItemId(geneFile.getId());
            geneFile.setId(geneId);

            log.info(getMessage(MessagesConstants.INFO_GENE_REGISTER, geneFile.getId(), geneFile.getPath()));
            GeneRegisterer geneRegisterer = new GeneRegisterer(referenceGenomeManager, fileManager, featureIndexManager,
                    geneFile, createHelperFiles);

            geneRegisterer.processRegistration(request);
            biologicalDataItemManager.createBiologicalDataItem(geneFile.getIndex());
            geneFileManager.create(geneFile);
        } catch (IOException e) {
            throw new RegistrationException("Error while Gene file registration: " + geneFile.getPath(), e);
        }  finally {
            if (geneFile.getId() != null && !geneFileManager.geneFileExists(geneFile.getId())) {
                if (Objects.nonNull(geneFile.getBioDataItemId())) {
                    biologicalDataItemManager.deleteBiologicalDataItem(geneFile.getBioDataItemId());
                }
                try {
                    fileManager.deleteFeatureFileDirectory(geneFile);
                } catch (IOException e) {
                    log.error("Unable to delete directory for " + geneFile.getName(), e);
                }
            }
        }
        return geneFile;
    }

    private GeneFile registerGeneFileFromUrl(final FeatureIndexedFileRegistrationRequest request) {
        Reference reference = referenceGenomeManager.load(request.getReferenceId());
        Map<String, Chromosome> chromosomeMap = Utils.makeChromosomeMap(reference);

        GeneFile geneFile = null;

        final String extension = Utils.getFileExtension(request.getPath());
        GffCodec.GffType gffType = GffCodec.GffType.forExt(extension);
        AsciiFeatureCodec<GeneFeature> codec = new GffCodec(gffType);

        try (FeatureReader<GeneFeature> reader = AbstractEnhancedFeatureReader.getFeatureReader(request.getPath(),
                request.getIndexPath(), codec, true, indexCache)) {
            geneFile = createGeneFile(request);
            boolean hasGenes = false;
            for (Map.Entry<String, Chromosome> chrEntry : chromosomeMap.entrySet()) {
                CloseableIterator<GeneFeature> iterator = Utils.query(reader, chrEntry.getKey(), 1,
                        chrEntry.getValue().getSize());
                if (iterator.hasNext()) {
                    hasGenes = true;
                    break;
                }
            }

            Assert.isTrue(hasGenes, getMessage(MessagesConstants.ERROR_FILE_CORRUPTED_OR_EMPTY, request.getPath(),
                    reference.getName()));

            biologicalDataItemManager.createBiologicalDataItem(geneFile.getIndex());
            geneFileManager.create(geneFile);
        }  catch (IOException e) {
            throw new RegistrationException(getMessage(ERROR_REGISTER_FILE, request.getName()), e);
        } finally {
            if (geneFile != null && geneFile.getId() != null &&
                    !geneFileManager.geneFileExists(geneFile.getId())) {
                biologicalDataItemManager.deleteBiologicalDataItem(geneFile.getBioDataItemId());
            }
        }

        return geneFile;
    }

    private GeneFile createGeneFile(final FeatureIndexedFileRegistrationRequest request) {
        GeneFile geneFile = new GeneFile();
        File file = new File(request.getPath());

        geneFile.setId(geneFileManager.createGeneFileId());
        geneFile.setCompressed(IOHelper.isGZIPFile(file.getName()));
        geneFile.setPath(request.getPath());
        geneFile.setName(request.getName() != null ? request.getName() : file.getName());
        geneFile.setType(request.getType());
        geneFile.setCreatedDate(new Date());
        geneFile.setReferenceId(request.getReferenceId());

        if (StringUtils.isNotBlank(request.getIndexPath())) {
            BiologicalDataItem indexItem = new BiologicalDataItem();
            indexItem.setCreatedDate(new Date());
            indexItem.setPath(request.getIndexPath());
            indexItem.setFormat(BiologicalDataItemFormat.GENE_INDEX);
            indexItem.setType(BiologicalDataItemResourceType.FILE);
            indexItem.setName("");

            geneFile.setIndex(indexItem);
        }

        long geneId = geneFile.getId();
        biologicalDataItemManager.createBiologicalDataItem(geneFile);
        geneFile.setBioDataItemId(geneFile.getId());
        geneFile.setId(geneId);

        return geneFile;
    }

    /**
     * Removes gene file metadata from the system, deleting all additional files that were created
     *
     * @param geneFileId {@code long} a gene fiel ID
     * @return deleted {@code GeneFile} entity
     * @throws IOException
     */
    public GeneFile unregisterGeneFile(final long geneFileId) throws IOException {
        Assert.notNull(geneFileId, MessagesConstants.ERROR_INVALID_PARAM);
        Assert.isTrue(geneFileId > 0, MessagesConstants.ERROR_INVALID_PARAM);
        final GeneFile fileToDelete = geneFileManager.load(geneFileId);

        activityService.deleteByFileId(geneFileId);
        geneFileManager.delete(fileToDelete);
        fileManager.deleteFeatureFileDirectory(fileToDelete);

        return fileToDelete;
    }

    /**
     * Load genes as a {@code NggbIntervalTreeMap} to allow fast region queries. Only gene and exon features are being
     * loaded: no transcripts and etc.
     *
     * @param geneFile a {@code GeneFile}, from which to load genes and exons
     * @param startIndex a start of an interval from which to load genes
     * @param endIndex an end of an interval from which to load genes
     * @param chromosome a {@code Chromosome} for which to load genes
     * @return a {@code NggbIntervalTreeMap}, containing gene and exon features
     * @throws GeneReadingException
     */
    public NggbIntervalTreeMap<Gene> loadGenesIntervalMap(GeneFile geneFile, int startIndex, int endIndex,
                                                          Chromosome chromosome) throws GeneReadingException {
        double time1 = Utils.getSystemTimeMilliseconds();
        int numOfSubIntervals = ParallelTaskExecutionUtils.splitFileReadingInterval(startIndex, endIndex, log,
                taskExecutorService.getTaskNumberOfThreads());
        final List<Callable<Boolean>> callables = new ArrayList<>(numOfSubIntervals);
        final NggbIntervalTreeMap<Gene> genesRangeMap = new NggbIntervalTreeMap<>();

        for (int i = 0; i < numOfSubIntervals; i++) {
            final int factor = i;
            final int num = numOfSubIntervals;
            callables.add(() -> addGenesToIntervalMap(geneFile, genesRangeMap, startIndex, endIndex, factor, num,
                                                      chromosome));
        }

        List<Future<Boolean>> results;
        try {
            results = taskExecutorService.getExecutorService().invokeAll(callables);
        } catch (InterruptedException | AssertionError e) {
            throw new GeneReadingException(geneFile, chromosome, startIndex, endIndex, e);
        }

        results.stream().map(future -> {
            try {
                return future != null ? future.get() : null;
            } catch (InterruptedException | ExecutionException e) {
                log.error(getMessage(MessagesConstants.ERROR_GENE_BATCH_LOAD, geneFile.getId(), chromosome.getId(),
                                        e));
            }
            return null;
        });

        genesRangeMap.setMaxEndIndex(endIndex);
        genesRangeMap.setMinStartIndex(startIndex);
        double time2 = Utils.getSystemTimeMilliseconds();
        log.debug(getMessage(MessagesConstants.DEBUG_GENE_BATCH_LOAD, geneFile.getName(), chromosome.getName(),
                                startIndex, endIndex, time2 - time1));

        return genesRangeMap;
    }

    private boolean addGenesToIntervalMap(GeneFile geneFile, NggbIntervalTreeMap<Gene> genesRangeMap, int startIndex,
                                       int endIndex, int factor, int num, Chromosome chromosome) throws IOException {
        double time0 = Utils.getSystemTimeMilliseconds();
        try (AbstractFeatureReader<GeneFeature, LineIterator> featureReader = fileManager.makeGeneReader(
            geneFile, GeneFileType.ORIGINAL)) {
            log.debug(getMessage(MessagesConstants.DEBUG_THREAD_STARTS,
                                    Thread.currentThread().getName()));
            double time11 = Utils.getSystemTimeMilliseconds();
            log.debug(getMessage(MessagesConstants.DEBUG_THREAD_READER_CREATED,
                                    Thread.currentThread().getName(), time11 - time0));

            int start = startIndex + factor * ParallelTaskExecutionUtils.MAX_BLOCK_SIZE;
            int end;
            if (factor != num - 1) {
                end = startIndex + (factor + 1) * ParallelTaskExecutionUtils.MAX_BLOCK_SIZE;
            } else {
                end = endIndex;
            }
            log.debug(getMessage(MessagesConstants.DEBUG_THREAD_INTERVAL),
                         Thread.currentThread().getName(), start, end);
            CloseableIterator<GeneFeature> iterator = Utils.query(featureReader, chromosome, start, end);
            double time21 = Utils.getSystemTimeMilliseconds();
            log.debug(getMessage(MessagesConstants.DEBUG_THREAD_QUERY_TIME,
                                    Thread.currentThread().getName(), time21 - time11));

            time11 = Utils.getSystemTimeMilliseconds();

            iterator.forEachRemaining(f -> {
                if (GeneUtils.isGene(f) || GeneUtils.isExon(f)) {
                    Gene g = new Gene(f);
                    synchronized (genesRangeMap) {
                        genesRangeMap.put(new Interval(chromosome.getName(), g.getStartIndex(),
                                                       g.getEndIndex()), g);
                    }
                }
            });

            time21 = Utils.getSystemTimeMilliseconds();
            log.debug(getMessage(MessagesConstants.DEBUG_THREAD_WALKTHROUGH_TIME,
                                    Thread.currentThread().getName(), time21 - time11));

            log.debug(getMessage(MessagesConstants.DEBUG_THREAD_ENDS, Thread.currentThread().getName()));
            return true;
        }
    }

    /**
     * Convert Gene track to transfer to client. Simplify gene objects to GeneHighLevel and GeneLowLevel objects,
     * and add amino acid sequences
     *
     * @param genes a list of Gene objects to convert
     * @param aminoAcids a list of amino acid sequences to add
     * @return a list of GeneHigLevel objects, ready to transfer to client
     */
    public List<GeneHighLevel> convertGeneTrackForClient(final List<Gene> genes,
                                                         final Map<Gene, List<ProteinSequenceEntry>> aminoAcids) {
        final List<GeneHighLevel> result = new ArrayList<>(genes.size());
        for (Gene gene : genes) {
            GeneHighLevel geneHighLevel = new GeneHighLevel(gene);
            geneHighLevel.setItems(recursiveConvert(gene, aminoAcids));
            result.add(geneHighLevel);
        }

        return result;
    }

    private List<GeneLowLevel> recursiveConvert(final Gene gene,
                                                final Map<Gene, List<ProteinSequenceEntry>> aminoAcids) {
        if (gene == null || CollectionUtils.isEmpty(gene.getItems())) {
            return Collections.emptyList();
        }

        final List<GeneLowLevel> items = new ArrayList<>();
        for (Gene item : gene.getItems()) {
            final GeneLowLevel geneLowLevel = new GeneLowLevel(item);
            items.add(geneLowLevel);

            setProteinSequences(aminoAcids, item, geneLowLevel);

            final List<GeneLowLevel> lows = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(item.getItems())) {
                for (Gene lowItem : item.getItems()) {
                    final GeneLowLevel itemLowLevel = new GeneLowLevel(lowItem);
                    itemLowLevel.setItems(recursiveConvert(lowItem, aminoAcids));
                    lows.add(itemLowLevel);
                }
            }

            geneLowLevel.setItems(lows);
        }

        return items;
    }

    private void setProteinSequences(final Map<Gene, List<ProteinSequenceEntry>> aminoAcids, Gene item,
                                     GeneLowLevel geneLowLevel) {
        if (MapUtils.isNotEmpty(aminoAcids) && GeneUtils.isTranscript(item)) {
            final List<ProteinSequenceEntry> psEntryList = aminoAcids.get(item);
            if (CollectionUtils.isNotEmpty(psEntryList)) {
                List<ProteinSequence> psList =
                    psEntryList.stream().map(ProteinSequence::new).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(psList)) {
                    geneLowLevel.setPsList(psList);
                }
            }
        }
    }

    /**
     * Returns a histogram of a gene track, showing amount of genes on chromosome regions. In a form of a Wig track
     *
     * @param track a {@code Track} to populate with histogram
     * @return a {@code Track}, populated with gene histogram
     * @throws HistogramReadingException
     */
    public Track<Wig> loadHistogram(final Track<Wig> track) throws HistogramReadingException {
        TrackHelper.validateHistogramTrack(track);

        final GeneFile geneFile = geneFileManager.load(track.getId());
        if (BiologicalDataItemResourceType.FILE != geneFile.getType() &&
                BiologicalDataItemResourceType.S3 != geneFile.getType()) {
            track.setBlocks(Collections.emptyList());
            return track;
        }

        final Chromosome chromosome = referenceGenomeManager.loadChromosome(track.getChromosome().getId());
        Assert.notNull(chromosome, getMessage(MessagesConstants.ERROR_CHROMOSOME_ID_NOT_FOUND));

        if (fileManager.checkHistogramExists(geneFile, chromosome.getName())) {
            try {
                track.setBlocks(fileManager.loadHistogram(geneFile, chromosome.getName()));
                return track;
            } catch (IOException e) {
                throw new HistogramReadingException(track, e);
            }
        } else {
            try {
                track.setBlocks(buildHistogram(chromosome, geneFile));
            } catch (HistogramWritingException e) {
                throw new HistogramReadingException(track, e);
            }
            return track;
        }
    }

    private List<Wig> buildHistogram(final Chromosome chromosome, final GeneFile geneFile)
        throws HistogramWritingException {

        final Track<Wig> track = new Track<>();

        track.setStartIndex(0);
        track.setEndIndex(chromosome.getSize());

        try {
            if (!trackHelper.loadAndSetBounds(track, geneFile, chromosome)) {
                return Collections.emptyList();
            }
        } catch (IOException e) {
            throw new HistogramWritingException(e);
        }

        final List<Pair<Integer, Integer>> intervals = HistogramUtils.createIntervals(0, chromosome.getSize());

        final double time1 = Utils.getSystemTimeMilliseconds();
        final int numberOfThreads = taskExecutorService.getTaskNumberOfThreads();
        final int portionSize = intervals.size() / numberOfThreads;
        final List<Callable<List<Wig>>> callables = new ArrayList<>(numberOfThreads);

        // Create threads and pass each a portion of intervals
        for (int i = 0; i < numberOfThreads; i++) {
            final List<Pair<Integer, Integer>> portion;
            if (i == numberOfThreads - 1) {
                final int remainder = intervals.size() % numberOfThreads;
                portion = intervals.subList(portionSize * i,
                                            Math.min(portionSize * (i + 1) + remainder, intervals.size()));
            } else {
                portion = intervals.subList(portionSize * i, Math.min(portionSize * (i + 1), intervals.size()));
            }

            callables.add(() -> readHistogramPortion(track, geneFile, chromosome, GeneFileType.ORIGINAL, portion));
        }

        final List<Wig> newHistogram =
                HistogramUtils.executeHistogramCreation(taskExecutorService.getExecutorService(), callables);
        final double time2 = Utils.getSystemTimeMilliseconds();
        log.debug("Reading histogram, took {} ms", time2 - time1);

        writeHistogram(newHistogram, geneFile, chromosome);

        return newHistogram;
    }

    private void writeHistogram(final List<Wig> newHistogram, GeneFile geneFile, Chromosome chromosome)
        throws HistogramWritingException {
        if (!newHistogram.isEmpty()) {
            try {
                fileManager.writeHistogram(geneFile, chromosome.getName(), newHistogram);
            } catch (IOException e) {
                throw new HistogramWritingException(e);
            }
        }
    }

    /**
     * Returns next/previous feature of the specified chromosome in specified gene file
     *
     * @param fromPosition {@code int} the position from which look for next/previous feature
     * @param geneFileId   {@code int} ID of the gene file
     * @param chromosomeId {@code int} ID of th chromosome
     * @param forward      {@code boolean} flag that determines direction to look for feature
     * @return {@code Gene} next or previous feature
     */
    public Gene getNextOrPreviousFeature(final int fromPosition, final long geneFileId, final long chromosomeId,
                                         final boolean forward) throws IOException {
        final GeneFile geneFile = geneFileManager.load(geneFileId);
        final Chromosome chromosome = referenceGenomeManager.loadChromosome(chromosomeId);
        Assert.notNull(chromosome, getMessage(MessagesConstants.ERROR_CHROMOSOME_ID_NOT_FOUND));

        int end = forward ? chromosome.getSize() : 0;
        if (geneFile.getCompressed()) {
            Pair<Integer, Integer> bounds = trackHelper.loadBounds(geneFile, chromosome);
            Assert.notNull(bounds, getMessage(MessageCode.NO_SUCH_CHROMOSOME));
            end = forward ? bounds.getRight() : bounds.getLeft();
        }

        double time1 = Utils.getSystemTimeMilliseconds();
        try (AbstractFeatureReader<GeneFeature, LineIterator> featureReader =
                     fileManager.makeGeneReader(geneFile, GeneFileType.ORIGINAL)) {
            double time2 = Utils.getSystemTimeMilliseconds();
            log.debug("Reader creation {} {} ms", Thread.currentThread().getName(), time2 - time1);

            if (forward) {
                return getNextGeneFeature(featureReader, chromosome, fromPosition, end);
            } else {
                return getPreviousGeneFeature(featureReader, chromosome, fromPosition, end);
            }
        }
    }

    private Gene getNextGeneFeature(AbstractFeatureReader<GeneFeature, LineIterator> featureReader,
                                    Chromosome chromosome, int fromPosition, int end) throws IOException {
        if (fromPosition + 1 >= end) { // no next features
            return null;
        }

        double time1 = Utils.getSystemTimeMilliseconds();
        CloseableIterator<GeneFeature> iterator = Utils.query(featureReader, chromosome.getName(), fromPosition + 1,
                                                              end);

        double time2 = Utils.getSystemTimeMilliseconds();
        log.debug(getMessage(MessagesConstants.DEBUG_QUERY_TIME, time2 - time1));

        time1 = Utils.getSystemTimeMilliseconds();

        while (iterator.hasNext()) {
            final GeneFeature feature = iterator.next();
            if (GeneUtils.isExon(feature)) {
                return new Gene(feature);
            }
        }

        time2 = Utils.getSystemTimeMilliseconds();
        log.debug(getMessage(MessagesConstants.DEBUG_WALKTHROUGH_TIME, time2 - time1));

        return null;
    }

    private Gene getPreviousGeneFeature(AbstractFeatureReader<GeneFeature, LineIterator> featureReader,
                                        Chromosome chromosome, int fromPosition, int end) throws IOException {
        if (fromPosition - 1 <= end) {
            return null;
        }

        GeneFeature lastFeature = null;
        int i = 0;
        boolean lastChunk = false;
        while (lastFeature == null) {
            if (lastChunk) {
                break;
            }

            int firstIndex = fromPosition - Constants.PREV_FEATURE_OFFSET * (i + 1);
            final int lastIndex = fromPosition - 1 - Constants.PREV_FEATURE_OFFSET * i;
            if (firstIndex < end) {
                firstIndex = end;
                lastChunk = true; // this is the last chunk to be traversed
            }

            double time1 = Utils.getSystemTimeMilliseconds();
            CloseableIterator<GeneFeature> iterator = Utils.query(featureReader, chromosome.getName(), firstIndex,
                                                                  lastIndex);
            // instead traversing the whole file, read it by small chunks, 10000 bps
            // long. Hopefully, the desired feature will be in first/second chunk

            double time2 = Utils.getSystemTimeMilliseconds();
            log.debug(getMessage(MessagesConstants.DEBUG_QUERY_TIME, time2 - time1));

            time1 = Utils.getSystemTimeMilliseconds();
            while (iterator.hasNext()) {
                GeneFeature feature = iterator.next();
                if (GeneUtils.isExon(feature)) {
                    lastFeature = feature;
                }
            }
            time2 = Utils.getSystemTimeMilliseconds();
            log.debug(getMessage(MessagesConstants.DEBUG_WALKTHROUGH_TIME, time2 - time1));

            i++;
        }

        return lastFeature != null ? new Gene(lastFeature) : null;
    }

    /**
     * Loads exon features in the requested viewport
     *
     * @param geneFileId an ID of GeneFile to load exons from
     * @param chromosomeId an ID of chromosome to load exons from
     * @param centerPosition a position of center of the screen
     * @param viewPortSize a size of the screen
     * @return a List of exon intervals. Overlapping exons are joined together
     * @throws IOException
     */
    public List<Block> loadExonsInViewPort(long geneFileId, long chromosomeId, int centerPosition, int viewPortSize,
                                          int intronLength)
            throws IOException {
        double time1 = Utils.getSystemTimeMilliseconds();
        final GeneFile geneFile = geneFileManager.load(geneFileId);
        final Chromosome chromosome = referenceGenomeManager.loadChromosome(chromosomeId);
        Assert.notNull(chromosome, getMessage(MessagesConstants.ERROR_CHROMOSOME_ID_NOT_FOUND));

        final Map<String, Pair<Integer, Integer>> metaMap = fileManager.loadIndexMetadata(geneFile);
        Pair<Integer, Integer> bounds = metaMap.get(chromosome.getName());
        if (bounds == null) {
            bounds = metaMap.get(Utils.changeChromosomeName(chromosome.getName()));
        }

        Assert.notNull(bounds, getMessage(MessageCode.NO_SUCH_CHROMOSOME));
        int end = bounds.getRight();
        int start = bounds.getLeft();

        IntervalTree<Block> intervalTree = new IntervalTree<>();

        try (AbstractFeatureReader<GeneFeature, LineIterator> featureReader =
                     fileManager.makeGeneReader(geneFile, GeneFileType.ORIGINAL)) {
            loadExonsForward(centerPosition, viewPortSize, chromosome, intronLength, end, intervalTree,
                    featureReader);
            loadExonsBackwards(centerPosition, viewPortSize, chromosome, intronLength, start, intervalTree,
                    featureReader);
        }

        List<Block> exons = new ArrayList<>();
        for (IntervalTree.Node<Block> node : intervalTree) {
            exons.add(node.getValue());
        }
        double time2 = Utils.getSystemTimeMilliseconds();
        log.info(getMessage(MessagesConstants.DEBUG_GENE_EXONS_LOAD, time2 - time1));
        return exons;
    }

    /**
     * Loads exons in the requested range
     *
     * @param geneFileId an ID of GeneFile to load exons from
     * @param chromosomeId an ID of chromosome to load exons from
     * @param startIndex a start of a range on a chromosome
     * @param endIndex an end of the range on a chromosome
     * @return a List of exon intervals. Overlapping exons are joined together
     * @throws IOException
     */
    public List<Block> loadExonsInTrack(long geneFileId, long chromosomeId, int startIndex, int endIndex,
                                        int intronLength)
            throws IOException {
        double time1 = Utils.getSystemTimeMilliseconds();
        final GeneFile geneFile = geneFileManager.load(geneFileId);
        final Chromosome chromosome = referenceGenomeManager.loadChromosome(chromosomeId);
        Assert.notNull(chromosome, getMessage(MessagesConstants.ERROR_CHROMOSOME_ID_NOT_FOUND));

        Track fakeTrack = new Track();
        fakeTrack.setStartIndex(startIndex);
        fakeTrack.setEndIndex(endIndex);

        if (geneFile.getCompressed() && !trackHelper.loadAndSetBounds(fakeTrack, geneFile, chromosome)) {
            return Collections.emptyList();
        }

        List<Block> exons = new ArrayList<>();
        try (AbstractFeatureReader<GeneFeature, LineIterator> featureReader =
                     fileManager.makeGeneReader(geneFile, GeneFileType.ORIGINAL);
             CloseableIterator<GeneFeature> iterator = Utils.query(featureReader, chromosome, fakeTrack.getStartIndex(),
                     fakeTrack.getEndIndex())) {

            while (iterator.hasNext()) {
                GeneFeature feature = iterator.next();
                processExonForTrack(feature, exons, intronLength);
            }
        }

        double time2 = Utils.getSystemTimeMilliseconds();
        log.info(getMessage(MessagesConstants.DEBUG_GENE_EXONS_LOAD, time2 - time1));
        return exons;
    }

    private void processExonForTrack(GeneFeature feature, List<Block> exons, int intronLength) {
        if (EXON_FEATURE_NAME.equalsIgnoreCase(feature.getFeature())) {
            Block exon = createExon(feature, intronLength);
            if (!exons.isEmpty() &&
                exons.get(exons.size() - 1).getEndIndex() > exon.getStartIndex()) {
                exons.set(exons.size() - 1, mergeExons(exons.get(exons.size() - 1), exon));
            } else {
                exons.add(exon);
            }
        }
    }


    /**
     * @param pdbID {@code String} id in BD
     * @return a {@code List} of {@code Record}, that entity from BD
     * @throws ExternalDbUnavailableException
     */
    public DimStructure getPBDItemsFromBD(final String pdbID) throws ExternalDbUnavailableException {
        Assert.notNull(pdbID);
        final List<Record> recordList = pBDataManager.fetchRCSBEntry(pdbID).getRecord();
        final List<Alignment> alignmentList = pBDataManager.fetchPdbMapEntry(pdbID).getAlignment();
        return parseTo(recordList, alignmentList);
    }

    public List<Activity> loadGeneActivity(final Long fileId, final String uid) {
        geneFileManager.load(fileId);
        return activityService.getByItemIdAndUid(fileId, uid);
    }

    private DimStructure parseTo(final List<Record> recordList, final List<Alignment> alignmentList) {
        DimStructure dimStructure = null;
        for (Record record : recordList) {
            if (null == dimStructure) {
                dimStructure = dimStructureFromRecord(record);
            } else {
                dimStructure.addToEntities(dimSEntityFromRecord(record));
            }
        }
        fillMinMax(dimStructure, alignmentList);
        return dimStructure;
    }


    private void fillMinMax(final DimStructure structure, final List<Alignment> alignmentList) {
        final Map<String, ChainMinMax> chainMap = parseAlignment(alignmentList);
        final String structureId = structure.getStructureId();
        for (DimEntity entity : structure.getEntities()) {
            final ChainMinMax chain = chainMap.get(structureId + "." + entity.getChainId());
            if (chain != null) {
                entity.setPdbStart(chain.getStart());
                entity.setPdbEnd(chain.getEnd());
                entity.setUnpEnd(chain.getOtherEnd());
                entity.setUnpStart(chain.getOtherStart());
            }
        }
    }

    /**
     * Parse query result. It's hard binding. it's need to do it simple with simple structure
     *
     * @param alignmentList
     */
    private Map<String, ChainMinMax> parseAlignment(final List<Alignment> alignmentList) {
        return alignmentList.stream().map(alignment -> {
            final ChainMinMax chainMinMax = new ChainMinMax();
            for (PdbBlock pdbBlock : alignment.getBlock()) {
                for (Segment segment : pdbBlock.getSegment()) {
                    if (segment.getIntObjectId().contains(".")) {
                        chainMinMax.setName(segment.getIntObjectId());
                        chainMinMax.addEnd(segment.getEnd());
                        chainMinMax.addStart(segment.getStart());
                    } else {
                        chainMinMax.addOtherEnd(segment.getEnd());
                        chainMinMax.addOtherStart(segment.getStart());
                    }
                }
            }
            return chainMinMax;
        }).collect(Collectors.toMap(ChainMinMax::getName, cMM -> cMM));
    }

    private List<Wig> readHistogramPortion(final Track<Wig> track, final GeneFile geneFile, final Chromosome
            chromosome, final GeneFileType type, final List<Pair<Integer, Integer>> portion) throws IOException {
        double time0 = Utils.getSystemTimeMilliseconds();
        try (AbstractFeatureReader<GeneFeature, LineIterator> featureReader = fileManager.makeGeneReader(
                geneFile, type)) {
            log.debug("Thread {} starts", Thread.currentThread().getName());
            double time11 = Utils.getSystemTimeMilliseconds();
            log.debug("Thread {} Reader creation {} ms", Thread.currentThread().getName(), time11 - time0);

            List<Wig> wigs = new ArrayList<>(portion.size());
            for (Pair<Integer, Integer> interval : portion) {
                if (interval.getRight() > track.getStartIndex() && interval.getLeft() < track.getEndIndex()) {
                    log.debug("Thread {} Interval: {} - {}", Thread.currentThread().getName(),
                            interval.getLeft(), interval.getRight());

                    CloseableIterator<GeneFeature> iterator = Utils.query(featureReader, chromosome.getName(),
                                Math.max(track.getStartIndex(), interval.getLeft()),
                                Math.min(interval.getRight(), track.getEndIndex()));

                    double time21 = Utils.getSystemTimeMilliseconds();
                    log.debug("Thread {} Query took {} ms", Thread.currentThread().getName(), time21 - time11);

                    time11 = Utils.getSystemTimeMilliseconds();

                    int genesCount = iterator.toList().size();

                    time21 = Utils.getSystemTimeMilliseconds();
                    log.debug("Thread {} Walkthrough took {} ms",
                            Thread.currentThread().getName(), time21 - time11);

                    log.debug("Thread {} ends", Thread.currentThread().getName());
                    HistogramUtils.addToHistogramPortion(wigs, genesCount, interval);
                }
            }

            return wigs;
        }
    }

    private DimStructure dimStructureFromRecord(final Record record) {
        DimStructure structure = new DimStructure();
        structure.setClassification(record.getClassification());
        structure.setStructureId(record.getStructureId());
        structure.setStructureTitle(record.getStructureTitle());
        structure.addToEntities(dimSEntityFromRecord(record));
        return structure;
    }

    private DimEntity dimSEntityFromRecord(final Record record) {
        DimEntity entity = new DimEntity();
        entity.setChainId(record.getChainId());
        entity.setCompound(!"null".equals(record.getCompound()) ? record.getCompound() :
                           record.getUniprotRecommendedName());
        return entity;
    }

    private int processExon(IntervalTree<Block> intervalTree, int totalLength, GeneFeature feature,
                            int intronLength, int centerPosition, boolean forward) {
        int currentLength = 0;
        if (EXON_FEATURE_NAME.equalsIgnoreCase(feature.getFeature())) {
            Block exon = createExon(feature, intronLength);

            Iterator<IntervalTree.Node<Block>> nodeIterator = intervalTree.overlappers(exon.getStartIndex(),
                    exon.getEndIndex());
            if (nodeIterator.hasNext()) {
                Block merged = exon;
                while (nodeIterator.hasNext()) {
                    Block toMerge = nodeIterator.next().getValue();
                    merged = mergeExons(merged, toMerge);
                    currentLength = currentLength - calculateExonLength(toMerge, centerPosition, forward)
                                  + calculateExonLength(merged, centerPosition, forward);
                    nodeIterator.remove();
                }

                intervalTree.put(merged.getStartIndex(), merged.getEndIndex(), merged);
            } else {
                currentLength += calculateExonLength(exon, centerPosition, forward);
                intervalTree.put(exon.getStartIndex(), exon.getEndIndex(), exon);
            }
        }
        return totalLength + currentLength;
    }

    /**
     * Calculates exon length for viewport exon query
     * @param exon {@code Block} exon to calculate length
     * @param centerPosition position of the center of a viewport
     * @param forward is block forward from center of the viewport
     * @return length of an exon
     */
    public int calculateExonLength(Block exon, int centerPosition, boolean forward) {
        int startPosition;
        int endPosition;
        if (forward) {
            startPosition = Math.max(exon.getStartIndex(), centerPosition);
            endPosition = exon.getEndIndex();
        } else {
            startPosition = exon.getStartIndex();
            endPosition = Math.min(exon.getEndIndex(), centerPosition);
        }

        return endPosition - startPosition;
    }

    private Block createExon(GeneFeature feature, int intronLength) {
        Block exon = new Block();
        exon.setStartIndex(Math.max(feature.getStart() - intronLength, 1));
        exon.setEndIndex(feature.getEnd() + intronLength);
        return exon;
    }

    private Block mergeExons(Block e1, Block e2) {
        Block res = new Block();
        res.setStartIndex(Math.min(e1.getStartIndex(), e2.getStartIndex()));
        res.setEndIndex(Math.max(e1.getEndIndex(), e2.getEndIndex()));

        return res;
    }

    private void loadExonsBackwards(int centerPosition, int viewPortSize, Chromosome chromosome,
                                    int intronLength, int featuresStart,
                                    final IntervalTree<Block> intervalTree,
                                    final AbstractFeatureReader<GeneFeature, LineIterator> featureReader)
            throws IOException {
        int totalLength = 0;
        // check if some of exons, got by forward lookup are good for backwards
        Iterator<IntervalTree.Node<Block>> nodeIterator = intervalTree.overlappers(featuresStart, centerPosition);
        while (nodeIterator.hasNext()) {
            Block exon = nodeIterator.next().getValue();
            totalLength += calculateExonLength(exon, centerPosition, false);
        }

        int i = 0;
        boolean lastChunk = false;

        while (totalLength < viewPortSize / 2) {
            if (lastChunk) {
                break;
            }

            int firstIndex = centerPosition - EXON_SEARCH_CHUNK_SIZE * (i + 1);
            final int lastIndex = centerPosition - 1 - EXON_SEARCH_CHUNK_SIZE * i;
            if (firstIndex < featuresStart) {
                firstIndex = featuresStart;
                lastChunk = true; // this is the last chunk to be traversed
            }

            CloseableIterator<GeneFeature> iterator = Utils.query(featureReader, chromosome, firstIndex, lastIndex);
                // instead traversing the whole file, read it by small chunks, 100000 bps
                // long. Hopefully, the desired window will be covered by first/second chunk

            if (iterator.hasNext()) {
                List<GeneFeature> featuresChunk = iterator.toList();
                ListIterator<GeneFeature> listIterator = featuresChunk.listIterator(featuresChunk.size() - 1);

                while (listIterator.hasPrevious() && totalLength < viewPortSize / 2) {
                    GeneFeature feature = listIterator.previous();
                    totalLength =
                        processExon(intervalTree, totalLength, feature, intronLength, centerPosition, false);
                }
            }

            i++;
        }
    }

    private void loadExonsForward(int centerPosition, int viewPortSize, Chromosome chromosome,
                                  int intronLength, int endFeatures, IntervalTree<Block> intervalTree,
                                  AbstractFeatureReader<GeneFeature, LineIterator> featureReader) throws IOException {
        CloseableIterator<GeneFeature> iterator = featureReader.query(chromosome.getName(), centerPosition,
                endFeatures);

        if (!iterator.hasNext()) {
            iterator = featureReader.query(Utils.changeChromosomeName(chromosome.getName()), centerPosition,
                    endFeatures);
        }

        int totalLength = 0;
        while (iterator.hasNext() && totalLength < viewPortSize / 2) {
            GeneFeature feature = iterator.next();
            totalLength = processExon(intervalTree, totalLength, feature, intronLength, centerPosition, true);
        }
    }

    private String buildGffFileNameFromFeatureCounts(final String featureCountsPath, final Long geneFileId) {
        final String geneFolder = fileManager.makeGeneDir(geneFileId);
        final String featureCountsFileName = FilenameUtils.getBaseName(featureCountsPath);
        return Paths.get(geneFolder, featureCountsFileName + GffCodec.GFF_EXTENSION).toString();
    }

    private boolean isFeatureCounts(final String path) {
        return CollectionUtils.emptyIfNull(featureCountsExtensions).stream()
                .anyMatch(path::endsWith);
    }
}
