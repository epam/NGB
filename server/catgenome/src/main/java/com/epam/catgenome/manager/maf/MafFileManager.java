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
import com.epam.catgenome.dao.maf.MafFileDao;
import com.epam.catgenome.dao.project.ProjectDao;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.maf.MafFile;
import com.epam.catgenome.entity.project.Project;

/**
 Provides service for managing {@code MafFile} in the system
 */
@Service
public class MafFileManager {
    @Autowired
    private MafFileDao mafFileDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private ProjectDao projectDao;

    /**
     * Saves a {@code MafFile} in the system
     * @param mafFile instance to save
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void createMafFile(MafFile mafFile) {
        mafFileDao.createMafFile(mafFile);
    }

    /**
     * Loads a persisted {@code MafFile} record by it's ID
     * @param mafFileId {@code long} a MafFile ID
     * @return {@code MafFile} instance
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public MafFile loadMafFile(long mafFileId) {
        MafFile mafFile = mafFileDao.loadMafFile(mafFileId);
        Assert.notNull(mafFile, "MAF file with requested ID not found: " + mafFileId);
        return mafFile;
    }

    /**
     * Loads a persisted {@code MafFile} record by it's ID
     * @param mafFileId {@code long} a MafFile ID
     * @return {@code MafFile} instance
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public MafFile loadMafFileNullable(long mafFileId) {
        MafFile mafFile = mafFileDao.loadMafFile(mafFileId);
        return mafFile;
    }

    /**
     * Creates a new ID for a {@code MafFile} instance
     * @return {@code Long} new {@code MafFile} ID
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Long createMafFileId() {
        return mafFileDao.createMafFileId();
    }

    /**
     * Loads {@code MafFile} records, saved for a specific reference ID
     * @param referenceId {@code long} a reference ID in the system
     * @return {@code List&lt;MafFile&gt;} instance
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public List<MafFile> loadMafFilesByReferenceId(long referenceId) {
        return mafFileDao.loadMafFilesByReferenceId(referenceId);
    }

    /**
     * Deletes {@code MafFile} from the database and the file system
     * @param mafFile {@code MafFile} ID to delete
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteMafFile(final MafFile mafFile) {
        List<Project> projectsWhereFileInUse = projectDao.loadProjectsByBioDataItemId(mafFile.getBioDataItemId());
        Assert.isTrue(projectsWhereFileInUse.isEmpty(), MessageHelper.getMessage(
                MessagesConstants.ERROR_FILE_IN_USE, mafFile.getName(), mafFile.getId(),
                projectsWhereFileInUse.stream().map(BaseEntity::getName).collect(Collectors.joining(", "))));
        mafFileDao.deleteMafFile(mafFile.getId());
        biologicalDataItemDao.deleteBiologicalDataItem(mafFile.getIndex().getId());
        biologicalDataItemDao.deleteBiologicalDataItem(mafFile.getBioDataItemId());
    }
}
