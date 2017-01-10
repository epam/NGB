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

package com.epam.catgenome.manager.gene;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.gene.GeneFileDao;
import com.epam.catgenome.dao.project.ProjectDao;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.project.Project;

/**
 * Source:      GeneFileManager
 * Created:     05.12.15, 13:21
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * <p>
 * A service class to handle gene file metadata operations.
 * </p>
 *
 *
 */
@Service
public class GeneFileManager {
    @Autowired
    private GeneFileDao geneFileDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private ProjectDao projectDao;

    /**
     * Persists {@code GeneFile} record to the database
     *
     * @param geneFile a {@code GeneFile} instance to be persisted
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void createGeneFile(GeneFile geneFile) {
        final Long realId = geneFile.getId();
        biologicalDataItemDao.createBiologicalDataItem(geneFile);
        geneFileDao.createGeneFile(geneFile, realId);
    }

    /**
     * Removes {@code GeneFile} record from the database
     *
     * @param geneFile a {@code GeneFile} record to remove
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteGeneFile(GeneFile geneFile) {
        List<Project> projectsWhereFileInUse = projectDao.loadProjectsByBioDataItemId(geneFile.getBioDataItemId());
        Assert.isTrue(projectsWhereFileInUse.isEmpty(), MessageHelper.getMessage(MessagesConstants.ERROR_FILE_IN_USE,
                geneFile.getName(), geneFile.getId(), projectsWhereFileInUse.stream().map(BaseEntity::getName)
                        .collect(Collectors.joining(", "))));

        geneFileDao.deleteGeneFile(geneFile.getId());
        biologicalDataItemDao.deleteBiologicalDataItem(geneFile.getIndex().getId());
        biologicalDataItemDao.deleteBiologicalDataItem(geneFile.getBioDataItemId());
    }

    /**
     * Loads a persisted {@code GeneFile} record by it's ID
     *
     * @param geneFileId {@code long} a GeneFile ID
     * @return {@code GeneFile} instance
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public GeneFile loadGeneFile(long geneFileId) {
        GeneFile geneFile = geneFileDao.loadGeneFile(geneFileId);
        Assert.notNull(geneFile, MessageHelper.getMessage(MessagesConstants.ERROR_FILE_NOT_FOUND, geneFileId));
        return geneFile;
    }

    /**
     * Creates a new ID for a {@code GeneFile} instance
     *
     * @return {@code Long} new {@code GeneFile} ID
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Long createGeneFileId() {
        return geneFileDao.createGeneFileId();
    }

    /**
     * Loads {@code GeneFile} records, saved for a specific reference ID
     *
     * @param referenceId {@code long} a reference ID in the system
     * @return {@code List&lt;GeneFile&gt;} instance
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public List<GeneFile> loadGeneFilesByReferenceId(long referenceId) {
        return geneFileDao.loadGeneFilesByReferenceId(referenceId);
    }
}
