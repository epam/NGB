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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.epam.catgenome.entity.security.NgbSecurityGroup;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.security.NgbUser;
import com.epam.catgenome.exception.NgbException;
import com.epam.catgenome.entity.user.DefaultRoles;
import com.epam.catgenome.entity.user.Role;
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
    private String loadUsersByNamesQuery;
    private String loadUserByNameAndGroupQuery;
    private String deleteUserGroupsQuery;
    private String deleteUserGroupByUserIdAndGroupIdQuery;
    private String loadUserListQuery;
    private String deleteRoleFromUserQuery;

    private String userSequence;
    private String groupSequence;

    @Autowired
    private DaoHelper daoHelper;

    /**
     * Loads user by name with roles.
     *
     * @param userName
     * @return user with roles
     */
    public NgbUser loadUserByName(String userName) {
        List<NgbUser> items = getJdbcTemplate().query(loadUserByNameQuery,
                UserParameters.getUserWithRolesAndGroupsExtractor(), userName.toLowerCase(), userName.toLowerCase());
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
        if (!CollectionUtils.isEmpty(user.getGroups())) {
            insertUserGroups(user.getId(), user.getGroups(), false);
        }
        return user;
    }

    public List<NgbUser> loadAllUsers() {
        return getJdbcTemplate().query(loadAllUsersQuery, UserParameters.getUserWithRolesAndGroupsExtractor());
    }

    public List<NgbUser> loadUsersByNames(List<String> names) {
        if (names.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> args = names.stream().map(String::toLowerCase).collect(Collectors.toList());
        String query = DaoHelper.getQueryFilledWithStringArray(loadUsersByNamesQuery, args);
        return getJdbcTemplate().query(query, UserParameters.getUserWithRolesAndGroupsExtractor());
    }

    public NgbUser loadUserById(Long id) {
        List<NgbUser> items =
            getJdbcTemplate().query(loadUserByIdQuery, UserParameters.getUserWithRolesAndGroupsExtractor(), id, id);
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
    public NgbUser updateUser(final NgbUser user) {
        getNamedParameterJdbcTemplate().update(updateUserQuery, UserParameters.getParameters(user));
        insertUserGroups(user.getId(), user.getGroups(), true);
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

    public List<NgbUser> findUsers(String prefix) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(UserParameters.USER_NAME.name(), daoHelper.escapeUnderscore(prefix.toLowerCase() + "%"));
        return getNamedParameterJdbcTemplate().query(findUsersByPrefixQuery, params,
                                                     UserParameters.getUserWithRolesAndGroupsExtractor());
    }

    public Map<Long, List<NgbSecurityGroup>> loadGroupsByUsersIds(List<Long> userIds) {
        String query = DaoHelper.replaceInClause(loadGroupsByUserIdsQuery, userIds.size());
        return getJdbcTemplate().query(query, GroupParameters.getGroupByUserExtractor(), userIds.toArray());
    }

    public List<NgbSecurityGroup> loadAllGroups() {
        return getJdbcTemplate().query(loadAllGroupsQuery, GroupParameters.getGroupRowMapper());
    }

    public List<NgbSecurityGroup> findGroups(String prefix) {
        return getJdbcTemplate().query(findGroupsQuery, GroupParameters.getGroupRowMapper(),
                prefix.toLowerCase() + "%");
    }

    public List<NgbUser> loadUsersByGroup(String group) {
        return getJdbcTemplate().query(loadUsersByGroupQuery, UserParameters.getUserWithRolesAndGroupsExtractor(),
                                       group.toLowerCase(), group.toLowerCase());
    }

    public List<NgbSecurityGroup> loadExistingGroupsFromList(List<String> groups) {
        if(CollectionUtils.isNotEmpty(groups)){
            String query = DaoHelper.replaceInClause(loadExistingGroupsFromListQuery, groups.size());
            return getJdbcTemplate().query(query, GroupParameters.getGroupRowMapper(), groups.toArray());
        }
        return Collections.emptyList();
    }

    public boolean isUserInGroup(String userName, String group) {
        List<NgbUser> user = getJdbcTemplate().query(loadUserByNameAndGroupQuery,
                UserParameters.getUserRowMapper(), userName, group,  userName, group);
        return !CollectionUtils.isEmpty(user);
    }

    public List<NgbUser> loadUsersList(List<Long> userIds) {
        return getNamedParameterJdbcTemplate().query(loadUserListQuery,
                RoleDao.RoleParameters.getIdListParameters(userIds),
                UserParameters.getUserWithRolesAndGroupsExtractor());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void assignRoleToUsers(Long roleId, List<Long> userIds) {
        processBatchQuery(addRoleToUserQuery, roleId, userIds);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void removeRoleFromUsers(Long roleId, List<Long> userIds) {
        processBatchQuery(deleteRoleFromUserQuery, roleId, userIds);
    }

    private void processBatchQuery(String query, Long roleId, List<Long> userIds) {
        MapSqlParameterSource[] batchParameters = userIds.stream()
                .map(id -> UserParameters.getUserRoleParameters(id, roleId))
                .toArray(MapSqlParameterSource[]::new);
        getNamedParameterJdbcTemplate().batchUpdate(query, batchParameters);
    }

    private void insertUserGroups(final Long id, final List<String> groups, final boolean cleanup) {
        final List<NgbSecurityGroup> existingGroups = loadExistingGroupsFromList(groups);
        createNotExistingGroups(groups, existingGroups);

        final List<NgbSecurityGroup> currentUsersGroups = loadGroupsByUsersIds(Collections.singletonList(id))
                .getOrDefault(id, Collections.emptyList());
        createNonExistingUserGroups(id, currentUsersGroups, groups, existingGroups);
        if (cleanup) {
            removeNotPresentUserGroups(groups, id, currentUsersGroups);
        }
    }

    private void removeNotPresentUserGroups(final List<String> groups, final Long userId,
                                            final List<NgbSecurityGroup> currentUsersGroups) {
        final MapSqlParameterSource[] userGroupParams = currentUsersGroups.stream()
                .filter(usersGroup -> !groups.contains(usersGroup.getGroupName()))
                .map(NgbSecurityGroup::getId)
                .map(groupId -> buildUserGroupParameter(userId, groupId))
                .toArray(MapSqlParameterSource[]::new);

        getNamedParameterJdbcTemplate().batchUpdate(deleteUserGroupByUserIdAndGroupIdQuery, userGroupParams);
    }

    private MapSqlParameterSource buildUserGroupParameter(final Long userId, final Long groupId) {
        final MapSqlParameterSource param = new MapSqlParameterSource();
        param.addValue(GroupParameters.USER_ID.name(), userId);
        param.addValue(GroupParameters.GROUP_ID.name(), groupId);
        return param;
    }

    private List<NgbSecurityGroup> getExistingNewGroups(final List<NgbSecurityGroup> userGroups,
                                                        final List<String> groups,
                                                        final List<NgbSecurityGroup> existingGroups) {
        final Set<String> userGroupNames = userGroups.stream()
                .map(NgbSecurityGroup::getGroupName)
                .collect(Collectors.toSet());
        final Set<String> newUserGroups = groups.stream()
                .filter(group -> !userGroupNames.contains(group))
                .collect(Collectors.toSet());
        return existingGroups
                .stream()
                .filter(group -> newUserGroups.contains(group.getGroupName()))
                .collect(Collectors.toList());
    }

    private void createNotExistingGroups(final List<String> groups, final List<NgbSecurityGroup> existingGroups) {
        final Set<String> existingGroupsNames = existingGroups.stream()
                .map(NgbSecurityGroup::getGroupName)
                .collect(Collectors.toSet());

        final List<String> newGroups = groups.stream()
                .filter(g -> !existingGroupsNames.contains(g))
                .collect(Collectors.toList());
        final List<Long> ids = daoHelper.createIds(groupSequence, newGroups.size());

        final MapSqlParameterSource[] groupParams = new MapSqlParameterSource[newGroups.size()];
        for (int i = 0; i < newGroups.size(); i++) {
            final NgbSecurityGroup group = new NgbSecurityGroup(ids.get(i), newGroups.get(i));
            final MapSqlParameterSource param = new MapSqlParameterSource();
            param.addValue(GroupParameters.GROUP_ID.name(), group.getId());
            param.addValue(GroupParameters.GROUP_NAME.name(), group.getGroupName());
            groupParams[i] = param;

            existingGroups.add(group);
        }

        getNamedParameterJdbcTemplate().batchUpdate(insertGroupQuery, groupParams);
    }

    private void createNonExistingUserGroups(final Long userId, final List<NgbSecurityGroup> userGroups,
                                             final List<String> groups, final List<NgbSecurityGroup> existingGroups) {
        final MapSqlParameterSource[] userGroupParams =
                getExistingNewGroups(userGroups, groups, existingGroups).stream()
                        .map(group -> buildUserGroupParameter(userId, group.getId()))
                        .toArray(MapSqlParameterSource[]::new);

        getNamedParameterJdbcTemplate().batchUpdate(insertUserGroupQuery, userGroupParams);
    }

    enum GroupParameters {
        ID,
        NAME,
        USER_ID,
        GROUP_ID,
        GROUP_NAME;

        private static RowMapper<NgbSecurityGroup> getGroupRowMapper() {
            return (rs, rowNum) -> NgbSecurityGroup
                        .builder()
                        .id(rs.getLong(ID.name()))
                        .groupName(rs.getString(NAME.name()))
                        .build();
        }

        private static ResultSetExtractor<Map<Long, List<NgbSecurityGroup>>> getGroupByUserExtractor() {
            return rs -> {
                Map<Long, List<NgbSecurityGroup>> result = new HashMap<>();
                while (rs.next()) {
                    result.merge(rs.getLong(UserParameters.USER_ID.name()),
                            new ArrayList<>(Collections.singletonList(NgbSecurityGroup
                                    .builder()
                                    .id(rs.getLong(GROUP_ID.name()))
                                    .groupName(rs.getString(GROUP_NAME.name()))
                                    .build())), (l1, l2) -> {
                            l1.addAll(l2);
                            return l1;
                        });
                }
                return result;
            };
        }
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

        private static ResultSetExtractor<List<NgbUser>> getUserWithRolesAndGroupsExtractor() {
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

                    rs.getLong(GroupParameters.GROUP_ID.name());
                    if (!rs.wasNull()) {
                        user.getGroups().add(rs.getString(GroupParameters.GROUP_NAME.name()));
                    }

                    rs.getLong(RoleDao.RoleParameters.ROLE_ID.name());
                    if (!rs.wasNull()) {
                        Role role = new Role();
                        RoleDao.RoleParameters.parseRole(rs, role);
                        user.getRoles().add(role);
                    }
                }
                return new ArrayList<>(users.values());
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
    public void setLoadUsersByNamesQuery(String loadUsersByNamesQuery) {
        this.loadUsersByNamesQuery = loadUsersByNamesQuery;
    }

    @Required
    public void setLoadUserByNameAndGroupQuery(String loadUserByNameAndGroupQuery) {
        this.loadUserByNameAndGroupQuery = loadUserByNameAndGroupQuery;
    }

    @Required
    public void setDeleteUserGroupsQuery(String deleteUserGroupsQuery) {
        this.deleteUserGroupsQuery = deleteUserGroupsQuery;
    }

    @Required
    public void setDeleteUserGroupByUserIdAndGroupIdQuery(String deleteUserGroupByUserIdAndGroupIdQuery) {
        this.deleteUserGroupByUserIdAndGroupIdQuery = deleteUserGroupByUserIdAndGroupIdQuery;
    }

    @Required
    public void setLoadUserListQuery(String loadUserListQuery) {
        this.loadUserListQuery = loadUserListQuery;
    }

    @Required
    public void setDeleteRoleFromUserQuery(String deleteRoleFromUserQuery) {
        this.deleteRoleFromUserQuery = deleteRoleFromUserQuery;
    }
}
