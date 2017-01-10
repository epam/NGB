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

package com.epam.catgenome.dao.bed;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.bed.BedFile;

/**
 * {@code BedFileDao} is a DAO component, that handles database interaction with Bed file metadata.
 */
public class BedFileDao extends NamedParameterJdbcDaoSupport {
    @Autowired
    private DaoHelper daoHelper;

    private String bedFileSequenceName;

    private String createBedFileQuery;
    private String loadBedFileQuery;
    private String loadBedFilesByReferenceIdQuery;
    private String deleteBedFileQuery;

    /**
     * Creates a new ID for a {@code BedFile} instance
     * @return {@code Long} new {@code BedFile} ID
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long createBedFileId() {
        return daoHelper.createId(bedFileSequenceName);
    }

    /**
     * Persists {@code BedFile} record to the database
     * @param bedFile a {@code BedFile} instance to be persisted
     * @param realId BedFile's real ID, generated with BedFileDao::createGeneFileId() method
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createBedFile(BedFile bedFile, long realId) {
        bedFile.setBioDataItemId(bedFile.getId());
        bedFile.setId(realId);

        getNamedParameterJdbcTemplate().update(createBedFileQuery, BiologicalDataItemDao.FeatureFileParameters
                .getLinkedTableParameters(BedParameters.BED_ID.name(), bedFile));
    }

    /**
     * Loads a persisted {@code BedFile} record by it's ID
     * @param id {@code long} a BedFile ID
     * @return {@code BedFile} instance
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public BedFile loadBedFile(long id) {
        List<BiologicalDataItem> files = getJdbcTemplate().query(loadBedFileQuery, BiologicalDataItemDao
                .BiologicalDataItemParameters.getRowMapper(), id);

        return !files.isEmpty() ? (BedFile) files.get(0) : null;
    }

    /**
     * Loads {@code BedFile} records, saved for a specific reference ID
     * @param referenceId {@code long} a reference ID in the system
     * @return a {@code List} of {@code BedFile} instances
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<BedFile> loadBedFilesByReferenceId(long referenceId) {
        return getJdbcTemplate().query(loadBedFilesByReferenceIdQuery, BiologicalDataItemDao
                .BiologicalDataItemParameters.getRowMapper(), referenceId)
                .stream().map(f -> (BedFile) f).collect(Collectors.toList());
    }

    /**
     * Deletes {@code BedFile} record from the databases
     * @param bedFileId {@code long} a file ID in the system to delete
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteBedFile(final long bedFileId) {
        getJdbcTemplate().update(deleteBedFileQuery, bedFileId);
    }

    private enum BedParameters {
        BED_ID
    }

    @Required
    public void setBedFileSequenceName(String bedFileSequenceName) {
        this.bedFileSequenceName = bedFileSequenceName;
    }

    @Required
    public void setCreateBedFileQuery(String createBedFileQuery) {
        this.createBedFileQuery = createBedFileQuery;
    }

    @Required
    public void setLoadBedFileQuery(String loadBedFileQuery) {
        this.loadBedFileQuery = loadBedFileQuery;
    }

    @Required
    public void setLoadBedFilesByReferenceIdQuery(String loadBedFilesByReferenceIdQuery) {
        this.loadBedFilesByReferenceIdQuery = loadBedFilesByReferenceIdQuery;
    }

    @Required
    public void setDeleteBedFileQuery(String deleteBedFileQuery) {
        this.deleteBedFileQuery = deleteBedFileQuery;
    }
}
