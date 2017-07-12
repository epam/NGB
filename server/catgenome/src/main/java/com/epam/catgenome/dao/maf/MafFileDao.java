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

package com.epam.catgenome.dao.maf;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.maf.MafFile;

/**
 * {@code MafFileDao} is a DAO component, that handles database interaction with MAF file metadata.
 */
public class MafFileDao extends NamedParameterJdbcDaoSupport {
    @Autowired
    private DaoHelper daoHelper;

    private String mafFileSequenceName;

    private String createMafFileQuery;
    private String loadMafFileQuery;
    private String loadMafFilesByReferenceIdQuery;

    private String deleteMafFileQuery;

    /**
     * Creates a new ID for a {@code MafFile} instance
     * @return {@code Long} new {@code MafFile} ID
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long createMafFileId() {
        return daoHelper.createId(mafFileSequenceName);
    }

    /**
     * Persists {@code MafFile} record to the database
     * @param mafFile a {@code MafFile} instance to be persisted
     * @param realId SegFile's real ID, generated with SegFileDao::createGeneFileId() method
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createMafFile(MafFile mafFile, long realId) {
        mafFile.setBioDataItemId(mafFile.getId());
        mafFile.setId(realId);

        MapSqlParameterSource params = BiologicalDataItemDao.FeatureFileParameters
                .getLinkedTableParameters(MafParameters.MAF_ID.name(), mafFile);
        params.addValue(MafParameters.REAL_PATH.name(), mafFile.getRealPath());

        getNamedParameterJdbcTemplate().update(createMafFileQuery, params);
    }

    /**
     * Persists {@code MafFile} record to the database
     * @param mafFile a {@code MafFile} instance to be persisted
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createMafFile(MafFile mafFile) {
        MapSqlParameterSource params = BiologicalDataItemDao.FeatureFileParameters
                .getLinkedTableParameters(MafParameters.MAF_ID.name(), mafFile);
        params.addValue(MafParameters.REAL_PATH.name(), mafFile.getRealPath());

        getNamedParameterJdbcTemplate().update(createMafFileQuery, params);
    }

    /**
     * Loads {@code MafFile} records, saved for a specific reference ID
     * @param referenceId {@code long} a reference ID in the system
     * @return a {@code List} of {@code MafFile} instances
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<MafFile> loadMafFilesByReferenceId(long referenceId) {
        return getJdbcTemplate().query(loadMafFilesByReferenceIdQuery, BiologicalDataItemDao
                .BiologicalDataItemParameters.getRowMapper(), referenceId)
                .stream().map(f -> (MafFile) f).collect(Collectors.toList());
    }

    /**
     * Loads a persisted {@code MafFile} record by it's ID
     * @param id {@code long} a MafFile ID
     * @return {@code MafFile} instance
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public MafFile loadMafFile(long id) {
        List<BiologicalDataItem> files = getJdbcTemplate().query(loadMafFileQuery, BiologicalDataItemDao
                .BiologicalDataItemParameters.getRowMapper(), id);

        return !files.isEmpty() ? (MafFile) files.get(0) : null;
    }

    /**
     * Deletes a persisted {@code MafFile} record by it's ID
     * @param id {@code long} a MafFile ID
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteMafFile(long id) {
        getJdbcTemplate().update(deleteMafFileQuery, id);
    }

    @Required
    public void setMafFileSequenceName(String mafFileSequenceName) {
        this.mafFileSequenceName = mafFileSequenceName;
    }

    @Required
    public void setCreateMafFileQuery(String createMafFileQuery) {
        this.createMafFileQuery = createMafFileQuery;
    }

    @Required
    public void setLoadMafFileQuery(String loadMafFileQuery) {
        this.loadMafFileQuery = loadMafFileQuery;
    }

    @Required
    public void setLoadMafFilesByReferenceIdQuery(String loadMafFilesByReferenceIdQuery) {
        this.loadMafFilesByReferenceIdQuery = loadMafFilesByReferenceIdQuery;
    }

    @Required
    public void setDeleteMafFileQuery(String deleteMafFileQuery) {
        this.deleteMafFileQuery = deleteMafFileQuery;
    }

    private enum MafParameters {
        MAF_ID,
        REAL_PATH
    }
}
