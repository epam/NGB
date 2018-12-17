/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2018 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.epam.catgenome.manager.user;

import com.epam.catgenome.controller.vo.RoleVO;
import com.epam.catgenome.entity.user.ExtendedRole;
import com.epam.catgenome.entity.user.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_ADMIN;

@Service
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
public class RoleSecurityService {

    @Autowired
    private RoleManager roleManager;

    @PreAuthorize(ROLE_ADMIN)
    public Collection<Role> loadRolesWithUsers() {
        return roleManager.loadAllRoles(true);
    }

    public Collection<Role> loadRoles() {
        return roleManager.loadAllRoles(false);
    }

    @PreAuthorize(ROLE_ADMIN)
    public Role loadRole(Long id) {
        return roleManager.loadRoleWithUsers(id);
    }

    @PreAuthorize(ROLE_ADMIN)
    public Role createRole(String name, boolean userDefault) {
        return roleManager.createRole(name, false, userDefault);
    }

    @PreAuthorize(ROLE_ADMIN)
    public Role updateRole(final Long roleId, final RoleVO roleVO) {
        return roleManager.update(roleId, roleVO);
    }

    @PreAuthorize(ROLE_ADMIN)
    public Role deleteRole(Long id) {
        return roleManager.deleteRole(id);
    }

    @PreAuthorize(ROLE_ADMIN)
    public ExtendedRole assignRole(Long roleId, List<Long> userIds) {
        return roleManager.assignRole(roleId, userIds);
    }

    @PreAuthorize(ROLE_ADMIN)
    public Role loadRoleByName(String name) {
        return roleManager.loadRoleByName(name);
    }

    @PreAuthorize(ROLE_ADMIN)
    public ExtendedRole removeRole(Long roleId, List<Long> userIds) {
        return roleManager.removeRole(roleId, userIds);
    }
}
