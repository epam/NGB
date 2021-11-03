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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private static final String LOAD_LINEAGE_TREE_EDGES_BY_ID_QUERY = "with recursive sub_tree(\n" +
            "                        lineage_tree_edge_id,\n" +
            "                        lineage_tree_id,\n" +
            "                        node_from,\n" +
            "                        node_to,\n" +
            "                        attributes,\n" +
            "                        type_of_interaction,\n" +
            "                        level\n" +
            "                ) as (\n" +
            "                    SELECT\n" +
            "                        lineage_tree_edge_id,\n" +
            "                        lineage_tree_id,\n" +
            "                        node_from,\n" +
            "                        node_to,\n" +
            "                        attributes,\n" +
            "                        type_of_interaction,\n" +
            "                        1\n" +
            "                    FROM catgenome.lineage_tree_edge where node_from = %s\n" +
            "                    union all\n" +
            "                    SELECT\n" +
            "                        tree.lineage_tree_edge_id,\n" +
            "                        tree.lineage_tree_id,\n" +
            "                        tree.node_from,\n" +
            "                        tree.node_to,\n" +
            "                        tree.attributes,\n" +
            "                        tree.type_of_interaction,\n" +
            "                        level + 1\n" +
            "                    FROM catgenome.lineage_tree_edge tree " +
            "                           join sub_tree st on tree.node_from = st.node_to\n" +
            "                    )\n" +
            "                    SELECT\n" +
            "                        lineage_tree_edge_id,\n" +
            "                        lineage_tree_id,\n" +
            "                        node_from,\n" +
            "                        node_to,\n" +
            "                        attributes,\n" +
            "                        type_of_interaction\n" +
            "                    FROM sub_tree\n";

    /**
     * Persists new LineageTreeEdges records.
     * @param edges {@code List<LineageTreeEdge>} LineageTreeEdges to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<LineageTreeEdge> save(final List<LineageTreeEdge> edges, final Map<String, LineageTreeNode> nodes) {
        if (!CollectionUtils.isEmpty(edges)) {
            List<Long> newIds = daoHelper.createIds(lineageTreeEdgeSequenceName, edges.size());
            List<MapSqlParameterSource> params = new ArrayList<>(edges.size());
            for (int i = 0; i < edges.size(); i++) {
                edges.get(i).setLineageTreeEdgeId(newIds.get(i));
            }
            setNodeIds(edges, nodes);
            for (LineageTreeEdge edge : edges) {
                MapSqlParameterSource param = LineageTreeEdgeParameters.getParameters(edge);
                params.add(param);
            }
            getNamedParameterJdbcTemplate().batchUpdate(insertLineageTreeEdgeQuery,
                    params.toArray(new MapSqlParameterSource[edges.size()]));
        }
        return edges;
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
        return getJdbcTemplate().query(String.format(LOAD_LINEAGE_TREE_EDGES_BY_ID_QUERY, lineageTreeNodeId),
                LineageTreeEdgeParameters.getRowMapper());
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
            params.addValue(ATTRIBUTES.name(), lineageTreeEdge.getAttributes());
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
                    .attributes(rs.getString(ATTRIBUTES.name()))
                    .typeOfInteraction(rs.getString(TYPE_OF_INTERACTION.name()))
                    .build();
        }
    }
}
