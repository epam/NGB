/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.blast.BlastListSpeciesTask;
import com.epam.catgenome.entity.blast.BlastTaskStatus;
import com.epam.catgenome.util.db.QueryParameters;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static com.epam.catgenome.util.Utils.addParametersToQuery;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BlastListSpeciesTaskDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;

    private String insertTaskQuery;
    private String updateTaskQuery;
    private String deleteTasksQuery;
    private String loadTaskQuery;
    private String loadTasksQuery;

    /**
     * Persists a new Blast list species task record.
     * @param task {@code BlastListSpeciesTask} a Blast list species task to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveTask(final BlastListSpeciesTask task) {
        getNamedParameterJdbcTemplate().update(insertTaskQuery, TaskParameters.getParameters(task));
    }

    /**
     * Updates existing Blast list species task record.
     * @param task {@code BlastListSpeciesTask} a Blast list species task to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateTask(final BlastListSpeciesTask task) {
        getNamedParameterJdbcTemplate().update(updateTaskQuery, TaskParameters.getParameters(task));
    }

    /**
     * Deletes all Blast list species task entities from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteTasks() {
        getJdbcTemplate().update(deleteTasksQuery);
    }

    /**
     * Loads {@code Blast list species tasks} from a database.
     * @return a {@code List<BlastListSpeciesTask>} from the database
     */
    public List<BlastListSpeciesTask> loadTasks(final QueryParameters queryParameters) {
        String query = addParametersToQuery(loadTasksQuery, queryParameters);
        return getJdbcTemplate().query(query, TaskParameters.getRowMapper());
    }

    /**
     * Loads a {@code Blast list species task} instance from the database by it's ID.
     * @param id {@code long} an ID of a database
     * @return a {@code BlastListSpeciesTask} instance from the database
     */
    public BlastListSpeciesTask loadTask(final long id) {
        List<BlastListSpeciesTask> tasks = getJdbcTemplate().query(loadTaskQuery,
                TaskParameters.getRowMapper(), id);
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    enum TaskParameters {
        TASK_ID,
        DATABASE_ID,
        CREATED_DATE,
        STATUS,
        STATUS_REASON,
        UPDATE_DATE;

        static MapSqlParameterSource getParameters(final BlastListSpeciesTask task) {
            MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(TASK_ID.name(), task.getTaskId());
            params.addValue(DATABASE_ID.name(), task.getDatabaseId());
            params.addValue(CREATED_DATE.name(), task.getCreatedDate() == null ? null :
                    Timestamp.valueOf(task.getCreatedDate()));
            params.addValue(STATUS.name(), task.getStatus().getId());
            params.addValue(STATUS_REASON.name(), task.getStatusReason());
            params.addValue(UPDATE_DATE.name(), task.getEndDate() == null ? null :
                    Timestamp.valueOf(task.getEndDate()));

            return params;
        }

        static RowMapper<BlastListSpeciesTask> getRowMapper() {
            return (rs, rowNum) -> parseTask(rs);
        }

        static BlastListSpeciesTask parseTask(final ResultSet rs) throws SQLException {
            return BlastListSpeciesTask.builder()
                    .taskId(rs.getLong(TASK_ID.name()))
                    .databaseId(rs.getLong(DATABASE_ID.name()))
                    .createdDate(rs.getTimestamp(CREATED_DATE.name()).toLocalDateTime())
                    .status(rs.getInt(STATUS.name()) == 0 ? null :
                            BlastTaskStatus.getById(rs.getInt(STATUS.name())))
                    .statusReason(rs.getString(STATUS_REASON.name()))
                    .endDate(rs.getTimestamp(UPDATE_DATE.name()) == null ? null :
                            rs.getTimestamp(UPDATE_DATE.name()).toLocalDateTime())
                    .build();
        }
    }
}
