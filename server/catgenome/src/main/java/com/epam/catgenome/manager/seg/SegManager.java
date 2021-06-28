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

package com.epam.catgenome.manager.seg;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.constant.MessagesConstants.ERROR_REGISTER_FILE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.seg.SegFile;
import com.epam.catgenome.entity.seg.SegRecord;
import com.epam.catgenome.entity.seg.SegSample;
import com.epam.catgenome.entity.track.SampledTrack;
import com.epam.catgenome.exception.RegistrationException;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.manager.DownloadFileManager;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.TrackHelper;
import com.epam.catgenome.manager.seg.parser.SegFeature;
import com.epam.catgenome.util.IOHelper;
import com.epam.catgenome.util.Utils;
import com.epam.catgenome.util.comparator.FeatureComparator;
import htsjdk.samtools.util.CloseableIterator;
import com.epam.catgenome.util.feature.reader.AbstractFeatureReader;
import htsjdk.tribble.readers.LineIterator;

/**
 * Source:      SegManager
 * Created:     09.06.16, 18:09
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * {@code SegManager} represents a service class designed to encapsulate all business
 * logic operations required to manage SEG files and corresponded tracks, e.g. to process
 * file registration and upload, position-based and/or zoom queries etc.
 */
@Service
public class SegManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SegManager.class);

    @Autowired
    private SegFileManager segFileManager;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private TrackHelper trackHelper;

    @Autowired
    private BiologicalDataItemManager biologicalDataItemManager;

    @Autowired
    private DownloadFileManager downloadFileManager;

    /**
     * Saves a {@code SegFile} in the system, writes it's metadata to the database and
     * creates feature index.
     * @param request with a {@code SegFile}
     * @return registered {@code SegFile}
     */
    public SegFile registerSegFile(IndexedFileRegistrationRequest request) {
        final String requestPath = request.getPath();
        Assert.isTrue(StringUtils.isNotBlank(requestPath), getMessage(
                MessagesConstants.ERROR_NULL_PARAM, "path"));
        Assert.notNull(request.getReferenceId(), getMessage(MessagesConstants.ERROR_NULL_PARAM, "referenceId"));
        if (request.getType() == null) {
            request.setType(BiologicalDataItemResourceType.FILE);
        }

        SegFile  segFile = getSegFile(request, requestPath);
        LOGGER.info(getMessage(MessagesConstants.INFO_GENE_REGISTER, segFile.getId(), segFile.getPath()));

        return segFile;
    }

    /**
     * Loads SEG track form the requested file
     *
     * @param track a {@code Track} to fill with SEG features
     * @return a {@code Track}, filled with SEG features
     * @throws IOException
     */
    public SampledTrack<SegRecord> loadFeatures(SampledTrack<SegRecord> track) throws IOException {
        Chromosome chromosome = trackHelper.validateTrack(track);

        SegFile segFile = segFileManager.load(track.getId());

        double time1 = Utils.getSystemTimeMilliseconds();
        try (AbstractFeatureReader<SegFeature, LineIterator> reader = fileManager.makeSegReader(segFile)) {
            CloseableIterator<SegFeature> iterator = reader.query(chromosome.getName(), track.getStartIndex(),
                    track.getEndIndex());
            if (!iterator.hasNext()) {
                iterator = reader.query(Utils.changeChromosomeName(chromosome.getName()), track.getStartIndex(),
                        track.getEndIndex());
            }

            Map<String, List<SegRecord>> sampledRecords = new HashMap<>();
            iterator.forEachRemaining(f -> {
                if ((double) (f.getEnd() - f.getStart()) * track.getScaleFactor() >= 1) {
                    if (!sampledRecords.containsKey(f.getId())) {
                        sampledRecords.put(f.getId(), new ArrayList<>());
                    }
                    sampledRecords.get(f.getId()).add(new SegRecord(f));
                }
            });

            track.setTracks(sampledRecords);
        }
        double time2 = Utils.getSystemTimeMilliseconds();
        LOGGER.debug("Reading records from SEG file, took {} ms", time2 - time1);
        return track;
    }

    /**
     * Deletes the {@code SegFile} and its index files from the file system and deletes its meta
     * information from the database
     * @param segFileId ID of a {@code SegFile} to delete
     * @return deleted {@code SegFile}
     * @throws IOException
     */
    public SegFile unregisterSegFile(final long segFileId) throws IOException {
        Assert.notNull(segFileId, MessagesConstants.ERROR_INVALID_PARAM);
        Assert.isTrue(segFileId > 0, MessagesConstants.ERROR_INVALID_PARAM);
        SegFile fileToDelete = segFileManager.load(segFileId);
        Assert.notNull(fileToDelete, MessagesConstants.ERROR_NO_SUCH_FILE);

        segFileManager.delete(fileToDelete);
        fileManager.deleteFeatureFileDirectory(fileToDelete);

        return fileToDelete;
    }

    private SegFile getSegFile(IndexedFileRegistrationRequest request, String requestPath) {
        SegFile segFile;
        switch (request.getType()) {
            case FILE:
            case S3:
                segFile = registerSegFileFromFile(request);
                break;
            case DOWNLOAD:
                segFile = registerSegFromURL(request, requestPath);
                break;
            default:
                throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_INVALID_PARAM));
        }
        return segFile;
    }

    private SegFile registerSegFromURL(IndexedFileRegistrationRequest request, String requestPath) {
        final File newFile;
        try {
            newFile = downloadFileManager.downloadFromURL(requestPath);
        } catch (IOException e) {
            LOGGER.error(getMessage(ERROR_REGISTER_FILE, request.getName()), e);
            throw new RegistrationException(getMessage(ERROR_REGISTER_FILE, request.getName()), e);
        }
        request.setIndexPath(null);
        request.setName(request.getName() != null ? request.getName() : FilenameUtils.getBaseName(requestPath));
        request.setPath(newFile.getPath());
        return registerSegFileFromFile(request);
    }

    private SegFile registerSegFileFromFile(IndexedFileRegistrationRequest request) {
        File file = new File(request.getPath());
        SegFile segFile = new SegFile();
        segFile.setId(segFileManager.createSegFileId());
        segFile.setCompressed(IOHelper.isGZIPFile(file.getName()));
        segFile.setPath(request.getPath());
        segFile.setSource(request.getPath());
        segFile.setName(request.getName() != null ? request.getName() : file.getName());
        segFile.setType(BiologicalDataItemResourceType.FILE); // For now we're working only with files
        segFile.setCreatedDate(new Date());
        segFile.setReferenceId(request.getReferenceId());
        segFile.setPrettyName(request.getPrettyName());

        long id = segFile.getId();
        biologicalDataItemManager.createBiologicalDataItem(segFile);
        segFile.setBioDataItemId(segFile.getId());
        segFile.setId(id);

        fileManager.makeSegDir(segFile.getId());

        Set<String> sampleNames = new HashSet<>();
        List<SegSample> samples = new ArrayList<>();
        List<SegFeature> allFeatures = new ArrayList<>();
        try {
            readSegFeatures(segFile, sampleNames, samples, allFeatures);
            LOGGER.debug("Sorting SEG file {}", segFile.getPath());
            Collections.sort(allFeatures, new FeatureComparator());
            writeSegFeatures(segFile, allFeatures);
            fileManager.makeSegIndex(segFile);
            segFile.setSamples(samples);
            biologicalDataItemManager.createBiologicalDataItem(segFile.getIndex());
            segFileManager.create(segFile);
        } catch (IOException e) {
            LOGGER.error(getMessage(ERROR_REGISTER_FILE, request.getName()), e);
            throw new RegistrationException(getMessage(ERROR_REGISTER_FILE, request.getName()));
        } finally {
            if (segFile.getId() != null &&
                    segFileManager.load(segFile.getId()) == null) {
                biologicalDataItemManager.deleteBiologicalDataItem(segFile.getBioDataItemId());
                try {
                    fileManager.deleteFeatureFileDirectory(segFile);
                } catch (IOException e) {
                    LOGGER.error("Unable to delete directory for " + segFile.getName(), e);
                }
            }
        }
        return segFile;
    }

    private void readSegFeatures(SegFile segFile, Set<String> sampleNames, List<SegSample> samples,
            List<SegFeature> allFeatures) throws IOException {
        try (AbstractFeatureReader<SegFeature, LineIterator> reader = fileManager.makeSegReader(segFile)) {
            CloseableIterator<SegFeature> iterator = reader.iterator();
            while (iterator.hasNext()) {
                SegFeature feature = iterator.next();
                allFeatures.add(feature);
                if (sampleNames.add(feature.getId())) {
                    samples.add(new SegSample(feature.getId()));
                }
            }
        }
    }

    private void writeSegFeatures(SegFile segFile, List<SegFeature> allFeatures)
            throws IOException {
        try (BufferedWriter writer = fileManager.makeSegFileWriter(segFile)) {
            for (SegFeature f : allFeatures) {
                writer.write(f.toString());
                writer.newLine();
            }

            writer.flush();
        }
    }


}
