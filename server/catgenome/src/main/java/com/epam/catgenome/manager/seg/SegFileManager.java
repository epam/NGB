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

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.project.ProjectDao;
import com.epam.catgenome.dao.seg.SegFileDao;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.seg.SegFile;
import com.epam.catgenome.util.NgbFileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;


/**
 * {@code SegFileManager} provides a service for handling seg file and it's metadata
 * in the system
 */
@Service
public class SegFileManager {
    @Autowired
    private SegFileDao segFileDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private ProjectDao projectDao;

    @Value("${files.base.directory.path}")
    private String baseDirPath;

    /**
     * Saves a {@code SegFile} in the system and extracts and saves samples from the file
     * @param segFile to save
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void createSegFile(SegFile segFile) {
        if (segFile.getBioDataItemId() == null) {
            long realId = segFile.getId();
            biologicalDataItemDao.createBiologicalDataItem(segFile);
            segFileDao.createSegFile(segFile, realId);
        } else {
            segFileDao.createSegFile(segFile);
        }
        segFileDao.createSamples(segFile.getSamples(), segFile.getId());
    }

    /**
     * Loads a persisted {@code SegFile} record by it's ID
     *
     * @param segFileId {@code long} a BedFile ID
     * @return {@code SegFile} instance
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public SegFile loadSegFile(long segFileId) {
        SegFile segFile = segFileDao.loadSegFile(segFileId);

        if (segFile != null) {
            NgbFileUtils.resolveRelativeIfNeeded(segFile, baseDirPath);
            segFile.setSamples(segFileDao.loadSamplesForFile(segFileId));
        }

        return segFile;
    }

    /**
     * Creates a new ID for a {@code SegFile} instance
     *
     * @return {@code Long} new {@code SegFile} ID
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Long createSegFileId() {
        return segFileDao.createSegFileId();
    }

    /**
     * Loads {@code SegFile} records, saved for a specific reference ID
     *
     * @param referenceId {@code long} a reference ID in the system
     * @return {@code List&lt;SegFile&gt;} instance
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<SegFile> loadSedFilesByReferenceId(long referenceId) {
        List<SegFile> segFiles = segFileDao.loadSegFilesByReferenceId(referenceId);
        segFiles.forEach(segFile -> NgbFileUtils.resolveRelativeIfNeeded(segFile, baseDirPath));
        return segFiles;
    }

    /**
     * Deletes {@code SegFile} from the system and all the related information
     * @param segFile to delete
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteSegFile(SegFile segFile) {
        List<Project> projectsWhereFileInUse = projectDao.loadProjectsByBioDataItemId(segFile.getBioDataItemId());
        Assert.isTrue(projectsWhereFileInUse.isEmpty(), MessageHelper.getMessage(MessagesConstants.ERROR_FILE_IN_USE,
                segFile.getName(), segFile.getId(), projectsWhereFileInUse.stream().map(BaseEntity::getName)
                        .collect(Collectors.joining(", "))));


        segFileDao.deleteSegFile(segFile.getId());
        biologicalDataItemDao.deleteBiologicalDataItem(segFile.getIndex().getId());
        biologicalDataItemDao.deleteBiologicalDataItem(segFile.getBioDataItemId());
    }
}
