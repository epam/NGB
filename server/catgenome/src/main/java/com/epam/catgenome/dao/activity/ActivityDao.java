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

package com.epam.catgenome.dao.activity;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.activity.Activity;
import com.epam.catgenome.entity.activity.ActivityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class ActivityDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;

    private String activitySequenceName;
    private String insertActivityQuery;
    private String loadActivityByItemIdAndUidQuery;
    private String deleteActivityByItemIdQuery;

    @Transactional(propagation = Propagation.MANDATORY)
    public void save(final Activity activity) {
        if (activity.getId() == null) {
            activity.setId(daoHelper.createId(activitySequenceName));
        }
        getNamedParameterJdbcTemplate().update(insertActivityQuery, ActivityParameters.getParameters(activity));
    }

    public List<Activity> getByItemIdAndUid(final Long itemId, final String uid) {
        return getJdbcTemplate().query(loadActivityByItemIdAndUidQuery, ActivityParameters.getRowMapper(), itemId, uid);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteByItemId(final Long itemId) {
        getJdbcTemplate().update(deleteActivityByItemIdQuery, itemId);
    }

    enum ActivityParameters {
        ID,
        UID,
        ITEM_ID,
        ITEM_TYPE,
        ACTION_TYPE,
        DATETIME,
        USERNAME,
        FIELD,
        OLD_VALUE,
        NEW_VALUE;

        static MapSqlParameterSource getParameters(final Activity activity) {
            final MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(ID.name(), activity.getId());
            params.addValue(UID.name(), activity.getUid());
            params.addValue(ITEM_ID.name(), activity.getItemId());
            params.addValue(ITEM_TYPE.name(), activity.getItemType().getId());
            params.addValue(ACTION_TYPE.name(), activity.getActionType().name());
            params.addValue(DATETIME.name(), Timestamp.valueOf(activity.getDatetime()));
            params.addValue(USERNAME.name(), activity.getUsername());
            params.addValue(FIELD.name(), activity.getField());
            params.addValue(OLD_VALUE.name(), activity.getOldValue());
            params.addValue(NEW_VALUE.name(), activity.getNewValue());

            return params;
        }

        static RowMapper<Activity> getRowMapper() {
            return (rs, rowNum) -> parseActivity(rs);
        }

        static Activity parseActivity(final ResultSet rs) throws SQLException {
            return Activity.builder()
                    .id(rs.getLong(ID.name()))
                    .uid(rs.getString(UID.name()))
                    .itemId(rs.getLong(ITEM_ID.name()))
                    .itemType(BiologicalDataItemFormat.getById(rs.getLong(ITEM_TYPE.name())))
                    .actionType(ActivityType.valueOf(rs.getString(ACTION_TYPE.name())))
                    .datetime(rs.getTimestamp(DATETIME.name()).toLocalDateTime())
                    .username(rs.getString(USERNAME.name()))
                    .field(rs.getString(FIELD.name()))
                    .oldValue(rs.getString(OLD_VALUE.name()))
                    .newValue(rs.getString(NEW_VALUE.name()))
                    .build();
        }
    }

    @Required
    public void setActivitySequenceName(String activitySequenceName) {
        this.activitySequenceName = activitySequenceName;
    }

    @Required
    public void setInsertActivityQuery(String insertActivityQuery) {
        this.insertActivityQuery = insertActivityQuery;
    }

    @Required
    public void setLoadActivityByItemIdAndUidQuery(String loadActivityByItemIdAndUidQuery) {
        this.loadActivityByItemIdAndUidQuery = loadActivityByItemIdAndUidQuery;
    }

    @Required
    public void setDeleteActivityByItemIdQuery(String deleteActivityByItemIdQuery) {
        this.deleteActivityByItemIdQuery = deleteActivityByItemIdQuery;
    }
}
