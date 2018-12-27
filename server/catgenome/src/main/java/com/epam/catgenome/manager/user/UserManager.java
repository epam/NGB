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

package com.epam.catgenome.manager.user;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.epam.catgenome.controller.vo.NgbUserVO;
import com.epam.catgenome.dao.user.RoleDao;
import com.epam.catgenome.entity.security.NgbSecurityGroup;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.user.UserDao;
import com.epam.catgenome.entity.security.NgbUser;
import com.epam.catgenome.entity.user.DefaultRoles;
import com.epam.catgenome.entity.user.Role;
import com.epam.catgenome.security.UserContext;

@Service
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
public class UserManager {

    private RoleDao roleDao;
    private UserDao userDao;

    @Autowired
    public UserManager(RoleDao roleDao, UserDao userDao) {
        this.roleDao = roleDao;
        this.userDao = userDao;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public NgbUser createUser(String name, List<Long> roles,
                              List<String> groups, Map<String, String> attributes) {
        Assert.isTrue(StringUtils.isNotBlank(name), MessageHelper.getMessage(
            MessagesConstants.ERROR_USER_NAME_REQUIRED));
        String userName = name.trim().toUpperCase();

        NgbUser loadedUser = userDao.loadUserByName(userName);
        Assert.isNull(loadedUser, MessageHelper.getMessage(MessagesConstants.ERROR_USER_NAME_EXISTS, name));

        NgbUser user = new NgbUser(userName);
        List<Long> userRoles = getNewUserRoles(roles);
        user.setRoles(roleDao.loadRoles(userRoles));
        user.setGroups(groups);
        user.setAttributes(attributes);

        return userDao.createUser(user, userRoles);
    }

    private List<Long> getNewUserRoles(List<Long> roles) {
        checkAllRolesPresent(roles);
        List<Long> userRoles = CollectionUtils.isEmpty(roles)
                ? roleDao.loadDefaultRoles().stream()
                    .map(Role::getId)
                    .collect(Collectors.toList())
                : roles;
        Long roleUserId = DefaultRoles.ROLE_USER.getRole().getId();
        if (userRoles.stream().noneMatch(roleUserId::equals)) {
            userRoles.add(roleUserId);
        }
        return userRoles;
    }

    private void checkAllRolesPresent(List<Long> roles) {
        if (CollectionUtils.isEmpty(roles)) {
            return;
        }
        Set<Long> presentIds = roleDao.loadRoles(roles).stream().map(Role::getId).collect(Collectors.toSet());
        roles.forEach(roleId -> Assert.isTrue(presentIds.contains(roleId), MessageHelper.getMessage(
            MessagesConstants.ERROR_ROLE_ID_NOT_FOUND, roleId)));
    }

    public UserContext loadUserContext(String name) {
        NgbUser pipelineUser = userDao.loadUserByName(name);
        Assert.notNull(pipelineUser, MessageHelper.getMessage(MessagesConstants.ERROR_USER_NAME_NOT_FOUND, name));
        return new UserContext(pipelineUser);
    }

    public NgbUser loadUserByName(String name) {
        return userDao.loadUserByName(name);
    }

    public List<NgbUser> loadAllUsers() {
        return userDao.loadAllUsers();
    }

    public List<NgbUser> loadUsersByNames(List<String> names) {
        return userDao.loadUsersByNames(names);
    }

    public NgbUser loadUserById(Long id) {
        NgbUser user =  userDao.loadUserById(id);
        Assert.notNull(user, MessageHelper.getMessage(MessagesConstants.ERROR_USER_ID_NOT_FOUND, id));
        return user;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public NgbUser deleteUser(Long id) {
        NgbUser userContext = loadUserById(id);
        userDao.deleteUserRoles(id);
        userDao.deleteUserGroups(id);
        userDao.deleteUser(id);
        return userContext;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public NgbUser updateUser(Long id, List<Long> roles) {
        loadUserById(id);
        updateUserRoles(id, roles);
        return loadUserById(id);
    }

    private void updateUserRoles(Long id, List<Long> roles) {
        checkAllRolesPresent(roles);
        userDao.deleteUserRoles(id);
        if (!CollectionUtils.isEmpty(roles)) {
            userDao.insertUserRoles(id, roles);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public NgbUser updateUserSAMLInfo(Long id, String name, List<Long> roles, List<String> groups,
                                           Map<String, String> attributes) {
        NgbUser user = loadUserById(id);
        if (attributes != null && groups != null && userUpdateRequired(groups, attributes, user)) {
            user.setUserName(name);
            user.setGroups(groups);
            user.setAttributes(attributes);
            userDao.updateUser(user);
        }

        updateUserRoles(id, roles);
        return loadUserById(id);
    }

    public boolean userUpdateRequired(List<String> groups, Map<String, String> attributes,
                                      NgbUser user) {

        return !CollectionUtils.isEqualCollection(user.getGroups(), groups)
               || !CollectionUtils.isEqualCollection(user.getAttributes().entrySet(), attributes.entrySet());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public NgbUser createUser(NgbUserVO userVO) {
        return createUser(userVO.getUserName(), userVO.getRoleIds(), null, null);
    }

    /**
     * Searches for user by prefix. Search is performed in user names and all attributes values.
     * @param prefix to search for
     * @return users matching prefix
     */
    public List<NgbUser> findUsers(String prefix) {
        Assert.isTrue(StringUtils.isNotBlank(prefix), MessageHelper.getMessage(MessagesConstants.ERROR_NULL_PARAM));
        List<NgbUser> users = userDao.findUsers(prefix);
        List<Long> userIds = users.stream().map(NgbUser::getId).collect(Collectors.toList());
        Map<Long, List<NgbSecurityGroup>> groups = userDao.loadGroupsByUsersIds(userIds);
        Map<Long, List<Role>> roles = roleDao.loadRolesByUserIds(userIds);

        users.forEach(u -> {
            u.setGroups(groups.get(u.getId()).stream()
                    .map(NgbSecurityGroup::getGroupName)
                    .collect(Collectors.toList()));
            u.setRoles(roles.get(u.getId()));
        });

        return users;
    }

    /**
     * Searches for a user group name by a prefix
     * @param prefix a prefix of a group name to search
     * @return a loaded {@code List} of group name that satisfy the prefix
     */
    public List<String> findGroups(String prefix) {
        if (StringUtils.isBlank(prefix)) {
            return userDao.loadAllGroups().stream().map(NgbSecurityGroup::getGroupName).collect(Collectors.toList());
        }
        return userDao.findGroups(prefix).stream().map(NgbSecurityGroup::getGroupName).collect(Collectors.toList());
    }

    /**
     * Loads a {@code UserContext} instances from the database specified by group
     * @param group a user group name
     * @return a loaded {@code Collection} of {@code UserContext} instances from the database
     */
    public Collection<NgbUser> loadUsersByGroup(String group) {
        Assert.isTrue(StringUtils.isNotBlank(group),
                      MessageHelper.getMessage(MessageHelper.getMessage(MessagesConstants.ERROR_NULL_PARAM)));
        return userDao.loadUsersByGroup(group);
    }

    /**
     * Checks whether a specific user is a member of a specific group
     * @param userName a name of {@code UserContext}
     * @param group a user group name
     * @return true if a specific user is a member of a specific group
     */
    public boolean checkUserByGroup(String userName, String group) {
        Assert.isTrue(StringUtils.isNotBlank(group), MessageHelper.getMessage(MessagesConstants.ERROR_NULL_PARAM));
        return userDao.isUserInGroup(userName, group);
    }

}
