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

package com.epam.catgenome.security.acl.aspect;

import java.util.List;

import com.epam.catgenome.security.acl.PermissionHelper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.entity.security.AbstractHierarchicalEntity;
import com.epam.catgenome.entity.security.AbstractSecuredEntity;
import com.epam.catgenome.manager.AuthManager;
import com.epam.catgenome.security.acl.AclPermission;
import com.epam.catgenome.security.acl.GrantPermissionManager;
import com.epam.catgenome.security.acl.JdbcMutableAclServiceImpl;

@Aspect
@Component
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
public class AclAspect {

    private static final String RETURN_OBJECT = "entity";
    private static final Logger LOGGER = LoggerFactory.getLogger(AclAspect.class);

    @Autowired
    private JdbcMutableAclServiceImpl aclService;

    @Autowired
    private GrantPermissionManager permissionManager;

    @Autowired
    private PermissionHelper permissionHelper;


    @AfterReturning(pointcut = "@within(com.epam.catgenome.security.acl.aspect.AclSync) && "
                               + "execution(* *.create(..))", returning = RETURN_OBJECT)
    @Transactional(propagation = Propagation.REQUIRED)
    public void createAclIdentity(JoinPoint joinPoint, AbstractSecuredEntity entity) {
        if (entity.getOwner().equals(AuthManager.UNAUTHORIZED_USER)) {
            return;
        }
        LOGGER.debug("Creating ACL Object {} {}", entity.getName(), entity.getClass());
        MutableAcl acl = aclService.createAcl(entity);
        if (entity.getParent() != null) {
            updateParent(entity, acl);
        }
        // owner has all permissions for a new object
        entity.setMask(AbstractSecuredEntity.ALL_PERMISSIONS_MASK);
    }

    @AfterReturning(pointcut = "@within(com.epam.catgenome.security.acl.aspect.AclSync) && "
                               + "execution(* *.update(..))", returning = RETURN_OBJECT)
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateAclIdentity(JoinPoint joinPoint, AbstractSecuredEntity entity) {
        if (entity.getOwner().equals(AuthManager.UNAUTHORIZED_USER)) {
            return;
        }
        LOGGER.debug("Updating ACL Object {} {} {}", entity.getName(), entity.getClass(), entity.getId());
        MutableAcl acl = aclService.getOrCreateObjectIdentity(entity);
        if (entity.getParent() == null && acl.getParentAcl() == null) {
            return;
        }
        if (entity.getParent() == null) {
            acl.setParent(null);
            aclService.updateAcl(acl);
        } else if (acl.getParentAcl() == null
                || acl.getParentAcl().getObjectIdentity().getIdentifier() != entity.getParent()
                .getId()) {
            updateParent(entity, acl);
        }
        setMask(joinPoint, entity);
    }

    @AfterReturning(pointcut = "@within(com.epam.catgenome.security.acl.aspect.AclSync) && "
                               + "execution(* *.delete(..))", returning = RETURN_OBJECT)
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteAclIdentity(JoinPoint joinPoint, AbstractSecuredEntity entity) {
        LOGGER.debug("Deleting ACL object for Object {} {}", entity.getName(), entity.getClass());
        aclService.deleteAcl(new ObjectIdentityImpl(entity), false);
        entity.setMask(null);
    }

    @AfterReturning(pointcut = "@annotation(com.epam.catgenome.security.acl.aspect.AclMask)",
            returning = RETURN_OBJECT)
    @Transactional(propagation = Propagation.REQUIRED)
    public void setMask(JoinPoint joinPoint, AbstractSecuredEntity entity) {
        entity.setMask(permissionHelper.getPermissionsMask(entity, true, true));
    }

    @AfterReturning(pointcut = "@annotation(com.epam.catgenome.security.acl.aspect.AclMaskList)",
            returning = "list")
    @Transactional(propagation = Propagation.REQUIRED)
    public void setMaskForList(JoinPoint joinPoint, List<? extends AbstractSecuredEntity> list) {
        list.forEach(entity ->
                entity.setMask(permissionHelper.getPermissionsMask(entity, true, true)));
    }

    @AfterReturning(pointcut = "@annotation(com.epam.catgenome.security.acl.aspect.AclTree)",
            returning = RETURN_OBJECT)
    @Transactional(propagation = Propagation.REQUIRED)
    public void filterTree(JoinPoint joinPoint, AbstractHierarchicalEntity entity) {
        permissionManager.filterTree(entity, AclPermission.READ);
    }

    /*@Before("@annotation(com.epam.catgenome.security.acl.aspect.AclFilter) && args(filter,..)") //TODO: maybe needed
    public void extendFilter(JoinPoint joinPoint, AclSecuredFilter filter) {
        permissionManager.extendFilter(filter);
    }*/


    private void updateParent(AbstractSecuredEntity entity, MutableAcl acl) {
        MutableAcl parentAcl = aclService.getOrCreateObjectIdentity(entity.getParent());
        acl.setParent(parentAcl);
        aclService.updateAcl(acl);
    }

}
