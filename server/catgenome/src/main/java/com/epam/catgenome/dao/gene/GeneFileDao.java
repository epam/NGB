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

package com.epam.catgenome.dao.gene;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.gene.GeneFile;

/**
 * Source:      GeneFileDao
 * Created:     04.12.15, 18:52
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A DAO class for managing {@code GeneFile} entities in the database.
 * </p>
 *
 * @author Mikhail Miroliubov
 */
public class GeneFileDao extends NamedParameterJdbcDaoSupport{
    @Autowired
    private DaoHelper daoHelper;

    private String geneFileSequenceName;

    // New queries
    private String createGeneFileQuery;
    private String loadGeneFileQuery;
    private String loadAllGeneFilesQuery;
    private String loadGeneFilesQuery;
    private String deleteGeneFileQuery;

    /**
     * Persists {@code GeneFile} record to the database
     * @param geneFile a {@code GeneFile} instance to be persisted
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createGeneFile(GeneFile geneFile, final Long realId) {
        geneFile.setBioDataItemId(geneFile.getId());
        geneFile.setId(realId);

        getNamedParameterJdbcTemplate().update(createGeneFileQuery, BiologicalDataItemDao.FeatureFileParameters
                .getLinkedTableParameters(GeneParameters.GENE_ITEM_ID.name(), geneFile));
    }

    /**
     * Persists {@code GeneFile} record to the database
     * @param geneFile a {@code GeneFile} instance to be persisted
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createGeneFile(GeneFile geneFile) {
        getNamedParameterJdbcTemplate().update(createGeneFileQuery, BiologicalDataItemDao.FeatureFileParameters
                .getLinkedTableParameters(GeneParameters.GENE_ITEM_ID.name(), geneFile));
    }

    /**
     * Loads a persisted {@code GeneFile} record by it's ID
     * @param id {@code long} a GeneFile ID
     * @return {@code GeneFile} instance
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public GeneFile loadGeneFile(long id) {
        List<BiologicalDataItem> files = getJdbcTemplate().query(loadGeneFileQuery, BiologicalDataItemDao
                .BiologicalDataItemParameters.getRowMapper(), id);

        return !files.isEmpty() ? (GeneFile) files.get(0) : null;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<GeneFile> loadGeneFiles(final Collection<Long> ids) {
        final List<Long> nonNullIds = ids.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(nonNullIds)) {
            return Collections.emptyList();
        }

        String query = DaoHelper.getQueryFilledWithIdArray(loadGeneFilesQuery, nonNullIds);
        List<BiologicalDataItem> files = getJdbcTemplate().query(query, BiologicalDataItemDao
                .BiologicalDataItemParameters.getRowMapper());

        return files.stream().map(f -> (GeneFile) f).collect(Collectors.toList());
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<GeneFile> loadGeneFiles() {
        final List<BiologicalDataItem> files = getJdbcTemplate().query(loadAllGeneFilesQuery,
                BiologicalDataItemDao.BiologicalDataItemParameters.getRowMapper());
        return CollectionUtils.isNotEmpty(files) ? files.stream().map(f -> (GeneFile) f).collect(Collectors.toList()) :
                Collections.emptyList();
    }

    /**
     * Deletes {@code GeneFile} record from the database
     *
     * @param geneFileId ID of a {@code GeneFile} record to delete
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteGeneFile(final long geneFileId) {
        getJdbcTemplate().update(deleteGeneFileQuery, geneFileId);
    }

    /**
     * Creates a new ID for a {@code GeneFile} instance
     * @return {@code Long} new {@code GeneFile} ID
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long createGeneFileId() {
        return daoHelper.createId(geneFileSequenceName);
    }

    @Required
    public void setGeneFileSequenceName(String geneFileSequenceName) {
        this.geneFileSequenceName = geneFileSequenceName;
    }

    @Required
    public void setCreateGeneFileQuery(String createGeneFileQuery) {
        this.createGeneFileQuery = createGeneFileQuery;
    }

    @Required
    public void setLoadGeneFileQuery(String loadGeneFileQuery) {
        this.loadGeneFileQuery = loadGeneFileQuery;
    }

    @Required
    public void setLoadAllGeneFilesQuery(String loadAllGeneFilesQuery) {
        this.loadAllGeneFilesQuery = loadAllGeneFilesQuery;
    }

    @Required
    public void setDeleteGeneFileQuery(String deleteGeneFileQuery) {
        this.deleteGeneFileQuery = deleteGeneFileQuery;
    }

    @Required
    public void setLoadGeneFilesQuery(String loadGeneFilesQuery) {
        this.loadGeneFilesQuery = loadGeneFilesQuery;
    }

    enum GeneParameters {
        GENE_ITEM_ID,
        REFERENCE_GENOME_ID,
        ORIGINAL_NAME,
        CREATED_DATE,
        FILE_PATH,
        INDEX_PATH,
        BOUNDS_PATH,
        COMPRESSED,
        ONLINE,
        EXTERNAL_DB_TYPE_ID,
        EXTERNAL_DB_ID,
        EXTERNAL_DB_ORGANISM
    }
}
