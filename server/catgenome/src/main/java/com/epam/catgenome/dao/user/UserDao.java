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

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.security.NgbUser;
import com.epam.catgenome.exception.NgbException;
import com.epam.catgenome.security.DefaultRoles;
import com.epam.catgenome.security.Role;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

public class UserDao extends NamedParameterJdbcDaoSupport {
    private String findUsersByPrefixQuery;
    private String loadGroupsByUserIdsQuery;
    private String createUserQuery;
    private String addRoleToUserQuery;
    private String insertGroupQuery;
    private String insertUserGroupQuery;
    private String loadExistingGroupsFromListQuery;
    private String loadUserByNameQuery;
    private String loadUserByIdQuery;
    private String loadAllUsersQuery;
    private String updateUserQuery;
    private String deleteUserRolesQuery;
    private String deleteUserQuery;
    private String loadAllGroupsQuery;
    private String findGroupsQuery;
    private String loadUsersByGroupQuery;
    private String loadUsesByNamesQuery;
    private String loadUserByNameAndGroupQuery;
    private String deleteUserGroupsQuery;

    private String userSequence;
    private String groupSequence;

    @Autowired
    private DaoHelper daoHelper;

    /**
     * Loads user by name with roles. Groups should be explicitly loaded with loadGroups method call
     *
     * @param userName
     * @return user with roles
     */
    public NgbUser loadUserByName(String userName) {
        Collection<NgbUser> items = getJdbcTemplate().query(loadUserByNameQuery,
                                    UserParameters.getUserWithRolesExtractor(), userName.toLowerCase());
        if (CollectionUtils.isEmpty(items)) {
            return null;
        } else {
            return items.stream().findFirst().orElse(null);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public NgbUser createUser(NgbUser user, List<Long> roles) {
        user.setId(daoHelper.createId(userSequence));
        getNamedParameterJdbcTemplate().update(createUserQuery, UserParameters.getParameters(user));

        List<Long> appliedRoles = new ArrayList<>();
        if (CollectionUtils.isEmpty(roles)) {
            appliedRoles.add(DefaultRoles.ROLE_USER.getId());
        } else {
            appliedRoles.addAll(roles);
        }

        insertUserRoles(user.getId(), appliedRoles);
        insertUserGroups(user.getId(), user.getGroups());
        return user;
    }

    private void insertUserGroups(Long id, List<String> groups) {
        List<Pair<Long, String>> existingGroups = loadExistingGroupsFromList(groups);
        Set<String> existingGroupNames = existingGroups.stream().map(Pair::getRight).collect(Collectors.toSet());

        List<String> newGroups = groups.stream().filter(g -> !existingGroupNames.contains(g))
            .collect(Collectors.toList());
        List<Long> ids = daoHelper.createIds(groupSequence, newGroups.size());

        MapSqlParameterSource[] groupParams = new MapSqlParameterSource[newGroups.size()];
        for (int i = 0; i < newGroups.size(); i++) {
            MapSqlParameterSource param = new MapSqlParameterSource();
            param.addValue(GroupParameters.GROUP_ID.name(), ids.get(i));
            param.addValue(GroupParameters.GROUP_NAME.name(), newGroups.get(i));
            groupParams[i] = param;
            existingGroups.add(new ImmutablePair<>(ids.get(i), newGroups.get(i)));
        }

        getNamedParameterJdbcTemplate().batchUpdate(insertGroupQuery, groupParams);

        MapSqlParameterSource[] userGroupParams = existingGroups.stream().map(g -> {
            MapSqlParameterSource param = new MapSqlParameterSource();
            param.addValue(GroupParameters.USER_ID.name(), id);
            param.addValue(GroupParameters.GROUP_ID.name(), g.getLeft());
            return param;
        }).toArray(MapSqlParameterSource[]::new);

        getNamedParameterJdbcTemplate().batchUpdate(insertUserGroupQuery, userGroupParams);
    }

    public Collection<NgbUser> loadAllUsers() {
        return getJdbcTemplate().query(loadAllUsersQuery, UserParameters.getUserWithRolesExtractor());
    }

    public Collection<NgbUser> loadUsersByNames(Collection<String> names) {
        if (names.isEmpty()) {
            return Collections.emptyList();
        }

        String query = DaoHelper.replaceInClause(loadUsesByNamesQuery, names.size());
        return getJdbcTemplate().query(query, UserParameters.getUserWithRolesExtractor(),
                                       names.stream().map(String::toLowerCase).toArray());
    }

    public NgbUser loadUserById(Long id) {
        Collection<NgbUser> items =
            getJdbcTemplate().query(loadUserByIdQuery, UserParameters.getUserWithRolesExtractor(), id);
        return items.stream().findFirst().orElse(null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteUser(Long id) {
        getJdbcTemplate().update(deleteUserQuery, id);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteUserGroups(Long id) {
        getJdbcTemplate().update(deleteUserGroupsQuery, id);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteUserRoles(Long id) {
        getJdbcTemplate().update(deleteUserRolesQuery, id);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void insertUserRoles(Long userId, List<Long> roleIds) {
        MapSqlParameterSource[] batchParameters = roleIds.stream()
            .map(id -> UserParameters.getUserRoleParameters(userId, id))
            .toArray(MapSqlParameterSource[]::new);

        getNamedParameterJdbcTemplate().batchUpdate(addRoleToUserQuery, batchParameters);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public NgbUser updateUser(NgbUser user) {
        getNamedParameterJdbcTemplate().update(updateUserQuery, UserParameters.getParameters(user));
        return user;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public NgbUser updateUserRoles(NgbUser user, List<Long> roleIds) {
        deleteUserRoles(user.getId());
        if (!CollectionUtils.isEmpty(roleIds)) {
            insertUserRoles(user.getId(), roleIds);
        }
        return user;
    }

    public Collection<NgbUser> findUsers(String prefix) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(UserParameters.USER_NAME.name(), daoHelper.escapeUnderscore(prefix.toLowerCase() + "%"));
        return getNamedParameterJdbcTemplate().query(findUsersByPrefixQuery, params,
                                                     UserParameters.getUserWithRolesExtractor());
    }

    public Map<Long, List<String>> loadGroups(List<Long> userIds) {
        String query = DaoHelper.replaceInClause(loadGroupsByUserIdsQuery, userIds.size());
        Map<Long, List<String>> result = new HashMap<>();
        getJdbcTemplate().query(query, rs -> {
            String groupName = rs.getString(GroupParameters.GROUP_NAME.name());
            result.merge(rs.getLong(UserParameters.USER_ID.name()),
                         new ArrayList<>(Collections.singletonList(groupName)),
                         (l1, l2) -> { l1.addAll(l2); return l1; });
        }, userIds.toArray(new Object[0]));

        return result;
    }

    public List<String> loadAllGroups() {
        return getJdbcTemplate().query(loadAllGroupsQuery, new SingleColumnRowMapper<>());
    }

    public List<String> findGroups(String prefix) {
        return getJdbcTemplate().query(findGroupsQuery, new SingleColumnRowMapper<>(), prefix.toLowerCase() + "%");
    }

    public Collection<NgbUser> loadUsersByGroup(String group) {
        return getJdbcTemplate().query(loadUsersByGroupQuery, UserParameters.getUserWithRolesExtractor(),
                                       group.toLowerCase());
    }

    public List<Pair<Long, String>> loadExistingGroupsFromList(List<String> groups) {
        String query = DaoHelper.replaceInClause(loadExistingGroupsFromListQuery, groups.size());
        return getJdbcTemplate().query(query,
                                       (rs, i) -> new ImmutablePair<>(rs.getLong(1), rs.getString(2)),
                                       groups.toArray(new Object[0]));
    }

    public boolean isUserInGroup(String userName, String group) {
        List<NgbUser> user = getJdbcTemplate().query(loadUserByNameAndGroupQuery,
                                                     UserParameters.getUserRowMapper(), userName, group);
        return !CollectionUtils.isEmpty(user);
    }

    enum GroupParameters {
        USER_ID,
        GROUP_ID,
        GROUP_NAME
    }

    enum UserParameters {
        USER_ID,
        USER_NAME,
        ATTRIBUTES;

        private static MapSqlParameterSource getUserRoleParameters(Long userId, Long roleId) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(UserParameters.USER_ID.name(), userId);
            params.addValue(RoleDao.RoleParameters.ROLE_ID.name(), roleId);
            return params;
        }

        private static MapSqlParameterSource getParameters(NgbUser user) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(USER_ID.name(), user.getId());
            params.addValue(USER_NAME.name(), user.getUserName());
            params.addValue(ATTRIBUTES.name(), convertDataToJsonStringForQuery(user.getAttributes()));
            return params;
        }

        private static RowMapper<NgbUser> getUserRowMapper() {
            return (rs, rowNum) -> parseUser(rs, rs.getLong(USER_ID.name()));
        }

        private static ResultSetExtractor<Collection<NgbUser>> getUserWithRolesExtractor() {
            return (rs) -> {
                Map<Long, NgbUser> users = new HashMap<>();
                while (rs.next()) {
                    Long userId = rs.getLong(USER_ID.name());
                    NgbUser user = users.compute(userId, (id, u) -> {
                        if (u == null) {
                            try {
                                return parseUser(rs, userId);
                            } catch (SQLException e) {
                                throw new NgbException(e);
                            }
                        }

                        return u;
                    });

                    rs.getLong(RoleDao.RoleParameters.ROLE_ID.name());
                    if (!rs.wasNull()) {
                        Role role = new Role();
                        RoleDao.RoleParameters.parseRole(rs, role);
                        user.getRoles().add(role);
                    }
                }
                return users.values();
            };
        }

        static NgbUser parseUser(ResultSet rs, Long userId) throws SQLException {
            NgbUser user = new NgbUser();
            user.setId(userId);
            user.setUserName(rs.getString(USER_NAME.name()));

            Map<String, String> data = parseData(rs.getString(ATTRIBUTES.name()));
            if (!rs.wasNull()) {
                user.setAttributes(data);
            }

            return user;
        }

        public static Map<String, String> parseData(String data) {
            if (StringUtils.isBlank(data)) {
                return null;
            }

            try {
                return JsonMapper.getInstance().readValue(data, new TypeReference<Map<String, String>>() {});
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not parse JSON data", e);
            }
        }

        private static String convertDataToJsonStringForQuery(Map<String, String> data) {
            if (data == null || data.isEmpty()) {
                return null;
            }

            try {
                return JsonMapper.getInstance().writeValueAsString(data);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    @Required
    public void setFindUsersByPrefixQuery(String findUsersByPrefixQuery) {
        this.findUsersByPrefixQuery = findUsersByPrefixQuery;
    }

    @Required
    public void setLoadGroupsByUserIdsQuery(String loadGroupsByUserIdsQuery) {
        this.loadGroupsByUserIdsQuery = loadGroupsByUserIdsQuery;
    }

    @Required
    public void setUserSequence(String userSequence) {
        this.userSequence = userSequence;
    }

    @Required
    public void setGroupSequence(String groupSequence) {
        this.groupSequence = groupSequence;
    }

    @Required
    public void setCreateUserQuery(String createUserQuery) {
        this.createUserQuery = createUserQuery;
    }

    @Required
    public void setAddRoleToUserQuery(String addRoleToUserQuery) {
        this.addRoleToUserQuery = addRoleToUserQuery;
    }

    @Required
    public void setInsertGroupQuery(String insertGroupQuery) {
        this.insertGroupQuery = insertGroupQuery;
    }

    @Required
    public void setLoadExistingGroupsFromListQuery(String loadExistingGroupsFromListQuery) {
        this.loadExistingGroupsFromListQuery = loadExistingGroupsFromListQuery;
    }

    @Required
    public void setInsertUserGroupQuery(String insertUserGroupQuery) {
        this.insertUserGroupQuery = insertUserGroupQuery;
    }

    @Required
    public void setLoadUserByNameQuery(String loadUserByNameQuery) {
        this.loadUserByNameQuery = loadUserByNameQuery;
    }

    @Required
    public void setLoadAllUsersQuery(String loadAllUsersQuery) {
        this.loadAllUsersQuery = loadAllUsersQuery;
    }

    @Required
    public void setLoadUserByIdQuery(String loadUserByIdQuery) {
        this.loadUserByIdQuery = loadUserByIdQuery;
    }

    @Required
    public void setUpdateUserQuery(String updateUserQuery) {
        this.updateUserQuery = updateUserQuery;
    }

    @Required
    public void setDeleteUserRolesQuery(String deleteUserRolesQuery) {
        this.deleteUserRolesQuery = deleteUserRolesQuery;
    }

    @Required
    public void setDeleteUserQuery(String deleteUserQuery) {
        this.deleteUserQuery = deleteUserQuery;
    }

    @Required
    public void setLoadAllGroupsQuery(String loadAllGroupsQuery) {
        this.loadAllGroupsQuery = loadAllGroupsQuery;
    }

    @Required
    public void setFindGroupsQuery(String findGroupsQuery) {
        this.findGroupsQuery = findGroupsQuery;
    }

    @Required
    public void setLoadUsersByGroupQuery(String loadUsersByGroupQuery) {
        this.loadUsersByGroupQuery = loadUsersByGroupQuery;
    }

    @Required
    public void setLoadUsesByNamesQuery(String loadUsesByNamesQuery) {
        this.loadUsesByNamesQuery = loadUsesByNamesQuery;
    }

    @Required
    public void setLoadUserByNameAndGroupQuery(String loadUserByNameAndGroupQuery) {
        this.loadUserByNameAndGroupQuery = loadUserByNameAndGroupQuery;
    }

    @Required
    public void setDeleteUserGroupsQuery(String deleteUserGroupsQuery) {
        this.deleteUserGroupsQuery = deleteUserGroupsQuery;
    }
}
