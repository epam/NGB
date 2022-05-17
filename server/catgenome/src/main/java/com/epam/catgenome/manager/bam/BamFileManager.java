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

package com.epam.catgenome.manager.bam;

import java.util.List;
import java.util.stream.Collectors;

import com.epam.catgenome.entity.security.AbstractSecuredEntity;
import com.epam.catgenome.entity.security.AclClass;
import com.epam.catgenome.manager.SecuredEntityManager;
import com.epam.catgenome.security.acl.aspect.AclSync;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.bam.BamFileDao;
import com.epam.catgenome.dao.project.ProjectDao;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.project.Project;

/**
 * Source:      BamFileManager.java
 * Created:     12/7/2015
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * Provides service for managing {@code BamFile} in the system
 */
@AclSync
@Service
public class BamFileManager implements SecuredEntityManager {

    @Autowired
    private BamFileDao bamFileDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private ProjectDao projectDao;

    /**
     * Persists {@code BamFile} record to the database
     *
     * @param bamFile a {@code BamFile} instance to be persisted
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public BamFile create(BamFile bamFile) {
        Assert.notNull(bamFile);
        Assert.notNull(bamFile.getName());
        Assert.notNull(bamFile.getReferenceId());
        Assert.notNull(bamFile.getPath());

        bamFileDao.createBamFile(bamFile);
        return bamFile;
    }

    /**
     * Loads a persisted {@code BamFile} record by it's ID
     *
     * @param bamFileId {@code long} a BamFile ID
     * @return {@code BamFile} instance
     */
    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public BamFile load(Long bamFileId) {
        return bamFileDao.loadBamFile(bamFileId);
    }

    /**
     * Deletes {@code BamFile} from the database and file system
     * @param bamFile a {@code BamFile} to delete
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public BamFile delete(BamFile bamFile) {
        List<Project> projectsWhereFileInUse = projectDao.loadProjectsByBioDataItemId(bamFile.getBioDataItemId());
        Assert.isTrue(projectsWhereFileInUse.isEmpty(), MessageHelper.getMessage(MessagesConstants.ERROR_FILE_IN_USE,
                bamFile.getName(), bamFile.getId(), projectsWhereFileInUse.stream().map(BaseEntity::getName)
                        .collect(Collectors.joining(", "))));

        bamFileDao.deleteBamFile(bamFile.getId());
        biologicalDataItemDao.deleteBiologicalDataItem(bamFile.getIndex().getId());
        biologicalDataItemDao.deleteBiologicalDataItem(bamFile.getBioDataItemId());
        return bamFile;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public AbstractSecuredEntity changeOwner(Long id, String owner) {
        BamFile bamFile = load(id);
        biologicalDataItemDao.updateOwner(bamFile.getBioDataItemId(), owner);
        bamFile.setOwner(owner);
        return bamFile;
    }

    @Override
    public AclClass getSupportedClass() {
        return AclClass.BAM;
    }

}
