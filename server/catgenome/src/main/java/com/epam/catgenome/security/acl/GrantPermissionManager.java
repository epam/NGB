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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.*;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.acls.domain.AccessControlEntryImpl;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PermissionFactory;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.security.AbstractHierarchicalEntity;
import com.epam.catgenome.entity.security.AbstractSecuredEntity;
import com.epam.catgenome.entity.security.AclClass;
import com.epam.catgenome.entity.security.AclPermissionEntry;
import com.epam.catgenome.entity.security.AclSecuredEntry;
import com.epam.catgenome.entity.security.AclSid;
import com.epam.catgenome.entity.security.EntityPermission;
import com.epam.catgenome.manager.CompositeSecuredEntityManager;
import com.epam.catgenome.manager.user.UserManager;
import com.epam.catgenome.security.UserContext;
import lombok.AllArgsConstructor;
import lombok.Data;

@Service
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
public class GrantPermissionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrantPermissionManager.class);

    @Autowired
    private PermissionFactory permissionFactory;

    @Autowired
    private JdbcMutableAclServiceImpl aclService;

    @Autowired
    private PermissionHelper permissionHelper;

    @Autowired
    private CompositeSecuredEntityManager entityManager;

    @Autowired
    private UserManager userManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public AclSecuredEntry setPermissions(AclClass aclClass, Long entityId, String userName, Boolean principal,
                                          Integer mask) {
        validateParameters(aclClass, entityId, userName, mask);
        AbstractSecuredEntity entity = entityManager.load(aclClass, entityId);

        MutableAcl acl = aclService.getOrCreateObjectIdentity(entity);
        Permission permission = permissionFactory.buildFromMask(mask);
        Sid sid = aclService.createOrGetSid(userName.toUpperCase(), principal);

        LOGGER.debug("Granting permissions for sid {}", sid);
        int sidEntryIndex = findSidEntry(acl, sid);
        if (sidEntryIndex != -1) {
            acl.deleteAce(sidEntryIndex);
        }

        acl.insertAce(Math.max(sidEntryIndex, 0), permission, sid, true);
        MutableAcl updatedAcl = aclService.updateAcl(acl);

        return convertAclToEntryForUser(entity, updatedAcl, sid);
    }

    public AclSecuredEntry getPermissions(Long id, AclClass aclClass) {
        AbstractSecuredEntity entity = entityManager.load(aclClass, id);
        MutableAcl acl = aclService.getOrCreateObjectIdentity(entity);
        return convertAclToEntry(entity, acl);
    }

    public Map<AbstractSecuredEntity, List<AclPermissionEntry>> getPermissions(
        Set<AbstractSecuredEntity> securedEntities) {
        Map<ObjectIdentity, Acl> acls = aclService.getObjectIdentities(securedEntities);

        Map<AbstractSecuredEntity, List<AclPermissionEntry>> result = securedEntities.stream()
            .map(securedEntity -> {
                Acl acl = acls.get(new ObjectIdentityImpl(securedEntity));
                Assert.isInstanceOf(MutableAcl.class, acl, MessageHelper.getMessage(
                    MessagesConstants.ERROR_MUTABLE_ACL_RETURN));
                List<AclPermissionEntry> permissions = acl.getEntries().stream()
                    .map(aclEntry -> new AclPermissionEntry(aclEntry.getSid(), aclEntry.getPermission().getMask()))
                    .collect(toList());
                return new ImmutablePair<>(securedEntity, permissions);
            })
            .collect(toMap(ImmutablePair::getLeft, ImmutablePair::getRight));

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AclSecuredEntry deletePermissions(Long id, AclClass aclClass,
            String user, boolean isPrincipal) {
        AbstractSecuredEntity entity = entityManager.load(aclClass, id);

        MutableAcl acl = aclService.getOrCreateObjectIdentity(entity);
        Sid sid = aclService.getSid(user.toUpperCase(), isPrincipal);
        int sidEntryIndex = findSidEntry(acl, sid);
        if (sidEntryIndex != -1) {
            acl.deleteAce(sidEntryIndex);
            acl = aclService.updateAcl(acl);
        }
        return convertAclToEntryForUser(entity, acl, sid);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AclSecuredEntry deleteAllPermissions(Long id, AclClass aclClass) {
        AbstractSecuredEntity entity = entityManager.load(aclClass, id);

        MutableAcl acl = aclService.getOrCreateObjectIdentity(entity);
        acl = deleteAllAces(acl);
        return convertAclToEntry(entity, acl);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteGrantedAuthority(String name) {
        Long sidId = aclService.getSidId(name, false);
        if (sidId == null) {
            LOGGER.debug("Granted authority with name {} was not found in ACL", name);
            return;
        }
        aclService.deleteSidById(sidId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AclSecuredEntry changeOwner(final Long id, final AclClass aclClass, final String userName) {
        Assert.isTrue(StringUtils.isNotBlank(userName), "User name is required "
                                                        + "to change owner of an object.");
        final AbstractSecuredEntity entity = entityManager.load(aclClass, id);
        final UserContext userContext = userManager.loadUserContext(userName);
        Assert.notNull(userContext, String.format("The user with name %s doesn't exist.", userName));
        if (entity.getOwner().equalsIgnoreCase(userName)) {
            LOGGER.info("The resource you're trying to change owner is already owned by this user.");
            return new AclSecuredEntry(entity);
        }
        aclService.changeOwner(entity, userName);
        return new AclSecuredEntry(entityManager.changeOwner(aclClass, id, userName));
    }

    public void filterTree(AbstractHierarchicalEntity entity, Permission permission) {
        filterTree(permissionHelper.getSids(), entity, permission);
    }

    public void filterTree(String userName, AbstractHierarchicalEntity entity, Permission permission) {
        filterTree(permissionHelper.convertUserToSids(userName), entity, permission);
    }

    private void filterTree(List<Sid> sids, AbstractHierarchicalEntity entity, Permission permission) {
        if (entity == null) {
            return;
        }
        if (permissionHelper.isAdmin(sids)) {
            return;
        }
        processHierarchicalEntity(0, entity, new HashMap<>(), permission, true, sids);
    }

    private void processHierarchicalEntity(int parentMask, AbstractHierarchicalEntity entity,
                                           Map<AclClass, Set<Long>> entitiesToRemove, Permission permission,
                                           boolean root, List<Sid> sids) {
        int defaultMask = 0;
        int currentMask = entity.getId() != null ?
                PermissionUtils.mergeParentMask(permissionHelper.retrieveMaskForSid(entity, false, root, sids),
                        parentMask) : defaultMask;
        entity.getChildren().forEach(
            leaf -> processHierarchicalEntity(currentMask, leaf, entitiesToRemove, permission, false, sids));
        filterChildren(currentMask, entity.getLeaves(), entitiesToRemove, permission, sids);
        entity.filterLeaves(entitiesToRemove);
        entity.filterChildren(entitiesToRemove);
        boolean permissionGranted = PermissionUtils.isPermissionGranted(currentMask, permission);
        if (!permissionGranted) {
            entity.clearForReadOnlyView();
        }
        if (CollectionUtils.isEmpty(entity.getLeaves()) && CollectionUtils
                .isEmpty(entity.getChildren()) && !permissionGranted) {
            entitiesToRemove.putIfAbsent(entity.getAclClass(), new HashSet<>());
            entitiesToRemove.get(entity.getAclClass()).add(entity.getId());
        }
        entity.setMask(PermissionUtils.mergeMask(currentMask));
    }

    private void filterChildren(int parentMask, List<? extends AbstractSecuredEntity> children,
                                Map<AclClass, Set<Long>> entitiesToRemove, Permission permission, List<Sid> sids) {
        children.forEach(child -> {
            int mask = PermissionUtils
                    .mergeParentMask(permissionHelper.getPermissionsMask(child, false, false, sids), parentMask);
            if (!PermissionUtils.isPermissionGranted(mask, permission)) {
                entitiesToRemove.putIfAbsent(child.getAclClass(), new HashSet<>());
                entitiesToRemove.get(child.getAclClass()).add(child.getId());
            }
            child.setMask(PermissionUtils.mergeMask(mask));
        });
    }

    private void validateParameters(AclClass aclClass, Long entityId, String userName, Integer mask) {
        Assert.notNull(entityId, MessageHelper.getMessage(MessagesConstants.ERROR_PERMISSION_PARAM_REQUIRED, "ID"));
        Assert.notNull(userName, MessageHelper.getMessage(
            MessagesConstants.ERROR_PERMISSION_PARAM_REQUIRED, "UserName"));
        Assert.notNull(aclClass, MessageHelper.getMessage(
            MessagesConstants.ERROR_PERMISSION_PARAM_REQUIRED, "ObjectClass"));
        Assert.notNull(mask, MessageHelper.getMessage(MessagesConstants.ERROR_PERMISSION_PARAM_REQUIRED, "Mask"));
        PermissionUtils.validateMask(mask);
    }

    private AclSecuredEntry convertAclToEntryForUser(AbstractSecuredEntity entity, MutableAcl acl,
            Sid sid) {
        AclSid aclSid = new AclSid(sid);
        AclSecuredEntry entry = convertAclToEntry(entity, acl);
        List<AclPermissionEntry> filteredPermissions =
                entry.getPermissions().stream().filter(p -> p.getSid().equals(aclSid))
                        .collect(toList());
        entry.setPermissions(filteredPermissions);
        return entry;
    }

    private int findSidEntry(MutableAcl acl, Sid sid) {
        List<AccessControlEntry> entries = acl.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getSid().equals(sid)) {
                return i;
            }
        }
        return -1;
    }

    private AclSecuredEntry convertAclToEntry(AbstractSecuredEntity entity, MutableAcl acl) {

        AclSecuredEntry entry = new AclSecuredEntry(entity);
        acl.getEntries().forEach(aclEntry -> entry.addPermission(
                new AclPermissionEntry(aclEntry.getSid(), aclEntry.getPermission().getMask())));
        return entry;
    }

    public void clearWritePermissions(AbstractSecuredEntity entity) {
        int readBits = AclPermission.READ.getMask() | AclPermission.NO_READ.getMask();
        MutableAcl acl = aclService.getOrCreateObjectIdentity(entity);
        List<AccessControlEntry> newAces = new ArrayList<>();
        List<AccessControlEntry> aces = acl.getEntries();
        for (int i = 0; i < aces.size(); i++) {
            AccessControlEntry ace = aces.get(i);
            if (PermissionUtils.isPermissionSet(ace.getPermission().getMask(),
                    (AclPermission) AclPermission.READ)) {
                Permission updated =
                        permissionFactory.buildFromMask(ace.getPermission().getMask() & readBits);
                AccessControlEntry newAce =
                        new AccessControlEntryImpl(ace.getId(), ace.getAcl(), ace.getSid(), updated,
                                                   true, false, false);
                newAces.add(newAce);
            }
        }
        clearAces(acl);
        for (int i = 0; i < newAces.size(); i++) {
            AccessControlEntry newAce = newAces.get(i);
            acl.insertAce(i, newAce.getPermission(), newAce.getSid(), true);
        }
        aclService.updateAcl(acl);
        if (entity instanceof AbstractHierarchicalEntity) {
            AbstractHierarchicalEntity tree = (AbstractHierarchicalEntity) entity;
            if (!CollectionUtils.isEmpty(tree.getChildren())) {
                tree.getChildren().forEach(this::clearWritePermissions);
            }
            if (!CollectionUtils.isEmpty(tree.getLeaves())) {
                tree.getLeaves().forEach(this::clearWritePermissions);
            }
        }
    }

    private void clearAces(MutableAcl acl) {
        while (!acl.getEntries().isEmpty()) {
            acl.deleteAce(0);
        }
    }


    private MutableAcl deleteAllAces(MutableAcl acl) {
        if (!CollectionUtils.isEmpty(acl.getEntries())) {
            clearAces(acl);
            acl = aclService.updateAcl(acl);
        }
        return acl;
    }

    private void mergePermissions(Map<AclSid, Integer> childPermissions, List<AclPermissionEntry> parentPermissions) {
        parentPermissions.forEach(aclPermissionEntry -> {
            childPermissions.computeIfPresent(aclPermissionEntry.getSid(), (acl, mask) -> PermissionUtils
                    .mergeParentMask(mask, aclPermissionEntry.getMask()));
            childPermissions.putIfAbsent(aclPermissionEntry.getSid(), aclPermissionEntry.getMask());
        });
    }

    public Set<AclPermissionEntry> buildAclPermissionEntries(Map<AclSid, Integer> permissions) {
        Set<AclPermissionEntry> result = new HashSet<>();
        permissions.forEach((acl, mask) -> result.add(new AclPermissionEntry(acl, mask)));
        return result;
    }

    public Map<AclSid, Integer> getEntityPermissions(AbstractSecuredEntity entity,
            Map<AbstractSecuredEntity, List<AclPermissionEntry>> allPermissions) {
        return allPermissions.get(entity).stream()
                .collect(toMap(AclPermissionEntry::getSid, AclPermissionEntry::getMask));
    }

    public void mergeWithParentPermissions(Map<AclSid, Integer> mergedPermissions, AbstractSecuredEntity parent,
                                            Map<AbstractSecuredEntity, List<AclPermissionEntry>> allPermissions) {
        AbstractSecuredEntity currentParent = parent;
        while (currentParent != null) {
            mergePermissions(mergedPermissions, allPermissions.get(currentParent));
            currentParent = currentParent.getParent();
        }
    }

    public Map<AbstractSecuredEntity, List<AclPermissionEntry>> getEntitiesPermissions(
            Collection<? extends AbstractSecuredEntity> entities) {
        Set<AbstractSecuredEntity> result = new HashSet<>(entities);
        entities.forEach(entity -> {
            AbstractSecuredEntity parent = entity.getParent();
            while (parent != null) {
                result.add(parent);
                parent = parent.getParent();
            }
        });
        return getPermissions(result);
    }

    public void expandGroups(List<EntityPermission> permissions) {
        if (CollectionUtils.isEmpty(permissions)) {
            return;
        }
        Map<String, Set<String>> groupToUsers = userManager.loadAllUsers().stream().map(user ->
                Stream.concat(
                        //groups
                        ListUtils.emptyIfNull(user.getGroups()).stream()
                                .map(authority -> new Pair<>(user.getUserName(), authority)),
                        //roles
                        ListUtils.emptyIfNull(user.getRoles()).stream()
                                .map(role -> new Pair<>(user.getUserName(), role.getName())))
                        .collect(toList()))
                .flatMap(Collection::stream)
                .collect(groupingBy(Pair::getSecond, mapping(Pair::getFirst, toSet())));
        permissions.forEach(permission -> {
            permission.setPermissions(expandGroupsAndMergePermissions(groupToUsers, permission));
        });

    }

    private Set<AclPermissionEntry> expandGroupsAndMergePermissions(Map<String, Set<String>> groupToUsers,
                                                                    EntityPermission permission) {
        Map<AclSid, List<SidAclEntry>> sidToEntries = SetUtils.emptyIfNull(permission.getPermissions())
                .stream()
                .map(aclEntry -> {
                    AclSid sid = aclEntry.getSid();
                    if (sid.isPrincipal()) {
                        return Collections.singletonList(new SidAclEntry(aclEntry, false));
                    }
                    return groupToUsers.getOrDefault(sid.getName(), Collections.emptySet())
                            .stream()
                            .map(username -> {
                                AclPermissionEntry aclPermissionEntry =
                                        new AclPermissionEntry(new AclSid(username, true), aclEntry.getMask());
                                return new SidAclEntry(aclPermissionEntry, true);
                            })
                            .collect(toList());
                })
                .flatMap(Collection::stream)
                .collect(groupingBy(sidEntry -> sidEntry.getEntry().getSid()));

        return sidToEntries.values().stream()
                .map(aces -> {
                    SidAclEntry userEntry = aces.stream()
                            .filter(e -> !e.isResolved()).findFirst()
                            .orElse(aces.get(0));
                    List<SidAclEntry> inheritedEntries = aces.stream().filter(ace -> !ace.equals(userEntry))
                            .collect(toList());
                    AclPermissionEntry entry = userEntry.getEntry();
                    if (CollectionUtils.isEmpty(inheritedEntries)) {
                        return entry;
                    }
                    int mask = entry.getMask();
                    for (SidAclEntry inheritedEntry : inheritedEntries) {
                        mask = PermissionUtils.mergeParentMask(mask, inheritedEntry.getEntry().getMask());
                    }
                    return new AclPermissionEntry(entry.getSid(), mask);
                })
                .collect(toSet());
    }

    public List<? extends AbstractSecuredEntity> filterList(List<? extends AbstractSecuredEntity> entities,
                                                            Permission permission) {
        return entities.stream()
                .filter(e -> PermissionUtils.isPermissionGranted(
                        permissionHelper.retrieveMaskForSid(e, false, false, permissionHelper.getSids()),
                        permission)
                ).collect(toList());
    }

    @Data
    @AllArgsConstructor
    private static class SidAclEntry {
        final AclPermissionEntry entry;
        final boolean resolved;
    }

}
