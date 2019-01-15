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

package com.epam.catgenome.manager.bed;

import static com.epam.catgenome.component.MessageHelper.getMessage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.epam.catgenome.manager.bed.parser.NggbBedCodec;
import com.epam.catgenome.util.feature.reader.AbstractEnhancedFeatureReader;
import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.bed.BedFile;
import com.epam.catgenome.entity.bed.BedRecord;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.exception.FeatureFileReadingException;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.manager.FeatureIndexManager;
import com.epam.catgenome.exception.HistogramReadingException;
import com.epam.catgenome.exception.HistogramWritingException;
import com.epam.catgenome.exception.RegistrationException;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.manager.DownloadFileManager;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.TrackHelper;
import com.epam.catgenome.manager.bed.parser.NggbBedFeature;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.util.HistogramUtils;
import com.epam.catgenome.util.IOHelper;
import com.epam.catgenome.util.Utils;
import htsjdk.samtools.util.CloseableIterator;
import com.epam.catgenome.util.feature.reader.AbstractFeatureReader;
import htsjdk.tribble.Feature;
import htsjdk.tribble.readers.LineIterator;

/**
 * Provides service for handling {@code BedFile}: CRUD operations and loading data from the files
 */
@Service
public class BedManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(BedManager.class);

    @Autowired
    private FileManager fileManager;

    @Autowired
    private BiologicalDataItemManager biologicalDataItemManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private TrackHelper trackHelper;

    @Autowired
    private BedFileManager bedFileManager;

    @Autowired
    private DownloadFileManager downloadFileManager;

    @Autowired
    private FeatureIndexManager featureIndexManager;

    @Autowired(required = false)
    private EhCacheBasedIndexCache indexCache;


    private static final Logger LOG = LoggerFactory.getLogger(BedManager.class);

    /**
     * Registers a BED file in the system to work with it in future
     *
     * @param request a registration request
     * @return a BedFile entity, representing BED file in the system
     */
    public BedFile registerBed(final IndexedFileRegistrationRequest request) {
        final String requestPath = request.getPath();
        Assert.isTrue(StringUtils.isNotBlank(requestPath), getMessage(
                MessagesConstants.ERROR_NULL_PARAM, "path"));
        Assert.notNull(request.getReferenceId(), getMessage(MessagesConstants.ERROR_NULL_PARAM, "referenceId"));
        if (request.getType() == null) {
            request.setType(BiologicalDataItemResourceType.FILE);
        }

        BedFile bedFile;
        try {
            bedFile = createBedFile(request);
        } catch (IOException | HistogramReadingException e) {
            throw new RegistrationException(e.getMessage(), e);
        }

        return bedFile;
    }

    /**
     * Loads BED track form the requested file
     *
     * @param track a {@code Track} to fill with BED features
     * @return a {@code Track}, filled with BED features
     * @throws IOException
     */
    public Track<BedRecord> loadFeatures(final Track<BedRecord> track) throws FeatureFileReadingException {
        final Chromosome chromosome = trackHelper.validateTrack(track);

        final BedFile bedFile = bedFileManager.load(track.getId());

        return loadTrackFromFile(track, bedFile, chromosome);
    }

    public Track<BedRecord> loadFeatures(final Track<BedRecord> track, String fileUrl, String indexUrl)
        throws FeatureFileReadingException {
        final Chromosome chromosome = trackHelper.validateUrlTrack(track, fileUrl, indexUrl);

        BedFile nonRegisteredFile;
        try {
            nonRegisteredFile = Utils.createNonRegisteredFile(BedFile.class, fileUrl, indexUrl, chromosome);
        } catch (InvocationTargetException  e) {
            throw new FeatureFileReadingException(fileUrl, e);
        }

        return loadTrackFromFile(track, nonRegisteredFile, chromosome);
    }

    private Track<BedRecord> loadTrackFromFile(Track<BedRecord> track, BedFile bedFile, Chromosome chromosome)
        throws FeatureFileReadingException {
        final double time1 = Utils.getSystemTimeMilliseconds();
        try (AbstractFeatureReader<NggbBedFeature, LineIterator> reader = fileManager.makeBedReader(bedFile)) {
            CloseableIterator<NggbBedFeature> iterator = reader.query(chromosome.getName(), track.getStartIndex(),
                                                                      track.getEndIndex());
            if (!iterator.hasNext()) {
                iterator = reader.query(Utils.changeChromosomeName(chromosome.getName()), track.getStartIndex(),
                                        track.getEndIndex());
            }

            final List<BedRecord> bedRecords;
            if (track.getScaleFactor() >= 1) {
                bedRecords = new ArrayList<>();
                iterator.forEachRemaining(f -> bedRecords.add(new BedRecord(f)));
            } else {
                bedRecords = loadStatisticRecords(track, iterator);
            }
            final double time2 = Utils.getSystemTimeMilliseconds();
            LOG.debug("Reading records from bed file, took {} ms", time2 - time1);
            track.setBlocks(bedRecords);
            return track;
        } catch (IOException e) {
            throw new FeatureFileReadingException(bedFile.getPath(), e);
        }
    }

    /**
     * Loads histogram track for a specified BED file, represented by {@code @Wig} {@code Track}
     *
     * @param track a {@code Track} to fill with histogram
     * @return a {@code Track}, willed with {@code Wig} blocks, representing the histogram
     * @throws HistogramReadingException
     */
    public Track<Wig> loadHistogram(final Track<Wig> track) throws HistogramReadingException {
        TrackHelper.validateHistogramTrack(track);

        final BedFile bedFile = bedFileManager.load(track.getId());
        final Chromosome chromosome = referenceGenomeManager.loadChromosome(track.getChromosome().getId());
        Assert.notNull(chromosome, getMessage(MessagesConstants.ERROR_CHROMOSOME_ID_NOT_FOUND));

        if (fileManager.checkHistogramExists(bedFile, chromosome.getName())) {
            track.setBlocks(loadHistogram(track, bedFile, chromosome));
            return track;
        } else {
            try {
                return trackHelper.createHistogram(track, chromosome, bedFile, (t, f, c, portion) ->
                                                                            readHistogram(t, (BedFile) f, c, portion));
            } catch (HistogramWritingException e) {
                throw new HistogramReadingException(track, e);
            }
        }
    }

    /**
     * Deletes a BED file, specified by ID, from the system, cleaning up all additional files created: indexes, etc.
     *
     * @param bedFileId ID of BedFile to delete
     * @return deleted {@code BedFile} entity
     * @throws IOException
     */
    public BedFile unregisterBedFile(long bedFileId) throws IOException {
        Assert.isTrue(bedFileId > 0, MessagesConstants.ERROR_INVALID_PARAM);
        final BedFile fileToDelete = bedFileManager.load(bedFileId);
        Assert.notNull(fileToDelete, MessagesConstants.ERROR_NO_SUCH_FILE);

        bedFileManager.delete(fileToDelete);
        fileManager.deleteFeatureFileDirectory(fileToDelete);

        return fileToDelete;
    }

    private BedFile createBedFile(IndexedFileRegistrationRequest request)
        throws IOException, HistogramReadingException {
        final BiologicalDataItemResourceType type = request.getType();
        BedFile bedFile;
        switch (type) {
            case URL:
            case FILE:
            case S3:
                bedFile = registerBedFileFromFile(request);
                break;
            case DOWNLOAD:
                bedFile = downloadBedFile(request);
                break;
            default:
                throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_INVALID_PARAM, "type",
                                                              request.getType()));
        }
        return bedFile;
    }

    private BedFile downloadBedFile(IndexedFileRegistrationRequest request)
        throws IOException, HistogramReadingException {
        BedFile bedFile;
        final File newFile = downloadFileManager.downloadFromURL(request.getPath());
        request.setIndexPath(null);
        request.setName(request.getName() != null ? request.getName() : FilenameUtils.getBaseName(request.getPath()));
        request.setPath(newFile.getPath());
        bedFile = registerBedFileFromFile(request);
        return bedFile;
    }

    private BedFile registerBedFileFromFile(final IndexedFileRegistrationRequest request)
        throws HistogramReadingException, IOException {

        Reference reference = referenceGenomeManager.load(request.getReferenceId());

        BiologicalDataItemResourceType resourceType = BiologicalDataItemResourceType.translateRequestType(
            request.getType());
        String fileName = FilenameUtils.getName(request.getPath());

        final BedFile bedFile = new BedFile();
        bedFile.setId(bedFileManager.createBedFileId());
        bedFile.setCompressed(resourceType == BiologicalDataItemResourceType.FILE && IOHelper.isGZIPFile(fileName));
        bedFile.setPath(request.getPath());
        bedFile.setName(request.getName() != null ? request.getName() : fileName);
        bedFile.setType(resourceType); // For now we're working only with files
        bedFile.setCreatedDate(new Date());
        bedFile.setReferenceId(reference.getId());
        bedFile.setPrettyName(request.getPrettyName());

        long bedId = bedFile.getId();

        try {
            biologicalDataItemManager.createBiologicalDataItem(bedFile);
            bedFile.setBioDataItemId(bedFile.getId());
            bedFile.setId(bedId);

            if (StringUtils.isNotBlank(request.getIndexPath())) {
                final BiologicalDataItem indexItem = new BiologicalDataItem();
                indexItem.setCreatedDate(new Date());
                indexItem.setPath(request.getIndexPath());
                indexItem.setFormat(BiologicalDataItemFormat.BED_INDEX);
                indexItem.setType(BiologicalDataItemResourceType
                        .translateRequestType(request.getIndexType()));
                indexItem.setName(bedFile.getName() + "_index");

                bedFile.setIndex(indexItem);
            } else {
                Assert.isTrue(resourceType == BiologicalDataItemResourceType.FILE ||
                                resourceType == BiologicalDataItemResourceType.S3,
                        "Auto indexing is supported only for FILE type requests");
                fileManager.makeBedDir(bedFile.getId());
                fileManager.makeBedIndex(bedFile);
            }

            double time1 = Utils.getSystemTimeMilliseconds();
            if (resourceType == BiologicalDataItemResourceType.FILE
                    || resourceType == BiologicalDataItemResourceType.S3) {
                createHistogram(bedFile);
            }
            double time2 = Utils.getSystemTimeMilliseconds();
            LOG.debug("Making BED histogram took {} ms", time2 - time1);
            LOG.info(getMessage(MessagesConstants.INFO_GENE_REGISTER, bedFile.getId(),
                    bedFile.getPath()));
            biologicalDataItemManager.createBiologicalDataItem(bedFile.getIndex());
            bedFileManager.create(bedFile);
            return bedFile;
        } finally {
            if (bedFile.getId() != null && bedFile.getBioDataItemId() != null
                    && bedFileManager.load(bedFile.getId()) == null) {
                biologicalDataItemManager.deleteBiologicalDataItem(bedFile.getBioDataItemId());
                try {
                    fileManager.deleteFeatureFileDirectory(bedFile);
                } catch (IOException e) {
                    LOGGER.error("Unable to delete directory for " + bedFile.getName(), e);
                }
            }
        }
    }

    private void createHistogram(BedFile bedFile) throws IOException {
        try (AbstractFeatureReader<NggbBedFeature, LineIterator> featureReader = fileManager.makeBedReader(bedFile)) {
            CloseableIterator<NggbBedFeature> iterator = featureReader.iterator();
            if (iterator.hasNext()) {
                makeHistogramFromIterator(iterator, bedFile);
            }
        }
    }

    private void makeHistogramFromIterator(CloseableIterator<NggbBedFeature> iterator, BedFile bedFile)
        throws IOException {
        List<Wig> histogram = new ArrayList<>();
        NggbBedFeature firstFeature = iterator.next();
        String currentContig = firstFeature.getContig();

        Map<String, Chromosome> chromosomeMap = referenceGenomeManager.loadChromosomes(bedFile.getReferenceId())
            .stream().collect(Collectors.toMap(BaseEntity::getName, c -> c));
        currentContig = checkFileNonEmpty(currentContig, iterator, chromosomeMap, bedFile);
        Chromosome currentChromosome = Utils.getFromChromosomeMap(chromosomeMap, currentContig);
        int histogramSize = Math.min((int) Math.ceil(currentChromosome.getSize() *
                                                     HistogramUtils.HISTOGAM_BLOCK_SIZE_PART),
                                     HistogramUtils.HISTOGRAM_SIZE_LIMIT);
        int intervalLength = currentChromosome.getSize() / histogramSize;
        int intervalEnd = intervalLength;

        Wig currentWig = new Wig();
        currentWig.setStartIndex(1);
        currentWig.setEndIndex(intervalLength);

        int featureCount = 1;
        while (iterator.hasNext()) {
            NggbBedFeature feature = iterator.next();
            if (!feature.getContig().equals(currentContig) && currentChromosome != null) {
                currentWig.setValue((float) featureCount);
                histogram.add(currentWig);
                fileManager.writeHistogram(bedFile, currentChromosome.getName(), histogram);
                histogram.clear();
                featureCount = 0;
                currentContig = getNextContig(feature.getContig(), iterator, chromosomeMap);
                if (currentContig == null) {
                    currentChromosome = null;
                } else {
                    currentChromosome = Utils.getFromChromosomeMap(chromosomeMap, currentContig);
                }
            }

            if (currentChromosome != null && feature.getEnd() > intervalEnd) {
                currentWig.setValue((float) featureCount);
                histogram.add(currentWig);

                currentWig = new Wig(intervalEnd + 1, intervalEnd + 1 + intervalLength);
                intervalEnd = intervalEnd + 1 + intervalLength;
                featureCount = 0;
            }

            featureCount++;
        }

        if (featureCount > 0 && currentChromosome != null) {
            currentWig.setValue((float) featureCount);
            histogram.add(currentWig);
            fileManager.writeHistogram(bedFile, currentChromosome.getName(), histogram);
        }
    }

    private String checkFileNonEmpty(String currentContig, CloseableIterator<? extends Feature> iterator,
                                   Map<String, Chromosome> chromosomeMap, BedFile bedFile) {
        String contig = getNextContig(currentContig, iterator, chromosomeMap);
        Assert.notNull(contig, "No chromosomes found in " + bedFile.getPath());
        return contig;
    }

    private String getNextContig(String currentContig, CloseableIterator<? extends Feature> iterator,
                                 Map<String, Chromosome> chromosomeMap) {
        Chromosome currentChromosome = Utils.getFromChromosomeMap(chromosomeMap, currentContig);
        if (currentChromosome == null) {
            while (iterator.hasNext()) {
                Feature feature = iterator.next();
                if (Utils.chromosomeMapContains(chromosomeMap, feature.getContig())) {
                    return feature.getContig();
                }
            }

            return null;
        }

        return currentContig;
    }

    private List<Wig> readHistogram(Track<Wig> track, BedFile file, Chromosome chromosome,
            List<Pair<Integer, Integer>> portion) {
        try {
            return readHistogramPortion(track, file, chromosome, portion);
        } catch (IOException e) {
            LOG.info(String.format("Failed to read histogram for file %s", file.getName()), e);
            return Collections.emptyList();
        }
    }

    private List<Wig> loadHistogram(Track<Wig> track, BedFile bedFile, Chromosome chromosome)
            throws HistogramReadingException {
        final List<Wig> histogram;
        try {
            histogram = fileManager.loadHistogram(bedFile, chromosome.getName());
        } catch (IOException e) {
            throw new HistogramReadingException(track, e);
        }
        return histogram;
    }

    private List<Wig> readHistogramPortion(final Track<Wig> track, final BedFile bedFile, final Chromosome
            chromosome, final List<Pair<Integer, Integer>> portion) throws IOException {
        try (AbstractFeatureReader<NggbBedFeature, LineIterator> featureReader = fileManager.makeBedReader(bedFile)) {
            return getWigFromHistogram(track, chromosome, portion, featureReader);
        }
    }

    @NotNull private List<Wig> getWigFromHistogram(Track<Wig> track, Chromosome chromosome,
            List<Pair<Integer, Integer>> portion,
            AbstractFeatureReader<NggbBedFeature, LineIterator> featureReader) throws IOException {
        final List<Wig> wigs = new ArrayList<>(portion.size());
        for (Pair<Integer, Integer> interval : portion) {
            if (interval.getRight() > track.getStartIndex() && interval.getLeft() < track.getEndIndex()) {
                final int startIndex = Math.max(interval.getLeft(), track.getStartIndex());
                final int endIndex = Math.min(interval.getRight(), track.getEndIndex());
                CloseableIterator<NggbBedFeature> iterator = featureReader.query(chromosome.getName(),
                        startIndex, endIndex);
                if (!iterator.hasNext()) {
                    iterator = featureReader.query(Utils.changeChromosomeName(chromosome.getName()),
                            startIndex, endIndex);
                }
                int genesCount = getGenesCount(iterator);
                HistogramUtils.addToHistogramPortion(wigs, genesCount, interval);
            }
        }
        return wigs;
    }

    private int getGenesCount(CloseableIterator<NggbBedFeature> iterator) {
        int genesCount = 0;
        while (iterator.hasNext()) {
            iterator.next();
            genesCount++;
        }
        return genesCount;
    }

    private List<BedRecord> loadStatisticRecords(final Track<BedRecord> track,
                                                 final CloseableIterator<NggbBedFeature> iterator) {
        final List<BedRecord> bedRecords = new ArrayList<>();
        int step = (int) Math.ceil(1 / (double)track.getScaleFactor());
        int from = track.getStartIndex();
        int to = from + step;
        boolean found = false;
        int featuresCount = 0; // On small scale we need to count overlapping variations
        final List<BedRecord> extendingRecords = new ArrayList<>(); // variations, that extend one pixel region
        NggbBedFeature lastFeature = null;

        while (iterator.hasNext()) {
            final NggbBedFeature feature = iterator.next();
            final BedRecord bedRecord = new BedRecord(feature);
            if (feature.getStart() > to) {
                found = false;
                processLastRecord(bedRecords, featuresCount, lastFeature);
                to = ((to + step) < feature.getStart()) ? (feature.getStart() + step) : (to + step);
                featuresCount = 0;
                bedRecords.addAll(extendingRecords);
                extendingRecords.clear();
            }

            if (feature.getEnd() > to && feature.getStart() < to) { //
                extendingRecords.add(bedRecord);
                continue;
            }

            if (!found) {
                bedRecords.add(bedRecord);
                found = true;
            }

            featuresCount++;
            lastFeature = feature;
        }

        bedRecords.addAll(extendingRecords);

        return bedRecords;
    }

    private void processLastRecord(List<BedRecord> bedRecords, int featuresCount,
            NggbBedFeature lastFeature) {
        if (!bedRecords.isEmpty()) {
            BedRecord lastRecord = bedRecords.get(bedRecords.size() - 1);

            if (lastRecord != null && lastFeature != null && featuresCount > 1) {
                lastRecord.setName(featuresCount + " Features");
                lastRecord.setDescription(null);
                lastRecord.setBlockCount(0);
                lastRecord.setBlockSizes(null);
                lastRecord.setBlockStarts(null);
                lastRecord.setId(null);
                lastRecord.setStrand(null);
                lastRecord.setRgb(null);
                lastRecord.setThickStart(null);
                lastRecord.setThickEnd(null);
                lastRecord.setScore(null);
                lastRecord.setEndIndex(lastFeature.getEnd());
            }
        }
    }

    public BedFile reindexBedFile(long bedFileId) throws FeatureIndexException {
        BedFile bedFile = bedFileManager.load(bedFileId);
        Reference reference = referenceGenomeManager.load(bedFile.getReferenceId());
        Map<String, Chromosome> chromosomeMap = reference.getChromosomes().stream().collect(
                Collectors.toMap(BaseEntity::getName, chromosome -> chromosome));

        try {
            fileManager.deleteFileFeatureIndex(bedFile);
            try (AbstractFeatureReader<NggbBedFeature, LineIterator> reader =
                    AbstractEnhancedFeatureReader
                                 .getFeatureReader(bedFile.getPath(), new NggbBedCodec(), false, indexCache)) {
                featureIndexManager.makeIndexForBedReader(bedFile, reader, chromosomeMap);
            }
        } catch (IOException e) {
            throw new FeatureIndexException(bedFile, e);
        }

        return bedFile;
    }
}
