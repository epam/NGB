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

package com.epam.catgenome.dao.seg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.epam.catgenome.entity.seg.SegFile;
import com.epam.catgenome.entity.seg.SegSample;

/**
 * <p>
 * {@code SegFileDao} is a DAO component, that handles database interaction with SEG file metadata.
 * </p>
 */
public class SegFileDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;

    private String segFileSequenceName;
    private String segSampleSequenceName;

    private String createSegFileQuery;
    private String loadSegFileQuery;
    private String deleteSegFileQuery;

    private String createSamplesForFileQuery;
    private String loadSamplesForFileQuery;
    private String loadSamplesByFileIdsQuery;
    private String loadSamplesForFilesByReferenceIdQuery;
    private String deleteSamplesQuery;

    /**
     * Creates a new ID for a {@code SegFile} instance
     * @return {@code Long} new {@code SegFile} ID
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long createSegFileId() {
        return daoHelper.createId(segFileSequenceName);
    }

    /**
     * Persists {@code SegFile} record to the database
     * @param segFile a {@code SegFile} instance to be persisted
     * @param realId SegFile's real ID, generated with SegFileDao::createGeneFileId() method
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createSegFile(SegFile segFile, long realId) {
        segFile.setBioDataItemId(segFile.getId());
        segFile.setId(realId);

        getNamedParameterJdbcTemplate().update(createSegFileQuery, BiologicalDataItemDao.FeatureFileParameters
                .getLinkedTableParameters(SegParameters.SEG_ID.name(), segFile));
    }

    /**
     * Persists {@code SegFile} record to the database
     * @param segFile a {@code SegFile} instance to be persisted
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createSegFile(SegFile segFile) {
        getNamedParameterJdbcTemplate().update(createSegFileQuery, BiologicalDataItemDao.FeatureFileParameters
                .getLinkedTableParameters(SegParameters.SEG_ID.name(), segFile));
    }

    /**
     * Loads a persisted {@code SegFile} record by it's ID
     * @param id {@code long} a SegFile ID
     * @return {@code SegFile} instance
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public SegFile loadSegFile(long id) {
        List<BiologicalDataItem> files = getJdbcTemplate().query(loadSegFileQuery, BiologicalDataItemDao
                .BiologicalDataItemParameters.getRowMapper(), id);

        return !files.isEmpty() ? (SegFile) files.get(0) : null;
    }

    /**
     * Deletes {@code SegFile} metadata from the database along with the samples metadata
     * @param segFileId of a  {@code SegFile} to delete
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteSegFile(final long segFileId) {
        getJdbcTemplate().update(deleteSamplesQuery, segFileId);
        getJdbcTemplate().update(deleteSegFileQuery, segFileId);
    }

    /**
     * Saves sample metadata to the database.
     *
     * @param samples   {@code List&lt;Sample&gt;} samples to save
     * @param segFileId {@code long} file ID to save samples for
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createSamples(List<SegSample> samples, long segFileId) {
        List<Long> sampleIds = daoHelper.createIds(segSampleSequenceName, samples.size());
        for (int i = 0; i < samples.size(); i++) {
            samples.get(i).setId(sampleIds.get(i));
        }

        final List<MapSqlParameterSource> params = new ArrayList<>();
        for (SegSample sample : samples) {

            MapSqlParameterSource param = new MapSqlParameterSource();
            param.addValue(SegSampleParameters.SEG_SAMPLE_ID.name(), sample.getId());
            param.addValue(SegSampleParameters.SEG_ID.name(), segFileId);
            param.addValue(SegSampleParameters.SAMPLE_NAME.name(), sample.getName());
            params.add(param);
        }

        getNamedParameterJdbcTemplate().batchUpdate(createSamplesForFileQuery, params.toArray(new
                MapSqlParameterSource[params.size()]));
    }

    /**
     * Load sample metadata from the database for a given file ID.
     *
     * @param segFileId {@code long} file ID for which samples were saved.
     * @return {@code List&lt;Sample&gt;} of samples for given file ID.
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<SegSample> loadSamplesForFile(long segFileId) {
        return getJdbcTemplate().query(loadSamplesForFileQuery, SegSampleParameters.getSegSampleMapper(), segFileId);
    }

    /**
     * Load sample metadata for multiple file IDs
     *
     * @param fileIds {@code List&lt;Long&gt;} of file IDs to load samples for
     * @return a map of {@code SegFile} IDs to lists of their samples
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, List<SegSample>> loadSamplesByFileIds(Collection<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<SegSample>> map = new HashMap<>();

        RowMapper<SegSample> sampleMapper = SegSampleParameters.getSegSampleMapper();

        String query = DaoHelper.getQueryFilledWithIdArray(loadSamplesByFileIdsQuery, fileIds);
        getJdbcTemplate().query(query, rs -> {
            SegSample sample = sampleMapper.mapRow(rs, 0);
            long vcfId = rs.getLong(SegSampleParameters.SEG_ID.name());
            if (!map.containsKey(vcfId)) {
                map.put(vcfId, new ArrayList<>());
            }
            map.get(vcfId).add(sample);
        });

        return map;
    }

    /**
     * Load sample metadata from the database for all files, corresponding the given reference ID.
     *
     * @param referenceId {@code long} reference ID for which files samples were saved.
     * @return {@code Map&lt;Long, List&lt;Sample&gt;&gt;} with file IDs for giver reference ID as keys, and with
     * lists of samples, corresponding this file IDs as values.
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public Map<Long, List<SegSample>> loadSamplesForFilesByReference(long referenceId) {
        Map<Long, List<SegSample>> sampleFileMap = new HashMap<>();

        RowMapper<SegSample> sampleMapper = SegSampleParameters.getSegSampleMapper();

        getJdbcTemplate().query(loadSamplesForFilesByReferenceIdQuery, rs -> {
            Long fileId = rs.getLong(SegSampleParameters.SEG_ID.name());
            if (!sampleFileMap.containsKey(fileId)) {
                sampleFileMap.put(fileId, new ArrayList<>());
            }

            SegSample segSample = sampleMapper.mapRow(rs, 0);

            sampleFileMap.get(fileId).add(segSample);
        }, referenceId);

        return sampleFileMap;
    }

    private enum SegParameters {
        SEG_ID
    }

    private enum SegSampleParameters {
        SEG_SAMPLE_ID,
        SEG_ID,
        SAMPLE_NAME;

        static RowMapper<SegSample> getSegSampleMapper() {
            return (rs, i) -> {
                SegSample sample = new SegSample();

                sample.setId(rs.getLong(SEG_SAMPLE_ID.name()));
                sample.setName(rs.getString(SAMPLE_NAME.name()));

                return sample;
            };
        }
    }

    @Required
    public void setSegFileSequenceName(String segFileSequenceName) {
        this.segFileSequenceName = segFileSequenceName;
    }

    @Required
    public void setCreateSegFileQuery(String createSegFileQuery) {
        this.createSegFileQuery = createSegFileQuery;
    }

    @Required
    public void setLoadSegFileQuery(String loadSegFileQuery) {
        this.loadSegFileQuery = loadSegFileQuery;
    }

    @Required
    public void setDeleteSegFileQuery(String deleteSegFileQuery) {
        this.deleteSegFileQuery = deleteSegFileQuery;
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
    public void setLoadSamplesByFileIdsQuery(String loadSamplesByFileIdsQuery) {
        this.loadSamplesByFileIdsQuery = loadSamplesByFileIdsQuery;
    }

    @Required
    public void setLoadSamplesForFilesByReferenceIdQuery(String loadSamplesForFilesByReferenceIdQuery) {
        this.loadSamplesForFilesByReferenceIdQuery = loadSamplesForFilesByReferenceIdQuery;
    }

    @Required
    public void setSegSampleSequenceName(String segSampleSequenceName) {
        this.segSampleSequenceName = segSampleSequenceName;
    }

    @Required
    public void setDeleteSamplesQuery(String deleteSamplesQuery) {
        this.deleteSamplesQuery = deleteSamplesQuery;
    }
}
