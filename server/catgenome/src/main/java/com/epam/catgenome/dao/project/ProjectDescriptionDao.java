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

package com.epam.catgenome.dao.project;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.project.ProjectDescription;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProjectDescriptionDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;

    private String projectDescriptionSequenceName;
    private String saveProjectDescriptionQuery;
    private String updateProjectDescriptionQuery;
    private String deleteProjectDescriptionByProjectIdQuery;
    private String deleteProjectDescriptionByIdQuery;
    private String findProjectDescriptionContentByIdQuery;
    private String findProjectDescriptionsByProjectIdQuery;
    private String findProjectDescriptionByIdQuery;
    private String findProjectDescriptionsByProjectIdsQuery;
    private String findProjectDescriptionsQuery;

    @Transactional(propagation = Propagation.MANDATORY)
    public void save(final ProjectDescription projectDescription, final byte[] content) {
        projectDescription.setId(daoHelper.createId(projectDescriptionSequenceName));

        getNamedParameterJdbcTemplate().update(saveProjectDescriptionQuery,
                ProjectDescriptionParameters.getParameters(projectDescription, content));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void update(final ProjectDescription projectDescription, final byte[] content) {
        getNamedParameterJdbcTemplate().update(updateProjectDescriptionQuery,
                ProjectDescriptionParameters.getParameters(projectDescription, content));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteByProjectId(final Long projectId) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(ProjectDescriptionParameters.PROJECT_ID.name(), projectId);

        getNamedParameterJdbcTemplate().update(deleteProjectDescriptionByProjectIdQuery, params);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteById(final Long id) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(ProjectDescriptionParameters.ID.name(), id);

        getNamedParameterJdbcTemplate().update(deleteProjectDescriptionByIdQuery, params);
    }

    public InputStream findContentById(final Long id) {
        final List<InputStream> result = getJdbcTemplate().query(findProjectDescriptionContentByIdQuery,
                ProjectDescriptionParameters.getContentRowMapper(), id);
        return result.isEmpty() ? null : result.get(0);
    }

    public ProjectDescription findById(final Long id) {
        final List<ProjectDescription> result = getJdbcTemplate().query(findProjectDescriptionByIdQuery,
                ProjectDescriptionParameters.getRowMapper(), id);
        return result.isEmpty() ? null : result.get(0);
    }

    public List<ProjectDescription> findByProjectId(final Long projectId) {
        return getJdbcTemplate().query(findProjectDescriptionsByProjectIdQuery,
                ProjectDescriptionParameters.getRowMapper(), projectId);
    }

    public Map<Long, List<ProjectDescription>> findAll() {
        return findAllIn(findProjectDescriptionsQuery);
    }

    public Map<Long, List<ProjectDescription>> findByProjectIdIn(final List<Long> projectIds) {
        if (CollectionUtils.isEmpty(projectIds)) {
            return Collections.emptyMap();
        }
        final String query = DaoHelper.getQueryFilledWithIdArray(findProjectDescriptionsByProjectIdsQuery, projectIds);
        return findAllIn(query);
    }

    private Map<Long, List<ProjectDescription>> findAllIn(final String query) {
        final List<ProjectDescription> result = getJdbcTemplate().query(query,
                ProjectDescriptionParameters.getRowMapper());
        return ListUtils.emptyIfNull(result).stream()
                .collect(Collectors.groupingBy(ProjectDescription::getProjectId));
    }

    enum ProjectDescriptionParameters {
        ID,
        PROJECT_ID,
        NAME,
        CONTENT;

        static RowMapper<InputStream> getContentRowMapper() {
            final LobHandler lobHandler = new DefaultLobHandler();
            return (rs, rowNum) -> lobHandler.getBlobAsBinaryStream(rs, ProjectDescriptionParameters.CONTENT.name());
        }

        static RowMapper<ProjectDescription> getRowMapper() {
            return (rs, rowNum) -> ProjectDescription.builder()
                    .id(rs.getLong(ID.name()))
                    .projectId(rs.getLong(PROJECT_ID.name()))
                    .name(rs.getString(NAME.name()))
                    .build();
        }

        static MapSqlParameterSource getParameters(final ProjectDescription projectDescription, final byte[] content) {
            final MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(ProjectDescriptionParameters.ID.name(), projectDescription.getId());
            params.addValue(ProjectDescriptionParameters.PROJECT_ID.name(), projectDescription.getProjectId());
            params.addValue(ProjectDescriptionParameters.NAME.name(), projectDescription.getName());
            params.addValue(ProjectDescriptionParameters.CONTENT.name(), content);
            return params;
        }
    }

    @Required
    public void setProjectDescriptionSequenceName(final String projectDescriptionSequenceName) {
        this.projectDescriptionSequenceName = projectDescriptionSequenceName;
    }

    @Required
    public void setSaveProjectDescriptionQuery(final String saveProjectDescriptionQuery) {
        this.saveProjectDescriptionQuery = saveProjectDescriptionQuery;
    }

    @Required
    public void setUpdateProjectDescriptionQuery(final String updateProjectDescriptionQuery) {
        this.updateProjectDescriptionQuery = updateProjectDescriptionQuery;
    }

    @Required
    public void setDeleteProjectDescriptionByProjectIdQuery(final String deleteProjectDescriptionByProjectIdQuery) {
        this.deleteProjectDescriptionByProjectIdQuery = deleteProjectDescriptionByProjectIdQuery;
    }

    @Required
    public void setDeleteProjectDescriptionByIdQuery(final String deleteProjectDescriptionByIdQuery) {
        this.deleteProjectDescriptionByIdQuery = deleteProjectDescriptionByIdQuery;
    }

    @Required
    public void setFindProjectDescriptionContentByIdQuery(final String findProjectDescriptionContentByIdQuery) {
        this.findProjectDescriptionContentByIdQuery = findProjectDescriptionContentByIdQuery;
    }

    @Required
    public void setFindProjectDescriptionsByProjectIdQuery(final String findProjectDescriptionsByProjectIdQuery) {
        this.findProjectDescriptionsByProjectIdQuery = findProjectDescriptionsByProjectIdQuery;
    }

    @Required
    public void setFindProjectDescriptionByIdQuery(final String findProjectDescriptionByIdQuery) {
        this.findProjectDescriptionByIdQuery = findProjectDescriptionByIdQuery;
    }

    @Required
    public void setFindProjectDescriptionsByProjectIdsQuery(final String findProjectDescriptionsByProjectIdsQuery) {
        this.findProjectDescriptionsByProjectIdsQuery = findProjectDescriptionsByProjectIdsQuery;
    }

    @Required
    public void setFindProjectDescriptionsQuery(final String findProjectDescriptionsQuery) {
        this.findProjectDescriptionsQuery = findProjectDescriptionsQuery;
    }
}
