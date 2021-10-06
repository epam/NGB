/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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
package com.epam.catgenome.dao.heatmap;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.heatmap.Heatmap;
import com.epam.catgenome.entity.heatmap.HeatmapAnnotationType;
import com.epam.catgenome.entity.heatmap.HeatmapDataType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String heatmapSequenceName;
    private String heatmapValueSequenceName;
    private String insertHeatmapQuery;
    private String deleteHeatmapQuery;
    private String loadHeatmapQuery;
    private String loadHeatmapsQuery;
    private String updateHeatmapContentQuery;
    private String loadHeatmapContentQuery;
    private String updateCellAnnotationQuery;
    private String updateLabelAnnotationQuery;
    private String loadCellAnnotationQuery;
    private String loadLabelAnnotationQuery;
    private String updateHeatmapRowTreeQuery;
    private String loadHeatmapRowTreeQuery;
    private String updateHeatmapColumnTreeQuery;
    private String loadHeatmapColumnTreeQuery;

    /**
     * Persists new Heatmap record.
     * @param heatmap {@code Heatmap} a Heatmap to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Heatmap saveHeatmap(final Heatmap heatmap,
                               final byte[] content,
                               final byte[] cellAnnotation,
                               final byte[] rowTree,
                               final byte[] columnTree) {
        heatmap.setHeatmapId(daoHelper.createId(heatmapSequenceName));
        final MapSqlParameterSource params = HeatmapParameters.getParameters(heatmap);
        params.addValue(HeatmapParameters.CONTENT.name(), content);
        params.addValue(HeatmapParameters.CELL_ANNOTATION.name(), cellAnnotation);
        params.addValue(HeatmapParameters.ROW_TREE.name(), rowTree);
        params.addValue(HeatmapParameters.COLUMN_TREE.name(), columnTree);
        getNamedParameterJdbcTemplate().update(insertHeatmapQuery, params);
        return heatmap;
    }

    /**
     * Deletes Heatmap from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteHeatmap(final Long heatmapId) {
        getJdbcTemplate().update(deleteHeatmapQuery, heatmapId);
    }

    /**
     * Loads {@code Heatmap} from a database by id.
     * @param heatmapId {@code long} query parameters
     * @return a {@code Heatmap} from the database
     */
    public Heatmap loadHeatmap(final long heatmapId) {
        List<Heatmap> heatmaps = getJdbcTemplate().query(loadHeatmapQuery, HeatmapParameters.getRowMapper(), heatmapId);
        return heatmaps.isEmpty() ? null : heatmaps.get(0);
    }

    /**
     * Loads {@code Heatmaps} from a database.
     * @return a {@code List<Heatmap>} from the database
     */
    public List<Heatmap> loadHeatmaps() {
        return getJdbcTemplate().query(loadHeatmapsQuery, HeatmapParameters.getRowMapper());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateHeatmapContent(final Long heatmapId, final byte[] content) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(HeatmapParameters.HEATMAP_ID.name(), heatmapId);
        params.addValue(HeatmapParameters.CONTENT.name(), content);

        getNamedParameterJdbcTemplate().update(updateHeatmapContentQuery, params);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateCellAnnotation(final Long heatmapId, final byte[] annotation, final String path) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(HeatmapParameters.HEATMAP_ID.name(), heatmapId);
        params.addValue(HeatmapParameters.CELL_ANNOTATION.name(), annotation);
        params.addValue(HeatmapParameters.CELL_ANNOTATION_PATH.name(), path);

        getNamedParameterJdbcTemplate().update(updateCellAnnotationQuery, params);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateLabelAnnotation(final Heatmap heatmap, final String path) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(HeatmapParameters.HEATMAP_ID.name(), heatmap.getHeatmapId());
        params.addValue(HeatmapParameters.COLUMN_LABELS.name(),
                HeatmapParameters.listToData(heatmap.getColumnLabels()));
        params.addValue(HeatmapParameters.ROW_LABELS.name(), HeatmapParameters.listToData(heatmap.getRowLabels()));
        params.addValue(HeatmapParameters.LABEL_ANNOTATION_PATH.name(), path);

        getNamedParameterJdbcTemplate().update(updateLabelAnnotationQuery, params);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateHeatmapRowTree(final Long heatmapId, final byte[] tree, final String path) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(HeatmapParameters.HEATMAP_ID.name(), heatmapId);
        params.addValue(HeatmapParameters.ROW_TREE.name(), tree);
        params.addValue(HeatmapParameters.ROW_TREE_PATH.name(), path);

        getNamedParameterJdbcTemplate().update(updateHeatmapRowTreeQuery, params);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateHeatmapColumnTree(final Long heatmapId, final byte[] tree, final String path) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(HeatmapParameters.HEATMAP_ID.name(), heatmapId);
        params.addValue(HeatmapParameters.COLUMN_TREE.name(), tree);
        params.addValue(HeatmapParameters.COLUMN_TREE_PATH.name(), path);

        getNamedParameterJdbcTemplate().update(updateHeatmapColumnTreeQuery, params);
    }

    public InputStream loadHeatmapContent(final Long heatmapId) {
        final LobHandler lobHandler = new DefaultLobHandler();
        final List<InputStream> result = getJdbcTemplate().query(loadHeatmapContentQuery, (rs, rowNum) ->
                lobHandler.getBlobAsBinaryStream(rs, HeatmapParameters.CONTENT.name()), heatmapId);
        return result.isEmpty() ? null : result.get(0);
    }

    public InputStream loadCellAnnotation(final Long heatmapId) {
        final LobHandler lobHandler = new DefaultLobHandler();
        final List<InputStream> result = getJdbcTemplate().query(loadCellAnnotationQuery, (rs, rowNum) ->
                lobHandler.getBlobAsBinaryStream(rs, HeatmapParameters.CELL_ANNOTATION.name()), heatmapId);
        return result.isEmpty() ? null : result.get(0);
    }

    public InputStream loadHeatmapRowTree(final Long heatmapId) {
        final LobHandler lobHandler = new DefaultLobHandler();
        final List<InputStream> result = getJdbcTemplate().query(loadHeatmapRowTreeQuery, (rs, rowNum) ->
                lobHandler.getBlobAsBinaryStream(rs, HeatmapParameters.ROW_TREE.name()), heatmapId);
        return result.isEmpty() ? null : result.get(0);
    }

    public InputStream loadHeatmapColumnTree(final Long heatmapId) {
        final LobHandler lobHandler = new DefaultLobHandler();
        final List<InputStream> result = getJdbcTemplate().query(loadHeatmapColumnTreeQuery, (rs, rowNum) ->
                lobHandler.getBlobAsBinaryStream(rs, HeatmapParameters.COLUMN_TREE.name()), heatmapId);
        return result.isEmpty() ? null : result.get(0);
    }

    enum HeatmapParameters {
        BIO_DATA_ITEM_ID,
        NAME,
        TYPE,
        PATH,
        SOURCE,
        FORMAT,
        CREATED_DATE,
        BUCKET_ID,
        PRETTY_NAME,
        OWNER,

        HEATMAP_ID,
        ROW_TREE_PATH,
        COLUMN_TREE_PATH,
        CELL_ANNOTATION_PATH,
        LABEL_ANNOTATION_PATH,
        MAX_CELL_VALUE,
        MIN_CELL_VALUE,
        COLUMN_LABELS,
        ROW_LABELS,
        CELL_VALUE_TYPE,
        CELL_VALUES,
        CONTENT,
        CELL_ANNOTATION,
        CELL_ANNOTATION_TYPE,
        ROW_ANNOTATION_TYPE,
        COLUMN_ANNOTATION_TYPE,
        ROW_TREE,
        COLUMN_TREE;

        static MapSqlParameterSource getParameters(final Heatmap heatmap) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(HEATMAP_ID.name(), heatmap.getHeatmapId());
            params.addValue(BIO_DATA_ITEM_ID.name(), heatmap.getBioDataItemId());
            params.addValue(ROW_TREE_PATH.name(), heatmap.getRowTreePath());
            params.addValue(COLUMN_TREE_PATH.name(), heatmap.getColumnTreePath());
            params.addValue(CELL_ANNOTATION_PATH.name(), heatmap.getCellAnnotationPath());
            params.addValue(LABEL_ANNOTATION_PATH.name(), heatmap.getLabelAnnotationPath());
            params.addValue(MAX_CELL_VALUE.name(), heatmap.getMaxCellValue());
            params.addValue(MIN_CELL_VALUE.name(), heatmap.getMinCellValue());
            params.addValue(CELL_VALUE_TYPE.name(), heatmap.getCellValueType().getId());
            params.addValue(COLUMN_LABELS.name(), listToData(heatmap.getColumnLabels()));
            params.addValue(ROW_LABELS.name(), listToData(heatmap.getRowLabels()));
            params.addValue(CELL_VALUES.name(), setToData(heatmap.getCellValues()));
            params.addValue(CELL_ANNOTATION_TYPE.name(), heatmap.getCellAnnotationType().name());
            params.addValue(ROW_ANNOTATION_TYPE.name(), heatmap.getRowAnnotationType().name());
            params.addValue(COLUMN_ANNOTATION_TYPE.name(), heatmap.getColumnAnnotationType().name());
            return params;
        }

        @SneakyThrows
        private static byte[] listToData(List<List<String>> data) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(data);
            oos.flush();
            return bos.toByteArray();
        }

        @SneakyThrows
        private static List<List<String>> dataToList(byte[] data) {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInput in = new ObjectInputStream(bis);
            return (List<List<String>>) in.readObject();
        }

        @SneakyThrows
        private static Set<?> dataToSet(byte[] data) {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInput in = new ObjectInputStream(bis);
            return (Set<?>) in.readObject();
        }

        @SneakyThrows
        private static byte[] setToData(Set<?> data) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(data);
            oos.flush();
            return bos.toByteArray();
        }

        static RowMapper<Heatmap> getRowMapper() {
            return (rs, rowNum) -> parseHeatmap(rs);
        }

        static Heatmap parseHeatmap(final ResultSet rs) throws SQLException {
            Heatmap heatmap = Heatmap.builder()
                    .heatmapId(rs.getLong(HEATMAP_ID.name()))
                    .cellValueType(rs.wasNull() ? null : HeatmapDataType.getById(rs.getInt(CELL_VALUE_TYPE.name())))
                    .rowTreePath(rs.getString(ROW_TREE_PATH.name()))
                    .columnTreePath(rs.getString(COLUMN_TREE_PATH.name()))
                    .cellAnnotationPath(rs.getString(CELL_ANNOTATION_PATH.name()))
                    .labelAnnotationPath(rs.getString(LABEL_ANNOTATION_PATH.name()))
                    .maxCellValue(rs.getDouble(MAX_CELL_VALUE.name()))
                    .minCellValue(rs.getDouble(MIN_CELL_VALUE.name()))
                    .columnLabels(dataToList(rs.getBytes(COLUMN_LABELS.name())))
                    .rowLabels(dataToList(rs.getBytes(ROW_LABELS.name())))
                    .cellValues(dataToSet(rs.getBytes(CELL_VALUES.name())))
                    .cellAnnotationType(HeatmapAnnotationType.valueOf(rs.getString(CELL_ANNOTATION_TYPE.name())))
                    .rowAnnotationType(HeatmapAnnotationType.valueOf(rs.getString(ROW_ANNOTATION_TYPE.name())))
                    .columnAnnotationType(HeatmapAnnotationType.valueOf(rs.getString(COLUMN_ANNOTATION_TYPE.name())))
                    .build();
            heatmap.setBioDataItemId(rs.getLong(BIO_DATA_ITEM_ID.name()));
            heatmap.setName(rs.getString(NAME.name()));
            heatmap.setType(BiologicalDataItemResourceType.getById(rs.getLong(TYPE.name())));
            heatmap.setPrettyName(rs.getString(PRETTY_NAME.name()));
            heatmap.setOwner(rs.getString(OWNER.name()));
            heatmap.setPath(rs.getString(PATH.name()));
            heatmap.setSource(rs.getString(SOURCE.name()));
            heatmap.setFormat(BiologicalDataItemFormat.getById(rs.getLong(FORMAT.name())));
            heatmap.setCreatedDate(rs.getDate(CREATED_DATE.name()));
            heatmap.setBucketId(rs.getLong(BUCKET_ID.name()));
            return heatmap;
        }
    }
}
