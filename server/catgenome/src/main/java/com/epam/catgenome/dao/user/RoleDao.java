/*
 * MIT License
 *
 * Copyright (c) 2018 EPAM Systems
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

package com.epam.catgenome.dao.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.security.Role;

public class RoleDao extends NamedParameterJdbcDaoSupport {
    private String loadRolesByUserIdsQuery;

    public List<Role> loadRolesList(List<Long> userRoleIds) {
        return null;
    }

    public Collection<Role> loadAllRoles(boolean b) {
        return null;
    }

    public Map<Long, List<Role>> loadRoles(List<Long> userIds) {
        String query = DaoHelper.replaceInClause(loadRolesByUserIdsQuery, userIds.size());
        Map<Long, List<Role>> result = new HashMap<>();

        RowMapper<Role> rowMapper = RoleParameters.getRowMapper();
        getJdbcTemplate().query(query, rs -> {
            Role role = rowMapper.mapRow(rs, 0);
            result.merge(rs.getLong(UserDao.UserParameters.USER_ID.name()),
                         new ArrayList<>(Collections.singletonList(role)),
                         (l1, l2) -> { l1.addAll(l2); return l1; });
        }, userIds.toArray(new Long[userIds.size()]));

        return result;
    }

    enum RoleParameters {
        ROLE_ID,
        ROLE_NAME,
        ROLE_PREDEFINED,
        ROLE_USER_DEFAULT;

        private static RowMapper<Role> getRowMapper() {
            return (rs, rowNum) -> {
                Role role = new Role();
                return parseRole(rs, role);
            };
        }

        static Role parseRole(ResultSet rs, Role role) throws SQLException {
            role.setId(rs.getLong(ROLE_ID.name()));
            role.setName(rs.getString(ROLE_NAME.name()));
            role.setPredefined(rs.getBoolean(ROLE_PREDEFINED.name()));
            role.setUserDefault(rs.getBoolean(ROLE_USER_DEFAULT.name()));

            return role;
        }
    }

    @Required
    public void setLoadRolesByUserIdsQuery(String loadRolesByUserIdsQuery) {
        this.loadRolesByUserIdsQuery = loadRolesByUserIdsQuery;
    }
}
