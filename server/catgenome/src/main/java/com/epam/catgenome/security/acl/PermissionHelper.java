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

package com.epam.catgenome.security.acl;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.manager.user.UserManager;
import com.epam.catgenome.security.DefaultRoles;
import com.epam.catgenome.security.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.epam.catgenome.entity.security.AbstractSecuredEntity;
import com.epam.catgenome.manager.AuthManager;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
public class PermissionHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionHelper.class);

    @Autowired
    private final PermissionEvaluator permissionEvaluator;

    @Autowired
    private final AuthManager authManager;

    @Autowired
    private SidRetrievalStrategy sidRetrievalStrategy;

    @Autowired
    private JdbcMutableAclServiceImpl aclService;

    @Autowired
    private UserManager userManager;

    public boolean isAllowed(String permissionName, AbstractSecuredEntity entity) {
        if (isOwner(entity)) {
            return true;
        }
        return permissionEvaluator
            .hasPermission(SecurityContextHolder.getContext().getAuthentication(), entity,
                           permissionName);
    }

    public boolean isOwner(AbstractSecuredEntity entity) {
        String owner = entity.getOwner();
        return StringUtils.isNotBlank(owner) && owner.equalsIgnoreCase(authManager.getAuthorizedUser());
    }

    public boolean isAdmin(List<Sid> sids) {
        GrantedAuthoritySid admin = new GrantedAuthoritySid(DefaultRoles.ROLE_ADMIN.getName());
        return sids.stream().anyMatch(sid -> sid.equals(admin));
    }

    public Integer getPermissionsMask(AbstractSecuredEntity entity, boolean merge,
                                      boolean includeInherited) {
        return getPermissionsMask(entity, merge, includeInherited, getSids());
    }

    public Integer getPermissionsMask(AbstractSecuredEntity entity, boolean merge,
                                      boolean includeInherited, List<Sid> sids) {
        if (isAdmin(sids)) {
            return merge ?
                    AbstractSecuredEntity.ALL_PERMISSIONS_MASK :
                    AbstractSecuredEntity.ALL_PERMISSIONS_MASK_FULL;
        }
        return retrieveMaskForSid(entity, merge, includeInherited, sids);
    }

    public List<Sid> getSids() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return sidRetrievalStrategy.getSids(authentication);
    }

    public List<Sid> convertUserToSids(String user) {
        String principal = user.toUpperCase();
        UserContext eventOwner = userManager.loadUserContext(user.toUpperCase());
        Assert.notNull(eventOwner, MessageHelper.getMessage(MessagesConstants.ERROR_USER_NAME_NOT_FOUND, principal));
        List<Sid> sids = new ArrayList<>();
        sids.add(new PrincipalSid(principal));
        sids.addAll(eventOwner.getAuthorities().stream()
                .map(GrantedAuthoritySid::new)
                .collect(toList()));
        return sids;
    }

    public Integer retrieveMaskForSid(AbstractSecuredEntity entity, boolean merge,
                                       boolean includeInherited, List<Sid> sids) {
        Acl child = aclService.getAcl(entity);
        //case for Runs and Nodes, that are not registered as ACL entities
        //check ownership
        if (child == null && isOwner(entity)) {
            return merge ?
                    AbstractSecuredEntity.ALL_PERMISSIONS_MASK :
                    AbstractSecuredEntity.ALL_PERMISSIONS_MASK_FULL;
        }
        if (child == null && entity.getParent() == null) {
            LOGGER.debug("Object is not registered in ACL {} {}", entity.getAclClass(), entity.getId());
            return 0;
        }
        //get parent
        Acl acl = child == null ? aclService.getAcl(entity.getParent()) : child;
        if (sids.stream().anyMatch(sid -> acl.getOwner().equals(sid))) {
            return merge ?
                    AbstractSecuredEntity.ALL_PERMISSIONS_MASK :
                    AbstractSecuredEntity.ALL_PERMISSIONS_MASK_FULL;
        }
        List<AclPermission> basicPermissions = PermissionUtils.getBasicPermissions();
        int extendedMask = collectPermissions(0, acl, sids, basicPermissions, includeInherited);
        return merge ? PermissionUtils.mergeMask(extendedMask, basicPermissions) : extendedMask;
    }

    private int collectPermissions(int mask, Acl acl, List<Sid> sids,
                                   List<AclPermission> permissionToCollect, boolean includeInherited) {
        if (PermissionUtils.allPermissionsSet(mask, permissionToCollect)) {
            return mask;
        }
        int currentMask = mask;
        final List<AccessControlEntry> aces = acl.getEntries();
        for (Sid sid : sids) {
            // Attempt to find exact match for this permission mask and SID
            for (AccessControlEntry ace : aces) {
                if (ace.getSid().equals(sid)) {
                    Permission permission = ace.getPermission();
                    for (AclPermission p : permissionToCollect) {
                        if (!PermissionUtils.isPermissionSet(currentMask, p)) {
                            //try to set granting mask
                            currentMask = currentMask | (permission.getMask() & p.getMask());
                            if (!PermissionUtils.isPermissionSet(currentMask, p)) {
                                //try to set denying mask
                                currentMask =
                                        currentMask | (permission.getMask() & p.getDenyPermission()
                                                .getMask());
                            }
                        }
                    }
                }
            }
        }
        if (PermissionUtils.allPermissionsSet(currentMask, permissionToCollect)) {
            return currentMask;
        }
        // No matches have been found so far
        if (includeInherited && acl.isEntriesInheriting() && (acl.getParentAcl() != null)) {
            // We have a parent, so let them try to find a matching ACE
            return collectPermissions(currentMask, acl.getParentAcl(), sids, permissionToCollect,
                    includeInherited);
        } else {
            return currentMask;
        }
    }
}
