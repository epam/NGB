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

package com.epam.catgenome.dao.project;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.epam.catgenome.entity.project.ProjectNote;
import com.epam.catgenome.util.db.Filter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.project.ProjectItem;
import com.epam.catgenome.entity.reference.Reference;

import static com.epam.catgenome.util.Utils.addFiltersToQuery;
import static org.apache.commons.lang3.StringUtils.join;

/**
 * Source:      ProjectDao
 * Created:     21.12.15, 18:15
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A DAO component that handles database interaction for {@code Project} instances.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDao extends NamedParameterJdbcDaoSupport {
    @Autowired
    private DaoHelper daoHelper;

    private String projectSequenceName;
    private String projectItemSequenceName;
    private String projectNoteSequenceName;

    private String insertProjectQuery;
    private String updateProjectQuery;
    private String updateProjectOwnerQuery;
    private String loadTopLevelProjectsForUserOrderByLastOpenedQuery;
    private String loadAllProjectsQuery;
    private String loadProjectByIdQuery;
    private String updateLastOpenedDateQuery;
    private String deleteProjectQuery;
    private String loadProjectsByParentIdQuery;
    private String moveProjectToParentQuery;

    private String addProjectItemQuery;
    private String deleteProjectItemQuery;
    private String deleteAllProjectItemsQuery;
    private String loadProjectItemsMaxNumberQuery;
    private String loadProjectItemsIdsQuery;
    private String updateProjectItemOrderingNumberQuery;
    private String hideProjectItemQuery;
    private String isProjectItemHiddenQuery;
    private String loadProjectByNameQuery;
    private String loadProjectItemsByProjectIdQuery;
    private String loadProjectItemsByProjectIdsQuery;
    private String loadProjectsByBioDataItemIdQuery;
    private String loadAllProjectItemsQuery;
    private String saveProjectDescriptionQuery;
    private String loadProjectDescriptionQuery;

    private String addProjectNoteQuery;
    private String deleteProjectNotesQuery;
    private String deleteAllProjectNotesQuery;
    private String loadProjectNotesQuery;
    private String loadAllProjectNotesQuery;
    private String updateProjectNotesQuery;

    /**
     * Persists a new or updates existing project in the database.
     * @param project {@code Project} a project to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveProject(Project project, Long parentId) {
        if (project.getId() == null) {
            project.setId(daoHelper.createId(projectSequenceName));
            getNamedParameterJdbcTemplate().update(insertProjectQuery, ProjectParameters.getParameters(project,
                                                                                                       parentId));
        } else {
            getNamedParameterJdbcTemplate().update(updateProjectQuery, ProjectParameters.getParameters(project, null));
        }
    }

    /**
     * Loads {@code Project}s for a user, specified bu user's ID, ordered by last opened date
     * @return a {@code List&lt;Project&gt;} of projects, created by specified user.
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Project> loadTopLevelProjectsOrderByLastOpened() {
        return getJdbcTemplate().query(loadTopLevelProjectsForUserOrderByLastOpenedQuery,
                                       ProjectParameters.getRowMapper());
    }


    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Project> loadAllProjects() {
        return getJdbcTemplate().query(loadAllProjectsQuery, ProjectParameters.getRowMapper());
    }

    /**
     * Loads nested {@code Project}s for a parent project, specified by ID
     * @param parentId a parent Project's ID to load nested projects
     * @return a list of nested {@code Project}s
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Project> loadNestedProjects(long parentId) {
        return getJdbcTemplate().query(loadProjectsByParentIdQuery, ProjectParameters.getRowMapper(), parentId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, Set<ProjectItem>> loadAllProjectItems() {
        Map<Long, Set<ProjectItem>> itemsMap = new HashMap<>();
        final RowMapper<ProjectItem> projectItemRowMapper = ProjectItemParameters.getSimpleItemMapper();
        getJdbcTemplate().query(loadAllProjectItemsQuery, rs -> {
            ProjectItem item = projectItemRowMapper.mapRow(rs, 0);

            Long projectId = rs.getLong(ProjectItemParameters.REFERRED_PROJECT_ID.name());
            if (!itemsMap.containsKey(projectId)) {
                itemsMap.put(projectId, new HashSet<>());
            }

            itemsMap.get(projectId).add(item);
        });
        return itemsMap;
    }

    /**
     * Loads a {@code Project} instance from a database by it's ID.
     * @param projectId {@code Long} an ID of a project from
     * @return a {@code Project} instance from the database
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public Project loadProject(long projectId) {
        List<Project> projects = getJdbcTemplate().query(loadProjectByIdQuery, ProjectParameters.getRowMapper(),
                                                         projectId);
        return projects.isEmpty() ? null : projects.get(0);
    }

    /**
     * Loads a {@code Project} instance from a database strictly by given name.
     * @param projectName {@code String} a name of a project
     * @return a {@code Project} instance from the database
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public Project loadProject(String projectName) {
        List<Project> projects = getJdbcTemplate().query(loadProjectByNameQuery, ProjectParameters.getRowMapper(),
                                                         projectName);
        return projects.isEmpty() ? null : projects.get(0);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Project> loadProjectsByBioDataItemId(final long bioDataItemId) {
        return getJdbcTemplate().query(loadProjectsByBioDataItemIdQuery, ProjectParameters.getRowMapper(),
                                       bioDataItemId);
    }

    /**
     * Updates {@code Project}'s last opened date field.
     * @param projectId {@code Long} ID of a project to update
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateLastOpenedDate(long projectId) {
        getJdbcTemplate().update(updateLastOpenedDateQuery, projectId);
    }

    /**
     * Moves a Project, specified by projectId to a parent project, specified by parentId
     * @param projectId an ID of a project, to move
     * @param parentId an ID of a parent project to move to
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void moveProjectToParent(long projectId, Long parentId) {
        if (parentId != null) {
            Assert.isTrue(projectId != parentId, "Project can't be it's own parent");
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(ProjectParameters.PROJECT_ID.name(), projectId);
        params.addValue(ProjectParameters.PARENT_ID.name(), parentId);

        getNamedParameterJdbcTemplate().update(moveProjectToParentQuery, params);
    }

    /**
     * Deletes a Project entity, specified by ID, from the database
     * @param projectId ID of a Project to delete
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteProject(long projectId) {
        getJdbcTemplate().update(deleteProjectQuery, projectId);
    }

    /**
     * Loads ProjectItems for a specified projectId
     * @param projectId Id of a project to load ProjectItems
     * @return a List of ProjectItems
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<ProjectItem> loadProjectItemsByProjectId(long projectId) {
        return getJdbcTemplate().query(loadProjectItemsByProjectIdQuery, ProjectItemParameters.getRowMapper(),
                projectId);
    }

    /**
     * Loads ProjectItems for a list of specified projectIds
     * @param projectIds a List of Ids of projects to load ProjectItems
     * @return a Map of ProjectItems to project IDs
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, Set<ProjectItem>> loadProjectItemsByProjectIds(List<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return loadProjectItemsByList(projectIds);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, Set<ProjectItem>> loadProjectItemsByProjects(List<Project> projects) {
        List<Long> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());
        return loadProjectItemsByProjectIds(projectIds);
    }

    private Map<Long, Set<ProjectItem>> loadProjectItemsByList(List<Long> projectIds) {
        Map<Long, Set<ProjectItem>> itemsMap = new HashMap<>();
        final RowMapper<ProjectItem> projectItemRowMapper = ProjectItemParameters.getSimpleItemMapper();
        String query = DaoHelper.getQueryFilledWithIdArray(loadProjectItemsByProjectIdsQuery, projectIds);
        getJdbcTemplate().query(query, rs -> {
            ProjectItem item = projectItemRowMapper.mapRow(rs, 0);

            Long projectId = rs.getLong(ProjectItemParameters.REFERRED_PROJECT_ID.name());
            if (!itemsMap.containsKey(projectId)) {
                itemsMap.put(projectId, new HashSet<>());
            }

            itemsMap.get(projectId).add(item);
        });
        return itemsMap;
    }

    /**
     * Adds an item to a project, specified by ID
     * @param projectId {@code Long} ID of a project
     * @param biologicalItemId {@code Long} ID of an item to add
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void addProjectItem(long projectId, long biologicalItemId) {
        Project project = loadProject(projectId);
        Assert.notNull(project, MessageHelper.getMessage(MessagesConstants.ERROR_PROJECT_NOT_FOUND, projectId));

        long newId = daoHelper.createId(projectItemSequenceName);

        Number countNumber = getJdbcTemplate().queryForObject(loadProjectItemsMaxNumberQuery,
                                                              new SingleColumnRowMapper<>(), projectId);
        Integer count = 1;
        if (countNumber != null) {
            count = countNumber.intValue() + 1;
        }

        MapSqlParameterSource params = new MapSqlParameterSource();

        params.addValue(ProjectItemParameters.PROJECT_ITEM_ID.name(), newId);
        params.addValue(ProjectItemParameters.PROJECT_ID.name(), projectId);
        params.addValue(ProjectItemParameters.BIO_DATA_ITEM_ID.name(), biologicalItemId);
        params.addValue(ProjectItemParameters.ORDINAL_NUMBER.name(), count);
        params.addValue(ProjectItemParameters.HIDDEN.name(), false);

        getNamedParameterJdbcTemplate().update(addProjectItemQuery, params);
    }

    /**
     * Adds multiple items to a project, specified by ID
     * @param projectId {@code Long} ID of a project
     * @param items a {@code List&lt;BiologicalDataItem&gt;} of items to add
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void addProjectItems(long projectId, List<ProjectItem> items) {
        Project project = loadProject(projectId);
        Assert.notNull(project, MessageHelper.getMessage(MessagesConstants.ERROR_PROJECT_NOT_FOUND, projectId));

        List<Long> newIds = daoHelper.createIds(projectItemSequenceName, items.size());

        Number countNumber = getJdbcTemplate().queryForObject(loadProjectItemsMaxNumberQuery,
                                                        new SingleColumnRowMapper<>(), projectId);
        Integer count = 1;
        if (countNumber != null) {
            count = countNumber.intValue() + 1;
        }

        ArrayList<MapSqlParameterSource> params = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            MapSqlParameterSource param = new MapSqlParameterSource();

            param.addValue(ProjectItemParameters.PROJECT_ITEM_ID.name(), newIds.get(i));
            param.addValue(ProjectItemParameters.PROJECT_ID.name(), projectId);
            param.addValue(ProjectItemParameters.BIO_DATA_ITEM_ID.name(),
                    getBioDataItemId(items.get(i).getBioDataItem()));
            param.addValue(ProjectItemParameters.ORDINAL_NUMBER.name(), count);
            param.addValue(ProjectItemParameters.HIDDEN.name(), items.get(i).getHidden() != null &&
                    items.get(i).getHidden());

            params.add(param);
        }

        getNamedParameterJdbcTemplate().batchUpdate(addProjectItemQuery,
                params.toArray(new MapSqlParameterSource[items.size()]));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<ProjectNote> saveProjectNotes(final List<ProjectNote> notes, final long projectId) {
        if (!CollectionUtils.isEmpty(notes)) {
            List<Long> newIds = daoHelper.createIds(projectNoteSequenceName, notes.size());
            List<MapSqlParameterSource> params = new ArrayList<>(notes.size());
            int i = 0;
            for (ProjectNote note: notes) {
                note.setNoteId(newIds.get(i));
                note.setProjectId(projectId);
                MapSqlParameterSource param = ProjectNoteParameters.getParameters(note);
                params.add(param);
                i++;
            }
            getNamedParameterJdbcTemplate().batchUpdate(addProjectNoteQuery,
                    params.toArray(new MapSqlParameterSource[notes.size()]));
            return notes;
        } else {
            return new ArrayList<>();
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<ProjectNote> updateProjectNotes(final List<ProjectNote> notes) {
        if (!CollectionUtils.isEmpty(notes)) {
            List<MapSqlParameterSource> params = new ArrayList<>(notes.size());
            for (ProjectNote note: notes) {
                MapSqlParameterSource param = ProjectNoteParameters.getParameters(note);
                params.add(param);
            }
            getNamedParameterJdbcTemplate().batchUpdate(updateProjectNotesQuery,
                    params.toArray(new MapSqlParameterSource[notes.size()]));
            return notes;
        } else {
            return new ArrayList<>();
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteProjectNotes(final List<Filter> filters) {
        String query = addFiltersToQuery(deleteProjectNotesQuery, filters);
        getJdbcTemplate().update(query);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteAllProjectNotes(final long projectId) {
        getJdbcTemplate().update(deleteAllProjectNotesQuery, projectId);
    }

    public List<ProjectNote> loadProjectNotes(final long projectId) {
        return getJdbcTemplate().query(loadProjectNotesQuery, ProjectNoteParameters.getRowMapper(), projectId);
    }

    public Map<Long, Set<ProjectNote>> loadAllProjectNotes(List<Project> projects) {
        String query = loadAllProjectNotesQuery;
        if (!CollectionUtils.isEmpty(projects)) {
            List<Long> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());
            Filter filter = Filter.builder()
                    .field("project_id")
                    .operator("in")
                    .value("(" + join(projectIds, ",") + ")")
                    .build();
            query = addFiltersToQuery(loadAllProjectNotesQuery, Collections.singletonList(filter));
        }
        List<ProjectNote> notes = getJdbcTemplate().query(query, ProjectNoteParameters.getRowMapper());
        Map<Long, Set<ProjectNote>> notesMap = new HashMap<>();
        for (ProjectNote note: notes) {
            Long projectId = note.getProjectId();
            if (!notesMap.containsKey(projectId)) {
                notesMap.put(projectId, new HashSet<>());
            }
            notesMap.get(projectId).add(note);
        }
        return notesMap;
    }

    /**
     * Removes an item from a project, specified by ID
     * @param projectId {@code Long} ID of a project
     * @param biologicalItemId {@code Long} ID of an item to remove
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteProjectItem(long projectId, long biologicalItemId) {
        List<Long> currentItemIds = getJdbcTemplate().query(loadProjectItemsIdsQuery, new SingleColumnRowMapper<>(),
                projectId);

        currentItemIds.remove(biologicalItemId);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(ProjectItemParameters.PROJECT_ID.name(), projectId);
        params.addValue(ProjectItemParameters.BIO_DATA_ITEM_ID.name(), biologicalItemId);

        getNamedParameterJdbcTemplate().update(deleteProjectItemQuery, params);

        ArrayList<MapSqlParameterSource> updateParams = new ArrayList<>();
        for (int i = 1; i <= currentItemIds.size(); i++) {
            MapSqlParameterSource param = new MapSqlParameterSource();
            param.addValue(ProjectItemParameters.PROJECT_ID.name(), projectId);
            param.addValue(ProjectItemParameters.BIO_DATA_ITEM_ID.name(), currentItemIds.get(i - 1));
            param.addValue(ProjectItemParameters.ORDINAL_NUMBER.name(), i);

            updateParams.add(param);
        }

        getNamedParameterJdbcTemplate().batchUpdate(updateProjectItemOrderingNumberQuery, updateParams.toArray(
                new MapSqlParameterSource[updateParams.size()]));
    }

    /**
     * Removes multiple items from a project, specified by ID
     * @param projectId {@code Long} ID of a project
     * @param items a {@code List&lt;BiologicalDataItem&gt;} of items to remove
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteProjectItems(long projectId, List<ProjectItem> items) {
        List<Long> currentItemIds = getJdbcTemplate().query(loadProjectItemsIdsQuery, new SingleColumnRowMapper<>(),
                projectId);

        List<Long> idsToRemove = items.stream().map(i ->
                getBioDataItemId(i.getBioDataItem())).collect(Collectors.toList());
        currentItemIds.removeAll(idsToRemove);

        ArrayList<MapSqlParameterSource> params = new ArrayList<>(items.size());

        for (ProjectItem item : items) {
            MapSqlParameterSource param = new MapSqlParameterSource();
            param.addValue(ProjectItemParameters.PROJECT_ID.name(), projectId);
            param.addValue(ProjectItemParameters.BIO_DATA_ITEM_ID.name(),
                    getBioDataItemId(item.getBioDataItem()));
            params.add(param);
        }

        getNamedParameterJdbcTemplate().batchUpdate(deleteProjectItemQuery, params.toArray(new
                MapSqlParameterSource[items.size()]));

        ArrayList<MapSqlParameterSource> updateParams = new ArrayList<>();
        for (int i = 1; i <= currentItemIds.size(); i++) {
            MapSqlParameterSource param = new MapSqlParameterSource();
            param.addValue(ProjectItemParameters.PROJECT_ID.name(), projectId);
            param.addValue(ProjectItemParameters.BIO_DATA_ITEM_ID.name(), currentItemIds.get(i - 1));
            param.addValue(ProjectItemParameters.ORDINAL_NUMBER.name(), i);

            updateParams.add(param);
        }

        getNamedParameterJdbcTemplate().batchUpdate(updateProjectItemOrderingNumberQuery, updateParams.toArray(
                new MapSqlParameterSource[updateParams.size()]));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteProjectItems(long projectId) {
        getJdbcTemplate().update(deleteAllProjectItemsQuery, projectId);
    }

    /**
     * Tests if project item from a database is hidden
     * @param projectId {@code Long} ID of a project
     * @param biologicalItemId {@code Long} ID of an item to test
     * @return {@code true} if project item is hidden, {@code false} if not
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public Boolean isProjectItemHidden(long projectId, long biologicalItemId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(ProjectItemParameters.PROJECT_ID.name(), projectId);
        params.addValue(ProjectItemParameters.BIO_DATA_ITEM_ID.name(), biologicalItemId);

        return getNamedParameterJdbcTemplate().queryForObject(isProjectItemHiddenQuery, params,
                new SingleColumnRowMapper<>());
    }

    /**
     * Hides a project item, specified by ID. Hidden item won't be shown on UI.
     * @param projectId {@code Long} ID of a project
     * @param biologicalItemId {@code Long} ID of an item to hide
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void hideProjectItem(long projectId, long biologicalItemId, boolean hide) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(ProjectItemParameters.PROJECT_ID.name(), projectId);
        params.addValue(ProjectItemParameters.BIO_DATA_ITEM_ID.name(), biologicalItemId);
        params.addValue(ProjectItemParameters.HIDDEN.name(), hide);

        getNamedParameterJdbcTemplate().update(hideProjectItemQuery, params);
    }

    /**
     * Hides a project item, specified by ID. Hidden item won't be shown on UI.
     * @param projectId {@code Long} ID of a project
     * @param items a {@code List&lt;ProjectItem&gt;} list of project items to update hidden status
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void hideProjectItems(long projectId, List<ProjectItem> items) {
        ArrayList<MapSqlParameterSource> params = new ArrayList<>(items.size());

        for (ProjectItem item : items) {
            MapSqlParameterSource param = new MapSqlParameterSource();
            param.addValue(ProjectItemParameters.PROJECT_ID.name(), projectId);
            param.addValue(ProjectItemParameters.BIO_DATA_ITEM_ID.name(),
                    getBioDataItemId(item.getBioDataItem()));
            param.addValue(ProjectItemParameters.HIDDEN.name(), item.getHidden() != null && item.getHidden());

            params.add(param);

        }

        getNamedParameterJdbcTemplate().batchUpdate(hideProjectItemQuery, params.toArray(new
                MapSqlParameterSource[items.size()]));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateOwner(Long id, String owner) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(ProjectParameters.PROJECT_ID.name(), id);
        params.addValue(ProjectParameters.OWNER.name(), owner);

        getNamedParameterJdbcTemplate().update(updateProjectOwnerQuery, params);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveProjectDescription(final Long projectId, final byte[] description) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(ProjectParameters.PROJECT_ID.name(), projectId);
        params.addValue(ProjectParameters.DESCRIPTION.name(), description);

        getNamedParameterJdbcTemplate().update(saveProjectDescriptionQuery, params);
    }

    public InputStream loadProjectDescription(final Long projectId) {
        final LobHandler lobHandler = new DefaultLobHandler();
        final List<InputStream> result = getJdbcTemplate().query(loadProjectDescriptionQuery, (rs, rowNum) ->
                lobHandler.getBlobAsBinaryStream(rs, ProjectParameters.DESCRIPTION.name()), projectId);
        return result.isEmpty() ? null : result.get(0);
    }

    enum ProjectParameters {
        PROJECT_ID,
        PROJECT_NAME,
        PARENT_ID,
        PROJECT_PRETTY_NAME,
        CREATED_DATE,
        LAST_OPENED_DATE,
        OWNER,
        DESCRIPTION;

        static MapSqlParameterSource getParameters(Project project, Long parentId) {
            MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(PROJECT_ID.name(), project.getId());
            params.addValue(PROJECT_NAME.name(), project.getName());
            params.addValue(CREATED_DATE.name(), project.getCreatedDate());
            params.addValue(PARENT_ID.name(), parentId);
            params.addValue(PROJECT_PRETTY_NAME.name(), project.getPrettyName());
            params.addValue(OWNER.name(), project.getOwner());

            return params;
        }

        static RowMapper<Project> getRowMapper() {
            return (rs, rowNum) -> {
                Project project = new Project();

                project.setId(rs.getLong(PROJECT_ID.name()));
                project.setName(rs.getString(PROJECT_NAME.name()));
                project.setCreatedDate(new Date(rs.getTimestamp(CREATED_DATE.name()).getTime()));
                project.setPrettyName(rs.getString(PROJECT_PRETTY_NAME.name()));
                project.setLastOpenedDate(new Date(rs.getTimestamp(LAST_OPENED_DATE.name()).getTime()));
                project.setOwner(rs.getString(OWNER.name()));

                long longVal = rs.getLong(PARENT_ID.name());
                if (!rs.wasNull()) {
                    project.setParentId(longVal);
                }

                return project;
            };
        }

    }

    enum ProjectItemParameters {
        PROJECT_ITEM_ID,
        PROJECT_ID,
        BIO_DATA_ITEM_ID,
        HIDDEN,
        ORDINAL_NUMBER,
        REFERRED_PROJECT_ID,

        NAME,
        TYPE,
        PATH,
        FORMAT,
        CREATED_DATE,

        VCF_ID,
        VCF_REFERENCE_GENOME_ID,
        VCF_COMPRESSED,

        GENE_ITEM_ID,
        GENE_REFERENCE_GENOME_ID,
        GENE_COMPRESSED,
        GENE_EXTERNAL_DB_TYPE_ID,
        GENE_EXTERNAL_DB_ID,
        GENE_EXTERNAL_DB_ORGANISM,

        REFERENCE_GENOME_ID,
        REFERENCE_GENOME_SIZE,

        BAM_ID,
        BAM_REFERENCE_GENOME_ID,
        BAM_INDEX_PATH,

        BED_GRAPH_ID,
        BED_GRAPH_REFERENCE_GENOME_ID,

        BED_ID,
        BED_REFERENCE_GENOME_ID,
        BED_INDEX_PATH,
        BED_COMPRESSED,

        SEG_ID,
        SEG_REFERENCE_GENOME_ID,
        SEG_INDEX_PATH,
        SEG_COMPRESSED,

        INDEX_ID,
        INDEX_NAME,
        INDEX_TYPE,
        INDEX_PATH,
        INDEX_FORMAT,
        INDEX_CREATED_DATE;

        /**
         * Megamapper
         * @return
         */
        static RowMapper<ProjectItem> getRowMapper() {
            return new ProjectItemMapper();
        }

        static RowMapper<ProjectItem> getSimpleItemMapper() {
            return new SimpleItemMapper();
        }

        static class ProjectItemMapper implements RowMapper<ProjectItem> {

            protected RowMapper<BiologicalDataItem> bioDataItemMapper = BiologicalDataItemDao
                    .BiologicalDataItemParameters.getRowMapper();
            @Override
            public ProjectItem mapRow(ResultSet rs, int rowNum) throws SQLException {
                ProjectItem item = new ProjectItem();
                BiologicalDataItem dataItem = bioDataItemMapper.mapRow(rs, rowNum);

                item.setBioDataItem(dataItem);
                item.setHidden(rs.getBoolean(HIDDEN.name()));
                item.setId(rs.getLong(PROJECT_ITEM_ID.name()));
                item.setOrdinalNumber(rs.getShort(ORDINAL_NUMBER.name()));

                return item;
            }

        }
        static class SimpleItemMapper extends ProjectItemMapper {

            SimpleItemMapper() {
                bioDataItemMapper = BiologicalDataItemDao.BiologicalDataItemParameters.getRowMapper(false);
            }
        }
    }

    enum ProjectNoteParameters {
        NOTE_ID,
        PROJECT_ID,
        TITLE,
        CONTENT;

        static MapSqlParameterSource getParameters(final ProjectNote note) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(NOTE_ID.name(), note.getNoteId());
            params.addValue(PROJECT_ID.name(), note.getProjectId());
            params.addValue(TITLE.name(), note.getTitle());
            params.addValue(CONTENT.name(), note.getContent());
            return params;
        }

        static RowMapper<ProjectNote> getRowMapper() {
            return (rs, rowNum) -> parseNote(rs);
        }

        static ProjectNote parseNote(final ResultSet rs) throws SQLException {
            return ProjectNote.builder()
                    .noteId(rs.getLong(NOTE_ID.name()))
                    .projectId(rs.getLong(PROJECT_ID.name()))
                    .title(rs.getString(TITLE.name()))
                    .content(rs.getString(CONTENT.name()))
                    .build();
        }
    }

    private Long getBioDataItemId(BiologicalDataItem item) {
        if (item instanceof FeatureFile) {
            return ((FeatureFile) item).getBioDataItemId();
        } else {
            if (item instanceof Reference) {
                return ((Reference) item).getBioDataItemId();
            } else {
                return item.getId();
            }
        }
    }
}
