/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

package com.epam.catgenome.manager.wig;

import com.epam.catgenome.component.MessageCode;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.registration.FileRegistrationRequest;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.track.TrackType;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.entity.wig.WigFile;
import com.epam.catgenome.exception.RegistrationException;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.manager.DownloadFileManager;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.TrackHelper;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.util.NgbFileUtils;
import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;

/**
 * Source:      FacadeWigManager.java
 * Created:     1/21/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * {@code FacadeWigManager} represents a service class designed to encapsulate all business
 * logic operations required to manage {@code WigFile} and corresponded tracks, e.g. to process
 * variants uploads, position-based and/or zoom queries etc.
 */
@Service
public class FacadeWigManager {

    @Autowired
    protected TrackHelper trackHelper;

    @Autowired
    protected WigFileManager wigFileManager;

    @Autowired
    protected ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    protected BiologicalDataItemManager biologicalDataItemManager;

    @Autowired
    protected FileManager fileManager;

    @Autowired
    protected DownloadFileManager downloadFileManager;

    @Autowired(required = false)
    protected EhCacheBasedIndexCache indexCache;

    protected static final Logger LOGGER = LoggerFactory.getLogger(FacadeWigManager.class);

    static final Set<String> WIG_EXTENSIONS = new HashSet<>();

    static final Set<String> BED_GRAPH_EXTENSIONS = new HashSet<>();
    static {
        WIG_EXTENSIONS.add(".bw");
        WIG_EXTENSIONS.add(".bigwig");
    }

    static {
        BED_GRAPH_EXTENSIONS.add(".bg");
        BED_GRAPH_EXTENSIONS.add(".bg.gz");
        BED_GRAPH_EXTENSIONS.add(".bdg");
        BED_GRAPH_EXTENSIONS.add(".bdg.gz");
        BED_GRAPH_EXTENSIONS.add(".bedGraph");
        BED_GRAPH_EXTENSIONS.add(".bedGraph.gz");
    }

    /**
     * Saves a {@code WigFile} on the server. File metadata is saved in the database
     * @param request
     * @return
     */
    public WigFile registerWigFile(final FileRegistrationRequest request) {
        Assert.notNull(request, MessagesConstants.ERROR_NULL_PARAM);
        final String requestPath = request.getPath();
        Assert.notNull(requestPath, getMessage(MessagesConstants.WRONG_WIG_FILE));
        Assert.notNull(request.getReferenceId(), getMessage(MessageCode.NO_SUCH_REFERENCE));
        WigFile wigFile = null;
        try {
            fetchWigManager(requestPath).assertFile(requestPath);
            if (request.getType() == null) {
                request.setType(BiologicalDataItemResourceType.FILE);
            }
            switch (request.getType()) {
                case FILE:
                case S3:
                    wigFile = fillWigFile(requestPath, request.getName(),
                            request.getPrettyName(), request.getReferenceId());
                    break;
                case DOWNLOAD:
                    final File newFile = downloadFileManager.downloadFromURL(requestPath);
                    request.setName(request.getName() != null ? request.getName() :
                            FilenameUtils.getBaseName(requestPath));
                    wigFile = fillWigFile(newFile.getPath(), request.getName(),
                            request.getPrettyName(), request.getReferenceId());
                    break;
                default:
                    throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_INVALID_PARAM,
                            "type", request.getType()));
            }
            long id = wigFileManager.create();
            biologicalDataItemManager.createBiologicalDataItem(wigFile);
            wigFile.setBioDataItemId(wigFile.getId());
            wigFile.setId(id);
            fileManager.makeWigDir(wigFile.getId());

            prepareWigFileToWork(wigFile);

            wigFileManager.save(wigFile);
        } catch (IOException e) {
            throw new RegistrationException(getMessage(MessagesConstants.ERROR_REGISTER_FILE, request.getName()), e);
        } finally {
            if (wigFile != null && wigFile.getId() != null &&
                    wigFileManager.load(wigFile.getId()) == null) {
                biologicalDataItemManager.deleteBiologicalDataItem(wigFile.getBioDataItemId());
                try {
                    fileManager.deleteFeatureFileDirectory(wigFile);
                } catch (IOException e) {
                    LOGGER.error("Unable to delete directory for " + wigFile.getName(), e);
                }
            }
        }
        return wigFile;
    }

    /**
     * Removes wig file metadata from the system, deleting all additional files that were created
     *
     * @param wigFileId {@code long} a wig fiel ID
     * @return deleted {@code WigFile} entity
     * @throws IOException
     */
    public WigFile unregisterWigFile(final long wigFileId) throws IOException {
        WigFile fileToDelete = wigFileManager.load(wigFileId);
        Assert.notNull(fileToDelete, getMessage(MessagesConstants.ERROR_FILE_NOT_FOUND));
        wigFileManager.delete(fileToDelete);
        return fileToDelete;
    }

    /**
     * Loads the wig data for a track
     * @param track to load data
     * @return track filled with data
     * @throws IOException
     */
    public Track<Wig> getWigTrack(Track<Wig> track) throws IOException {
        track.setType(TrackType.WIG);
        final Chromosome chromosome = trackHelper.validateTrackWithBlockCount(track);
        final WigFile wigFile = wigFileManager.load(track.getId());
        return fetchWigManager(wigFile.getPath()).getWigFromFile(wigFile, track, chromosome, indexCache);
    }

    protected void prepareWigFileToWork(final WigFile wigFile) throws IOException {
        AbstractWigProcessor wigProcessor = fetchWigManager(wigFile.getPath());
        final Reference reference = referenceGenomeManager.load(wigFile.getReferenceId());
        final Map<String, Chromosome> chromosomeMap = reference.getChromosomes().stream().collect(Collectors.toMap(
                BaseEntity::getName, chromosome -> chromosome));
        wigProcessor.prepareWigFileToWork(wigFile);
        wigProcessor.splitByChromosome(wigFile, chromosomeMap, indexCache);
    }

    private WigFile fillWigFile(final String wigFilePath, final String alternativeName, String prettyName,
                                final long referenceId) {
        final WigFile wigFile = new WigFile();

        wigFile.setName(parseName(FilenameUtils.getName(wigFilePath), alternativeName));
        wigFile.setPrettyName(prettyName);
        wigFile.setType(BiologicalDataItemResourceType.FILE);
        wigFile.setFormat(BiologicalDataItemFormat.WIG);
        wigFile.setReferenceId(referenceId);
        wigFile.setCreatedDate(new Date());
        wigFile.setPath(wigFilePath);
        return wigFile;

    }

    protected String parseName(final String fileName, final String alternativeName) {
        boolean supported = false;
        List<String> exts = getSupportedExts();
        for (final String ext : exts) {
            if (NgbFileUtils.getFileExtension(fileName).endsWith(ext)) {
                supported = true;
                break;
            }
        }
        if (!supported) {
            throw new IllegalArgumentException(getMessage("error.illegal.file.type",
                    StringUtils.join(getSupportedExts(), ", ")));
        }
        return StringUtils.defaultString(StringUtils.trimToNull(alternativeName), fileName);
    }

    @NotNull
    private List<String> getSupportedExts() {
        List<String> exts = new ArrayList<>(WIG_EXTENSIONS);
        exts.addAll(BED_GRAPH_EXTENSIONS);
        return exts;
    }

    private AbstractWigProcessor fetchWigManager(String path) {
        String fileExtension = NgbFileUtils.getFileExtension(path);
        boolean isBedGraph = BED_GRAPH_EXTENSIONS
                .stream()
                .anyMatch(fileExtension::endsWith);
        if (isBedGraph) {
            return new BedGraphProcessor(biologicalDataItemManager, fileManager);
        } else {
            return new WigProcessor(biologicalDataItemManager, fileManager);
        }
    }

}
