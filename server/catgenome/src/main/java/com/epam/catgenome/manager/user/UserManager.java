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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.user.RoleDao;
import com.epam.catgenome.dao.user.UserDao;
import com.epam.catgenome.entity.security.NgbUser;
import com.epam.catgenome.manager.AuthManager;
import com.epam.catgenome.security.DefaultRoles;
import com.epam.catgenome.security.Role;
import com.epam.catgenome.security.UserContext;

@Service
public class UserManager {
    private AuthManager authManager;
    private RoleManager roleManager;
    private UserDao userDao;
    private RoleDao roleDao;

    @Autowired
    public UserManager(AuthManager authManager, RoleManager roleManager) {
        this.authManager = authManager;
        this.roleManager = roleManager;
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
        user.setRoles(roleDao.loadRolesList(userRoles));
        user.setGroups(groups);
        user.setAttributes(attributes);

        return userDao.createUser(user, userRoles);
    }

    private List<Long> getNewUserRoles(List<Long> roles) {
        checkAllRolesPresent(roles);
        List<Long> userRoles = CollectionUtils.isEmpty(roles) ? roleManager.getDefaultRolesIds() : roles;
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
        Set<Long> presentIds = roleDao.loadRolesList(roles).stream().map(Role::getId).collect(Collectors.toSet());
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

    public Collection<NgbUser> loadAllUsers() {
        return userDao.loadAllUsers();
    }
}
