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
import com.epam.catgenome.entity.lineage.LineageTreeNode;
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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LineageTreeNodeDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String lineageTreeNodeSequenceName;
    private String insertLineageTreeNodeQuery;
    private String deleteLineageTreeNodesQuery;
    private String loadLineageTreeNodesQuery;
    private String loadLineageTreeNodesByIdQuery;
    private String loadLineageTreeNodesByReferenceIdQuery;

    /**
     * Persists new LineageTreeNode records.
     * @param nodes {@code List<LineageTreeNode>} LineageTreeNodes to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<LineageTreeNode> save(final List<LineageTreeNode> nodes) {
        if (!CollectionUtils.isEmpty(nodes)) {
            List<Long> newIds = daoHelper.createIds(lineageTreeNodeSequenceName, nodes.size());
            List<MapSqlParameterSource> params = new ArrayList<>(nodes.size());
            for (int i = 0; i < nodes.size(); i++) {
                nodes.get(i).setLineageTreeNodeId(newIds.get(i));
                MapSqlParameterSource param = LineageTreeNodeParameters.getParameters(nodes.get(i));
                params.add(param);
            }
            getNamedParameterJdbcTemplate().batchUpdate(insertLineageTreeNodeQuery,
                    params.toArray(new MapSqlParameterSource[nodes.size()]));
        }
        return nodes;
    }

    /**
     * Deletes LineageTreeNodes from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteLineageTreeNodes(final Long lineageTreeId) {
        getJdbcTemplate().update(deleteLineageTreeNodesQuery, lineageTreeId);
    }

    /**
     * Loads {@code LineageTreeNodes} from a database.
     * @return a {@code List<LineageTreeNode>} from the database
     */
    public List<LineageTreeNode> loadLineageTreeNodes(final long lineageTreeId) {
        return getJdbcTemplate().query(loadLineageTreeNodesQuery,
                LineageTreeNodeParameters.getRowMapper(), lineageTreeId);
    }

    /**
     * Loads {@code LineageTreeNodes} from a database.
     * @return a {@code List<LineageTreeNode>} from the database
     */
    public List<LineageTreeNode> loadLineageTreeNodesByReference(final long referenceId) {
        return getJdbcTemplate().query(loadLineageTreeNodesByReferenceIdQuery,
                LineageTreeNodeParameters.getRowMapper(), referenceId);
    }

    /**
     * Loads {@code LineageTreeNodes} from a database.
     * @return a {@code List<LineageTreeNode>} from the database
     */
    public List<LineageTreeNode> loadLineageTreeNodesById(final Set<Long> lineageTreeNodeIds) {
        String query = DaoHelper.replaceInClause(loadLineageTreeNodesByIdQuery, lineageTreeNodeIds.size());
        return getJdbcTemplate().query(query, LineageTreeNodeParameters.getRowMapper(), lineageTreeNodeIds.toArray());
    }

    enum LineageTreeNodeParameters {
        LINEAGE_TREE_NODE_ID,
        LINEAGE_TREE_ID,
        NAME,
        DESCRIPTION,
        REFERENCE_ID,
        CREATION_DATE,
        ATTRIBUTES;

        static MapSqlParameterSource getParameters(final LineageTreeNode lineageTreeNode) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(LINEAGE_TREE_NODE_ID.name(), lineageTreeNode.getLineageTreeNodeId());
            params.addValue(LINEAGE_TREE_ID.name(), lineageTreeNode.getLineageTreeId());
            params.addValue(NAME.name(), lineageTreeNode.getName());
            params.addValue(DESCRIPTION.name(), lineageTreeNode.getDescription());
            params.addValue(REFERENCE_ID.name(), lineageTreeNode.getReferenceId());
            params.addValue(CREATION_DATE.name(), Timestamp.valueOf(lineageTreeNode.getCreationDate().atStartOfDay()));
            params.addValue(ATTRIBUTES.name(), lineageTreeNode.getAttributes());
            return params;
        }

        static RowMapper<LineageTreeNode> getRowMapper() {
            return (rs, rowNum) -> parseLineageTreeNode(rs);
        }

        static LineageTreeNode parseLineageTreeNode(final ResultSet rs) throws SQLException {
            return LineageTreeNode.builder()
                    .lineageTreeNodeId(rs.getLong(LINEAGE_TREE_NODE_ID.name()))
                    .lineageTreeId(rs.getLong(LINEAGE_TREE_ID.name()))
                    .name(rs.getString(NAME.name()))
                    .description(rs.getString(DESCRIPTION.name()))
                    .referenceId(rs.getLong(REFERENCE_ID.name()))
                    .creationDate(rs.getTimestamp(CREATION_DATE.name()).toLocalDateTime().toLocalDate())
                    .attributes(rs.getString(ATTRIBUTES.name()))
                    .build();
        }
    }
}
