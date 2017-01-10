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

package com.epam.catgenome.dao.vcf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfSample;

/**
 * <p>
 * {@code VcfFileDao} is a DAO component, that handles database interaction with VCF file metadata.
 * </p>
 */
public class VcfFileDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;

    private String vcfFileSequenceName;
    private String vcfSampleSequenceName;

    private String createSamplesForFileQuery;
    private String loadSamplesForFileQuery;
    private String loadSamplesByFileIdsQuery;
    private String loadSamplesForFilesByReferenceIdQuery;

    private String createVcfFileQuery;
    private String loadVcfFileQuery;
    private String loadVcfFilesQuery;
    private String loadVcfFilesByReferenceIdQuery;

    private String deleteVcfFileQuery;
    private String deleteVcfSampleQuery;

    /**
     * Persists {@code VcfFile} record to the database
     *
     * @param vcfFile a {@code VcfFile} instance to be persisted
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createVcfFile(VcfFile vcfFile, final Long realId) {
        vcfFile.setBioDataItemId(vcfFile.getId());
        vcfFile.setId(realId);

        getNamedParameterJdbcTemplate().update(createVcfFileQuery, BiologicalDataItemDao.FeatureFileParameters
                .getLinkedTableParameters(VcfParameters.VCF_ID.name(), vcfFile));
    }

    /**
     * Deletes {@code VcfFile}  metadata from database and corresponding vcf samples.
     *
     * @param vcfFileId id of file to remove
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteVcfFile(final Long vcfFileId) {
        getJdbcTemplate().update(deleteVcfSampleQuery, vcfFileId);
        getJdbcTemplate().update(deleteVcfFileQuery, vcfFileId);
    }

    /**
     * Loads a persisted {@code VcfFile} record by it's ID
     *
     * @param id {@code long} a VcfFile ID
     * @return {@code VcfFile} instance
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public VcfFile loadVcfFile(long id) {
        List<BiologicalDataItem> files = getJdbcTemplate().query(loadVcfFileQuery, BiologicalDataItemDao
                .BiologicalDataItemParameters.getRowMapper(), id);

        return !files.isEmpty() ? (VcfFile) files.get(0) : null;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<VcfFile> loadVcfFiles(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }

        long listId = daoHelper.createTempLongList(ids);
        List<BiologicalDataItem> files = getJdbcTemplate().query(loadVcfFilesQuery, BiologicalDataItemDao
            .BiologicalDataItemParameters.getRowMapper(), listId);
        daoHelper.clearTempList(listId);

        return files.stream().map(i -> (VcfFile) i).collect(Collectors.toList());
    }

    /**
     * Loads {@code VcfFile} records, saved for a specific reference ID. <br>
     *
     * @param referenceId {@code long} a reference ID in the system
     * @return {@code List&lt;VcfFile&gt;} instance
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<VcfFile> loadVcfFilesByReferenceId(long referenceId) {
        return getJdbcTemplate().query(loadVcfFilesByReferenceIdQuery, BiologicalDataItemDao
                .BiologicalDataItemParameters.getRowMapper(), referenceId)
                .stream().map(f -> (VcfFile) f).collect(Collectors.toList());
    }

    /**
     * Creates a new ID for a {@code VcfFile} instance
     *
     * @return {@code Long} new {@code VcfFile} ID
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long createVcfFileId() {
        return daoHelper.createId(vcfFileSequenceName);
    }

    /**
     * Saves sample metadata to the database.
     *
     * @param samples   {@code List&lt;Sample&gt;} samples to save
     * @param vcfFileId {@code long} file ID to save samples for
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createSamples(List<VcfSample> samples, long vcfFileId) {
        List<Long> sampleIds = daoHelper.createIds(vcfSampleSequenceName, samples.size());
        for (int i = 0; i < samples.size(); i++) {
            samples.get(i).setId(sampleIds.get(i));
        }

        final List<MapSqlParameterSource> params = new ArrayList<>();
        for (VcfSample sample : samples) {

            MapSqlParameterSource param = new MapSqlParameterSource();
            param.addValue(SampleParameters.VCF_SAMPLE_ID.name(), sample.getId());
            param.addValue(SampleParameters.VCF_ID.name(), vcfFileId);
            param.addValue(SampleParameters.SAMPLE_NAME.name(), sample.getName());
            param.addValue(SampleParameters.ORDER_INDEX.name(), sample.getIndex());
            params.add(param);
        }

        getNamedParameterJdbcTemplate().batchUpdate(createSamplesForFileQuery, params.toArray(new
                MapSqlParameterSource[params.size()]));
    }

    /**
     * Load sample metadata from the database for a given file ID.
     *
     * @param vcfFileId {@code long} file ID for which samples were saved.
     * @return {@code List&lt;Sample&gt;} of samples for given file ID.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<VcfSample> loadSamplesForFile(long vcfFileId) {
        return getJdbcTemplate().query(loadSamplesForFileQuery, SampleParameters.getVcfSampleMapper(), vcfFileId);
    }

    /**
     * Load sample metadata for multiple file IDs
     *
     * @param fileIds {@code List&lt;Long&gt;} of file IDs to load samples for
     * @return a map of {@code VcfFile} IDs to lists of their samples
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, List<VcfSample>> loadSamplesByFileIds(Collection<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<VcfSample>> map = new HashMap<>();

        long listId = daoHelper.createTempLongList(fileIds);

        getJdbcTemplate().query(loadSamplesByFileIdsQuery, rs -> {
            VcfSample sample = SampleParameters.getVcfSampleMapper().mapRow(rs, 0);
            long vcfId = rs.getLong(VcfParameters.VCF_ID.name());
            if (!map.containsKey(vcfId)) {
                map.put(vcfId, new ArrayList<>());
            }
            map.get(vcfId).add(sample);
        }, listId);

        daoHelper.clearTempList(listId);
        return map;
    }

    /**
     * Load sample metadata from the database for all files, corresponding the given reference ID.
     *
     * @param vcfFileId {@code long} reference ID for which files samples were saved.
     * @return {@code Map&lt;Long, List&lt;Sample&gt;&gt;} with file IDs for giver reference ID as keys, and with
     * lists of samples, corresponding this file IDs as values.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, List<VcfSample>> loadSamplesForFilesByReference(long vcfFileId) {
        Map<Long, List<VcfSample>> sampleFileMap = new HashMap<>();

        getJdbcTemplate().query(loadSamplesForFilesByReferenceIdQuery, rs -> {
            Long fileId = rs.getLong(SampleParameters.VCF_ID.name());
            if (!sampleFileMap.containsKey(fileId)) {
                sampleFileMap.put(fileId, new ArrayList<>());
            }

            VcfSample sample = new VcfSample();

            sample.setId(rs.getLong(SampleParameters.VCF_SAMPLE_ID.name()));
            sample.setName(rs.getString(SampleParameters.SAMPLE_NAME.name()));
            sample.setIndex(rs.getInt(SampleParameters.ORDER_INDEX.name()));

            sampleFileMap.get(fileId).add(sample);
        }, vcfFileId);

        return sampleFileMap;
    }

    enum VcfParameters {
        VCF_ID,
        REFERENCE_GENOME_ID,
        ORIGINAL_NAME,
        CREATED_BY,
        CREATED_DATE,
        FILE_PATH,
        INDEX_PATH,
        BOUNDS_PATH,
        COMPRESSED
    }

    enum SampleParameters {
        VCF_SAMPLE_ID,
        VCF_ID,
        SAMPLE_NAME,
        ORDER_INDEX;

        static RowMapper<VcfSample> getVcfSampleMapper() {
            return (rs, i) -> {
                VcfSample sample = new VcfSample();

                sample.setId(rs.getLong(VCF_SAMPLE_ID.name()));
                sample.setName(rs.getString(SAMPLE_NAME.name()));
                sample.setIndex(rs.getInt(ORDER_INDEX.name()));

                return sample;
            };
        }
    }

    @Required
    public void setVcfFileSequenceName(String vcfFileSequenceName) {
        this.vcfFileSequenceName = vcfFileSequenceName;
    }

    @Required
    public void setVcfSampleSequenceName(String vcfSampleSequenceName) {
        this.vcfSampleSequenceName = vcfSampleSequenceName;
    }

    @Required
    public void setCreateSamplesForFileQuery(String createSamplesForFileQuery) {
        this.createSamplesForFileQuery = createSamplesForFileQuery;
    }

    @Required
    public void setLoadSamplesForFileQuery(String loadSamplesForFileQuery) {
        this.loadSamplesForFileQuery = loadSamplesForFileQuery;
    }

    @Required
    public void setLoadSamplesForFilesByReferenceIdQuery(String loadSamplesForFilesByReferenceIdQuery) {
        this.loadSamplesForFilesByReferenceIdQuery = loadSamplesForFilesByReferenceIdQuery;
    }

    @Required
    public void setCreateVcfFileQuery(String createVcfFileQuery) {
        this.createVcfFileQuery = createVcfFileQuery;
    }

    @Required
    public void setLoadVcfFileQuery(String loadVcfFileQuery) {
        this.loadVcfFileQuery = loadVcfFileQuery;
    }

    @Required
    public void setLoadVcfFilesByReferenceIdQuery(String loadVcfFilesByReferenceIdQuery) {
        this.loadVcfFilesByReferenceIdQuery = loadVcfFilesByReferenceIdQuery;
    }

    @Required
    public void setLoadSamplesByFileIdsQuery(String loadSamplesByFileIdsQuery) {
        this.loadSamplesByFileIdsQuery = loadSamplesByFileIdsQuery;
    }

    @Required
    public void setDeleteVcfFileQuery(final String deleteVcfFileQuery) {
        this.deleteVcfFileQuery = deleteVcfFileQuery;
    }

    @Required
    public void setDeleteVcfSampleQuery(final String deleteVcfSampleQuery) {
        this.deleteVcfSampleQuery = deleteVcfSampleQuery;
    }

    @Required
    public void setLoadVcfFilesQuery(String loadVcfFilesQuery) {
        this.loadVcfFilesQuery = loadVcfFilesQuery;
    }
}

