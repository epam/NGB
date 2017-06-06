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

package com.epam.catgenome.dao.reference;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;

/**
 * {@code ReferenceGenomeDao} represents DAO which provides different calls to support
 * metadata management for reference genomes, e.g. save new entities, retrieves reference
 * genomes and corresponded chromosomes matched different conditions etc.
 */
public class ReferenceGenomeDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;

    /**
     * {@code String} specifies the fully-qualified sequence name used to generate unique
     * primary key values to manage entities of a chromosome.
     */
    private String chromosomeSequenceName;

    /**
     * {@code String} specifies the fully-qualified sequence name used to generate unique
     * primary key values to manage entities of reference genomes.
     */
    private String referenceGenomeSequenceName;

    /**
     * {@code String} specifies parametrized INSERT statement used to create a record
     * about a new reference genome entity which becomes available in the system.
     */
    private String createReferenceGenomeQuery;

    /**
     * {@code String} specifies parametrized SELECT statement used to load data about
     * a single reference genome associated with the given id.
     */
    private String loadReferenceGenomeByIdQuery;

    /**
     * {@code String} specifies parametrized SELECT statement used to all reference
     * genomes that are available in the system at the moment.
     */
    private String loadAllReferenceGenomesQuery;
    /**
     * {@code String} specifies parametrized INSERT statement used to create a record
     * about a new chromosome entity which becomes available in the system.
     */
    private String createChromosomeQuery;

    /**
     * {@code String} specifies parametrized SELECT statement used to load data about
     * a single chromosome associated with the given id.
     */
    private String loadChromosomeByIdQuery;


    /**
     * {@code String} specifies parametrized SELECT statement used to load data about
     * a single chromosome associated with the given biological data item id.
     */
    private String loadReferenceGenomeByBioIdQuery;

    /**
     * {@code String} specifies parametrized SELECT statement used to all chromosomes that
     * are available in the system for reference genomes identified by the given ID.
     */
    private String loadAllChromosomesByReferenceIdQuery;

    /**
     * {@code String} delete reference by specified id
     */
    private String deleteReferenceQuery;

    /**
     * {@code String} delete chromosomes by specified reference genome id
     */
    private String deleteReferenceChromosomeQuery;
    private String loadBiologicalItemsQuery;
    private String updateReferenceGeneFileIdQuery;
    private String loadReferenceGenomeByNameQuery;

    @Transactional(propagation = Propagation.MANDATORY)
    public Long createReferenceGenomeId() {
        return daoHelper.createId(referenceGenomeSequenceName);
    }

    /**
     * Persists a {@code Reference} entity in database with a specified ID
     * @param reference to persist
     * @param referenceId ID for the reference
     * @return saved {@code Reference} instance
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Reference createReferenceGenome(final Reference reference, final long referenceId) {

        reference.setBioDataItemId(reference.getId());
        reference.setId(referenceId);

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(GenomeParameters.REFERENCE_GENOME_ID.name(), reference.getId());
        params.addValue(GenomeParameters.BIO_DATA_ITEM_ID.name(), reference.getBioDataItemId());
        params.addValue(GenomeParameters.SIZE.name(), reference.getSize());
        params.addValue(GenomeParameters.INDEX_ID.name(), reference.getIndex().getId());
        params.addValue(GenomeParameters.GENE_ITEM_ID.name(), reference.getGeneFile() != null ?
                                                              reference.getGeneFile().getId() : null);

        getNamedParameterJdbcTemplate().update(createReferenceGenomeQuery, params);
        return reference;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Reference createReferenceGenome(final Reference reference) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(GenomeParameters.REFERENCE_GENOME_ID.name(), reference.getId());
        params.addValue(GenomeParameters.BIO_DATA_ITEM_ID.name(), reference.getBioDataItemId());
        params.addValue(GenomeParameters.SIZE.name(), reference.getSize());
        params.addValue(GenomeParameters.INDEX_ID.name(), reference.getIndex().getId());
        params.addValue(GenomeParameters.GENE_ITEM_ID.name(), reference.getGeneFile() != null ?
                reference.getGeneFile().getId() : null);

        getNamedParameterJdbcTemplate().update(createReferenceGenomeQuery, params);
        return reference;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateReferenceGeneFileId(long referenceId, Long geneFileId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(GenomeParameters.REFERENCE_GENOME_ID.name(), referenceId);
        params.addValue(GenomeParameters.GENE_ITEM_ID.name(), geneFileId);

        getNamedParameterJdbcTemplate().update(updateReferenceGeneFileIdQuery, params);
    }

    /**
     * Deletes a persisted {@code Reference} entity from the database by a specified ID
     * @param referenceId {@code Reference} ID to delete
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void unregisterReferenceGenome(final Long referenceId) {
        getJdbcTemplate().update(deleteReferenceChromosomeQuery, referenceId);
        getJdbcTemplate().update(deleteReferenceQuery, referenceId);
    }

    /**
     * Loads a persisted {@code Reference} entity from the database by a specified ID
     * @param referenceId {@code Reference} ID to load
     * @return loaded {@code Reference} instance
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Reference loadReferenceGenome(final Long referenceId) {
        final List<Reference> list = getNamedParameterJdbcTemplate().query(loadReferenceGenomeByIdQuery,
                new MapSqlParameterSource(GenomeParameters.REFERENCE_GENOME_ID.name(), referenceId),
                GenomeParameters.getReferenceGenomeMapper());
        return CollectionUtils.isNotEmpty(list) ? list.get(0) : null;
    }

    /**
     * Loads a persisted {@code Reference} entity from the database by a specified Biological dataItemID
     * @param itemID {@code Reference} Biological DataItemID to load
     * @return loaded {@code Reference} instance
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Reference loadReferenceGenomeByBioItemId(final Long itemID) {
        final List<Reference> list = getNamedParameterJdbcTemplate().query(loadReferenceGenomeByBioIdQuery,
                new MapSqlParameterSource(GenomeParameters.BIO_DATA_ITEM_ID.name(), itemID),
                GenomeParameters.getReferenceGenomeMapper());
        return CollectionUtils.isNotEmpty(list) ? list.get(0) : null;
    }

    public Reference loadReferenceGenomeByName(final String name) {
        final List<Reference> list = getNamedParameterJdbcTemplate().query(loadReferenceGenomeByNameQuery,
                new MapSqlParameterSource(GenomeParameters.NAME.name(), name),
                GenomeParameters.getReferenceGenomeMapper());
        return CollectionUtils.isNotEmpty(list) ? list.get(0) : null;
    }

    /**
     * Loads all persisted {@code Reference} entities from the database
     * @return all {@code Reference} instances, saved in the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Reference> loadAllReferenceGenomes() {
        return getNamedParameterJdbcTemplate().query(loadAllReferenceGenomesQuery,
                GenomeParameters.getReferenceGenomeMetaDataMapper());
    }

    /**
     * Saves {@code List} of {@code Chromosome} entities with a specified ID in the database
     * as one reference
     * @param referenceId for the chromosomes
     * @param chromosomes {@code List} of {@code Chromosome} entities to store int the database
     * @return an array containing the numbers of rows affected by each update in the batch
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int[] saveChromosomes(final Long referenceId, final List<Chromosome> chromosomes) {
        final int count = chromosomes.size();
        final List<Long> chromosomeIds = daoHelper.createIds(chromosomeSequenceName, count);
        final MapSqlParameterSource[] batchArgs = new MapSqlParameterSource[count];
        for (int i = 0; i < count; i++) {
            final Chromosome chromosome = chromosomes.get(i);
            chromosome.setId(chromosomeIds.get(i));
            chromosome.setReferenceId(referenceId);
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(GenomeParameters.NAME.name(), chromosome.getName());
            params.addValue(GenomeParameters.SIZE.name(), chromosome.getSize());
            params.addValue(GenomeParameters.PATH.name(), chromosome.getPath());
            params.addValue(GenomeParameters.HEADER.name(), chromosome.getHeader());
            params.addValue(GenomeParameters.CHROMOSOME_ID.name(), chromosome.getId());
            params.addValue(GenomeParameters.REFERENCE_GENOME_ID.name(), chromosome.getReferenceId());
            batchArgs[i] = params;
        }
        return getNamedParameterJdbcTemplate().batchUpdate(createChromosomeQuery, batchArgs);
    }

    @Required
    public void setLoadChromosomeByIdQuery(final String loadChromosomeByIdQuery) {
        this.loadChromosomeByIdQuery = loadChromosomeByIdQuery;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Chromosome loadChromosome(final Long chromosomeId) {
        final List<Chromosome> list = getNamedParameterJdbcTemplate().query(loadChromosomeByIdQuery,
                new MapSqlParameterSource(GenomeParameters.CHROMOSOME_ID.name(), chromosomeId),
                GenomeParameters.getChromosomeMapper());
        return CollectionUtils.isNotEmpty(list) ? list.get(0) : null;
    }


    @Transactional(propagation = Propagation.MANDATORY)
    public List<Chromosome> loadAllChromosomesByReferenceId(final Long referenceId) {
        return getNamedParameterJdbcTemplate().query(loadAllChromosomesByReferenceIdQuery,
                new MapSqlParameterSource(GenomeParameters.REFERENCE_GENOME_ID.name(), referenceId),
                GenomeParameters.getChromosomeMapper());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<BaseEntity> loadAllFileByReferenceId(final Long referenceId) {
        return getNamedParameterJdbcTemplate().query(loadBiologicalItemsQuery, new MapSqlParameterSource(
            GenomeParameters.REFERENCE_GENOME_ID.name(), referenceId), GenomeParameters.getBioDataMapper());
    }


    @Required
    public void setReferenceGenomeSequenceName(final String referenceGenomeSequenceName) {
        this.referenceGenomeSequenceName = referenceGenomeSequenceName;
    }

    @Required
    public void setChromosomeSequenceName(final String chromosomeSequenceName) {
        this.chromosomeSequenceName = chromosomeSequenceName;
    }

    @Required
    public void setCreateChromosomeQuery(final String createChromosomeQuery) {
        this.createChromosomeQuery = createChromosomeQuery;
    }

    @Required
    public void setLoadAllChromosomesByReferenceIdQuery(final String loadAllChromosomesByReferenceIdQuery) {
        this.loadAllChromosomesByReferenceIdQuery = loadAllChromosomesByReferenceIdQuery;
    }

    @Required
    public void setCreateReferenceGenomeQuery(String createReferenceGenomeQuery) {
        this.createReferenceGenomeQuery = createReferenceGenomeQuery;
    }

    @Required
    public void setLoadReferenceGenomeByIdQuery(String loadReferenceGenomeByIdQuery) {
        this.loadReferenceGenomeByIdQuery = loadReferenceGenomeByIdQuery;
    }

    @Required
    public void setLoadReferenceGenomeByBioIdQuery(String loadReferenceGenomeByBioIdQuery) {
        this.loadReferenceGenomeByBioIdQuery = loadReferenceGenomeByBioIdQuery;
    }

    @Required
    public void setLoadAllReferenceGenomesQuery(String loadAllReferenceGenomesQuery) {
        this.loadAllReferenceGenomesQuery = loadAllReferenceGenomesQuery;
    }

    @Required
    public void setDeleteReferenceQuery(final String deleteReferenceQuery) {
        this.deleteReferenceQuery = deleteReferenceQuery;
    }

    @Required
    public void setDeleteReferenceChromosomeQuery(final String deleteReferenceChromosomeQuery) {
        this.deleteReferenceChromosomeQuery = deleteReferenceChromosomeQuery;
    }

    @Required
    public void setLoadBiologicalItemsQuery(String loadBiologicalItemsQuery) {
        this.loadBiologicalItemsQuery = loadBiologicalItemsQuery;
    }

    @Required
    public String getLoadBiologicalItemsQuery() {
        return loadBiologicalItemsQuery;
    }

    @Required
    public void setUpdateReferenceGeneFileIdQuery(String updateReferenceGeneFileIdQuery) {
        this.updateReferenceGeneFileIdQuery = updateReferenceGeneFileIdQuery;
    }

    @Required
    public void setLoadReferenceGenomeByNameQuery(String loadReferenceGenomeByNameQuery) {
        this.loadReferenceGenomeByNameQuery = loadReferenceGenomeByNameQuery;
    }

    enum GenomeParameters {
        NAME,
        SIZE,
        PATH,
        FORMAT,
        TYPE,

        CREATED_DATE,
        CREATED_BY,
        REFERENCE_GENOME_ID,
        REFERENCE_GENOME_SIZE,
        BIO_DATA_ITEM_ID,
        GENE_ITEM_ID,

        INDEX_ID,
        INDEX_NAME,
        INDEX_TYPE,
        INDEX_PATH,
        INDEX_FORMAT,
        INDEX_CREATED_BY,
        INDEX_BUCKET_ID,
        INDEX_CREATED_DATE,

        HEADER,
        CHROMOSOME_ID;

        static RowMapper<Reference> getReferenceGenomeMetaDataMapper() {
            return (rs, i) -> {
                final Reference reference = new Reference();
                reference.setBioDataItemId(rs.getLong(BIO_DATA_ITEM_ID.name()));
                reference.setSize(rs.getLong(REFERENCE_GENOME_SIZE.name()));
                reference.setName(rs.getString(NAME.name()));
                reference.setPath(rs.getString(PATH.name()));
                reference.setId(rs.getLong(REFERENCE_GENOME_ID.name()));
                reference.setCreatedDate(new Date(rs.getTimestamp(CREATED_DATE.name()).getTime()));

                Long longVal = rs.getLong(TYPE.name());
                reference.setType(rs.wasNull() ? null : BiologicalDataItemResourceType.getById(longVal));

                longVal = rs.getLong(GENE_ITEM_ID.name());
                if (!rs.wasNull()) {
                    GeneFile geneFile = new GeneFile();
                    geneFile.setId(longVal);

                    reference.setGeneFile(geneFile);
                }
                return reference;
            };
        }

        static RowMapper<Reference> getReferenceGenomeMapper() {
            return (rs, i) -> mapReferenceRow(rs);
        }

        static Reference mapReferenceRow(ResultSet rs) throws SQLException {
            final Reference reference = new Reference();
            reference.setSize(rs.getLong(REFERENCE_GENOME_SIZE.name()));
            reference.setName(rs.getString(NAME.name()));
            reference.setPath(rs.getString(PATH.name()));
            reference.setId(rs.getLong(REFERENCE_GENOME_ID.name()));
            reference.setCreatedDate(rs.getDate(CREATED_DATE.name()));
            reference.setCreatedBy(rs.getLong(CREATED_BY.name()));
            reference.setBioDataItemId(rs.getLong(BIO_DATA_ITEM_ID.name()));
            Long longVal = rs.getLong(TYPE.name());
            reference.setType(rs.wasNull() ? null : BiologicalDataItemResourceType.getById(longVal));

            longVal = rs.getLong(GENE_ITEM_ID.name());
            if (!rs.wasNull()) {
                GeneFile geneFile = new GeneFile();
                geneFile.setId(longVal);
                reference.setGeneFile(geneFile);
            }

            long indexId = rs.getLong(INDEX_ID.name());
            if (!rs.wasNull()) {
                BiologicalDataItem index = new BiologicalDataItem();
                index.setId(indexId);
                index.setName(rs.getString(INDEX_NAME.name()));
                index.setType(BiologicalDataItemResourceType.getById(rs.getLong(INDEX_TYPE.name())));
                index.setPath(rs.getString(INDEX_PATH.name()));
                index.setFormat(BiologicalDataItemFormat.getById(rs.getLong(INDEX_FORMAT.name())));
                index.setCreatedBy(rs.getLong(CREATED_BY.name()));
                index.setCreatedDate(new Date(rs.getTimestamp(INDEX_CREATED_DATE.name()).getTime()));
                reference.setIndex(index);
            }

            return reference;
        }

        static RowMapper<Chromosome> getChromosomeMapper() {
            return (rs, i) -> {
                final Chromosome chromosome = new Chromosome();
                chromosome.setSize(rs.getInt(SIZE.name()));
                chromosome.setName(rs.getString(NAME.name()));
                chromosome.setPath(rs.getString(PATH.name()));
                chromosome.setHeader(rs.getString(HEADER.name()));
                chromosome.setId(rs.getLong(CHROMOSOME_ID.name()));
                chromosome.setReferenceId(rs.getLong(REFERENCE_GENOME_ID.name()));
                return chromosome;
            };
        }

        static RowMapper<BaseEntity> getBioDataMapper() {
            return (rs, i) -> {
                final BaseEntity item = new BaseEntity();
                item.setName(rs.getString(NAME.name()));
                item.setId(rs.getLong(BIO_DATA_ITEM_ID.name()));
                return item;
            };
        }
    }

}
