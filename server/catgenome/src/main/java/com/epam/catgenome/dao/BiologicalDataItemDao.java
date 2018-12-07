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

package com.epam.catgenome.dao;

import static com.epam.catgenome.dao.BiologicalDataItemDao.BiologicalDataItemParameters.getRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.bed.BedFile;
import com.epam.catgenome.entity.externaldb.ExternalDB;
import com.epam.catgenome.entity.externaldb.ExternalDBType;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.maf.MafFile;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.seg.SegFile;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.wig.WigFile;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Source:      BiologicalDataItemDao
 * Created:     17.12.15, 13:12
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A DAO class, managing BiologicalDataItem entities and containing common logic for it's ancestors retrieval
 * </p>
 */
public class BiologicalDataItemDao extends NamedParameterJdbcDaoSupport {
    private String biologicalDataItemSequenceName;
    private String insertBiologicalDataItemQuery;
    private String updateOwnerQuery;
    private String loadBiologicalDataItemsByIdsQuery;
    private String deleteBiologicalDataItemQuery;
    private String loadBiologicalDataItemsByNameStrictQuery;
    private String loadBiologicalDataItemsByNamesStrictQuery;
    private String loadBiologicalDataItemsByNameQuery;
    private String loadBiologicalDataItemsByNameCaseInsensitiveQuery;

    @Autowired
    private DaoHelper daoHelper;

    /**
     * Persists a BiologicalDataItem instance into the database
     * @param item BiologicalDataItem to persist
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createBiologicalDataItem(BiologicalDataItem item) {
        if (!item.getFormat().isIndex() ||
                (item.getFormat().isIndex() && !StringUtils.isEmpty(item.getName()))) {

            Assert.isTrue(!StringUtils.isEmpty(item.getName()),
                    "File name is required for registration.");
            List<BiologicalDataItem> items = loadFilesByNameStrict(item.getName());
            Assert.isTrue(items.isEmpty(), MessageHelper
                    .getMessage(MessagesConstants.ERROR_FILE_NAME_EXISTS, item.getName()));
            item.setId(daoHelper.createId(biologicalDataItemSequenceName));
        } else {
            item.setId(daoHelper.createId(biologicalDataItemSequenceName));
            item.setName("INDEX " + item.getId());
        }

        final MapSqlParameterSource params = new MapSqlParameterSource();

        params.addValue(BiologicalDataItemParameters.BIO_DATA_ITEM_ID.name(), item.getId());
        params.addValue(BiologicalDataItemParameters.NAME.name(), item.getName());
        params.addValue(BiologicalDataItemParameters.TYPE.name(), item.getType().getId());
        params.addValue(BiologicalDataItemParameters.PATH.name(), item.getPath());
        params.addValue(BiologicalDataItemParameters.FORMAT.name(), item.getFormat().getId());
        params.addValue(BiologicalDataItemParameters.CREATED_DATE.name(), item.getCreatedDate());
        params.addValue(BiologicalDataItemParameters.BUCKET_ID.name(), item.getBucketId());
        params.addValue(BiologicalDataItemParameters.PRETTY_NAME.name(), item.getPrettyName());
        params.addValue(BiologicalDataItemParameters.OWNER.name(), item.getOwner());

        getNamedParameterJdbcTemplate().update(insertBiologicalDataItemQuery, params);
    }

    /**
     * Loads a List of BiologicalDataItem from the database by their IDs
     * @param ids List of IDs of BiologicalDataItem instances
     * @return List of BiologicalDataItem, matching specified IDs
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<BiologicalDataItem> loadBiologicalDataItemsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        String query = DaoHelper.getQueryFilledWithIdArray(loadBiologicalDataItemsByIdsQuery, ids);
        return getNamedParameterJdbcTemplate().query(query, getRowMapper());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Long createBioItemId() {
        return daoHelper.createId(biologicalDataItemSequenceName);
    }

    /**
     * Deletes a BiologicalDataItem instance from the database by it's ID
     * @param bioDataItemId ID of BiologicalDataItem instance to delete
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteBiologicalDataItem(final long bioDataItemId) {
        getJdbcTemplate().update(deleteBiologicalDataItemQuery, bioDataItemId);
    }

    /**
     * Update owner for given bioItemId
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateOwner(long bioItemId, String newOwner) {
        Assert.isTrue(StringUtils.isNotBlank(newOwner), "Owner cannot be empty");
        final MapSqlParameterSource params = new MapSqlParameterSource();

        params.addValue(BiologicalDataItemParameters.BIO_DATA_ITEM_ID.name(), bioItemId);
        params.addValue(BiologicalDataItemParameters.OWNER.name(), newOwner);

        getNamedParameterJdbcTemplate().update(updateOwnerQuery, params);
    }

    /**
     * An enum of BiologicalDataItem's ancestor's fields
     */
    /**
     * Finds files with a specified file name, checks name for strict, case sensitive equality
     * @param name search query
     * @return {@code List} of files with a matching name
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<BiologicalDataItem> loadFilesByNameStrict(final String name) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(BiologicalDataItemParameters.NAME.name(), name);
        return getNamedParameterJdbcTemplate().query(loadBiologicalDataItemsByNameStrictQuery,
                params, getRowMapper());
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<BiologicalDataItem> loadFilesByNameCaseInsensitive(final String name) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(BiologicalDataItemParameters.NAME.name(), name.toLowerCase());
        return getNamedParameterJdbcTemplate().query(loadBiologicalDataItemsByNameCaseInsensitiveQuery,
                params, getRowMapper());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<BiologicalDataItem> loadFilesByNamesStrict(final List<String> names) {
        if (CollectionUtils.isEmpty(names)) {
            return Collections.emptyList();
        }

        List<String> quotedNames = names.stream().map(n -> "'" + n + "'").collect(Collectors.toList());
        String query = DaoHelper.getQueryFilledWithIdArray(loadBiologicalDataItemsByNamesStrictQuery, quotedNames);

        return getJdbcTemplate().query(query, getRowMapper());
    }

    /**
     * Finds files with names matching a specified file name, performs substring, case insensitive search
     * @param name search query
     * @return {@code List} of files with a matching name
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<BiologicalDataItem> loadFilesByName(final String name) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(BiologicalDataItemParameters.NAME.name(), "%" + name.toLowerCase() + "%");
        return getNamedParameterJdbcTemplate().query(loadBiologicalDataItemsByNameQuery,
                params, getRowMapper());
    }


    public enum BiologicalDataItemParameters {
        BIO_DATA_ITEM_ID,
        NAME,
        TYPE,
        PATH,
        FORMAT,
        CREATED_DATE,
        BUCKET_ID,
        PRETTY_NAME,
        OWNER,

        VCF_ID,
        VCF_REFERENCE_GENOME_ID,
        VCF_COMPRESSED,

        GENE_ITEM_ID,
        GENE_REFERENCE_GENOME_ID,
        GENE_COMPRESSED,
        GENE_EXTERNAL_DB_TYPE_ID,
        GENE_EXTERNAL_DB_ID,
        GENE_EXTERNAL_DB_ORGANISM,

        REFERENCE_GENOME_ID,
        REFERENCE_GENOME_SIZE,
        REFERENCE_GENE_ITEM_ID,
        REFERENCE_GENE_ITEM_NAME,
        REFERENCE_GENE_BIO_DATA_ITEM_ID,
        REFERENCE_GENE_TYPE,
        REFERENCE_GENE_PATH,
        REFERENCE_GENE_FORMAT,
        REFERENCE_GENE_CREATED_DATE,
        REFERENCE_GENE_REFERENCE_GENOME_ID,
        REFERENCE_GENE_COMPRESSED,

        BAM_ID,
        BAM_REFERENCE_GENOME_ID,

        BED_GRAPH_ID,
        BED_GRAPH_REFERENCE_GENOME_ID,

        BED_ID,
        BED_REFERENCE_GENOME_ID,
        BED_COMPRESSED,

        SEG_ID,
        SEG_REFERENCE_GENOME_ID,
        SEG_COMPRESSED,

        MAF_ID,
        MAF_REFERENCE_GENOME_ID,
        MAF_COMPRESSED,
        MAF_REAL_PATH,

        VG_ID,
        VG_REFERENCE_GENOME_ID,
        VG_REAL_PATH,

        INDEX_ID,
        INDEX_NAME,
        INDEX_TYPE,
        INDEX_PATH,
        INDEX_FORMAT,
        INDEX_BUCKET_ID,
        INDEX_CREATED_DATE;

        /**
         * Returns a universal row mapper for all BiologicalDataItem's ancestor entities
         * @return a universal row mapper
         */
        public static RowMapper<BiologicalDataItem> getRowMapper() {
            return (rs, rowNum) -> mapRow(rs, true);
        }

        public static RowMapper<BiologicalDataItem> getRowMapper(final boolean loadIndex) {
            return (rs, rowNum) -> mapRow(rs, loadIndex);
        }

        private static BiologicalDataItem mapRow(ResultSet rs, boolean loadIndex) throws SQLException {
            long longVal = rs.getLong(FORMAT.name());
            BiologicalDataItemFormat format = rs.wasNull() ? null : BiologicalDataItemFormat.getById(longVal);

            BiologicalDataItem index = null;
            if (loadIndex) {
                long indexId = rs.getLong(INDEX_ID.name());
                if (!rs.wasNull()) {
                    index = new BiologicalDataItem();
                    index.setId(indexId);
                    index.setName(rs.getString(INDEX_NAME.name()));
                    index.setType(BiologicalDataItemResourceType.getById(rs.getLong(INDEX_TYPE.name())));
                    index.setPath(rs.getString(INDEX_PATH.name()));
                    index.setFormat(BiologicalDataItemFormat.getById(rs.getLong(INDEX_FORMAT.name())));
                    index.setCreatedDate(new Date(rs.getTimestamp(INDEX_CREATED_DATE.name()).getTime()));
                }
            }

            BiologicalDataItem dataItem;
            if (format != null) {
                dataItem = mapSpecialFields(rs, format, index);
            } else {
                dataItem = mapBioDataItem(rs);
            }

            dataItem.setName(rs.getString(NAME.name()));

            longVal = rs.getLong(TYPE.name());
            dataItem.setType(rs.wasNull() ? null : BiologicalDataItemResourceType.getById(longVal));

            dataItem.setPath(rs.getString(PATH.name()));
            dataItem.setFormat(format);
            dataItem.setCreatedDate(new Date(rs.getTimestamp(CREATED_DATE.name()).getTime()));
            dataItem.setPrettyName(rs.getString(PRETTY_NAME.name()));
            dataItem.setOwner(rs.getString(OWNER.name()));

            return dataItem;
        }

        @NotNull
        private static BiologicalDataItem mapSpecialFields(ResultSet rs, BiologicalDataItemFormat format,
                                                           BiologicalDataItem index) throws SQLException {
            BiologicalDataItem dataItem;
            switch (format) {
                case REFERENCE:
                    dataItem = mapReference(rs);
                    break;
                case VCF:
                    dataItem = mapVcfFile(rs, index);
                    break;
                case GENE:
                    dataItem = mapGeneFile(rs, index);
                    break;
                case BAM:
                    dataItem = mapBamFile(rs, index);
                    break;
                case WIG:
                    dataItem = mapWigFile(rs, index);
                    break;
                case BED:
                    dataItem = mapBedFile(rs, index);
                    break;
                case SEG:
                    dataItem = mapSegFile(rs, index);
                    break;
                case MAF:
                    dataItem = mapMafFile(rs, index);
                    break;
                default:
                    dataItem = mapBioDataItem(rs);
            }
            return dataItem;
        }

        private static BiologicalDataItem mapBioDataItem(ResultSet rs) throws SQLException {
            BiologicalDataItem dataItem = new BiologicalDataItem();
            dataItem.setId(rs.getLong(BIO_DATA_ITEM_ID.name()));
            return dataItem;
        }

        @NotNull
        private static BiologicalDataItem mapMafFile(ResultSet rs, BiologicalDataItem index) throws SQLException {
            MafFile mafFile = new MafFile();
            mafFile.setId(rs.getLong(MAF_ID.name()));
            mafFile.setBioDataItemId(rs.getLong(BIO_DATA_ITEM_ID.name()));
            mafFile.setCompressed(rs.getBoolean(MAF_COMPRESSED.name()));
            mafFile.setReferenceId(rs.getLong(MAF_REFERENCE_GENOME_ID.name()));
            mafFile.setRealPath(rs.getString(MAF_REAL_PATH.name()));
            mafFile.setIndex(index);

            return mafFile;
        }

        @NotNull
        private static BiologicalDataItem mapSegFile(ResultSet rs, BiologicalDataItem index) throws SQLException {
            SegFile segFile = new SegFile();
            segFile.setId(rs.getLong(SEG_ID.name()));
            segFile.setBioDataItemId(rs.getLong(BIO_DATA_ITEM_ID.name()));
            segFile.setCompressed(rs.getBoolean(SEG_COMPRESSED.name()));
            segFile.setReferenceId(rs.getLong(SEG_REFERENCE_GENOME_ID.name()));
            segFile.setIndex(index);

            return segFile;
        }

        @NotNull
        private static BiologicalDataItem mapBedFile(ResultSet rs, BiologicalDataItem index) throws SQLException {
            BedFile bedFile = new BedFile();
            bedFile.setId(rs.getLong(BED_ID.name()));
            bedFile.setBioDataItemId(rs.getLong(BIO_DATA_ITEM_ID.name()));
            bedFile.setCompressed(rs.getBoolean(BED_COMPRESSED.name()));
            bedFile.setReferenceId(rs.getLong(BED_REFERENCE_GENOME_ID.name()));
            bedFile.setIndex(index);

            return bedFile;
        }

        @NotNull
        private static BiologicalDataItem mapWigFile(ResultSet rs, BiologicalDataItem index) throws SQLException {
            WigFile wigFile = new WigFile();
            wigFile.setId(rs.getLong(BED_GRAPH_ID.name()));
            wigFile.setBioDataItemId(rs.getLong(BIO_DATA_ITEM_ID.name()));
            wigFile.setReferenceId(rs.getLong(BED_GRAPH_REFERENCE_GENOME_ID.name()));
            wigFile.setIndex(index);
            return wigFile;
        }

        @NotNull
        private static BiologicalDataItem mapBamFile(ResultSet rs, BiologicalDataItem index) throws SQLException {
            BamFile bamFile = new BamFile();
            bamFile.setId(rs.getLong(BAM_ID.name()));
            bamFile.setBioDataItemId(rs.getLong(BIO_DATA_ITEM_ID.name()));
            bamFile.setReferenceId(rs.getLong(BAM_REFERENCE_GENOME_ID.name()));
            bamFile.setIndex(index);

            return bamFile;
        }

        @NotNull
        private static BiologicalDataItem mapGeneFile(ResultSet rs, BiologicalDataItem index) throws SQLException {
            GeneFile geneFile = new GeneFile();
            geneFile.setId(rs.getLong(GENE_ITEM_ID.name()));
            geneFile.setBioDataItemId(rs.getLong(BIO_DATA_ITEM_ID.name()));
            geneFile.setCompressed(rs.getBoolean(GENE_COMPRESSED.name()));
            geneFile.setReferenceId(rs.getLong(GENE_REFERENCE_GENOME_ID.name()));

            long longVal = rs.getLong(GENE_EXTERNAL_DB_TYPE_ID.name());
            if (!rs.wasNull()) {
                geneFile.setExternalDBType(ExternalDBType.getById(longVal));
            }

            longVal = rs.getLong(GENE_EXTERNAL_DB_ID.name());
            if (!rs.wasNull()) {
                geneFile.setExternalDB(ExternalDB.getById(longVal));
            }

            geneFile.setExternalDBOrganism(rs.getString(GENE_EXTERNAL_DB_ORGANISM.name()));
            geneFile.setIndex(index);

            return geneFile;
        }

        @NotNull
        private static BiologicalDataItem mapVcfFile(ResultSet rs, BiologicalDataItem index) throws SQLException {
            VcfFile vcfFile = new VcfFile();
            vcfFile.setId(rs.getLong(VCF_ID.name()));
            vcfFile.setBioDataItemId(rs.getLong(BIO_DATA_ITEM_ID.name()));
            vcfFile.setCompressed(rs.getBoolean(VCF_COMPRESSED.name()));
            vcfFile.setReferenceId(rs.getLong(VCF_REFERENCE_GENOME_ID.name()));
            vcfFile.setIndex(index);

            return vcfFile;
        }

        @NotNull
        private static BiologicalDataItem mapReference(ResultSet rs) throws SQLException {
            Reference reference = new Reference();
            reference.setBioDataItemId(rs.getLong(BIO_DATA_ITEM_ID.name()));
            reference.setSize(rs.getLong(REFERENCE_GENOME_SIZE.name()));
            reference.setId(rs.getLong(REFERENCE_GENOME_ID.name()));

            long longVal = rs.getLong(REFERENCE_GENE_ITEM_ID.name());
            if (!rs.wasNull()) {
                GeneFile geneFile = new GeneFile();
                geneFile.setId(longVal);
                geneFile.setName(rs.getString(REFERENCE_GENE_ITEM_NAME.name()));
                geneFile.setBioDataItemId(rs.getLong(REFERENCE_GENE_BIO_DATA_ITEM_ID.name()));

                longVal = rs.getLong(REFERENCE_GENE_TYPE.name());
                geneFile.setType(rs.wasNull() ? null : BiologicalDataItemResourceType.getById(longVal));

                geneFile.setPath(rs.getString(REFERENCE_GENE_PATH.name()));

                longVal = rs.getLong(REFERENCE_GENE_FORMAT.name());
                geneFile.setFormat(rs.wasNull() ? null : BiologicalDataItemFormat.getById(longVal));

                geneFile.setCreatedDate(new Date(rs.getTimestamp(REFERENCE_GENE_CREATED_DATE.name()).getTime()));
                geneFile.setReferenceId(rs.getLong(REFERENCE_GENE_REFERENCE_GENOME_ID.name()));
                geneFile.setCompressed(rs.getBoolean(REFERENCE_GENE_COMPRESSED.name()));
                reference.setGeneFile(geneFile);
            }

            return reference;
        }

    }
    /**
     * Enum, containing common FeatureFile's ancestor's fields
     */
    public enum FeatureFileParameters {
        REFERENCE_GENOME_ID,
        INDEX_ID,
        COMPRESSED,
        EXTERNAL_DB_TYPE_ID,
        EXTERNAL_DB_ID,
        EXTERNAL_DB_ORGANISM;

        /**
         * Creates a MapSqlParameterSource with common FeatureFile's ancestor's fields for future use in DB queries
         * @param idFieldName a name of ID fields of FeatureFile's ancestor's
         * @param featureFile a FeatureFile's ancestor's entity, which fields to add to parameters
         * @return a MapSqlParameterSource with common FeatureFile's ancestor's fields
         */
        public static MapSqlParameterSource getLinkedTableParameters(String idFieldName, FeatureFile featureFile) {
            MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(idFieldName, featureFile.getId());
            params.addValue(BiologicalDataItemParameters.BIO_DATA_ITEM_ID.name(), featureFile.getBioDataItemId());
            params.addValue(REFERENCE_GENOME_ID.name(), featureFile.getReferenceId());
            params.addValue(INDEX_ID.name(), featureFile.getIndex() != null ? featureFile.getIndex().getId() : null);
            params.addValue(COMPRESSED.name(), featureFile.getCompressed());

            if (featureFile instanceof GeneFile) {
                GeneFile geneFile = (GeneFile) featureFile;
                params.addValue(EXTERNAL_DB_TYPE_ID.name(), geneFile.getExternalDBType() != null ?
                        geneFile.getExternalDBType().getId() : null);
                params.addValue(EXTERNAL_DB_ID.name(), geneFile.getExternalDB() != null ?
                        geneFile.getExternalDB().getId() : null);
                params.addValue(EXTERNAL_DB_ORGANISM.name(), geneFile.getExternalDBOrganism());
            }

            return params;
        }

    }
    @Required
    public void setInsertBiologicalDataItemQuery(String insertBiologicalDataItemQuery) {
        this.insertBiologicalDataItemQuery = insertBiologicalDataItemQuery;
    }

    @Required
    public void setLoadBiologicalDataItemsByIdsQuery(String loadBiologicalDataItemsByIdsQuery) {
        this.loadBiologicalDataItemsByIdsQuery = loadBiologicalDataItemsByIdsQuery;
    }

    @Required
    public void setBiologicalDataItemSequenceName(String biologicalDataItemSequenceName) {
        this.biologicalDataItemSequenceName = biologicalDataItemSequenceName;
    }

    @Required
    public void setDeleteBiologicalDataItemQuery(String deleteBiologicalDataItemQuery) {
        this.deleteBiologicalDataItemQuery = deleteBiologicalDataItemQuery;
    }

    @Required
    public void setLoadBiologicalDataItemsByNameStrictQuery(
            String loadBiologicalDataItemsByNameStrictQuery) {
        this.loadBiologicalDataItemsByNameStrictQuery = loadBiologicalDataItemsByNameStrictQuery;
    }

    @Required
    public void setLoadBiologicalDataItemsByNameQuery(String loadBiologicalDataItemsByNameQuery) {
        this.loadBiologicalDataItemsByNameQuery = loadBiologicalDataItemsByNameQuery;
    }

    @Required
    public void setLoadBiologicalDataItemsByNamesStrictQuery(String loadBiologicalDataItemsByNamesStrictQuery) {
        this.loadBiologicalDataItemsByNamesStrictQuery = loadBiologicalDataItemsByNamesStrictQuery;
    }

    @Required
    public void setLoadBiologicalDataItemsByNameCaseInsensitiveQuery(
            String loadBiologicalDataItemsByNameCaseInsensitiveQuery) {
        this.loadBiologicalDataItemsByNameCaseInsensitiveQuery = loadBiologicalDataItemsByNameCaseInsensitiveQuery;
    }

    @Required
    public void setUpdateOwnerQuery(String updateOwnerQuery) {
        this.updateOwnerQuery = updateOwnerQuery;
    }
}
