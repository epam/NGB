/*
 * MIT License
 *
 * Copyright (c) 2018-2022 EPAM Systems
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

import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.heatmap.Heatmap;
import com.epam.catgenome.entity.lineage.LineageTree;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.security.AbstractSecuredEntity;
import com.epam.catgenome.entity.security.AclClass;
import com.epam.catgenome.entity.session.NGBSession;
import com.epam.catgenome.security.acl.PermissionHelper;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.Authentication;

import java.util.List;

public class NGBMethodSecurityExpressionRoot extends SecurityExpressionRoot
                                                implements MethodSecurityExpressionOperations {

    private static final String ADMIN = "ADMIN";
    private Object filterObject;
    private Object returnObject;
    private Object target;

    private PermissionHelper permissionHelper;

    public NGBMethodSecurityExpressionRoot(Authentication authentication) {
        super(authentication);
    }

    public boolean isAllowed(Object target, String permission) {
        AbstractSecuredEntity item = (AbstractSecuredEntity) target;
        List<Sid> sids = permissionHelper.getSids();
        if (permissionHelper.isAdmin(sids) || permissionHelper.isOwner(item)) {
            return true;
        }
        return permissionHelper.isAllowed(permission, item);
    }

    public boolean isAllowed(final Long entityId, final AclClass entityClass, final String permission) {
        return permissionHelper.isAllowed(permission, entityId, entityClass);
    }

    public boolean isAllowed(final Long id, final String permission) {
        return permissionHelper.isAllowedByBioItemId(permission, id);
    }

    public boolean hasSpecificRole(AclClass aclClass) {
        switch (aclClass) {
            case BAM:
                return hasRole("BAM_MANAGER");
            case BED:
                return hasRole("BED_MANAGER");
            case MAF:
                return hasRole("MAF_MANAGER");
            case SEG:
                return hasRole("SEG_MANAGER");
            case VCF:
                return hasRole("VCF_MANAGER");
            case WIG:
                return hasRole("WIG_MANAGER");
            case GENE:
                return hasRole("GENE_MANAGER");
            case BUCKET:
                return hasRole("BUCKET_MANAGER");
            case PROJECT:
                return hasRole("PROJECT_MANAGER");
            case SPECIES:
            case REFERENCE:
                return hasRole("REFERENCE_MANAGER");
            case BOOKMARK:
                return hasRole("BOOKMARK_MANAGER");
            default:
                return false;
        }
    }

    public boolean isOwner(AclClass aclClass, long id) {
        return permissionHelper.isOwner(aclClass, id);
    }

    public boolean hasPermissionOnProject(Long projectId, String permission) {
        // if projectId does not specified just return true
        // case: when we ask permission for creating project in the root
        if (projectId == null) {
            return true;
        } else {
            return hasPermission(projectId, Project.class.getCanonicalName(), permission);
        }
    }

    public boolean hasPermissionOnProjectByDescription(final Long descriptionId, String permission) {
        // if projectId does not specified just return true
        // case: when we ask permission for creating project in the root
        if (descriptionId == null) {
            return true;
        }
        return hasPermission(permissionHelper.findProjectIdByDescription(descriptionId),
                Project.class.getCanonicalName(), permission);
    }

    public boolean hasPermissionOnSession(Long sessionId, String permission) {
        // if sessionId does not specified just return true
        if (sessionId == null) {
            return true;
        } else {
            return isOwner(AclClass.SESSION, sessionId)
                    || hasPermission(sessionId, NGBSession.class.getCanonicalName(), permission);
        }
    }

    public boolean sessionIsReadable(final NGBSession session) {
        if (session == null) {
            return false;
        }
        return permissionHelper.sessionIsReadable(session);
    }

    public boolean hasPermissionOnGeneFile(Long projectId, String permission) {
        // if projectId does not specified just return true
        // case: when we ask permission for creating project in the root
        if (projectId == null) {
            return true;
        } else {
            return hasPermission(projectId, Project.class.getCanonicalName(), permission);
        }
    }

    public boolean hasPermissionOnFileOrParentProject(Long fileId, String type, Long projectId, String permission) {
        // if fileId == null we assume that track is unregistered and will be fetched from url,
        // so no need to have permission
        if (hasRole(ADMIN) || fileId == null) {
            return true;
        }
        boolean hasPermission = hasPermission(fileId, type, permission);
        if (!hasPermission && projectId != null) {
            hasPermission = hasPermissionOnProject(projectId, permission);
        }
        return hasPermission;
    }

    public boolean readOnGeneFileIsAllowed(Long id, Long projectId) {
        boolean allowed = hasPermissionOnFileOrParentProject(id, Gene.class.getCanonicalName(), projectId, "READ");
        if (!allowed) {
            //when load gene track - check reference gene file, and if it is the same file - allow read
            allowed = permissionHelper.isGeneRegisteredForReference(id);
        }
        return allowed;
    }

    public boolean readOnAHeatmapIsAllowed(Long id, Long projectId) {
        boolean allowed = hasPermissionOnFileOrParentProject(id, Heatmap.class.getTypeName(), projectId, "READ");
        if (!allowed) {
            allowed = permissionHelper.isHeatmapRegisteredForReference(id);
        }
        return allowed;
    }

    public boolean readOnALineageTreeIsAllowed(Long id, Long projectId) {
        boolean allowed = hasPermissionOnFileOrParentProject(id, LineageTree.class.getTypeName(),
                projectId, "READ");
        if (!allowed) {
            allowed = permissionHelper.isLineageTreeRegisteredForReference(id);
        }
        return allowed;
    }

    public boolean projectCanBeMoved(Long projectId, Long newParentId) {
        return permissionHelper.projectCanBeMoved(projectId, newParentId);
    }

    public boolean projectCanBeDeleted(Long projectId, Boolean force) {
        return permissionHelper.projectCanBeDeleted(projectId, force);
    }

    public boolean hasPermissionByBioItemId(Long bioItemId, String permission) {
        return permissionHelper.isAllowedByBioItemId(permission, bioItemId);
    }

    public boolean isDownloadAllowedByBioItemId(Long bioItemId) {
        return permissionHelper.isDownloadBioDataItemAllowed(bioItemId);
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
}
