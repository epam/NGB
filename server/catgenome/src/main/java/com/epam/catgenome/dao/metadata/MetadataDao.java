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

package com.epam.catgenome.dao.metadata;

import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.entity.metadata.EntityVO;
import com.epam.catgenome.entity.metadata.MetadataVO;
import com.epam.catgenome.entity.security.AclClass;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MetadataDao extends NamedParameterJdbcDaoSupport {

    private static final Pattern ENTITIES_PATTERN = Pattern.compile("@ENTITIES@");

    private String insertMetadataQuery;
    private String loadMetadataQuery;
    private String updateMetadataQuery;
    private String deleteMetadataQuery;
    private String loadMetadataItemsQuery;

    @Transactional(propagation = Propagation.MANDATORY)
    public void save(final MetadataVO metadataVO) {
        getNamedParameterJdbcTemplate().update(insertMetadataQuery, MetadataParameters.getParameters(metadataVO));
    }

    public MetadataVO get(final Long entityId, final String entityClass) {
        final List<MetadataVO> result = getJdbcTemplate().query(loadMetadataQuery, MetadataParameters.getRowMapper(),
                entityId, entityClass);
        return result.isEmpty() ? null : result.get(0);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void update(final MetadataVO metadataVO) {
        getNamedParameterJdbcTemplate().update(updateMetadataQuery, MetadataParameters.getParameters(metadataVO));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(final Long entityId, final String entityClass) {
        getJdbcTemplate().update(deleteMetadataQuery, entityId, entityClass);
    }

    public List<MetadataVO> getItems(final List<EntityVO> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        return getNamedParameterJdbcTemplate().query(
                convertEntitiesToString(loadMetadataItemsQuery, entities),
                MetadataParameters.getParametersWithArrays(entities),
                MetadataParameters.getRowMapper());
    }

    public enum MetadataParameters {
        ENTITY_ID,
        ENTITY_CLASS,
        DATA,
        IDS,
        CLASSES;

        static MapSqlParameterSource getParameters(final MetadataVO metadataVO) {
            final MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(ENTITY_ID.name(), metadataVO.getId());
            params.addValue(ENTITY_CLASS.name(), metadataVO.getAclClass().name());
            params.addValue(DATA.name(), JsonMapper.convertDataToJsonStringForQuery(metadataVO.getMetadata()));
            return params;
        }

        static RowMapper<MetadataVO> getRowMapper() {
            return (rs, rowNum) -> MetadataVO.builder()
                    .id(rs.getLong(ENTITY_ID.name()))
                    .aclClass(AclClass.valueOf(rs.getString(ENTITY_CLASS.name())))
                    .metadata(parseData(rs.getString(DATA.name())))
                    .build();
        }

        static MapSqlParameterSource getParametersWithArrays(final List<EntityVO> entities) {
            final MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(IDS.name(), entities.stream()
                    .map(EntityVO::getEntityId)
                    .collect(Collectors.toList()));
            params.addValue(CLASSES.name(), entities.stream()
                    .map(entity-> entity.getEntityClass().name())
                    .collect(Collectors.toList()));
            return params;
        }

        static Map<String, String> parseData(final String data) {
            return JsonMapper.parseData(data, new TypeReference<Map<String, String>>() {});
        }
    }

    @Required
    public void setInsertMetadataQuery(final String insertMetadataQuery) {
        this.insertMetadataQuery = insertMetadataQuery;
    }

    @Required
    public void setLoadMetadataQuery(final String loadMetadataQuery) {
        this.loadMetadataQuery = loadMetadataQuery;
    }

    @Required
    public void setUpdateMetadataQuery(final String updateMetadataQuery) {
        this.updateMetadataQuery = updateMetadataQuery;
    }

    @Required
    public void setDeleteMetadataQuery(final String deleteMetadataQuery) {
        this.deleteMetadataQuery = deleteMetadataQuery;
    }

    @Required
    public void setLoadMetadataItemsQuery(final String loadMetadataItemsQuery) {
        this.loadMetadataItemsQuery = loadMetadataItemsQuery;
    }

    private String convertEntitiesToString(final String query, final List<EntityVO> entities) {
        return ENTITIES_PATTERN.matcher(query)
                .replaceAll(entities.stream()
                        .map(entity -> String.format("(%d,'%s')", entity.getEntityId(), entity.getEntityClass().name()))
                        .collect(Collectors.joining(",")));
    }
}
