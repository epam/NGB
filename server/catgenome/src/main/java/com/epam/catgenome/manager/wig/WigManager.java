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

package com.epam.catgenome.manager.wig;

import static com.epam.catgenome.component.MessageHelper.getMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import com.epam.catgenome.manager.BiologicalDataItemManager;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.bio.big.BigSummary;
import org.jetbrains.bio.big.BigWigFile;
import org.jetbrains.bio.big.FixedStepSection;
import org.jetbrains.bio.big.WigSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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
import com.epam.catgenome.manager.DownloadFileManager;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.TrackHelper;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.util.AuthUtils;
import com.epam.catgenome.util.Utils;
import gnu.trove.list.TFloatList;
import gnu.trove.list.array.TFloatArrayList;
import kotlin.Pair;

/**
 * Source:      WigManager.java
 * Created:     1/21/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * {@code WigManager} represents a service class designed to encapsulate all business
 * logic operations required to manage {@code WigFile} and corresponded tracks, e.g. to process
 * variants uploads, position-based and/or zoom queries etc.
 */
@Service
public class WigManager {

    private static final Set<String> WIG_EXTENSIONS = new HashSet<>();

    static {
        WIG_EXTENSIONS.add(".bw");
        WIG_EXTENSIONS.add(".bigwig");
    }

    @Autowired
    private TrackHelper trackHelper;

    @Autowired
    private WigFileManager wigFileManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private BiologicalDataItemManager biologicalDataItemManager;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private DownloadFileManager downloadFileManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(WigManager.class);
    private static final int WIG_DOWNSAMPLING_WINDOW = 100_000;
    private static final int WIG_MIN_DOWNSAMPLING_CHROMOSOME_SIZE = 10_000_000;
    private static final float WIG_DOWNSAMPLING_SCALE_FACTOR = 0.00005F;

    /**
     * Loads the wig data for a track
     * @param track to load data
     * @return track filled with data
     * @throws IOException
     */
    public Track<Wig> getWigTrack(Track<Wig> track) throws IOException {
        track.setType(TrackType.WIG);
        final Chromosome chromosome = trackHelper.validateTrackWithBlockCount(track);
        return getWigFromFile(track, chromosome);
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
            Assert.isTrue(parseWig(requestPath), getMessage(MessagesConstants.WRONG_WIG_FILE));
            if (request.getType() == null) {
                request.setType(BiologicalDataItemResourceType.FILE);
            }
            switch (request.getType()) {
                case FILE:
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
                    throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_INVALID_PARAM));
            }
            long id = wigFileManager.createWigFileId();
            biologicalDataItemManager.createBiologicalDataItem(wigFile);
            wigFile.setBioDataItemId(wigFile.getId());
            wigFile.setId(id);

            fileManager.makeWigDir(wigFile.getId(), AuthUtils.getCurrentUserId());
            splitWigFile(wigFile);
            wigFileManager.save(wigFile);
        } catch (IOException e) {
            throw new RegistrationException(getMessage(MessagesConstants.ERROR_REGISTER_FILE, request.getName()), e);
        } finally {
            if (wigFile != null && wigFile.getId() != null &&
                    wigFileManager.loadWigFile(wigFile.getId()) == null) {
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
        WigFile fileToDelete = wigFileManager.loadWigFile(wigFileId);
        Assert.notNull(fileToDelete, getMessage(MessagesConstants.ERROR_FILE_NOT_FOUND));
        wigFileManager.deleteWigFile(fileToDelete);
        return fileToDelete;
    }

    private Track<Wig> getWigFromFile(final Track<Wig> track, final Chromosome chromosome) throws IOException {
        final WigFile wigFile = wigFileManager.loadWigFile(track.getId());
        Assert.notNull(wigFile, getMessage(MessagesConstants.ERROR_FILE_NOT_FOUND));
        TrackHelper.fillBlocks(track, indexes -> new Wig(indexes.getLeft(), indexes.getRight()));
        String downsamplePath = fileManager.getWigFilePath(wigFile, chromosome);
        if (track.getScaleFactor() > WIG_DOWNSAMPLING_SCALE_FACTOR || chromosome.getSize() <
                WIG_MIN_DOWNSAMPLING_CHROMOSOME_SIZE) {
            fillBlocksFromFile(wigFile.getPath(), track, chromosome.getName());
        } else {
            if (downsamplePath == null) {
                LOGGER.debug("Downsampled WIG for file {}:{} not found, using original", wigFile.getId(),
                        wigFile.getPath());
                fillBlocksFromFile(wigFile.getPath(), track, chromosome.getName());
            } else {
                fillBlocksFromFile(downsamplePath, track, chromosome.getName());
            }
        }
        return track;
    }

    private WigFile fillWigFile(final String wigFilePath, final String alternativeName, String prettyName,
            final long referenceId) {
        final WigFile wigFile = new WigFile();

        wigFile.setName(parseName(new File(wigFilePath).getName(), alternativeName));
        wigFile.setPrettyName(prettyName);
        wigFile.setType(BiologicalDataItemResourceType.FILE);
        wigFile.setFormat(BiologicalDataItemFormat.WIG);
        wigFile.setCreatedBy(AuthUtils.getCurrentUserId());
        wigFile.setReferenceId(referenceId);
        wigFile.setCreatedDate(new Date());
        wigFile.setPath(wigFilePath);
        return wigFile;

    }

    protected String parseName(final String fileName, final String alternativeName) {
        boolean supported = false;
        for (final String ext : WIG_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                supported = true;
                break;
            }
        }
        if (!supported) {
            throw new IllegalArgumentException(getMessage("error.illegal.file.type",
                                                          StringUtils.join(WIG_EXTENSIONS, ", ")));
        }
        return StringUtils.defaultString(StringUtils.trimToNull(alternativeName), fileName);
    }

    private boolean parseWig(final String wigFilePath) throws IOException {
        Assert.isTrue(new File(wigFilePath).exists(), getMessage(MessageCode.RESOURCE_NOT_FOUND));
        Path path = Paths.get(wigFilePath);
        BigWigFile wigFile = BigWigFile.read(path);
        wigFile.close();
        return true;
    }

    private void splitWigFile(final WigFile wigFile) throws IOException {
        final Reference reference = referenceGenomeManager.loadReferenceGenome(wigFile.getReferenceId());
        final Map<String, Chromosome> chromosomeMap = reference.getChromosomes().stream().collect(Collectors.toMap(
                BaseEntity::getName, chromosome -> chromosome));

        try (BigWigFile bigWigFile = BigWigFile.read(new File(wigFile.getPath()).toPath())) {
            readFromFile(bigWigFile, chromosomeMap, wigFile);
        }
    }

    private void readFromFile(final BigWigFile bigWigFile, final Map<String, Chromosome> chromosomeMap,
                       final WigFile wigFile) throws IOException {
        for (Object o : bigWigFile.getChromosomes().values()) {
            String chr = (String) o;
            if (chromosomeMap.containsKey(chr) || chromosomeMap.containsKey(Utils.changeChromosomeName(chr))) {
                String realChrName = chr;
                if (chromosomeMap.containsKey(Utils.changeChromosomeName(chr))) {
                    realChrName = Utils.changeChromosomeName(chr);
                }
                Chromosome chromosome = chromosomeMap.get(realChrName);
                if (chromosome.getSize() < WIG_MIN_DOWNSAMPLING_CHROMOSOME_SIZE) {
                    continue;
                }
                int start = 0;
                int stop = chromosomeMap.get(realChrName).getSize();
                LOGGER.debug("Processing chromosome " + chr);
                int bp = start;
                List<WigSection> sectionList = new ArrayList<>();
                List<Pair<String, Integer>> chrSizes = Collections.singletonList(new Pair<>(chr, stop - start));

                while (bp < stop) {
                    int chunkStart = bp;
                    int chunkStop = Math.min(bp + WIG_DOWNSAMPLING_WINDOW - 1, stop);

                    List<BigSummary> summaries = bigWigFile.summarize(chr, chunkStart, chunkStop, 1, true);
                    TFloatList values = new TFloatArrayList();
                    BigSummary bigSummary = summaries.get(0);
                    values.add((float) bigSummary.getMaxValue());
                    WigSection wigSection = new FixedStepSection(chr, chunkStart, chunkStop, 1, values);
                    sectionList.add(wigSection);

                    bp = chunkStop + 1;
                }

                fileManager.writeToBigWigFile(wigFile, sectionList, chrSizes, realChrName);
            }
        }
    }

    private void fillBlocksFromFile(final String filePath, final Track<Wig> track, final String chromosomeName)
            throws IOException {
        final Path wigPath = Paths.get(filePath);
        LOGGER.debug(getMessage(MessagesConstants.DEBUG_FILE_READING, filePath));
        double time1 = Utils.getSystemTimeMilliseconds();
        try (BigWigFile bigWigFile = BigWigFile.read(wigPath)) {
            fillBlocksNew(track, chromosomeName, bigWigFile);
        }
        double time2 = Utils.getSystemTimeMilliseconds();
        LOGGER.debug("Reading from WIG file {}, took {} ms", filePath, time2 - time1);
    }

    private void fillBlocksNew(final Track<Wig> track, final String chromosomeName,
            final BigWigFile bigWigFile) {
        String realName = null;
        for (Object o : bigWigFile.getChromosomes().values()) {
            String chr = (String) o;
            if (chromosomeName.equals(chr)) {
                realName = chr;
                break;
            } else if (Utils.changeChromosomeName(chromosomeName).equals(chr)) {
                realName = chr;
                break;
            }
        }
        if (realName == null) {
            LOGGER.info("Chromosome not found in big wig file");
        }
        for (Wig wigElement : track.getBlocks()) {
            double score = getNewArrays(bigWigFile, realName, wigElement.getStartIndex() - 1,
                    wigElement.getEndIndex());
            wigElement.setValue((float) score);
        }
    }

    private double getNewArrays(final BigWigFile bigWigFile, final String chrName, final int start,
                                 final int end) {
        try {
            double res = 0.0;
            res +=  queryWig(bigWigFile, chrName, start, end);
            return res;
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
            return 0;
        }
    }

    private double queryWig(BigWigFile bigWigFile, String chrName, int start, int end)
            throws IOException {
        List<BigSummary> summarize;
        try {
            summarize = bigWigFile.summarize(chrName, start, end, 1, true);
        } catch (NoSuchElementException e) {
            LOGGER.info(e.getMessage(), e);
            return 0;
        }
        double res = 0.0;
        for (BigSummary summary : summarize) {
            if (!Double.isNaN(summary.getMaxValue()) && !Double.isInfinite(summary.getMaxValue())) {
                res += summary.getMaxValue();
            }
        }
        return res;
    }
}
