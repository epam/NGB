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

package com.epam.catgenome.security.acl;

import com.epam.catgenome.controller.vo.security.PermissionGrantVO;
import com.epam.catgenome.entity.security.AclClass;
import com.epam.catgenome.entity.security.AclSecuredEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import static com.epam.catgenome.security.acl.SecurityExpressions.*;

@Service
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
public class AclPermissionSecurityService {

    private static final String OWNER_BY_ID = "isOwner(#aclClass, #id)";
    private static final String HAS_SPECIFIC_ROLE_BY_ACL_CLASS = "hasSpecificRole(#aclClass)";


    @Autowired
    private GrantPermissionManager permissionManager;

    @PreAuthorize(ROLE_ADMIN)
    public AclSecuredEntry setPermissions(PermissionGrantVO grantVO) {
        return permissionManager.setPermissions(grantVO.getAclClass(), grantVO.getId(), grantVO.getUserName(),
                grantVO.getPrincipal(), grantVO.getMask());
    }

    @PreAuthorize(ROLE_ADMIN + OR + OWNER_BY_ID + OR + HAS_SPECIFIC_ROLE_BY_ACL_CLASS)
    public AclSecuredEntry getPermissions(Long id, AclClass aclClass) {
        return permissionManager.getPermissions(id, aclClass);
    }

    @PreAuthorize(ROLE_ADMIN)
    public AclSecuredEntry deletePermissions(Long id, AclClass aclClass, String user, boolean isPrincipal) {
        return permissionManager.deletePermissions(id, aclClass, user, isPrincipal);
    }

    @PreAuthorize(ROLE_ADMIN)
    public AclSecuredEntry deleteAllPermissions(Long id, AclClass aclClass) {
        return permissionManager.deleteAllPermissions(id, aclClass);
    }

    @PreAuthorize(ROLE_ADMIN)
    public AclSecuredEntry changeOwner(Long id, AclClass aclClass, String userName) {
        return permissionManager.changeOwner(id, aclClass, userName);
    }

    @PreAuthorize(ROLE_ADMIN)
    public void syncEntities() {
        permissionManager.syncEntities();
    }

    @PreAuthorize(ROLE_ADMIN)
    public void fillCache() {
        permissionManager.fillCache();
    }
}
