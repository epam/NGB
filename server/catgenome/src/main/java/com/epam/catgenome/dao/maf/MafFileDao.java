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

import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.maf.MafFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

/**
 * {@code MafFileDao} is a DAO component, that handles database interaction with MAF file metadata.
 */
public class MafFileDao extends NamedParameterJdbcDaoSupport {
    @Autowired
    private DaoHelper daoHelper;

    private String mafFileSequenceName;

    private String createMafFileQuery;
    private String loadMafFileQuery;

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

    public void setMafFileSequenceName(String mafFileSequenceName) {
        Assert.hasText(mafFileSequenceName, "mafFileSequenceName cannot be null or empty");
        this.mafFileSequenceName = mafFileSequenceName;
    }

    public void setCreateMafFileQuery(String createMafFileQuery) {
        Assert.hasText(createMafFileQuery, "createMafFileQuery cannot be null or empty");
        this.createMafFileQuery = createMafFileQuery;
    }

    public void setLoadMafFileQuery(String loadMafFileQuery) {
        Assert.hasText(loadMafFileQuery, "loadMafFileQuery cannot be null or empty");
        this.loadMafFileQuery = loadMafFileQuery;
    }

    public void setDeleteMafFileQuery(String deleteMafFileQuery) {
        Assert.hasText(deleteMafFileQuery, "deleteMafFileQuery cannot be null or empty");
        this.deleteMafFileQuery = deleteMafFileQuery;
    }
    private enum MafParameters {
        MAF_ID,
        REAL_PATH
    }
}
