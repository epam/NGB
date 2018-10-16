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

package com.epam.catgenome.security.acl.customexpression;

import com.epam.catgenome.entity.security.AbstractSecuredEntity;
import com.epam.catgenome.security.acl.JdbcMutableAclServiceImpl;
import com.epam.catgenome.security.acl.PermissionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.Authentication;

import java.util.*;

public class NGBMethodSecurityExpressionRoot extends SecurityExpressionRoot
                                                implements MethodSecurityExpressionOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(NGBMethodSecurityExpressionRoot.class);

    private static final String READ_PERMISSION = "READ";

    private Object filterObject;
    private Object returnObject;
    private Object target;

    private PermissionHelper permissionHelper;
    private JdbcMutableAclServiceImpl aclService;

    public NGBMethodSecurityExpressionRoot(Authentication authentication) {
        super(authentication);
    }

    /**
     * Checks whether action is allowed for a user
     * @param entity
     * @param user
     * @param permission
     * @return
     */
    public boolean isActionAllowedForUser(AbstractSecuredEntity entity, String user, Permission permission) {
        return isActionAllowedForUser(entity, user, Collections.singletonList(permission));
    }

    /**
     * Checks whether actions is allowed for a user
     * Each {@link Permission} is processed individually since default permission resolving will
     * allow actions if any of {@link Permission} is allowed, and we need all {@link Permission}
     * to be granted
     * @param entity
     * @param user
     * @param permissions
     * @return
     */
    public boolean isActionAllowedForUser(AbstractSecuredEntity entity, String user, List<Permission> permissions) {
        List<Sid> sids = permissionHelper.convertUserToSids(user);
        if (permissionHelper.isAdmin(sids) || entity.getOwner().equalsIgnoreCase(user)) {
            return true;
        }
        MutableAcl acl = aclService.getOrCreateObjectIdentity(entity);

        try {
            for (Permission permission : permissions) {
                boolean isGranted = acl.isGranted(Collections.singletonList(permission), sids, true);
                if (!isGranted) {
                    return false;
                }
            }
        } catch (AclDataAccessException e) {
            LOGGER.warn(e.getMessage());
            return false;
        }
        return true;
    }

    public boolean hasUserOrRoleAccess(String permissionName) {
        return permissionName.equals(READ_PERMISSION);
    }

    public boolean isAllowed(Object target, String permission) {

        AbstractSecuredEntity item = (AbstractSecuredEntity) target;
        List<Sid> sids = permissionHelper.getSids();
        if (permissionHelper.isAdmin(sids) || permissionHelper.isOwner(item)) {
            return true;
        }
        return permissionHelper.isAllowed(permission, item);
    }

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public Object getThis() {
        return target;
    }

    public void setThis(Object target) {
        this.target = target;
    }

    public void setPermissionHelper(PermissionHelper permissionHelper) {
        this.permissionHelper = permissionHelper;
    }

    public void setAclService(JdbcMutableAclServiceImpl aclService) {
        this.aclService = aclService;
    }
}