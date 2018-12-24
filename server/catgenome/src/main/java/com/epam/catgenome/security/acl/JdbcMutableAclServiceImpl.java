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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.*;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.security.AbstractSecuredEntity;

public class JdbcMutableAclServiceImpl extends JdbcMutableAclService {

    private String deleteSidByIdQuery;
    private String deleteEntriesBySidQuery;

    public JdbcMutableAclServiceImpl(DataSource dataSource, LookupStrategy lookupStrategy,
                                     AclCache aclCache) {
        super(dataSource, lookupStrategy, aclCache);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public MutableAcl createAcl(AbstractSecuredEntity securedEntity) {
        Assert.notNull(securedEntity, "Object Identity required");

        ObjectIdentity objectIdentity = new ObjectIdentityImpl(securedEntity);
        // Check this object identity hasn't already been persisted
        if (retrieveObjectIdentityPrimaryKey(objectIdentity) != null) {
            throw new AlreadyExistsException("Object identity '" + objectIdentity
                                             + "' already exists");
        }

        PrincipalSid sid = new PrincipalSid(securedEntity.getOwner().toUpperCase());

        // Create the acl_object_identity row
        createObjectIdentity(objectIdentity, sid);

        // Retrieve the ACL via superclass (ensures cache registration, proper retrieval
        // etc)
        Acl acl = readAclById(objectIdentity);
        Assert.isInstanceOf(MutableAcl.class, acl, MessageHelper.getMessage(
                MessagesConstants.ERROR_MUTABLE_ACL_RETURN));

        return (MutableAcl) acl;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public MutableAcl getOrCreateObjectIdentity(AbstractSecuredEntity securedEntity) {
        ObjectIdentity identity = new ObjectIdentityImpl(securedEntity);
        if (retrieveObjectIdentityPrimaryKey(identity) != null) {
            Acl acl = readAclById(identity);
            Assert.isInstanceOf(MutableAcl.class, acl, MessageHelper.getMessage(
                    MessagesConstants.ERROR_MUTABLE_ACL_RETURN));
            return (MutableAcl) acl;
        } else {
            MutableAcl acl = createAcl(identity);
            if (securedEntity.getParent() != null && securedEntity.getParent().getId() != null) {
                MutableAcl parentAcl = getOrCreateObjectIdentity(securedEntity.getParent());
                acl.setParent(parentAcl);
                updateAcl(acl);
            }
            return acl;
        }
    }

    public Map<ObjectIdentity, Acl> getObjectIdentities(Set<AbstractSecuredEntity> securedEntities) {
        List<ObjectIdentity> objectIdentities = securedEntities.stream()
            .map(ObjectIdentityImpl::new)
            .collect(Collectors.toList());
        return readAclsById(objectIdentities);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteSidById(Long sidId) {
        jdbcTemplate.update(deleteEntriesBySidQuery, sidId);
        jdbcTemplate.update(deleteSidByIdQuery, sidId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Sid createOrGetSid(String userName, boolean isPrincipal) {
        createOrRetrieveSidPrimaryKey(userName, isPrincipal, true);
        return isPrincipal ? new PrincipalSid(userName) : new GrantedAuthoritySid(userName);
    }

    public Sid getSid(String user, boolean isPrincipal) {
        Assert.notNull(createOrRetrieveSidPrimaryKey(user, isPrincipal, false),
                       MessageHelper.getMessage(MessagesConstants.ERROR_USER_NAME_NOT_FOUND, user));
        return isPrincipal ? new PrincipalSid(user) : new GrantedAuthoritySid(user);
    }

    public Long getSidId(String user, boolean isPrincipal) {
        Sid sid = isPrincipal ? new PrincipalSid(user) : new GrantedAuthoritySid(user);
        return createOrRetrieveSidPrimaryKey(sid, false);
    }

    public MutableAcl getAcl(AbstractSecuredEntity securedEntity) {
        ObjectIdentity identity = new ObjectIdentityImpl(securedEntity);
        if (retrieveObjectIdentityPrimaryKey(identity) != null) {
            Acl acl = readAclById(identity);
            Assert.isInstanceOf(MutableAcl.class, acl, MessageHelper.getMessage(
                    MessagesConstants.ERROR_MUTABLE_ACL_RETURN));
            return (MutableAcl) acl;
        } else {
            return null;
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void getOrCreateObjectIdentityWithParent(AbstractSecuredEntity entity,
                                                    AbstractSecuredEntity parent) {
        MutableAcl acl = getOrCreateObjectIdentity(entity);
        if ((parent == null || parent.getId() == null) && acl.getParentAcl() == null) {
            return;
        }
        if (parent == null || parent.getId() == null) {
            acl.setParent(null);
            updateAcl(acl);
        } else if (acl.getParentAcl() == null
                   || acl.getParentAcl().getObjectIdentity().getIdentifier() != parent.getId()) {
            MutableAcl parentAcl = getOrCreateObjectIdentity(parent);
            acl.setParent(parentAcl);
            updateAcl(acl);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void changeOwner(final AbstractSecuredEntity entity, final String owner) {
        final MutableAcl acl = getOrCreateObjectIdentity(entity);
        acl.setOwner(createOrGetSid(owner, true));
        updateAcl(acl);
    }

    @Required
    public void setDeleteSidByIdQuery(String deleteSidByIdQuery) {
        this.deleteSidByIdQuery = deleteSidByIdQuery;
    }

    @Required
    public void setDeleteEntriesBySidQuery(String deleteEntriesBySidQuery) {
        this.deleteEntriesBySidQuery = deleteEntriesBySidQuery;
    }
}
