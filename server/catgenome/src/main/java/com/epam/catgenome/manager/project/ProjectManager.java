/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

package com.epam.catgenome.manager.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.epam.catgenome.entity.FeatureFile;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.project.ProjectDao;
import com.epam.catgenome.dao.seg.SegFileDao;
import com.epam.catgenome.dao.vcf.VcfFileDao;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.project.ProjectItem;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.seg.SegFile;
import com.epam.catgenome.entity.seg.SegSample;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfSample;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.util.AuthUtils;

/**
 * Source:      ProjectManager
 * Created:     11.01.16, 12:53
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * A service class to execute project related tasks
 * </p>
 */
@Service
public class ProjectManager {
    @Autowired
    private ProjectDao projectDao;

    @Autowired
    private VcfFileDao vcfFileDao;

    @Autowired
    private SegFileDao segFileDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private FileManager fileManager;

    /**
     * Loads all top-level projects for current user from the database.
     * Projects are being loaded with single reference item.
     *
     * @return a {@code List&lt;Project&gt;} of projects, created by current user
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public List<Project> loadTopLevelProjectsForCurrentUser() {
        List<Project> projects = projectDao.loadTopLevelProjectsOrderByLastOpened(AuthUtils.getCurrentUserId());
        final Map<Long, Set<ProjectItem>> itemsMap = projectDao.loadProjectItemsByProjectIds(
                projects.parallelStream().map(BaseEntity::getId).collect(Collectors.toList()));

        projects.parallelStream().forEach(p -> {
            if (itemsMap.containsKey(p.getId())) {
                Set<ProjectItem> items = itemsMap.get(p.getId());
                List<ProjectItem> referenceItems = new ArrayList<>();
                Map<BiologicalDataItemFormat, Integer> itemsCountPerFormat = new EnumMap<>(
                                                                                        BiologicalDataItemFormat.class);

                items.stream().forEach(pi -> countProjectItem(pi, referenceItems, itemsCountPerFormat));

                p.setItems(referenceItems);
                p.setItemsCountPerFormat(itemsCountPerFormat);
                p.setItemsCount(items.size() - referenceItems.size());
            }
        });
        return projects;
    }

    /**
     * Loads all project hierarchy for current user, with all items
     * @param parentId specifies the root project for loading, if null, all projects will be loaded
     * @return all project hierarchy for current user, with all items
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public List<Project> loadProjectTree(final Long parentId, String referenceName) {
        List<Project> allProjects;
        if (StringUtils.isEmpty(referenceName)) {
            allProjects = projectDao.loadAllProjects(AuthUtils.getCurrentUserId());
        } else {
            List<BiologicalDataItem> referenceList =
                    biologicalDataItemDao.loadFilesByNameCaseInsensitive(referenceName);
            Assert.notNull(referenceList,
                    MessageHelper.getMessage(MessagesConstants.ERROR_BIO_NAME_NOT_FOUND, referenceName));
            Assert.isTrue(!referenceList.isEmpty(),
                    MessageHelper.getMessage(MessagesConstants.ERROR_BIO_NAME_NOT_FOUND, referenceName));
            Assert.isTrue(referenceList.get(0) instanceof Reference,
                    MessageHelper.getMessage(MessagesConstants.ERROR_BIO_NAME_NOT_FOUND, referenceName));
            Reference reference = (Reference) referenceList.get(0);
            allProjects = projectDao.loadProjectsByBioDataItemId(reference.getBioDataItemId());
        }

        Map<Long, List<Project>> hierarchyMap = new HashMap<>();
        Map<Long, Set<ProjectItem>> itemMap;

        if (StringUtils.isEmpty(referenceName)) {
            itemMap = projectDao.loadAllProjectItems();
        } else {
            itemMap = projectDao.loadProjectItemsByProjects(allProjects);
        }

        allProjects.stream().forEach(p -> {
            if (itemMap.containsKey(p.getId())) {
                p.setItems(new ArrayList<>(itemMap.get(p.getId())));
            }

            if (!hierarchyMap.containsKey(p.getParentId())) {
                hierarchyMap.put(p.getParentId(), new ArrayList<>());
            }
            hierarchyMap.get(p.getParentId()).add(p);
        });

        if (parentId != null) {
            Project topProject = loadProject(parentId);
            Assert.notNull(topProject,
                    MessageHelper.getMessage(MessagesConstants.ERROR_PROJECT_NOT_FOUND, parentId));
            topProject.setNestedProjects(hierarchyMap.get(parentId));
            return Collections.singletonList(topProject);
        }
        allProjects.forEach(p -> p.setNestedProjects(hierarchyMap.get(p.getId())));
        return hierarchyMap.get(null);
    }

    /**
     * Loads a project from the database by its ID with all its items
     *
     * @param projectId {@code long} ID of a project to load
     * @return a {@code Project} from the database
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Project loadProject(long projectId) {
        Project project = projectDao.loadProject(projectId);
        Assert.notNull(project, MessageHelper.getMessage(MessagesConstants.ERROR_PROJECT_NOT_FOUND, projectId));

        loadProjectStuff(project);

        return project;
    }

    /**
     * Loads a project from the database by its name, with all its items
     *
     * @param projectName {@code String} name of a project to load
     * @return a {@code Project} from the database
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Project loadProject(String projectName) {
        Project project = projectDao.loadProject(projectName);
        Assert.notNull(project, MessageHelper.getMessage(MessagesConstants.ERROR_PROJECT_NOT_FOUND, projectName));

        loadProjectStuff(project);

        return project;
    }

    private void loadProjectStuff(Project project) {
        loadProjectItems(project);
        project.setNestedProjects(projectDao.loadNestedProjects(project.getId()));
        projectDao.updateLastOpenedDate(project.getId());
    }

    /**
     * Moves a Project, specified by projectId to a parent project, specified by parentId
     * @param projectId an ID of a project, to move
     * @param parentId an ID of a parent project to move to
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void moveProjectToParent(long projectId, Long parentId) {
        Assert.notNull(projectDao.loadProject(projectId), MessageHelper.getMessage(
            MessagesConstants.ERROR_PROJECT_NOT_FOUND, projectId));

        if (parentId != null) {
            Assert.notNull(projectDao.loadProject(parentId), MessageHelper.getMessage(
                MessagesConstants.ERROR_PROJECT_NOT_FOUND, parentId));
        }

        projectDao.moveProjectToParent(projectId, parentId);
    }

    private void loadProjectItems(Project project) {
        project.setItems(projectDao.loadProjectItemsByProjectId(project.getId()));
        List<ProjectItem> referenceGeneItems = project.getItems().stream()
            .filter(i -> i.getBioDataItem().getFormat() == BiologicalDataItemFormat.REFERENCE
                         && ((Reference) i.getBioDataItem()).getGeneFile() != null)
            .map(i -> new ProjectItem(((Reference) i.getBioDataItem()).getGeneFile()))
            .collect(Collectors.toList());
        project.getItems().addAll(referenceGeneItems);

        // Set Vcf files samples
        Set<Long> vcfIds = new HashSet<>();
        Set<Long> segIds = new HashSet<>();
        project.getItems().stream().forEach(item -> {
            if (item.getBioDataItem() != null) {
                switch (item.getBioDataItem().getFormat()) {
                    case VCF:
                        VcfFile vcfFile = (VcfFile) item.getBioDataItem();
                        vcfIds.add(vcfFile.getId());
                        break;
                    case SEG:
                        SegFile segFile = (SegFile) item.getBioDataItem();
                        segIds.add(segFile.getId());
                        break;
                    default:
                        break;
                }
            }
        });

        loadSamples(project, vcfIds, segIds);

        project.setItemsCount(project.getItems().size());
    }

    /**
     * Saves a new project to the database or updates an existing one. Also saves all project items passed, if they
     * are not null.
     *
     * @param project a {@code Project} to be saved
     * @return saved {@code Project} from the database
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Project saveProject(final Project project) {
        return saveProject(project, null);
    }

    /**
     * Saves a new project to the database or updates an existing one. Also saves all project items passed, if they
     * are not null. If a {@param parentId} is specified, saves a new project into existing parent project. Works only
     * for creating new project
     *
     * @param project a {@code Project} to be saved
     * @param parentId a {@code Long} ID of a project, that is a parent of new project, being created
     * @return saved {@code Project} from the database
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Project saveProject(final Project project, Long parentId) {
        Project helpProject = project;
        boolean newProject = checkNewProject(helpProject);

        projectDao.saveProject(helpProject, parentId);

        if (helpProject.getItems() != null) {
            List<ProjectItem> newProjectItems = helpProject.getItems()
                    .stream().distinct().collect(Collectors.toList());
            if (!newProject) {
                Project loadedProject = loadProject(helpProject.getId());

                Set<Long> existingBioIds = new HashSet<>();
                Set<Long> newBioIds = new HashSet<>();
                existingBioIds.addAll(loadedProject.getItems().stream()
                        .map(item -> BiologicalDataItem.getBioDataItemId(item.getBioDataItem()))
                        .collect(Collectors.toList()));
                newBioIds.addAll(newProjectItems.stream()
                        .map(item -> BiologicalDataItem.getBioDataItemId(item.getBioDataItem()))
                        .collect(Collectors.toList()));

                List<ProjectItem> itemsToAdd = newProjectItems.stream()
                        .filter(item -> !existingBioIds.contains(BiologicalDataItem
                                .getBioDataItemId(item.getBioDataItem())))
                        .collect(Collectors.toList());

                List<ProjectItem> itemsToRemove = loadedProject.getItems().stream()
                        .filter(item -> !newBioIds.contains(BiologicalDataItem.getBioDataItemId(item.getBioDataItem())))
                        .collect(Collectors.toList());

                List<ProjectItem> itemsToUpdateHidden = newProjectItems.stream()
                        .filter(item -> existingBioIds.contains(BiologicalDataItem
                                .getBioDataItemId(item.getBioDataItem())))
                        .collect(Collectors.toList());

                List<BiologicalDataItem> itemsAdded = biologicalDataItemDao.loadBiologicalDataItemsByIds(itemsToAdd
                        .stream()
                        .map(projectItem -> BiologicalDataItem.getBioDataItemId(projectItem.getBioDataItem()))
                        .collect(Collectors.toList()));
                Reference reference;
                if (loadedProject.getItems().isEmpty()) {
                    reference = findReferenceFromBioItems(itemsAdded);
                } else {
                    reference = findReference(loadedProject.getItems());
                }

                Assert.isTrue(itemsToAdd.stream().noneMatch(
                    item -> item.getBioDataItem().getFormat() == BiologicalDataItemFormat.REFERENCE),
                        MessageHelper.getMessage(MessagesConstants.ERROR_PROJECT_INVALID_REFERENCE));
                checkReference(reference, itemsAdded);

                projectDao.addProjectItems(helpProject.getId(), itemsToAdd);
                projectDao.deleteProjectItems(helpProject.getId(), itemsToRemove);
                projectDao.hideProjectItems(helpProject.getId(), itemsToUpdateHidden);


                List<BiologicalDataItem> itemsRemoved = biologicalDataItemDao.loadBiologicalDataItemsByIds(itemsToRemove
                        .stream().map(projectItem -> BiologicalDataItem.getBioDataItemId(projectItem.getBioDataItem()))
                        .collect(Collectors.toList()));

                List<GeneFile> geneFilesToAdd = new ArrayList<>();
                List<VcfFile> vcfFilesToAdd = new ArrayList<>();
                itemsAdded.stream().forEach(item -> fillFileTypeLists(item, geneFilesToAdd, vcfFilesToAdd));

                List<GeneFile> geneFileToDelete = new ArrayList<>();
                List<VcfFile> vcfFilesToDelete = new ArrayList<>();
                itemsRemoved.stream().forEach(item -> fillFileTypeLists(item, geneFileToDelete, vcfFilesToDelete));

                helpProject = loadProject(helpProject.getId());

            } else {
                List<BiologicalDataItem> dataItems = biologicalDataItemDao.loadBiologicalDataItemsByIds(
                        newProjectItems.parallelStream()
                                .map(projectItem -> projectItem.getBioDataItem().getId())
                                .collect(Collectors.toList()));

                Reference reference = findReferenceFromBioItems(dataItems);
                checkReference(reference, dataItems);

                projectDao.addProjectItems(helpProject.getId(), newProjectItems);
                helpProject = loadProject(helpProject.getId());
                List<VcfFile> newVcfFiles = new ArrayList<>();
                List<GeneFile> newGeneFiles = new ArrayList<>();
                dataItems.forEach(i -> fillFileTypeLists(i, newGeneFiles, newVcfFiles));
            }
        }

        return helpProject;
    }

    private void checkReference(Reference reference, List<BiologicalDataItem> projectItems) {
        for (BiologicalDataItem item : projectItems) {
            if (FeatureFile.class.isAssignableFrom(item.getClass())) {
                FeatureFile file = (FeatureFile) item;
                Assert.isTrue(reference.getId().equals(file.getReferenceId()),
                        MessageHelper.getMessage(MessagesConstants.ERROR_PROJECT_NON_MATCHING_REFERENCE,
                                file.getName()));
            }
        }
    }

    private Reference findReference(List<ProjectItem> projectItems) {
        List<Reference> references = projectItems.stream()
                .filter(item -> item.getBioDataItem().getFormat()
                        == BiologicalDataItemFormat.REFERENCE)
                .map(item -> (Reference) item.getBioDataItem()).collect(Collectors.toList());
        Assert.isTrue(!references.isEmpty(),
                MessageHelper.getMessage(MessagesConstants.ERROR_PROJECT_INVALID_REFERENCE));
        Assert.isTrue(references.size() == 1,
                MessageHelper.getMessage(MessagesConstants.ERROR_PROJECT_INVALID_REFERENCE));
        return references.get(0);
    }

    private Reference findReferenceFromBioItems(List<BiologicalDataItem> projectItems) {
        List<Reference> references = projectItems.stream()
                .filter(item -> item.getFormat()
                        == BiologicalDataItemFormat.REFERENCE)
                .map(item -> (Reference) item).collect(Collectors.toList());
        Assert.isTrue(!references.isEmpty(),
                MessageHelper.getMessage(MessagesConstants.ERROR_PROJECT_INVALID_REFERENCE));
        Assert.isTrue(references.size() == 1,
                MessageHelper.getMessage(MessagesConstants.ERROR_PROJECT_INVALID_REFERENCE));
        return references.get(0);
    }

    /**
     * Deletes a project, specified by ID, with it's items and bookmarks
     *
     * @param projectId {@code Long} an ID of a project to delete
     * @param force {@code Boolean} if project is parent and flag is true that project will be deleted
     *                             with nested projects
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Project deleteProject(Long projectId, Boolean force) throws IOException {
        Project projectToDelete = loadProject(projectId); // Throws an error if there is no such project
        //if force flag is disabled that we can't delete parent project
        if (!force) {
            List<Project> nestedProjects = projectDao.loadNestedProjects(projectId);
            Assert.isTrue(
                    nestedProjects.isEmpty(),
                    MessageHelper.getMessage(
                            MessagesConstants.ERROR_PROJECT_DELETE_HAS_NESTED,
                            projectToDelete.getName()
                    )
            );
        }

        deleteProjectWithNested(projectToDelete);
        return projectToDelete;
    }

    /**
     * Adds an item to a project, specified by ID
     *
     * @param projectId        {@code Long} ID of a project
     * @param biologicalItemId {@code Long} ID of an item to add
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Project addProjectItem(long projectId, long biologicalItemId) {
        Project loadedProject = loadProject(projectId);
        Reference reference = findReference(loadedProject.getItems());
        List<BiologicalDataItem> itemsToAdd = biologicalDataItemDao
                .loadBiologicalDataItemsByIds(Collections.singletonList(biologicalItemId));
        Assert.isTrue(itemsToAdd.stream().noneMatch(
            item -> item.getFormat() == BiologicalDataItemFormat.REFERENCE),
                MessageHelper.getMessage(MessagesConstants.ERROR_PROJECT_INVALID_REFERENCE));
        Set<Long> existingBioIds = loadedProject.getItems().stream()
                .map(item -> BiologicalDataItem.getBioDataItemId(item.getBioDataItem()))
                .collect(Collectors.toSet());
        if (!existingBioIds.contains(biologicalItemId)) {
            checkReference(reference, itemsToAdd);
            projectDao.addProjectItem(projectId, biologicalItemId);
        }
        return loadProject(projectId);
    }

    /**
     * Removes an item from a project, specified by ID
     *
     * @param projectId        {@code Long} ID of a project
     * @param biologicalItemId {@code Long} ID of an item to remove
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Project removeProjectItem(long projectId, long biologicalItemId)
            throws FeatureIndexException {
        projectDao.deleteProjectItem(projectId, biologicalItemId);

        return loadProject(projectId);
    }

    /**
     * Hides a project item, specified by ID. Hidden item won't be shown on UI.
     *
     * @param projectId        {@code Long} ID of a project
     * @param biologicalItemId {@code Long} ID of an item to hide
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void hideProjectItem(long projectId, long biologicalItemId) {
        Boolean isHidden = projectDao.isProjectItemHidden(projectId, biologicalItemId);
        projectDao.hideProjectItem(projectId, biologicalItemId, !isHidden);
    }

    private void countProjectItem(ProjectItem projectItem, List<ProjectItem> referenceItems,
                                  Map<BiologicalDataItemFormat, Integer> itemsCountPerFormat) {
        BiologicalDataItemFormat format = projectItem.getBioDataItem().getFormat();
        if (format == BiologicalDataItemFormat.REFERENCE) {
            referenceItems.add(projectItem);
        } else {
            if (!itemsCountPerFormat.containsKey(format)) {
                itemsCountPerFormat.put(format, 1);
            } else {
                itemsCountPerFormat.put(format, itemsCountPerFormat.get(format) + 1);
            }
        }
    }

    private void deleteProjectWithNested(Project projectToDelete) throws IOException {
        deleteNestedProjects(projectToDelete.getId());
        projectDao.deleteProjectItems(projectToDelete.getId());
        projectDao.deleteProject(projectToDelete.getId());
        fileManager.deleteProjectDirectory(projectToDelete);
    }

    private void deleteNestedProjects(Long projectId) throws IOException {
        List<Project> nestedProjects = projectDao.loadNestedProjects(projectId);
        for (Project nestedProject : nestedProjects) {
            deleteProjectWithNested(nestedProject);
        }
    }

    private void loadSamples(Project project, Set<Long> vcfIds, Set<Long> segIds) {
        if (!vcfIds.isEmpty() || !segIds.isEmpty()) {
            Map<Long, List<VcfSample>> vcfSampleMap = vcfFileDao.loadSamplesByFileIds(vcfIds);
            Map<Long, List<SegSample>> segSampleMap = segFileDao.loadSamplesByFileIds(segIds);

            project.getItems().parallelStream().forEach(item -> {
                if (item.getBioDataItem() != null) {
                    switch (item.getBioDataItem().getFormat()) {
                        case VCF:
                            VcfFile vcfFile = (VcfFile) item.getBioDataItem();
                            vcfFile.setSamples(vcfSampleMap.get(vcfFile.getId()));
                            item.setBioDataItem(vcfFile);
                            break;
                        case SEG:
                            SegFile segFile = (SegFile) item.getBioDataItem();
                            segFile.setSamples(segSampleMap.get(segFile.getId()));
                            item.setBioDataItem(segFile);
                            break;
                        default:
                            break;
                    }
                }
            });
        }
    }

    private void fillFileTypeLists(BiologicalDataItem biologicalDataItem, List<GeneFile> geneFileList,
                                   List<VcfFile> vcfFileList) {
        if (biologicalDataItem instanceof GeneFile) {
            geneFileList.add((GeneFile) biologicalDataItem);
        } else if (biologicalDataItem instanceof VcfFile) {
            vcfFileList.add((VcfFile) biologicalDataItem);
        }
    }

    private boolean checkNewProject(Project helpProject) {
        Project existingProject = projectDao.loadProject(helpProject.getName());

        boolean newProject = false;
        if (helpProject.getId() == null) {
            helpProject.setCreatedBy(AuthUtils.getCurrentUserId());
            helpProject.setCreatedDate(new Date());

            // for new project ensure that there is no project with this name
            Assert.isNull(existingProject, MessageHelper.getMessage(MessagesConstants.ERROR_PROJECT_NAME_EXISTS,
                                                                    helpProject.getName()));
            newProject = true;
        } else {
            // for updated one - ensure that if there is a project with that name,
            // its ID is equal to this project's id
            Assert.isTrue(existingProject == null || existingProject.getId().equals(helpProject.getId()),
                          MessageHelper.getMessage(MessagesConstants.ERROR_PROJECT_NAME_EXISTS, helpProject.getName()));
        }
        return newProject;
    }
}
