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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.manager.parallel.TaskExecutorService;
import com.epam.catgenome.manager.project.ProjectManager;
import com.epam.catgenome.manager.user.RoleManager;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PermissionFactory;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.NotFoundException;
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
import com.epam.catgenome.manager.CompositeSecuredEntityManager;
import com.epam.catgenome.manager.user.UserManager;
import com.epam.catgenome.security.UserContext;

@Service
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
public class GrantPermissionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrantPermissionManager.class);
    private static final String READ = "READ";
    private static final int PAGE_SIZE = 10000;

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

    @Autowired
    private RoleManager roleManager;

    @Autowired
    private BiologicalDataItemManager dataItemManager;

    @Autowired
    private ProjectManager projectManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public AclSecuredEntry setPermissions(AclClass aclClass, Long entityId, String userName, Boolean principal,
                                          Integer mask) {
        validateParameters(aclClass, entityId, userName, mask, principal);
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

    public void syncEntities() {
        LOGGER.debug("Starting ACL entities synchronisation");
        final List<Project> projects = projectManager.loadProjectTree(null, null);
        ListUtils.emptyIfNull(projects).forEach(this::syncProject);
        LOGGER.debug("Finished ACL entities synchronization");
    }

    private void syncProject(final Project project) {
        aclService.getOrCreateObjectIdentity(project);
        ListUtils.emptyIfNull(project.getItems())
                .forEach(item -> aclService.getOrCreateObjectIdentity(item.getBioDataItem()));
        ListUtils.emptyIfNull(project.getNestedProjects()).forEach(this::syncProject);
    }

    public void fillCache() {
        LOGGER.debug("Warming up cache for ACL");
        final List<Project> projects = projectManager.loadProjectTree(null, null);
        final List<ObjectIdentity> batch = new ArrayList<>(PAGE_SIZE);
        projects.forEach(p -> processProject(p, batch));
        flushBatch(batch);
        LOGGER.debug("Finished ACL cache warm up");
    }

    private void processProject(Project project, List<ObjectIdentity> batch) {
        batch.add(new ObjectIdentityImpl(project));
        ListUtils.emptyIfNull(project.getItems())
                .forEach(item -> batch.add(new ObjectIdentityImpl(item.getBioDataItem())));
        flushBatch(batch);
        ListUtils.emptyIfNull(project.getNestedProjects()).forEach(child -> processProject(child, batch));
    }

    private void flushBatch(List<ObjectIdentity> batch) {
        if (batch.size() >= PAGE_SIZE) {
            try {
                aclService.readAclsById(batch);
            } catch (NotFoundException e) {
                LOGGER.debug(e.getMessage(), e);
            }
            batch.clear();
        }
    }

    public boolean filterTree(AbstractHierarchicalEntity entity, Permission permission) {
        return filterTree(permissionHelper.getSids(), entity, permission);
    }

    public boolean filterTree(String userName, AbstractHierarchicalEntity entity, Permission permission) {
        return filterTree(permissionHelper.convertUserToSids(userName), entity, permission);
    }

    public boolean isGroupRegistered(final List<String> groups) {
        Set<Long> sidIds = groups.stream()
                .map(group ->  aclService.getSidId(group, false))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(sidIds)) {
            return false;
        }
        Integer entriesCount = aclService.loadEntriesBySidsCount(sidIds);
        return entriesCount != null && entriesCount != 0;
    }

    private boolean filterTree(List<Sid> sids, AbstractHierarchicalEntity entity, Permission permission) {
        if (entity == null) {
            return true;
        }
        if (permissionHelper.isAdmin(sids)) {
            return true;
        }
        return processHierarchicalEntity(0, entity, new HashMap<>(), permission, true, sids);
    }

    // return true if permission granted or we have any feature file inside the project with granted permission
    private boolean processHierarchicalEntity(int parentMask, AbstractHierarchicalEntity entity,
                                              Map<AclClass, Set<Long>> entitiesToRemove, Permission permission,
                                              boolean root, List<Sid> sids) {
        int defaultMask = 0;
        int currentMask = entity.getId() != null ?
                PermissionUtils.mergeParentMask(permissionHelper.retrieveMaskForSid(entity, false, root, sids),
                        parentMask) : defaultMask;

        entity.getChildren().forEach(
            leaf -> processHierarchicalEntity(currentMask, leaf, entitiesToRemove, permission, false, sids));
        filterLeafs(currentMask, entity.getLeaves(), entitiesToRemove, permission, sids);
        entity.filterLeaves(entitiesToRemove);
        entity.filterChildren(entitiesToRemove);
        boolean permissionGranted = PermissionUtils.isPermissionGranted(currentMask, permission);
        if (!permissionGranted) {
            entity.clearForReadOnlyView();
        }

        boolean hasFeatureFilesOrNesterProject = !CollectionUtils.isEmpty(entity.getChildren())
                || (!CollectionUtils.isEmpty(entity.getLeaves()) && entity.getLeaves().stream()
                .anyMatch(e -> ((BiologicalDataItem) e).getFormat() != BiologicalDataItemFormat.REFERENCE));

        if (!hasFeatureFilesOrNesterProject && !permissionGranted) {
            entitiesToRemove.putIfAbsent(entity.getAclClass(), new HashSet<>());
            entitiesToRemove.get(entity.getAclClass()).add(entity.getId());
        }
        entity.setMask(PermissionUtils.mergeMask(currentMask));
        return hasFeatureFilesOrNesterProject || permissionGranted;
    }

    private void filterLeafs(int parentMask, List<? extends AbstractSecuredEntity> children,
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

    private void validateParameters(AclClass aclClass, Long entityId, String userName,
                                    Integer mask, Boolean principal) {
        Assert.notNull(entityId, MessageHelper.getMessage(MessagesConstants.ERROR_PERMISSION_PARAM_REQUIRED, "ID"));
        Assert.notNull(userName, MessageHelper.getMessage(
            MessagesConstants.ERROR_PERMISSION_PARAM_REQUIRED, "UserName"));
        if (principal) {
            Assert.notNull(userManager.loadUserByName(userName),
                    MessageHelper.getMessage(MessagesConstants.ERROR_ROLE_OR_USER_NOT_FOUND, userName));
        } else {
            Assert.isTrue(userManager.findGroups(null).stream()
                            .anyMatch(group -> group.equalsIgnoreCase(userName))
                            || roleManager.loadAllRoles(false).stream()
                            .anyMatch(r -> r.getName().equalsIgnoreCase(userName)),
                    MessageHelper.getMessage(MessagesConstants.ERROR_ROLE_OR_USER_NOT_FOUND, userName));
        }
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

    public void vcfFilterFormFilter(VcfFilterForm filter) {
        filter.setVcfFileIdsByProject(
                filter.getVcfFileIdsByProject().entrySet().stream().peek(
                    entry -> {
                        List<Long> filtered = entry.getValue().stream().filter(
                            fileId -> permissionHelper.isAllowed(READ, fileId, VcfFile.class) ||
                                    permissionHelper.isAllowed(READ, entry.getKey(), Project.class)
                        ).collect(toList());
                        entry.setValue(filtered);
                    }
                ).collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public void extendMapFilter(Map<Long, List<Long>> fileIdsByProject) {
        for (Long projectId : fileIdsByProject.keySet()) {
            fileIdsByProject.compute(projectId, (key, fileIds) -> {
                List<Long> filtered = fileIds;
                if (fileIds != null) {
                    filtered = fileIds.stream().filter(fileId -> {
                        AbstractSecuredEntity entity = entityManager.load(AclClass.VCF, fileId);
                        return permissionHelper.isAllowed(READ, fileId, VcfFile.class)
                                || PermissionUtils.permissionIsNotDenied(
                                        permissionHelper.getPermissionsMask(entity, true, false),
                                        AclPermission.READ) && permissionHelper.isAllowed(READ, key, Project.class);
                    }).collect(toList());
                }
                return filtered;
            });
        }
    }

}
