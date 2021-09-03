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

package com.epam.catgenome.util;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.security.AbstractSecuredEntity;
import com.epam.catgenome.security.acl.AclPermission;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A helpful test DAO to store mock ACL data in database for security testing
 */
@Service
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
public class AclTestDao extends NamedParameterJdbcDaoSupport {
    private static final String SID_PARAM = "SID";

    private String classSequenceName;

    private String createAclSidQuery;
    private String createAclObjectIdentityQuery;
    private String loadAclClassQuery;
    private String loadAclSidQuery;
    private String loadAclObjectIdentityQuery;
    private String loadAclEntriesQuery;
    private String createAclClassQuery;
    private String createAclEntryQuery;

    @Autowired
    private DaoHelper daoHelper;

    @Transactional(propagation = Propagation.MANDATORY)
    public Pair<AclSid, AclObjectIdentity> createAclForObject(AbstractSecuredEntity entity) {
        Optional<AclSid> existingSid = loadAclSid(entity.getOwner());

        AclSid sid = existingSid.orElseGet(() -> {
            AclSid newSid = new AclSid(true, entity.getOwner());
            createAclSid(newSid);
            return newSid;
        });

        AclClass aclClass = new AclClass(entity.getClass().getCanonicalName());
        createAclClassIfNotPresent(aclClass);

        AclObjectIdentity identity = new AclObjectIdentity(sid, entity.getId(), aclClass.getId(), null, true);
        createObjectIdentity(identity);

        return new ImmutablePair<>(sid, identity);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void grantPermissions(AbstractSecuredEntity entity, String userName, List<AclPermission> permissions) {
        Optional<AclSid> existingSid = loadAclSid(userName);

        AclSid sid = existingSid.orElseGet(() -> {
            AclSid newSid = new AclSid(true, entity.getOwner());
            createAclSid(newSid);
            return newSid;
        });

        Optional<AclObjectIdentity> existingIdentity = loadAclObjectIdentity(entity.getId());
        AclObjectIdentity identity = existingIdentity.orElseGet(() -> createAclForObject(entity).getRight());

        int maxOrder = loadAclEntries(identity.getId()).stream()
            .map(AclEntry::getOrder)
            .max(Comparator.naturalOrder())
            .orElse(0) + 1;

        for (AclPermission p : permissions) {
            AclTestDao.AclEntry groupAclEntry = new AclTestDao.AclEntry(identity, maxOrder++, sid,
                                                                        p.getMask(), p.isGranting());
            groupAclEntry.setId(entity.getId());
            createAclEntry(groupAclEntry);
        }

    }

    public Optional<AclSid> loadAclSid(String sid) {
        List<AclSid> sids = getJdbcTemplate().query(loadAclSidQuery, (rs, i) -> {
            AclSid s = new AclSid();

            s.setId(rs.getLong("ID"));
            s.setPrinciple(rs.getBoolean("PRINCIPAL"));
            s.setSid(rs.getString(SID_PARAM));

            return s;
        }, sid);
        return sids.isEmpty() ? Optional.empty() : Optional.of(sids.get(0));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createAclSid(AclSid sid) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ID", sid.id);
        params.addValue("PRINCIPAL", sid.principle);
        params.addValue(SID_PARAM, sid.sid);

        getNamedParameterJdbcTemplate().update(createAclSidQuery, params);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createObjectIdentity(AclObjectIdentity objectIdentity) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ID", objectIdentity.id);
        params.addValue("OBJECT_ID_CLASS", objectIdentity.classId);
        params.addValue("OBJECT_ID_IDENTITY", objectIdentity.objectId);
        params.addValue("PARENT_OBJECT", objectIdentity.parent != null ? objectIdentity.parent.id : null);
        params.addValue("OWNER_SID", objectIdentity.owner.id);
        params.addValue("ENTRIES_INHERITING", objectIdentity.inheriting);

        getNamedParameterJdbcTemplate().update(createAclObjectIdentityQuery, params);
    }

    public Optional<AclObjectIdentity> loadAclObjectIdentity(long objectId) {
        List<AclObjectIdentity> identities = getJdbcTemplate().query(loadAclObjectIdentityQuery, (rs, i) -> {
            AclObjectIdentity identity = new AclObjectIdentity(
                new AclSid(true, rs.getString("OWNER_SID")),
                rs.getLong("OBJECT_ID_IDENTITY"),
                rs.getLong("OBJECT_ID_CLASS"),
                null,
                rs.getBoolean("ENTRIES_INHERITING"));

            identity.setId(rs.getLong("ID"));
            return identity;
        }, objectId);
        return identities.isEmpty() ? Optional.empty() : Optional.of(identities.get(0));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createAclClassIfNotPresent(AclClass aclClass) {
        List<Long> ids = getJdbcTemplate().query(loadAclClassQuery, new SingleColumnRowMapper<>(),
                                                 aclClass.getClassName());
        if (!ids.isEmpty()) {
            aclClass.setId(ids.get(0));
            return;
        }

        aclClass.setId(daoHelper.createId(classSequenceName));
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ID", aclClass.id);
        params.addValue("CLASS", aclClass.className);

        getNamedParameterJdbcTemplate().update(createAclClassQuery, params);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createAclEntry(AclEntry entry) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ID", entry.id);
        params.addValue("ACL_OBJECT_IDENTITY", entry.objectIdentity.id);
        params.addValue("ACE_ORDER", entry.order);
        params.addValue(SID_PARAM, entry.sid.id);
        params.addValue("MASK", entry.mask);
        params.addValue("GRANTING", entry.granting);
        params.addValue("AUDIT_SUCCESS", entry.auditSuccess);
        params.addValue("AUDIT_FAILURE", entry.auditFailure);

        getNamedParameterJdbcTemplate().update(createAclEntryQuery, params);
    }

    public List<AclEntry> loadAclEntries(long aclObjectIdentityId) {
        return getJdbcTemplate().query(loadAclEntriesQuery, (rs, i) -> {
            AclObjectIdentity identity = new AclObjectIdentity();
            identity.setId(rs.getLong("ACL_OBJECT_IDENTITY"));
            AclSid sid = new AclSid();
            sid.setId(rs.getLong(SID_PARAM));
            return new AclEntry(identity,
                                rs.getInt("ACE_ORDER"),
                                sid,
                                rs.getInt("MASK"),
                                rs.getBoolean("GRANTING"));
        }, aclObjectIdentityId);
    }

    public static class AclSid {

        private boolean principle;
        private String sid;
        private Long id;

        public AclSid(boolean principle, String sid) {
            this.principle = principle;
            this.sid = sid;
        }

        public AclSid() {
        }

        public boolean isPrinciple() {
            return principle;
        }

        public void setPrinciple(boolean principle) {
            this.principle = principle;
        }

        public String getSid() {
            return sid;
        }

        public void setSid(String sid) {
            this.sid = sid;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    public static class AclObjectIdentity {

        private Long id;
        private AclSid owner;
        private Long objectId;
        private Long classId;
        private AclObjectIdentity parent;
        private boolean inheriting;

        public AclObjectIdentity(AclSid owner, Long objectId, Long classId,
                                 AclObjectIdentity parent, boolean inheriting) {
            this.owner = owner;
            this.objectId = objectId;
            this.classId = classId;
            this.parent = parent;
            this.inheriting = inheriting;
        }

        public AclObjectIdentity() {}

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public AclSid getOwner() {
            return owner;
        }

        public void setOwner(AclSid owner) {
            this.owner = owner;
        }

        public Long getObjectId() {
            return objectId;
        }

        public void setObjectId(Long objectId) {
            this.objectId = objectId;
        }

        public Long getClassId() {
            return classId;
        }

        public void setClassId(Long classId) {
            this.classId = classId;
        }

        public AclObjectIdentity getParent() {
            return parent;
        }

        public void setParent(AclObjectIdentity parent) {
            this.parent = parent;
        }

        public boolean isInheriting() {
            return inheriting;
        }

        public void setInheriting(boolean inheriting) {
            this.inheriting = inheriting;
        }
    }

    public static class AclClass {
        private Long id;
        private String className;

        public AclClass(String className) {
            this.className = className;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }
    }

    public static class AclEntry {
        private Long id;
        private AclObjectIdentity objectIdentity;
        private Integer order;
        private AclSid sid;
        private Integer mask;
        private boolean granting;
        private boolean auditSuccess;
        private boolean auditFailure;

        public AclEntry(AclObjectIdentity objectIdentity, Integer order, AclSid sid, Integer mask, boolean granting) {
            this.objectIdentity = objectIdentity;
            this.order = order;
            this.sid = sid;
            this.mask = mask;
            this.granting = granting;
            this.auditSuccess = false;
            this.auditFailure = false;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public AclObjectIdentity getObjectIdentity() {
            return objectIdentity;
        }

        public void setObjectIdentity(AclObjectIdentity objectIdentity) {
            this.objectIdentity = objectIdentity;
        }

        public Integer getOrder() {
            return order;
        }

        public void setOrder(Integer order) {
            this.order = order;
        }

        public AclSid getSid() {
            return sid;
        }

        public void setSid(AclSid sid) {
            this.sid = sid;
        }

        public Integer getMask() {
            return mask;
        }

        public void setMask(Integer mask) {
            this.mask = mask;
        }

        public boolean isGranting() {
            return granting;
        }

        public void setGranting(boolean granting) {
            this.granting = granting;
        }

        public boolean isAuditSuccess() {
            return auditSuccess;
        }

        public void setAuditSuccess(boolean auditSuccess) {
            this.auditSuccess = auditSuccess;
        }

        public boolean isAuditFailure() {
            return auditFailure;
        }

        public void setAuditFailure(boolean auditFailure) {
            this.auditFailure = auditFailure;
        }
    }

    @Required
    public void setCreateAclSidQuery(String createAclSidQuery) {
        this.createAclSidQuery = createAclSidQuery;
    }

    @Required
    public void setCreateAclObjectIdentityQuery(String createAclObjectIdentityQuery) {
        this.createAclObjectIdentityQuery = createAclObjectIdentityQuery;
    }

    @Required
    public void setLoadAclClassQuery(String loadAclClassQuery) {
        this.loadAclClassQuery = loadAclClassQuery;
    }

    @Required
    public void setCreateAclClassQuery(String createAclClassQuery) {
        this.createAclClassQuery = createAclClassQuery;
    }

    @Required
    public void setClassSequenceName(String classSequenceName) {
        this.classSequenceName = classSequenceName;
    }

    @Required
    public void setCreateAclEntryQuery(String createAclEntryQuery) {
        this.createAclEntryQuery = createAclEntryQuery;
    }

    @Required
    public void setLoadAclSidQuery(String loadAclSidQuery) {
        this.loadAclSidQuery = loadAclSidQuery;
    }

    @Required
    public void setLoadAclObjectIdentityQuery(String loadAclObjectIdentityQuery) {
        this.loadAclObjectIdentityQuery = loadAclObjectIdentityQuery;
    }

    @Required
    public void setLoadAclEntriesQuery(String loadAclEntriesQuery) {
        this.loadAclEntriesQuery = loadAclEntriesQuery;
    }
}
