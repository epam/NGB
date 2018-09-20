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
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.security.Role;

public class RoleDao extends NamedParameterJdbcDaoSupport {
    private static final String LIST_PARAM = "LIST";

    private String roleSequence;

    private String loadRolesByUserIdsQuery;
    private String createRoleQuery;
    private String updateRoleQuery;
    private String deleteRoleQuery;
    private String loadAllRolesQuery;
    private String loadRolesQuery;
    private String loadRoleQuery;
    private String loadRoleByNameQuery;
    private String deleteRolesReferencesQuery;

    @Autowired
    private DaoHelper daoHelper;

    @Transactional(propagation = Propagation.MANDATORY)
    public Role createRole(String name) {
        return createRole(name, false, false);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Role createRole(String name, boolean predefined, boolean userDefault) {
        Role role = new Role();

        role.setName(name);
        role.setId(daoHelper.createId(roleSequence));
        role.setUserDefault(userDefault);
        role.setPredefined(predefined);

        getNamedParameterJdbcTemplate().update(createRoleQuery, RoleParameters.getParameters(role));
        return role;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateRole(Role role) {
        getNamedParameterJdbcTemplate().update(updateRoleQuery, RoleParameters.getParameters(role));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteRole(Long id) {
        getJdbcTemplate().update(deleteRoleQuery, id);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteRoleReferences(Long id) {
        getJdbcTemplate().update(deleteRolesReferencesQuery, id);
    }

    public List<Role> loadRoles(List<Long> roleIds) {
        return getNamedParameterJdbcTemplate().query(loadRolesQuery,
                                                     new MapSqlParameterSource(LIST_PARAM, roleIds),
                                                     RoleParameters.getRowMapper());
    }

    public Collection<Role> loadAllRoles() {
        return getJdbcTemplate().query(loadAllRolesQuery, RoleParameters.getRowMapper());
    }

    public Map<Long, List<Role>> loadRolesByUserIds(List<Long> userIds) {
        String query = DaoHelper.replaceInClause(loadRolesByUserIdsQuery, userIds.size());
        Map<Long, List<Role>> result = new HashMap<>();

        RowMapper<Role> rowMapper = RoleParameters.getRowMapper();
        getNamedParameterJdbcTemplate().query(query, new MapSqlParameterSource(LIST_PARAM, userIds),
                                              rs -> {
                                                        Role role = rowMapper.mapRow(rs, 0);
                                                        result.merge(rs.getLong(UserDao.UserParameters.USER_ID.name()),
                                                                     new ArrayList<>(Collections.singletonList(role)),
                                                                     (l1, l2) -> { l1.addAll(l2); return l1; });
                                                    });

        return result;
    }

    public Optional<Role> loadRole(Long id) {
        return loadRoleByParameter(id, loadRoleQuery);
    }

    public Optional<Role> loadRoleByName(String name) {
        return loadRoleByParameter(name, loadRoleByNameQuery);
    }

    private Optional<Role> loadRoleByParameter(Object parameter, String loadRoleQuery) {
        List<Role> result =
            getJdbcTemplate().query(loadRoleQuery, RoleParameters.getRowMapper(), parameter);
        return result.stream().findFirst();
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

        private static MapSqlParameterSource getParameters(Role role) {
            MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(ROLE_ID.name(), role.getId());
            params.addValue(ROLE_NAME.name(), role.getName());
            params.addValue(ROLE_PREDEFINED.name(), role.isPredefined());
            params.addValue(ROLE_USER_DEFAULT.name(), role.isUserDefault());

            return params;
        }
    }

    @Required
    public void setLoadRolesByUserIdsQuery(String loadRolesByUserIdsQuery) {
        this.loadRolesByUserIdsQuery = loadRolesByUserIdsQuery;
    }

    @Required
    public void setRoleSequence(String roleSequence) {
        this.roleSequence = roleSequence;
    }

    @Required
    public void setCreateRoleQuery(String createRoleQuery) {
        this.createRoleQuery = createRoleQuery;
    }

    @Required
    public void setUpdateRoleQuery(String updateRoleQuery) {
        this.updateRoleQuery = updateRoleQuery;
    }

    @Required
    public void setDeleteRoleQuery(String deleteRoleQuery) {
        this.deleteRoleQuery = deleteRoleQuery;
    }

    @Required
    public void setLoadAllRolesQuery(String loadAllRolesQuery) {
        this.loadAllRolesQuery = loadAllRolesQuery;
    }

    @Required
    public void setLoadRolesQuery(String loadRolesQuery) {
        this.loadRolesQuery = loadRolesQuery;
    }

    @Required
    public void setLoadRoleQuery(String loadRoleQuery) {
        this.loadRoleQuery = loadRoleQuery;
    }

    @Required
    public void setLoadRoleByNameQuery(String loadRoleByNameQuery) {
        this.loadRoleByNameQuery = loadRoleByNameQuery;
    }

    @Required
    public void setDeleteRolesReferencesQuery(String deleteRolesReferencesQuery) {
        this.deleteRolesReferencesQuery = deleteRolesReferencesQuery;
    }
}
