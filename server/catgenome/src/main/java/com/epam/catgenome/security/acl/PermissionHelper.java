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
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.security.AbstractHierarchicalEntity;
import com.epam.catgenome.entity.security.AclClass;
import com.epam.catgenome.entity.session.NGBSession;
import com.epam.catgenome.entity.session.NGBSessionValue;
import com.epam.catgenome.manager.CompositeSecuredEntityManager;
import com.epam.catgenome.manager.dataitem.DataItemManager;
import com.epam.catgenome.manager.gene.GeneFileManager;
import com.epam.catgenome.manager.project.ProjectManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.user.UserManager;
import com.epam.catgenome.entity.user.DefaultRoles;
import com.epam.catgenome.security.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
public class PermissionHelper {

    public static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionHelper.class);
    private static final String WRITE_PERMISSION = "WRITE";
    public static final String READ = "READ";

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

    @Autowired
    private GeneFileManager geneFileManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private DataItemManager dataItemManager;

    @Autowired
    private CompositeSecuredEntityManager securedEntityManager;

    @Autowired
    private ProjectManager projectManager;

    public boolean isAllowed(String permissionName, AbstractSecuredEntity entity) {
        return isAllowed(permissionName, entity.getId(), entity.getClass().getCanonicalName());
    }

    public boolean isAllowed(String permissionName, Long id, Class type) {
        return isAllowed(permissionName, id, type.getCanonicalName());
    }

    public boolean isAllowed(String permissionName, Long id, String type) {
        if (isAdmin(getSids())){
            return true;
        }
        return permissionEvaluator
                .hasPermission(SecurityContextHolder.getContext().getAuthentication(), id,
                        type, permissionName);
    }

    public boolean isAllowed(final String permissionName, final Long entityId, final AclClass entityClass) {
        final AbstractSecuredEntity entity = securedEntityManager.getEntityManager(entityClass).load(entityId);
        Assert.notNull(entity, "Entity cannot be found");

        if (isAdmin(getSids()) || isOwner(entity)) {
            return true;
        }

        return permissionEvaluator.hasPermission(SecurityContextHolder.getContext().getAuthentication(), entity.getId(),
                entity.getClass().getCanonicalName(), permissionName);
    }

    public boolean isAllowedByBioItemId(String permissionName, Long bioItemId) {
        BiologicalDataItem bioItem = dataItemManager.findFileByBioItemId(bioItemId);
        if (isOwner(bioItem)) {
            return true;
        }
        return permissionEvaluator
                .hasPermission(SecurityContextHolder.getContext().getAuthentication(),
                        bioItem,
                        permissionName);
    }

    public boolean projectCanBeMoved(Long projectId, Long newParentId) {
        Project project = projectManager.load(projectId);
        boolean isAllowed = true;
        if (project.getParentId() != null) {
            isAllowed = permissionEvaluator.hasPermission(
                    SecurityContextHolder.getContext().getAuthentication(),
                    projectManager.load(project.getParentId()), WRITE_PERMISSION);
        }

        boolean parentWritePermission = true;
        if (newParentId != null) {
            parentWritePermission = permissionEvaluator.hasPermission(
                    SecurityContextHolder.getContext().getAuthentication(),
                    projectManager.load(newParentId), WRITE_PERMISSION);
        }

        return isAllowed && permissionEvaluator.hasPermission(
                SecurityContextHolder.getContext().getAuthentication(), project, WRITE_PERMISSION)
                && parentWritePermission;
    }

    public boolean projectCanBeDeleted(Long projectId, Boolean force) {
        Project project = projectManager.load(projectId);
        if (force == null || !force) {
            return isAllowed(WRITE_PERMISSION, project);
        }
        return hasPermissionOnWholeProject(project, WRITE_PERMISSION);
    }

    public boolean isOwner(AbstractSecuredEntity entity) {
        String owner = entity.getOwner();
        return StringUtils.isNotBlank(owner) && owner.equalsIgnoreCase(authManager.getAuthorizedUser());
    }

    public boolean isOwner(AclClass aclClass, Long id) {
        AbstractSecuredEntity load = securedEntityManager.load(aclClass, id);
        return isOwner(load);
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
        //check ownership
        if (child == null && isOwner(entity)) {
            return merge ?
                    AbstractSecuredEntity.ALL_PERMISSIONS_MASK :
                    AbstractSecuredEntity.ALL_PERMISSIONS_MASK_FULL;
        }
        //case for objects, that are not registered as ACL entities
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

    private boolean hasPermissionOnWholeProject(AbstractHierarchicalEntity project, String permission) {
        if (!isAllowed(permission, project)) {
            return false;
        }
        return project.getChildren() == null || project.getChildren().stream()
                .map(p -> hasPermissionOnWholeProject(p, permission))
                .reduce((acc, b) -> acc && b).orElse(true);
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

    public boolean isGeneRegisteredForReference(Long id) {
        GeneFile geneFile = geneFileManager.load(id);
        Reference reference = referenceGenomeManager.load(geneFile.getReferenceId());
        GeneFile referenceGeneFile = reference.getGeneFile();
        return referenceGeneFile != null && referenceGeneFile.getId().equals(id);
    }

    public boolean isAnnotationRegisteredForReference(Long id) {
        return CollectionUtils.isNotEmpty(referenceGenomeManager.loadReferenceIdsByAnnotationFileId(id));
    }

    public boolean sessionIsReadable(final NGBSession session) {
        if (isOwner(AclClass.SESSION, session.getId())) {
            return true;
        }
        try {
            final NGBSessionValue value = MAPPER.readValue(session.getSessionValue(), NGBSessionValue.class);
            return value.getTracks().stream().anyMatch(t -> {
                final Optional<Project> project = Optional.ofNullable(t.getProject()).map(projectManager::load);
                final Optional<BiologicalDataItem> bioDataItem = Optional.ofNullable(t.getBiologicalDataItem())
                        .flatMap(i -> dataItemManager.findFilesByName(i, true).stream().findFirst());
                if (bioDataItem.isPresent()) {
                    return isAllowedByBioItemId(READ, BiologicalDataItem.getBioDataItemId(bioDataItem.get()))
                            || project.map(p -> isAllowed(READ, p)).orElse(false);
                } else if (project.isPresent()) {
                    return isAllowed(READ, project.get());
                }
                return false;
            });
        } catch (IOException e) {
            LOGGER.warn("Can't parse session_value and check availability of the session id: " + session.getId(), e);
        }
        return false;
    }
}
