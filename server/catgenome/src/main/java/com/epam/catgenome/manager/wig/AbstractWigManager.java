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
import com.epam.catgenome.util.AuthUtils;
import com.epam.catgenome.util.NgbFileUtils;
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

@Service
public abstract class AbstractWigManager {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractWigManager.class);

    static final int WIG_DOWNSAMPLING_WINDOW = 100_000;
    static final int WIG_MIN_DOWNSAMPLING_CHROMOSOME_SIZE = 10_000_000;
    static final float WIG_DOWNSAMPLING_SCALE_FACTOR = 0.00005F;

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
            assertFile(requestPath);
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

            prepareToWorkWigFile(wigFile);

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

    /**
     * Loads the wig data for a track
     * @param track to load data
     * @return track filled with data
     * @throws IOException
     */
    public Track<Wig> getWigTrack(Track<Wig> track) throws IOException {
        track.setType(TrackType.WIG);
        final Chromosome chromosome = trackHelper.validateTrackWithBlockCount(track);
        final WigFile wigFile = wigFileManager.loadWigFile(track.getId());
        return getWigFromFile(wigFile, track, chromosome);
    }

    protected void prepareToWorkWigFile(final WigFile wigFile) throws IOException {
        final Reference reference = referenceGenomeManager.loadReferenceGenome(wigFile.getReferenceId());
        final Map<String, Chromosome> chromosomeMap = reference.getChromosomes().stream().collect(Collectors.toMap(
                BaseEntity::getName, chromosome -> chromosome));
        splitByChromosome(wigFile, chromosomeMap);
    }

    protected abstract void splitByChromosome(WigFile wigFile, Map<String, Chromosome> chromosomeMap)
            throws IOException;

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

    boolean dontNeedToUseDownsampling(Track<Wig> track, Chromosome chromosome) {
        return track.getScaleFactor() > WIG_DOWNSAMPLING_SCALE_FACTOR || chromosome.getSize() <
                WIG_MIN_DOWNSAMPLING_CHROMOSOME_SIZE;
    }

    @NotNull
    private List<String> getSupportedExts() {
        List<String> exts = new ArrayList<>(WIG_EXTENSIONS);
        exts.addAll(BED_GRAPH_EXTENSIONS);
        return exts;
    }

    protected abstract void assertFile(String requestPath) throws IOException;

    protected abstract Track<Wig> getWigFromFile(WigFile wigFile, Track<Wig> track,
                                                 Chromosome chromosome) throws IOException;

}
