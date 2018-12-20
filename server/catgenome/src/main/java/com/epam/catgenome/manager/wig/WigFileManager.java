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
import com.epam.catgenome.dao.project.ProjectDao;
import com.epam.catgenome.dao.wig.WigFileDao;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.wig.WigFile;

/**
 * Source:      WigFileManager.java
 * Created:     1/25/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code WigFileManager} provides a service for handling wig file and it's metadata
 * in the system
 * </p>
 */
@AclSync
@Service
public class WigFileManager implements SecuredEntityManager {

    @Autowired
    private WigFileDao wigFileDao;

    @Autowired
    private ProjectDao projectDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    /**
     * Persists {@code WigFile} record to the database
     *
     * @param wigFile a {@code WigFile} instance to be persisted
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void save(WigFile wigFile) {

        Assert.notNull(wigFile.getName());
        Assert.notNull(wigFile.getReferenceId());
        Assert.notNull(wigFile.getPath());
        Assert.notNull(wigFile.getType());
        Assert.notNull(wigFile.getFormat());
        wigFileDao.createWigFile(wigFile);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Long create() {
        return wigFileDao.createWigFileId();
    }

    /**
     * Loads a persisted {@code WigFile} record by it's ID
     *
     * @param wigFileId {@code long} a WigFile ID
     * @return {@code WigFile} instance
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public WigFile load(Long wigFileId) {
        return wigFileDao.loadWigFile(wigFileId);
    }

    /**
     * Deletes {@code WigFile} from the system and all the related information
     * @param wigFile to delete
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(WigFile wigFile) {
        List<Project> projectsWhereFileInUse = projectDao.loadProjectsByBioDataItemId(wigFile.getBioDataItemId());
        Assert.isTrue(projectsWhereFileInUse.isEmpty(), MessageHelper.getMessage(MessagesConstants.ERROR_FILE_IN_USE,
                wigFile.getName(), wigFile.getId(), projectsWhereFileInUse.stream().map(BaseEntity::getName)
                        .collect(Collectors.joining(", "))));

        wigFileDao.deleteWigFile(wigFile.getId());
        biologicalDataItemDao.deleteBiologicalDataItem(wigFile.getBioDataItemId());
        if (wigFile.getIndex() != null) {
            biologicalDataItemDao.deleteBiologicalDataItem(wigFile.getIndex().getId());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public AbstractSecuredEntity changeOwner(Long id, String owner) {
        WigFile file = load(id);
        biologicalDataItemDao.updateOwner(file.getBioDataItemId(), owner);
        file.setOwner(owner);
        return file;
    }

    @Override
    public AclClass getSupportedClass() {
        return AclClass.WIG;
    }

}
