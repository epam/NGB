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
import com.epam.catgenome.entity.lineage.LineageTreeEdge;
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
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.util.Utils.parseAttributes;
import static com.epam.catgenome.util.Utils.serializeAttributes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LineageTreeEdgeDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String lineageTreeEdgeSequenceName;
    private String insertLineageTreeEdgeQuery;
    private String deleteLineageTreeEdgesQuery;
    private String loadLineageTreeEdgesQuery;
    private String loadLineageTreeEdgesByIdQuery;

    /**
     * Persists new LineageTreeEdges records.
     * @param edges {@code List<LineageTreeEdge>} LineageTreeEdges to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(final List<LineageTreeEdge> edges, final Map<String, LineageTreeNode> nodes) {
        if (!CollectionUtils.isEmpty(edges)) {
            final List<Long> newIds = daoHelper.createIds(lineageTreeEdgeSequenceName, edges.size());
            final MapSqlParameterSource[] params = new MapSqlParameterSource[edges.size()];
            for (int i = 0; i < edges.size(); i++) {
                edges.get(i).setLineageTreeEdgeId(newIds.get(i));
            }
            setNodeIds(edges, nodes);
            for (int i = 0; i < edges.size(); i++) {
                params[i] = LineageTreeEdgeParameters.getParameters(edges.get(i));
                edges.get(i).setLineageTreeEdgeId(newIds.get(i));
            }
            getNamedParameterJdbcTemplate().batchUpdate(insertLineageTreeEdgeQuery, params);
        }
    }

    /**
     * Deletes LineageTreeEdges from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteLineageTreeEdges(final Long lineageTreeId) {
        getJdbcTemplate().update(deleteLineageTreeEdgesQuery, lineageTreeId);
    }

    /**
     * Loads {@code LineageTreeEdges} from a database.
     * @return a {@code List<LineageTreeEdge>} from the database
     */
    public List<LineageTreeEdge> loadLineageTreeEdges(final long lineageTreeId) {
        return getJdbcTemplate().query(loadLineageTreeEdgesQuery,
                LineageTreeEdgeParameters.getRowMapper(), lineageTreeId);
    }

    /**
     * Loads {@code LineageTreeEdges} from a database.
     * @return a {@code List<LineageTreeEdge>} from the database
     */
    public List<LineageTreeEdge> loadLineageTreeEdgesById(final long lineageTreeNodeId) {
        return getJdbcTemplate().query(String.format(loadLineageTreeEdgesByIdQuery, lineageTreeNodeId),
                LineageTreeEdgeParameters.getRowMapper());
    }

    private void setNodeIds(final List<LineageTreeEdge> edges, final Map<String, LineageTreeNode> nodes) {
        for (LineageTreeEdge edge: edges) {
            final Long fromId = nodes.containsKey(edge.getNodeFromName()) ?
                    nodes.get(edge.getNodeFromName()).getLineageTreeNodeId() : null;
            edge.setNodeFromId(fromId);
            final Long toId = nodes.containsKey(edge.getNodeToName()) ?
                    nodes.get(edge.getNodeToName()).getLineageTreeNodeId() : null;
            edge.setNodeToId(toId);
        }
    }

    enum LineageTreeEdgeParameters {
        LINEAGE_TREE_EDGE_ID,
        LINEAGE_TREE_ID,
        NODE_FROM,
        NODE_TO,
        ATTRIBUTES,
        TYPE_OF_INTERACTION;

        static MapSqlParameterSource getParameters(final LineageTreeEdge lineageTreeEdge) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(LINEAGE_TREE_EDGE_ID.name(), lineageTreeEdge.getLineageTreeEdgeId());
            params.addValue(LINEAGE_TREE_ID.name(), lineageTreeEdge.getLineageTreeId());
            params.addValue(NODE_FROM.name(), lineageTreeEdge.getNodeFromId());
            params.addValue(NODE_TO.name(), lineageTreeEdge.getNodeToId());
            params.addValue(ATTRIBUTES.name(), serializeAttributes(lineageTreeEdge.getAttributes()));
            params.addValue(TYPE_OF_INTERACTION.name(), lineageTreeEdge.getTypeOfInteraction());
            return params;
        }

        static RowMapper<LineageTreeEdge> getRowMapper() {
            return (rs, rowNum) -> parseLineageTreeEdge(rs);
        }

        static LineageTreeEdge parseLineageTreeEdge(final ResultSet rs) throws SQLException {
            return LineageTreeEdge.builder()
                    .lineageTreeEdgeId(rs.getLong(LINEAGE_TREE_EDGE_ID.name()))
                    .lineageTreeId(rs.getLong(LINEAGE_TREE_ID.name()))
                    .nodeFromId(rs.getLong(NODE_FROM.name()))
                    .nodeToId(rs.getLong(NODE_TO.name()))
                    .attributes(parseAttributes(rs.getString(ATTRIBUTES.name())))
                    .typeOfInteraction(rs.getString(TYPE_OF_INTERACTION.name()))
                    .build();
        }
    }
}
