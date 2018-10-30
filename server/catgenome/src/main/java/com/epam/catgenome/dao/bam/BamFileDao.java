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

package com.epam.catgenome.dao.bam;

import static com.epam.catgenome.component.MessageCode.WRONG_NAME;
import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.dao.bam.BamFileDao.BamParameters.BAM_ID;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.bam.BamFile;

/**
 * <p>
 * {@code BamFileDao} is a DAO component, that handles database interaction with BAM file metadata.
 * </p>
 */
public class BamFileDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;

    private String bamFileSequenceName;

    private String loadBamFileQuery;
    private String createBamFileQuery;
    private String searchByNameBamFileQuery;
    private String deleteBamFileQuery;
    /**
     * Creates a new ID for a {@code BamFile} instance
     * @return {@code Long} new {@code BamFile} ID
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long createBamFileId() {
        return daoHelper.createId(bamFileSequenceName);
    }


    /**
     * Persists {@code BamFile} record to the database
     * @param bamFile a {@code BamFile} instance to be persisted
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createBamFile(BamFile bamFile) {

        bamFile.setBioDataItemId(bamFile.getId());
        Assert.isTrue(!hasThisName(bamFile.getName()), getMessage(WRONG_NAME));
        bamFile.setId(createBamFileId());
        getNamedParameterJdbcTemplate().update(createBamFileQuery, BiologicalDataItemDao.FeatureFileParameters
                .getLinkedTableParameters(BAM_ID.name(), bamFile));
    }

    /**
     * Loads a persisted {@code BamFile} record by it's ID
     * @param id {@code long} a BamFile ID
     * @return {@code BamFile} instance
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public BamFile loadBamFile(long id) {
        List<BiologicalDataItem> files = getJdbcTemplate().query(loadBamFileQuery, BiologicalDataItemDao
                .BiologicalDataItemParameters.getRowMapper(), id);

        return !files.isEmpty() ? (BamFile) files.get(0) : null;
    }

    /**
     * Checks whether BAM file with a given name is already registered in the system.
     * @param name BAM file name
     * @return true, is file is already registered otherwise false
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public Boolean hasThisName(String name) {
        List<String> names = getJdbcTemplate().query(searchByNameBamFileQuery,
                new SingleColumnRowMapper<>(), name);
        return names.isEmpty();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteBamFile(final long bamFileId) {
        getJdbcTemplate().update(deleteBamFileQuery, bamFileId);
    }

    public void setDeleteBamFileQuery(String deleteGeneFileQuery) {
        this.deleteBamFileQuery = deleteGeneFileQuery;
    }

    @Required
    public void setBamFileSequenceName(String bamFileSequenceName) {
        this.bamFileSequenceName = bamFileSequenceName;
    }

    @Required
    public void setLoadBamFileQuery(String loadBamFileQuery) {
        this.loadBamFileQuery = loadBamFileQuery;
    }

    @Required
    public void setCreateBamFileQuery(String createBamFileQuery) {
        this.createBamFileQuery = createBamFileQuery;
    }


    @Required
    public void setSearchByNameBamFileQuery(String searchByNameBamFileQuery) {
        this.searchByNameBamFileQuery = searchByNameBamFileQuery;
    }

    enum BamParameters {
        BAM_ID,
        NAME
    }
}
