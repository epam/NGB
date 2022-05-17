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

package com.epam.catgenome.manager.project;

import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.security.acl.aspect.AclMask;
import com.epam.catgenome.security.acl.aspect.AclMaskList;
import com.epam.catgenome.security.acl.aspect.AclTree;
import com.epam.catgenome.security.acl.aspect.AclFilterAndTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

import static com.epam.catgenome.security.acl.SecurityExpressions.*;

@Service
public class ProjectSecurityService {

    private static final String PROJECT_WRITE_AND_ITEM_READ = "(hasPermissionOnProject(#projectId, 'WRITE')" +
            " AND hasPermissionByBioItemId(#biologicalItemId, 'READ'))";

    @Autowired
    private ProjectManager projectManager;

    @AclFilterAndTree
    @AclMaskList
    @PreAuthorize(ROLE_USER)
    public List<Project> loadTopLevelProjects() {
        return projectManager.loadTopLevelProjects();
    }

    @AclFilterAndTree
    @AclMaskList
    @PreAuthorize(ROLE_USER)
    public List<Project> loadProjectTree(Long parentId, String referenceName) {
        return projectManager.loadProjectTree(parentId, referenceName);
    }

    @AclTree
    @AclMask
    @PreAuthorize(ROLE_USER)
    public Project load(Long projectId) {
        return projectManager.load(projectId);
    }

    @AclTree
    @AclMask
    @PreAuthorize(ROLE_USER)
    public Project load(String projectName) {
        return projectManager.load(projectName);
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_PROJECT_MANAGER + OR + "hasPermissionOnProject(#parentId, 'WRITE')")
    public Project create(Project convertFrom, Long parentId) {
        return projectManager.create(convertFrom, parentId);
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_PROJECT_MANAGER + OR + "projectCanBeMoved(#projectId, #parentId)")
    public void moveProjectToParent(Long projectId, Long parentId) {
        projectManager.moveProjectToParent(projectId, parentId);
    }

    @AclTree
    @PreAuthorize(ROLE_ADMIN + OR + PROJECT_WRITE_AND_ITEM_READ)
    public Project addProjectItem(Long projectId, Long biologicalItemId) {
        return projectManager.addProjectItem(projectId, biologicalItemId);
    }

    @AclTree
    @PreAuthorize(ROLE_ADMIN + OR + PROJECT_WRITE_AND_ITEM_READ)
    public Project removeProjectItem(Long projectId, Long biologicalItemId) throws FeatureIndexException {
        return projectManager.removeProjectItem(projectId, biologicalItemId);
    }

    @PreAuthorize(ROLE_ADMIN + OR + PROJECT_WRITE_AND_ITEM_READ)
    public void hideProjectItem(Long projectId, Long biologicalItemId) {
        projectManager.hideProjectItem(projectId, biologicalItemId);
    }

    @PreAuthorize(ROLE_ADMIN + OR + "projectCanBeDeleted(#projectId, #force)")
    public Project deleteProject(long projectId, Boolean force) throws IOException {
        return projectManager.delete(projectId, force);
    }
}
