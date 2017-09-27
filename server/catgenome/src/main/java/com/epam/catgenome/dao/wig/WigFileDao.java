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

package com.epam.catgenome.dao.wig;

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
import com.epam.catgenome.entity.wig.WigFile;

/**
 * <p>
 * {@code WigFileDao} is a DAO component, that handles database interaction with Wig
 *  file metadata.
 * </p>
 */
public class WigFileDao extends NamedParameterJdbcDaoSupport {

    private String wigFileSequenceName;
    private String createWigFileQuery;
    private String loadWigFileQuery;
    private String loadWigFilesByReferenceIdQuery;
    private String deleteWigFileQuery;

    @Autowired
    private DaoHelper daoHelper;

    /**
     * Persists {@code WigFile} record to the database
     * @param file a {@code WigFile} instance to be persisted
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createWigFile(WigFile file) {
        getNamedParameterJdbcTemplate().update(createWigFileQuery, BiologicalDataItemDao.FeatureFileParameters
                .getLinkedTableParameters(BedGraphParameters.BED_GRAPH_ID.name(), file));
    }

    /**
     * Loads {@code WigFile} records, saved for a specific reference ID
     * @param referenceId {@code long} a reference ID in the system
     * @return a {@code List} of {@code WigFile} instances
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<WigFile> loadWigFilesByReferenceId(long referenceId) {
        return getJdbcTemplate().query(loadWigFilesByReferenceIdQuery, BiologicalDataItemDao
                .BiologicalDataItemParameters.getRowMapper(), referenceId)
                .stream().map(f -> (WigFile) f).collect(Collectors.toList());
    }

    /**
     * Loads a persisted {@code WigFile} record by it's ID
     * @param id {@code long} a WigFile ID
     * @return {@code WigFile} instance
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public WigFile loadWigFile(long id) {
        List<BiologicalDataItem> files = getJdbcTemplate().query(loadWigFileQuery, BiologicalDataItemDao
                .BiologicalDataItemParameters.getRowMapper(), id);
        return !files.isEmpty() ? (WigFile) files.get(0) : null;
    }

    /**
     * Deletes {@code WigFile} metadata from the database
     * @param wigFileId of a  {@code WigFile} to delete
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteWigFile(final long wigFileId) {
        getJdbcTemplate().update(deleteWigFileQuery, wigFileId);
    }

    public void setDeleteWigFileQuery(String deleteWigFileQuery) {
        this.deleteWigFileQuery = deleteWigFileQuery;
    }

    public String getDeleteWigFileQuery() {
        return deleteWigFileQuery;
    }

    /**
     * Creates a new ID for a {@code WigFile} instance
     * @return {@code Long} new {@code WigFile} ID
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long createWigFileId() {
        return daoHelper.createId(wigFileSequenceName);
    }

    enum BedGraphParameters {
        BED_GRAPH_ID
    }

    @Required
    public void setWigFileSequenceName(String wigFileSequenceName) {
        this.wigFileSequenceName = wigFileSequenceName;
    }

    @Required
    public void setCreateWigFileQuery(String createWigFileQuery) {
        this.createWigFileQuery = createWigFileQuery;
    }

    @Required
    public void setLoadWigFileQuery(String loadWigFileQuery) {
        this.loadWigFileQuery = loadWigFileQuery;
    }

    @Required
    public void setLoadWigFilesByReferenceIdQuery(String loadWigFilesByReferenceIdQuery) {
        this.loadWigFilesByReferenceIdQuery = loadWigFilesByReferenceIdQuery;
    }
}
