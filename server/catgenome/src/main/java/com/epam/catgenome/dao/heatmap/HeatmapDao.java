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
import com.epam.catgenome.dao.project.ProjectDao;
import com.epam.catgenome.entity.heatmap.Heatmap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
    private String updateHeatmapContentQuery;
    private String loadHeatmapContentQuery;
    private String updateHeatmapAnnotationQuery;
    private String loadHeatmapAnnotationQuery;
    private String saveHeatmapTreeQuery;
    private String loadHeatmapTreeQuery;
    private String insertHeatmapValueQuery;
    private String loadHeatmapValuesQuery;
    private String deleteHeatmapValuesQuery;

    /**
     * Persists new Heatmap record.
     * @param heatmap {@code Heatmap} a Heatmap to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Heatmap save(final Heatmap heatmap) {
        heatmap.setHeatmapId(daoHelper.createId(heatmapSequenceName));
        getNamedParameterJdbcTemplate().update(insertHeatmapQuery, HeatmapParameters.getParameters(heatmap));
        return heatmap;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveHeatmapValues(final Set<String> values, final long heatmapId) {
        if (!CollectionUtils.isEmpty(values)) {
            List<Long> newIds = daoHelper.createIds(heatmapValueSequenceName, values.size());
            int i = 0;
            List<MapSqlParameterSource> params = new ArrayList<>(values.size());
            for (String value : values) {
                MapSqlParameterSource param = HeatmapValueParameters.getParameters(value, newIds.get(i), heatmapId);
                params.add(param);
                i++;
            }
            getNamedParameterJdbcTemplate().batchUpdate(insertHeatmapValueQuery,
                    params.toArray(new MapSqlParameterSource[values.size()]));
        }
    }

    /**
     * Deletes Heatmap from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteHeatmap(final Long heatmapId) {
        getJdbcTemplate().update(deleteHeatmapQuery, heatmapId);
    }

    /**
     * Deletes Heatmap values from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteHeatmapValues(final Long heatmapId) {
        getJdbcTemplate().update(deleteHeatmapValuesQuery, heatmapId);
    }

    /**
     * Loads {@code Heatmap} from a database by id.
     * @param heatmapId {@code long} query parameters
     * @return a {@code List<Heatmap>} from the database
     */
    public Heatmap loadHeatmap(final long heatmapId) {
        List<Heatmap> heatmaps = getJdbcTemplate().query(loadHeatmapQuery, HeatmapParameters.getRowMapper(), heatmapId);
        return heatmaps.isEmpty() ? null : heatmaps.get(0);
    }

    /**
     * Loads {@code Heatmap values} from a database by id.
     * @param heatmapId {@code long} query parameters
     * @return a {@code List<String>} from the database
     */
    public List<String> loadHeatmapValues(final long heatmapId) {
        return getJdbcTemplate().queryForList(loadHeatmapValuesQuery, String.class, heatmapId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateHeatmapAnnotation(final Long heatmapId, final byte[] annotation) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(HeatmapParameters.HEATMAP_ID.name(), heatmapId);
        params.addValue(HeatmapParameters.ANNOTATION.name(), annotation);

        getNamedParameterJdbcTemplate().update(updateHeatmapAnnotationQuery, params);
    }

    public InputStream loadHeatmapAnnotation(final Long heatmapId) {
        final LobHandler lobHandler = new DefaultLobHandler();
        final List<InputStream> result = getJdbcTemplate().query(loadHeatmapAnnotationQuery, (rs, rowNum) ->
                lobHandler.getBlobAsBinaryStream(rs, HeatmapParameters.ANNOTATION.name()), heatmapId);
        return result.isEmpty() ? null : result.get(0);
    }

    enum HeatmapParameters {
        HEATMAP_ID,
        NAME,
        PRETTY_NAME,
        TYPE,
        PATH,
        ROW_TREE_PATH,
        COLUMN_TREE_PATH,
        CELL_ANNOTATION_PATH,
        LABEL_ANNOTATION_PATH,
        ROW_COUNT,
        COLUMN_COUNT,
        MAX_CELL_VALUE,
        MIN_CELL_VALUE,
        CONTENT,
        ANNOTATION,
        TREE;

        static MapSqlParameterSource getParameters(final Heatmap heatmap) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(HEATMAP_ID.name(), heatmap.getHeatmapId());
            params.addValue(NAME.name(), heatmap.getName());
            params.addValue(PRETTY_NAME.name(), heatmap.getPrettyName());
            params.addValue(TYPE.name(), heatmap.getType());
            params.addValue(PATH.name(), heatmap.getPath());
            params.addValue(ROW_TREE_PATH.name(), heatmap.getRowTreePath());
            params.addValue(COLUMN_TREE_PATH.name(), heatmap.getColumnTreePath());
            params.addValue(CELL_ANNOTATION_PATH.name(), heatmap.getCellAnnotationPath());
            params.addValue(LABEL_ANNOTATION_PATH.name(), heatmap.getLabelAnnotationPath());
            params.addValue(ROW_COUNT.name(), heatmap.getRowCount());
            params.addValue(COLUMN_COUNT.name(), heatmap.getColumnCount());
            params.addValue(MAX_CELL_VALUE.name(), heatmap.getMaxCellValue());
            params.addValue(MIN_CELL_VALUE.name(), heatmap.getMinCellValue());
            return params;
        }

        static RowMapper<Heatmap> getRowMapper() {
            return (rs, rowNum) -> parseHeatmap(rs);
        }

        static Heatmap parseHeatmap(final ResultSet rs) throws SQLException {
            return Heatmap.builder()
                    .heatmapId(rs.getLong(HEATMAP_ID.name()))
                    .name(rs.getString(NAME.name()))
                    .prettyName(rs.getString(PRETTY_NAME.name()))
                    .type(rs.getString(TYPE.name()))
                    .path(rs.getString(PATH.name()))
                    .rowTreePath(rs.getString(ROW_TREE_PATH.name()))
                    .columnTreePath(rs.getString(COLUMN_TREE_PATH.name()))
                    .cellAnnotationPath(rs.getString(CELL_ANNOTATION_PATH.name()))
                    .labelAnnotationPath(rs.getString(LABEL_ANNOTATION_PATH.name()))
                    .rowCount(rs.getLong(ROW_COUNT.name()))
                    .columnCount(rs.getLong(COLUMN_COUNT.name()))
                    .maxCellValue(rs.getDouble(MAX_CELL_VALUE.name()))
                    .minCellValue(rs.getDouble(MIN_CELL_VALUE.name()))
                    .build();
        }
    }

    enum HeatmapValueParameters {
        VALUE_ID,
        HEATMAP_ID,
        HEATMAP_VALUE;

        static MapSqlParameterSource getParameters(final String heatmapValue,
                                                   final long valueId,
                                                   final long heatmapId) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(VALUE_ID.name(), valueId);
            params.addValue(HEATMAP_ID.name(), heatmapId);
            params.addValue(HEATMAP_VALUE.name(), heatmapValue);
            return params;
        }
    }
}
