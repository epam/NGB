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

package com.epam.catgenome.manager.maf;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.constant.MessagesConstants.ERROR_EMPTY_FOLDER;
import static com.epam.catgenome.util.IOHelper.checkResource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
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
import com.epam.catgenome.entity.maf.MafFile;
import com.epam.catgenome.entity.maf.MafRecord;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.exception.RegistrationException;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.manager.DownloadFileManager;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.TrackHelper;
import com.epam.catgenome.manager.maf.parser.MafCodec;
import com.epam.catgenome.manager.maf.parser.MafFeature;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.util.IOHelper;
import com.epam.catgenome.util.Utils;
import com.epam.catgenome.util.comparator.FeatureComparator;
import htsjdk.samtools.util.CloseableIterator;
import com.epam.catgenome.util.feature.reader.AbstractFeatureReader;
import htsjdk.tribble.readers.LineIterator;

/**
 * Provides service for handling {@code MafFile}: CRUD operations and loading data from the files
 */
@Service
public class MafManager {
    @Autowired
    private FileManager fileManager;

    @Autowired
    private MafFileManager mafFileManager;

    @Autowired
    private BiologicalDataItemManager biologicalDataItemManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private TrackHelper trackHelper;

    @Autowired
    private DownloadFileManager downloadFileManager;

    @Autowired(required = false)
    private EhCacheBasedIndexCache indexCache;

    private static final Logger LOGGER = LoggerFactory.getLogger(MafManager.class);

    /**
     * Registers a MAF file or a directory with MAF files
     * @param request a file registration request
     * @return a {@code MafFile} object, that references file's representation in the system
     * @throws IOException
     */
    public MafFile registerMafFile(IndexedFileRegistrationRequest request) {
        final String requestPath = request.getPath();
        Assert.isTrue(StringUtils.isNotBlank(requestPath), getMessage(
                MessagesConstants.ERROR_NULL_PARAM, "path"));
        Assert.notNull(request.getReferenceId(), getMessage(MessagesConstants.ERROR_NULL_PARAM, "referenceId"));
        if (request.getType() == null) {
            request.setType(BiologicalDataItemResourceType.FILE);
        }
        MafFile mafFile;
        try {
            switch (request.getType()) {
                case FILE:
                    mafFile = registerMafFileFromFile(request);
                    break;
                case DOWNLOAD:
                    mafFile = downloadMafFile(request, requestPath);
                    break;
                default:
                    throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_INVALID_PARAM));
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RegistrationException("Error while registering MAF file " + requestPath, e);
        }
        return mafFile;
    }

    private MafFile downloadMafFile(IndexedFileRegistrationRequest request, String requestPath)
            throws IOException, NoSuchAlgorithmException {
        MafFile mafFile;
        final File newFile = downloadFileManager.downloadFromURL(requestPath);
        request.setIndexPath(null);
        request.setName(request.getName() != null ? request.getName() : FilenameUtils.getBaseName(requestPath));
        request.setPath(newFile.getPath());
        mafFile = registerMafFileFromFile(request);
        return mafFile;
    }

    private MafFile registerMafFileFromFile(IndexedFileRegistrationRequest request) throws IOException {
        double time1 = Utils.getSystemTimeMilliseconds();
        File file = new File(request.getPath());

        MafFile mafFile = new MafFile();
        mafFile.setId(mafFileManager.createMafFileId());
        mafFile.setCompressed(IOHelper.isGZIPFile(file.getName()));
        mafFile.setName(request.getName() != null ? request.getName() : file.getName());
        mafFile.setType(BiologicalDataItemResourceType.FILE); // For now we're working only with files
        mafFile.setCreatedDate(new Date());
        mafFile.setReferenceId(request.getReferenceId());
        mafFile.setRealPath(request.getPath());
        mafFile.setPrettyName(request.getPrettyName());
        try {
            processRegistration(mafFile, file, request);
            double time2 = Utils.getSystemTimeMilliseconds();
            LOGGER.debug("MAF registration completed in {} ms", time2 - time1);
            biologicalDataItemManager.createBiologicalDataItem(mafFile.getIndex());
            mafFileManager.create(mafFile);
        } finally {
            if (mafFile.getId() != null && mafFile.getBioDataItemId() != null &&
                    mafFileManager.loadMafFileNullable(mafFile.getId()) == null) {
                biologicalDataItemManager.deleteBiologicalDataItem(mafFile.getBioDataItemId());
                try {
                    fileManager.deleteFeatureFileDirectory(mafFile);
                } catch (IOException e) {
                    LOGGER.error("Unable to delete directory for " + mafFile.getName(), e);
                }
            }
        }
        return mafFile;
    }

    public MafFile unregisterMafFile(final long mafFileId) throws IOException {
        Assert.notNull(mafFileId, MessagesConstants.ERROR_INVALID_PARAM);
        Assert.isTrue(mafFileId > 0, MessagesConstants.ERROR_INVALID_PARAM);
        MafFile fileToDelete = mafFileManager.load(mafFileId);
        Assert.notNull(fileToDelete, MessagesConstants.ERROR_NO_SUCH_FILE);

        mafFileManager.delete(fileToDelete);
        fileManager.deleteFeatureFileDirectory(fileToDelete);

        return fileToDelete;
    }

    public MafFile updateMafFile(long mafFileId) throws IOException {
        LOGGER.debug("Updating MAF file " + mafFileId);
        MafFile mafFile = mafFileManager.load(mafFileId);
        fileManager.deleteFeatureFileDirectory(mafFile);

        File file = new File(mafFile.getRealPath());
        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(mafFile.getRealPath());

        processRegistration(mafFile, file, request);

        return mafFile;
    }

    public Track<MafRecord> loadFeatures(Track<MafRecord> track) throws IOException {
        Chromosome chromosome = trackHelper.validateTrack(track);

        MafFile mafFile = mafFileManager.load(track.getId());
        checkResource(mafFile.getPath());

        double time1 = Utils.getSystemTimeMilliseconds();
        try (AbstractFeatureReader<MafFeature, LineIterator> reader = fileManager.makeMafReader(mafFile)) {
            CloseableIterator<MafFeature> iterator = reader.query(chromosome.getName(), track.getStartIndex(),
                    track.getEndIndex());
            if (!iterator.hasNext()) {
                iterator = reader.query(Utils.changeChromosomeName(chromosome.getName()), track.getStartIndex(),
                        track.getEndIndex());
            }

            track.setBlocks(iterator.stream().map(MafRecord::new).collect(Collectors.toList()));
        }
        double time2 = Utils.getSystemTimeMilliseconds();
        LOGGER.debug("Reading records from MAF file, took {} ms", time2 - time1);
        return track;
    }

    private void processRegistration(MafFile mafFile, File file, IndexedFileRegistrationRequest request)
            throws IOException {
        LOGGER.debug("Registering MAF file " + mafFile.getRealPath());
        fileManager.makeMafDir(mafFile.getId());
        mafFile.setSource(request.getPath());
        if (file.isDirectory()) {
            mergeMaf(file, mafFile);
        } else {
            mafFile.setPath(request.getPath());
            createMafBioItem(mafFile);
            fileManager.makeMafIndex(mafFile);
        }
    }

    private void createMafBioItem(MafFile mafFile) {
        if (mafFile.getBioDataItemId() == null) {
            long id = mafFile.getId();
            biologicalDataItemManager.createBiologicalDataItem(mafFile);
            mafFile.setBioDataItemId(mafFile.getId());
            mafFile.setId(id);
        }
    }

    private void mergeMaf(File directory, MafFile mafFile) throws IOException {
        Assert.notNull(directory.listFiles(), getMessage(ERROR_EMPTY_FOLDER));
        Assert.isTrue(directory.listFiles().length > 0, getMessage(ERROR_EMPTY_FOLDER));
        Reference reference = referenceGenomeManager.load(mafFile.getReferenceId());
        try (BufferedWriter writer = fileManager.makeMafFileWriter(mafFile)) {
            createMafBioItem(mafFile);
            for (File f : directory.listFiles()) {
                if (f.getAbsolutePath().endsWith(MafCodec.MAF_EXTENSION) ||
                        f.getAbsolutePath().endsWith(MafCodec.MAF_COMPRESSED_EXTENSION)) {
                    fileManager.makeMafTempIndex(f, mafFile);
                }
            }
            for (Chromosome chromosome : reference.getChromosomes()) {
                List<MafFeature> currChrFeatures = new ArrayList<>();
                LOGGER.debug("Reading MAF records for chromosome {}", chromosome.getName());
                for (File f : directory.listFiles()) {
                    addFeaturesFromFile(mafFile, chromosome, currChrFeatures, f);
                }
                LOGGER.debug("Sorting MAF records for chromosome {}", chromosome.getName());
                Collections.sort(currChrFeatures, new FeatureComparator());
                LOGGER.debug("Writing MAF records for chromosome {}", chromosome.getName());
                for (MafFeature feature : currChrFeatures) {
                    writer.write(feature.toBigMafString());
                    writer.newLine();
                }
                writer.flush();
            }
            writer.flush();
        } finally {
            fileManager.deleteMafTempDir(mafFile.getId());
        }
        fileManager.makeBigMafIndex(mafFile);
    }

    private void addFeaturesFromFile(MafFile mafFile, Chromosome chromosome,
            List<MafFeature> currChrFeatures, File f) throws IOException {
        if (f.getAbsolutePath().endsWith(MafCodec.MAF_EXTENSION) ||
                f.getAbsolutePath().endsWith(MafCodec.MAF_COMPRESSED_EXTENSION)) {
            File indexFile = fileManager.getMafTempIndex(f, mafFile);
            MafCodec mafCodec = new MafCodec(f.getName());

            try (AbstractFeatureReader<MafFeature, LineIterator> reader = AbstractFeatureReader
                    .getFeatureReader(f.getAbsolutePath(), indexFile.getAbsolutePath(), mafCodec, true, indexCache)) {
                CloseableIterator<MafFeature> iterator = reader.query(chromosome.getName(), 1,
                        chromosome.getSize());
                if (!iterator.hasNext()) {
                    iterator = reader.query(Utils.changeChromosomeName(chromosome.getName()), 1,
                            chromosome.getSize());
                }

                currChrFeatures.addAll(iterator.toList());
            }
        }
    }
}
