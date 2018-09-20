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

package com.epam.catgenome.manager.vcf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.project.ProjectDao;
import com.epam.catgenome.dao.vcf.VcfFileDao;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.security.AbstractSecuredEntity;
import com.epam.catgenome.entity.security.AclClass;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfSample;
import com.epam.catgenome.manager.SecuredEntityManager;


/**
 * Source:      VcfFileManager.java
 * Created:     12.11.15, 13:54
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code VcfFileManager} is a service component that performs transactional actions with the database
 * concerning {@code VcfFile}.
 * </p>
 */
@Service
public class VcfFileManager implements SecuredEntityManager {

    @Autowired
    private VcfFileDao vcfFileDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private ProjectDao projectDao;

    /**
     * Persists {@code VcfFile} record to the database
     *
     * @param vcfFile a {@code VcfFile} instance to be persisted
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void createVcfFile(VcfFile vcfFile) {
        vcfFileDao.createVcfFile(vcfFile);
        if (vcfFile.getSamples() != null) {
            vcfFileDao.createSamples(vcfFile.getSamples(), vcfFile.getId());
        }
    }

    /**
     * Loads a persisted {@code VcfFile} record by it's ID
     *
     * @param vcfFileId {@code long} a VcfFile ID
     * @return {@code VcfFile} instance
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public VcfFile load(Long vcfFileId) {
        VcfFile vcfFile = vcfFileDao.loadVcfFile(vcfFileId);
        if (vcfFile != null) {
            vcfFile.setSamples(vcfFileDao.loadSamplesForFile(vcfFileId));
        }

        return vcfFile;
    }

    @Override
    public AbstractSecuredEntity changeOwner(Long id, String owner) {
        throw new NotImplementedException();
    }

    @Override
    public AclClass getSupportedClass() {
        return AclClass.VCF;
    }

    @Override
    public Collection<? extends AbstractSecuredEntity> loadAllWithParents(Integer page, Integer pageSize) {
        throw new UnsupportedOperationException();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public List<VcfFile> loadVcfFiles(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }

        List<VcfFile> files = vcfFileDao.loadVcfFiles(ids);
        if (files.size() != ids.size()) {
            List<Long> notFound = new ArrayList<>(ids);
            notFound.removeAll(files.stream().map(BaseEntity::getId).collect(Collectors.toList()));
            Assert.isTrue(notFound.isEmpty(), MessageHelper.getMessage(MessagesConstants.ERROR_FILE_NOT_FOUND,
                                                                       notFound.stream()
                                                                           .map(i -> i.toString())
                                                                           .collect(Collectors.joining(", "))));
        }

        return files;
    }

    /**
     * Delete vcf file metadata from database and corresponding vcf samples.
     *
     * @param vcfFile file to delete
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteVcfFile(final VcfFile vcfFile) {
        Assert.notNull(vcfFile, MessagesConstants.ERROR_INVALID_PARAM);
        Assert.notNull(vcfFile.getId(), MessagesConstants.ERROR_INVALID_PARAM);

        List<Project> projectsWhereFileInUse = projectDao.loadProjectsByBioDataItemId(vcfFile.getBioDataItemId());
        Assert.isTrue(projectsWhereFileInUse.isEmpty(), MessageHelper.getMessage(MessagesConstants.ERROR_FILE_IN_USE,
                vcfFile.getName(), vcfFile.getId(), projectsWhereFileInUse.stream().map(BaseEntity::getName)
                        .collect(Collectors.joining(", "))));

        vcfFileDao.deleteVcfFile(vcfFile.getId());
        biologicalDataItemDao.deleteBiologicalDataItem(vcfFile.getIndex().getId());
        biologicalDataItemDao.deleteBiologicalDataItem(vcfFile.getBioDataItemId());
    }

    /**
     * Creates a new ID for a {@code VcfFile} instance
     *
     * @return {@code Long} new {@code VcfFile} ID
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Long createVcfFileId() {
        return vcfFileDao.createVcfFileId();
    }

    /**
     * Loads {@code VcfFile} records, saved for a specific reference ID
     *
     * @param referenceId {@code long} a reference ID in the system
     * @return {@code List&lt;VcfFile&gt;} instance
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<VcfFile> loadVcfFilesByReferenceId(long referenceId) {
        List<VcfFile> files = vcfFileDao.loadVcfFilesByReferenceId(referenceId);

        Map<Long, List<VcfSample>> sampleMap = vcfFileDao.loadSamplesForFilesByReference(referenceId);

        for (VcfFile vcfFile : files) {
            vcfFile.setSamples(sampleMap.get(vcfFile.getId()));
        }

        return files;
    }
}
