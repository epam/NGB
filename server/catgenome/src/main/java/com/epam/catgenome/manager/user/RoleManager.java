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
import java.util.Optional;
import java.util.stream.Collectors;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.RoleVO;
import com.epam.catgenome.dao.user.UserDao;
import com.epam.catgenome.entity.security.NgbUser;
import com.epam.catgenome.entity.user.ExtendedRole;
import com.epam.catgenome.security.acl.GrantPermissionManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.epam.catgenome.dao.user.RoleDao;
import com.epam.catgenome.entity.user.Role;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
public class RoleManager {

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private GrantPermissionManager permissionManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public Role createRole(final String name, final boolean predefined, final boolean userDefault) {
        String formattedName = getValidName(name);
        Assert.isTrue(!roleDao.loadRoleByName(formattedName).isPresent(),
                MessageHelper.getMessage(MessagesConstants.ERROR_ROLE_ALREADY_EXIST, getPrettyName(formattedName)));
        return roleDao.createRole(formattedName, predefined, userDefault);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Role update(final Long roleId, final RoleVO roleVO) {
        Role role = loadRole(roleId);
        role.setName(getValidName(roleVO.getName()));
        role.setUserDefault(roleVO.isUserDefault());
        roleDao.updateRole(role);
        return role;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Role deleteRole(Long id) {
        Role role = loadRole(id);
        Assert.isTrue(!role.isPredefined(), "Predefined system roles cannot be deleted");
        permissionManager.deleteGrantedAuthority(role.getName());
        roleDao.deleteRoleReferences(id);
        roleDao.deleteRole(id);
        return role;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public ExtendedRole assignRole(Long roleId, List<Long> userIds) {
        loadRole(roleId);
        Assert.isTrue(CollectionUtils.isNotEmpty(userIds),
                MessageHelper.getMessage(MessagesConstants.ERROR_USER_LIST_EMPTY));
        Collection<NgbUser> users = userDao.loadUsersList(userIds);
        List<Long> idsToAdd = users.stream()
                .filter(user -> user.getRoles().stream().noneMatch(role -> role.getId().equals(roleId)))
                .map(NgbUser::getId)
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(idsToAdd)) {
            userDao.assignRoleToUsers(roleId, idsToAdd);
        }
        return roleDao.loadExtendedRole(roleId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public ExtendedRole removeRole(Long roleId, List<Long> userIds) {
        loadRole(roleId);
        Assert.isTrue(CollectionUtils.isNotEmpty(userIds),
                MessageHelper.getMessage(MessagesConstants.ERROR_USER_LIST_EMPTY));
        Collection<NgbUser> users = userDao.loadUsersList(userIds);
        List<Long> idsToRemove = users.stream()
                .filter(user -> user.getRoles().stream().anyMatch(role -> role.getId().equals(roleId)))
                .map(NgbUser::getId)
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(idsToRemove)) {
            userDao.removeRoleFromUsers(roleId, idsToRemove);
        }
        return roleDao.loadExtendedRole(roleId);
    }

    public Role loadRole(Long roleId) {
        return roleDao.loadRole(roleId)
                .orElseThrow(() -> new IllegalArgumentException(
                        MessageHelper.getMessage(MessagesConstants.ERROR_ROLE_ID_NOT_FOUND, roleId)));
    }

    private String getValidName(final String name) {
        Assert.isTrue(StringUtils.isNotBlank(name),
                MessageHelper.getMessage(MessagesConstants.ERROR_ROLE_NAME_REQUIRED));
        String formattedName = name.toUpperCase();
        if (!formattedName.startsWith(Role.ROLE_PREFIX)) {
            formattedName = Role.ROLE_PREFIX + formattedName;
        }
        return formattedName;
    }

    private String getPrettyName(String name) {
        if (name.startsWith(Role.ROLE_PREFIX)) {
            name = name.replace(Role.ROLE_PREFIX, "");
        }
        return name;
    }

    public List<Long> getDefaultRolesIds() {
        return loadDefaultRoles()
            .stream()
            .map(Role::getId)
            .collect(Collectors.toList());
    }

    public List<Role> loadDefaultRoles() {
        return roleDao.loadDefaultRoles();
    }

    public List<Role> loadRoles(List<Long> roleIds) {
        return roleDao.loadRoles(roleIds);
    }

    public Role loadRoleByName(String name) {
        return roleDao.loadRoleByName(getValidName(name)).orElseThrow(
            () -> new IllegalArgumentException("Role with name " + name + " not found!"));
    }

    public Optional<Role> findRoleByName(final String name) {
        return roleDao.loadRoleByName(getValidName(name));
    }

    public Map<Long, List<Role>> loadRolesByUserIds(List<Long> userIds) {
        return roleDao.loadRolesByUserIds(userIds);
    }

    public List<Role> loadAllRoles(boolean loadUsers) {
        return roleDao.loadAllRoles(loadUsers);
    }

    public Role loadRoleWithUsers(Long id) {
        return roleDao.loadExtendedRole(id);
    }
}
