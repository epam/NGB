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

package com.epam.catgenome.dao.blast;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.blast.BlastTaskOrganism;
import com.epam.catgenome.entity.blast.BlastTask;
import com.epam.catgenome.entity.blast.TaskParameter;
import com.epam.catgenome.entity.blast.TaskStatus;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.QueryParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.epam.catgenome.util.Utils.addFiltersToQuery;
import static com.epam.catgenome.util.Utils.addParametersToQuery;

@Slf4j
public class BlastTaskDao extends NamedParameterJdbcDaoSupport {
    private String insertTaskQuery;
    private String updateTaskStatusQuery;
    private String deleteTaskQuery;
    private String loadTaskByIdQuery;
    private String loadAllTasksQuery;
    private String getTaskCountQuery;

    private String organismSequenceName;
    private String insertTaskOrganismsQuery;
    private String deleteTaskOrganismsQuery;
    private String loadTaskOrganismsQuery;

    private String exclOrganismSequenceName;
    private String insertTaskExclOrganismsQuery;
    private String deleteTaskExclOrganismsQuery;
    private String loadTaskExclOrganismsQuery;

    private String taskParameterSequenceName;
    private String insertTaskParametersQuery;
    private String deleteTaskParametersQuery;
    private String loadTaskParametersQuery;

    @Autowired
    private DaoHelper daoHelper;

    /**
     * Saves or updates a {@code Task} instance in the database
     * @param blastTask to save
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveTask(final BlastTask blastTask) {
        getNamedParameterJdbcTemplate().update(insertTaskQuery, TaskParameters.getParameters(blastTask));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateTask(final BlastTask blastTask) {
        getNamedParameterJdbcTemplate().update(updateTaskStatusQuery, TaskParameters.getParameters(blastTask));
    }

    /**
     * Loads a {@code Task} instance from the database specified by it's ID
     * @param id of the task
     * @return a loaded {@code Task} instance or
     *          {@code null} if task with a given ID doesn't exist
     */
    public BlastTask loadTaskById(final long id) {
        List<BlastTask> blastTasks = getJdbcTemplate().query(loadTaskByIdQuery, TaskParameters.getRowMapper(), id);
        return blastTasks.isEmpty() ? null : blastTasks.get(0);
    }

    public List<BlastTask> loadAllTasks(final QueryParameters queryParameters) {
        String query = addParametersToQuery(loadAllTasksQuery, queryParameters);
        return getJdbcTemplate().query(query, TaskParameters.getRowMapper());
    }

    public long getTasksCount(final List<Filter> filters) {
        String query = addFiltersToQuery(getTaskCountQuery, filters);
        return getJdbcTemplate().queryForObject(query, Long.class);
    }

    /**
     * Deletes {@code Task}  from database.
     *
     * @param id of the task to remove
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteTask(final Long id) {
        getJdbcTemplate().update(deleteTaskQuery, id);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveOrganism(final BlastTaskOrganism blastTaskOrganism) {
        blastTaskOrganism.setId(daoHelper.createId(organismSequenceName));
        getNamedParameterJdbcTemplate().update(insertTaskOrganismsQuery,
                OrganismParameters.getParameters(blastTaskOrganism));
    }

    /**
     * Loads {@code Organisms} from the database by Task Id
     * @param taskId id of the task
     * @return a loaded {@code List<Organism>}
     */
    public List<BlastTaskOrganism> loadOrganisms(final long taskId) {
        return getJdbcTemplate().query(loadTaskOrganismsQuery, OrganismParameters.getRowMapper(), taskId);
    }

    /**
     * Loads {@code Organisms} from the database by Task Id
     * @param taskId id of the task
     * @return a loaded {@code List<Organism>}
     */
    public List<BlastTaskOrganism> loadExclOrganisms(final long taskId) {
        return getJdbcTemplate().query(loadTaskExclOrganismsQuery, OrganismParameters.getRowMapper(), taskId);
    }

    /**
     * Deletes {@code Organisms}  from database.
     *
     * @param id of the task
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteOrganisms(final Long id) {
        getJdbcTemplate().update(deleteTaskOrganismsQuery, id);
    }

    /**
     * Deletes {@code Organisms}  from database.
     *
     * @param id of the task
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteExclOrganisms(final Long id) {
        getJdbcTemplate().update(deleteTaskExclOrganismsQuery, id);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveTaskParameter(final TaskParameter taskParameter) {
        taskParameter.setParameterId(daoHelper.createId(taskParameterSequenceName));
        getNamedParameterJdbcTemplate().update(insertTaskParametersQuery,
                TaskParameterParameters.getParameters(taskParameter));
    }

    public List<TaskParameter> loadTaskParameters(final long id) {
        return getJdbcTemplate().query(loadTaskParametersQuery, TaskParameterParameters.getRowMapper(), id);
    }

    /**
     * Deletes {@code Task Parameters}  from database.
     *
     * @param id of the task
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteParameters(final Long id) {
        getJdbcTemplate().update(deleteTaskParametersQuery, id);
    }

    enum TaskParameters {
        TASK_ID,
        TITLE,
        CREATED_DATE,
        STATUS,
        END_DATE,
        STATUS_REASON,
        QUERY,
        DATABASE,
        EXECUTABLE,
        ALGORITHM,
        OPTIONS,
        OWNER;

        static MapSqlParameterSource getParameters(final BlastTask blastTask) {
            MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(TASK_ID.name(), blastTask.getId());
            params.addValue(TITLE.name(), blastTask.getTitle());
            params.addValue(CREATED_DATE.name(), blastTask.getCreatedDate() == null ? null
                    : Timestamp.valueOf(blastTask.getCreatedDate()));
            params.addValue(STATUS.name(), blastTask.getStatus().getId());
            params.addValue(END_DATE.name(), blastTask.getEndDate() == null ? null
                    : Timestamp.valueOf(blastTask.getEndDate()));
            params.addValue(STATUS_REASON.name(), blastTask.getStatusReason());
            params.addValue(QUERY.name(), blastTask.getQuery());
            params.addValue(DATABASE.name(), blastTask.getDatabase());
            params.addValue(EXECUTABLE.name(), blastTask.getExecutable());
            params.addValue(ALGORITHM.name(), blastTask.getAlgorithm());
            params.addValue(OPTIONS.name(), blastTask.getOptions());
            params.addValue(OWNER.name(), blastTask.getOwner());

            return params;
        }

        static RowMapper<BlastTask> getRowMapper() {
            return (rs, rowNum) -> {
                BlastTask blastTask = new BlastTask();

                blastTask.setId(rs.getLong(TASK_ID.name()));
                blastTask.setTitle(rs.getString(TITLE.name()));
                blastTask.setCreatedDate(rs.getTimestamp(CREATED_DATE.name()).toLocalDateTime());
                blastTask.setEndDate(rs.getTimestamp(END_DATE.name()) == null ? null
                        : rs.getTimestamp(END_DATE.name()).toLocalDateTime());
                blastTask.setStatusReason(rs.getString(STATUS_REASON.name()));
                blastTask.setQuery(rs.getString(QUERY.name()));
                blastTask.setDatabase(rs.getString(DATABASE.name()));
                blastTask.setExecutable(rs.getString(EXECUTABLE.name()));
                blastTask.setAlgorithm(rs.getString(ALGORITHM.name()));
                blastTask.setOptions(rs.getString(OPTIONS.name()));
                blastTask.setOwner(rs.getString(OWNER.name()));

                long longVal = rs.getLong(STATUS.name());
                if (!rs.wasNull()) {
                    blastTask.setStatus(TaskStatus.getById(longVal));
                }

                return blastTask;
            };
        }
    }

    enum OrganismParameters {
        ORGANISM_ID,
        TASK_ID,
        ORGANISM;

        static MapSqlParameterSource getParameters(final BlastTaskOrganism blastTaskOrganism) {
            MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(ORGANISM_ID.name(), blastTaskOrganism.getId());
            params.addValue(TASK_ID.name(), blastTaskOrganism.getTaskId());
            params.addValue(ORGANISM.name(), blastTaskOrganism.getOrganism());

            return params;
        }

        static RowMapper<BlastTaskOrganism> getRowMapper() {
            return (rs, rowNum) -> {
                BlastTaskOrganism blastTaskOrganism = new BlastTaskOrganism();

                blastTaskOrganism.setId(rs.getLong(ORGANISM_ID.name()));
                blastTaskOrganism.setTaskId(rs.getLong(TASK_ID.name()));
                blastTaskOrganism.setOrganism(rs.getLong(ORGANISM.name()));

                return blastTaskOrganism;
            };
        }
    }

    enum TaskParameterParameters {
        PARAMETER_ID,
        TASK_ID,
        PARAMETER,
        VALUE;

        static MapSqlParameterSource getParameters(final TaskParameter taskParameter) {
            MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(PARAMETER_ID.name(), taskParameter.getParameterId());
            params.addValue(TASK_ID.name(), taskParameter.getTaskId());
            params.addValue(PARAMETER.name(), taskParameter.getParameter());
            params.addValue(VALUE.name(), taskParameter.getValue());

            return params;
        }

        static RowMapper<TaskParameter> getRowMapper() {
            return (rs, rowNum) -> {
                TaskParameter taskParameter = new TaskParameter();

                taskParameter.setParameterId(rs.getLong(PARAMETER_ID.name()));
                taskParameter.setTaskId(rs.getLong(TASK_ID.name()));
                taskParameter.setParameter(rs.getString(PARAMETER.name()));
                taskParameter.setValue(rs.getString(VALUE.name()));

                return taskParameter;
            };
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveOrganisms(final long taskId, final List<Long> organisms) {
        if (CollectionUtils.isEmpty(organisms)) {
            return;
        }
        List<Long> newIds = daoHelper.createIds(organismSequenceName, organisms.size());

        ArrayList<MapSqlParameterSource> params = new ArrayList<>(organisms.size());
        for (int i = 0; i < organisms.size(); i++) {
            MapSqlParameterSource param = new MapSqlParameterSource();

            param.addValue(OrganismParameters.ORGANISM_ID.name(), newIds.get(i));
            param.addValue(OrganismParameters.TASK_ID.name(), taskId);
            param.addValue(OrganismParameters.ORGANISM.name(),
                    organisms.get(i));

            params.add(param);
        }

        getNamedParameterJdbcTemplate().batchUpdate(insertTaskOrganismsQuery,
                params.toArray(new MapSqlParameterSource[organisms.size()]));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveExclOrganisms(final long taskId, final List<Long> exclOrganisms) {
        if (CollectionUtils.isEmpty(exclOrganisms)) {
            return;
        }
        List<Long> newIds = daoHelper.createIds(exclOrganismSequenceName, exclOrganisms.size());

        ArrayList<MapSqlParameterSource> params = new ArrayList<>(exclOrganisms.size());
        for (int i = 0; i < exclOrganisms.size(); i++) {
            MapSqlParameterSource param = new MapSqlParameterSource();

            param.addValue(OrganismParameters.ORGANISM_ID.name(), newIds.get(i));
            param.addValue(OrganismParameters.TASK_ID.name(), taskId);
            param.addValue(OrganismParameters.ORGANISM.name(),
                    exclOrganisms.get(i));

            params.add(param);
        }

        getNamedParameterJdbcTemplate().batchUpdate(insertTaskExclOrganismsQuery,
                params.toArray(new MapSqlParameterSource[exclOrganisms.size()]));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveTaskParameters(final long taskId, final Map<String, String> parameters) {
        if (MapUtils.isEmpty(parameters)) {
            return;
        }
        List<Long> newIds = daoHelper.createIds(taskParameterSequenceName, parameters.size());

        ArrayList<MapSqlParameterSource> params = new ArrayList<>(parameters.size());
        int i = 0;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            MapSqlParameterSource param = new MapSqlParameterSource();

            param.addValue(TaskParameterParameters.PARAMETER_ID.name(), newIds.get(i));
            param.addValue(TaskParameterParameters.TASK_ID.name(), taskId);
            param.addValue(TaskParameterParameters.PARAMETER.name(), entry.getKey());
            param.addValue(TaskParameterParameters.VALUE.name(), entry.getValue());

            params.add(param);
            i++;
        }

        getNamedParameterJdbcTemplate().batchUpdate(insertTaskParametersQuery,
                params.toArray(new MapSqlParameterSource[parameters.size()]));
    }

    @Required
    public void setOrganismSequenceName(final String organismSequenceName) {
        this.organismSequenceName = organismSequenceName;
    }

    @Required
    public void setExclOrganismSequenceName(final String exclOrganismSequenceName) {
        this.exclOrganismSequenceName = exclOrganismSequenceName;
    }

    @Required
    public void setTaskParameterSequenceName(final String taskParameterSequenceName) {
        this.taskParameterSequenceName = taskParameterSequenceName;
    }

    @Required
    public void setInsertTaskQuery(final String insertTaskQuery) {
        this.insertTaskQuery = insertTaskQuery;
    }

    @Required
    public void setLoadTaskByIdQuery(final String loadTaskByIdQuery) {
        this.loadTaskByIdQuery = loadTaskByIdQuery;
    }

    @Required
    public void setLoadAllTasksQuery(final String loadAllTasksQuery) {
        this.loadAllTasksQuery = loadAllTasksQuery;
    }

    @Required
    public void setUpdateTaskStatusQuery(final String updateTaskStatusQuery) {
        this.updateTaskStatusQuery = updateTaskStatusQuery;
    }

    @Required
    public void setDeleteTaskQuery(final String deleteTaskQuery) {
        this.deleteTaskQuery = deleteTaskQuery;
    }

    @Required
    public void setInsertTaskOrganismsQuery(final String insertTaskOrganismsQuery) {
        this.insertTaskOrganismsQuery = insertTaskOrganismsQuery;
    }

    @Required
    public void setDeleteTaskOrganismsQuery(final String deleteTaskOrganismsQuery) {
        this.deleteTaskOrganismsQuery = deleteTaskOrganismsQuery;
    }

    @Required
    public void setLoadTaskOrganismsQuery(final String loadTaskOrganismsQuery) {
        this.loadTaskOrganismsQuery = loadTaskOrganismsQuery;
    }

    @Required
    public void setInsertTaskExclOrganismsQuery(final String insertTaskExclOrganismsQuery) {
        this.insertTaskExclOrganismsQuery = insertTaskExclOrganismsQuery;
    }

    @Required
    public void setDeleteTaskExclOrganismsQuery(final String deleteTaskExclOrganismsQuery) {
        this.deleteTaskExclOrganismsQuery = deleteTaskExclOrganismsQuery;
    }

    @Required
    public void setLoadTaskExclOrganismsQuery(final String loadTaskExclOrganismsQuery) {
        this.loadTaskExclOrganismsQuery = loadTaskExclOrganismsQuery;
    }

    @Required
    public void setInsertTaskParametersQuery(final String insertTaskParametersQuery) {
        this.insertTaskParametersQuery = insertTaskParametersQuery;
    }

    @Required
    public void setDeleteTaskParametersQuery(final String deleteTaskParametersQuery) {
        this.deleteTaskParametersQuery = deleteTaskParametersQuery;
    }

    @Required
    public void setLoadTaskParametersQuery(final String loadTaskParametersQuery) {
        this.loadTaskParametersQuery = loadTaskParametersQuery;
    }

    @Required
    public void setGetTaskCountQuery(final String getTaskCountQuery) {
        this.getTaskCountQuery = getTaskCountQuery;
    }
}
