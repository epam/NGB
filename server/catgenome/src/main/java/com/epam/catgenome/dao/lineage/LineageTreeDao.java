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
package com.epam.catgenome.dao.lineage;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.lineage.LineageTree;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LineageTreeDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String lineageTreeSequenceName;
    private String insertLineageTreeQuery;
    private String deleteLineageTreeQuery;
    private String loadLineageTreeQuery;
    private String loadLineageTreesQuery;
    private String loadAllLineageTreesQuery;

    /**
     * Persists new LineageTree record.
     * @param lineageTree {@code LineageTree} a LineageTree to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveLineageTree(final LineageTree lineageTree) {
        lineageTree.setLineageTreeId(daoHelper.createId(lineageTreeSequenceName));
        final MapSqlParameterSource params = LineageTreeParameters.getParameters(lineageTree);
        getNamedParameterJdbcTemplate().update(insertLineageTreeQuery, params);
    }

    /**
     * Deletes LineageTree from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteLineageTree(final Long lineageTreeId) {
        getJdbcTemplate().update(deleteLineageTreeQuery, lineageTreeId);
    }

    /**
     * Loads {@code LineageTree} from a database by id.
     * @param lineageTreeId {@code long} query parameters
     * @return a {@code LineageTree} from the database
     */
    public LineageTree loadLineageTree(final long lineageTreeId) {
        List<LineageTree> trees = getJdbcTemplate().query(loadLineageTreeQuery,
                LineageTreeParameters.getRowMapper(), lineageTreeId);
        return CollectionUtils.isEmpty(trees) ? null : trees.get(0);
    }

    /**
     * Loads {@code LineageTrees} from a database.
     * @return a {@code List<LineageTree>} from the database
     */
    public List<LineageTree> loadLineageTrees(final Set<Long> lineageTreeIds) {
        final String query = DaoHelper.replaceInClause(loadLineageTreesQuery, lineageTreeIds.size());
        return getJdbcTemplate().query(query, LineageTreeParameters.getRowMapper(), lineageTreeIds.toArray());
    }

    /**
     * Loads {@code LineageTrees} from a database.
     * @return a {@code List<LineageTree>} from the database
     */
    public List<LineageTree> loadAllLineageTrees() {
        return getJdbcTemplate().query(loadAllLineageTreesQuery, LineageTreeParameters.getRowMapper());
    }

    enum LineageTreeParameters {
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

        LINEAGE_TREE_ID,
        DESCRIPTION,
        NODES_PATH,
        EDGES_PATH;

        static MapSqlParameterSource getParameters(final LineageTree lineageTree) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(LINEAGE_TREE_ID.name(), lineageTree.getLineageTreeId());
            params.addValue(BIO_DATA_ITEM_ID.name(), lineageTree.getBioDataItemId());
            params.addValue(DESCRIPTION.name(), lineageTree.getDescription());
            params.addValue(NODES_PATH.name(), lineageTree.getNodesPath());
            params.addValue(EDGES_PATH.name(), lineageTree.getEdgesPath());
            return params;
        }

        static RowMapper<LineageTree> getRowMapper() {
            return (rs, rowNum) -> parseTree(rs);
        }

        static LineageTree parseTree(final ResultSet rs) throws SQLException {
            LineageTree tree = LineageTree.builder()
                    .lineageTreeId(rs.getLong(LINEAGE_TREE_ID.name()))
                    .description(rs.getString(DESCRIPTION.name()))
                    .nodesPath(rs.getString(NODES_PATH.name()))
                    .edgesPath(rs.getString(EDGES_PATH.name()))
                    .build();
            tree.setBioDataItemId(rs.getLong(BIO_DATA_ITEM_ID.name()));
            tree.setName(rs.getString(NAME.name()));
            tree.setType(BiologicalDataItemResourceType.getById(rs.getLong(TYPE.name())));
            tree.setPrettyName(rs.getString(PRETTY_NAME.name()));
            tree.setOwner(rs.getString(OWNER.name()));
            tree.setPath(rs.getString(PATH.name()));
            tree.setSource(rs.getString(SOURCE.name()));
            tree.setFormat(BiologicalDataItemFormat.getById(rs.getLong(FORMAT.name())));
            tree.setCreatedDate(rs.getDate(CREATED_DATE.name()));
            tree.setBucketId(rs.getLong(BUCKET_ID.name()));
            return tree;
        }
    }
}
