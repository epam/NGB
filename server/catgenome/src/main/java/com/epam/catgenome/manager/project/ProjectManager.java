/*
 * MIT License
 *
 * Copyright (c) 2016-2022 EPAM Systems
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.epam.catgenome.dao.project.ProjectDescriptionDao;
import com.epam.catgenome.dao.reference.ReferenceGenomeDao;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.project.ProjectDescription;
import com.epam.catgenome.entity.project.ProjectNote;
import com.epam.catgenome.entity.metadata.EntityVO;
import com.epam.catgenome.entity.metadata.MetadataVO;
import com.epam.catgenome.entity.security.AbstractSecuredEntity;
import com.epam.catgenome.entity.security.AclClass;
import com.epam.catgenome.manager.SecuredEntityManager;
import com.epam.catgenome.manager.metadata.MetadataManager;
import com.epam.catgenome.security.acl.aspect.AclSync;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import com.epam.catgenome.util.db.Filter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.project.ProjectDao;
import com.epam.catgenome.dao.seg.SegFileDao;
import com.epam.catgenome.dao.vcf.VcfFileDao;
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
import com.epam.catgenome.manager.AuthManager;
import com.epam.catgenome.manager.FileManager;
import org.springframework.util.CollectionUtils;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.constant.MessagesConstants.*;
import static org.apache.commons.lang3.StringUtils.join;

/**
 * Source:      ProjectManager
 * Created:     11.01.16, 12:53
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * A service class to execute project related tasks
 * </p>
 */
@AclSync
@Service
@RequiredArgsConstructor
public class ProjectManager implements SecuredEntityManager {
    private final ProjectDao projectDao;
    private final VcfFileDao vcfFileDao;
    private final SegFileDao segFileDao;
    private final ReferenceGenomeDao referenceGenomeDao;
    private final BiologicalDataItemDao biologicalDataItemDao;
    private final FileManager fileManager;
    private final AuthManager authManager;
    private final MetadataManager metadataManager;
    private final ProjectDescriptionDao projectDescriptionDao;

    /**
     * Loads all top-level projects from the database.
     * Projects are being loaded with single reference item and statistics regarding item counts by bio data type.
     *
     * @return a {@code List&lt;Project&gt;} of projects
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Project> loadTopLevelProjects() {
        return projectDao.loadTopLevelProjectsStats();
    }

    /**
     * Loads all project hierarchy for current user, with all items
     * @param parentId specifies the root project for loading, if null, all projects will be loaded
     * @param referenceName
     * @return all project hierarchy for current user, with all items
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Project> loadProjectTree(final Long parentId, final String referenceName) {
        List<Project> allProjects;
        if (StringUtils.isEmpty(referenceName)) {
            allProjects = projectDao.loadAllProjects();
        } else {
            Reference reference = referenceGenomeDao.loadReferenceGenomeByName(referenceName.toLowerCase());
            Assert.notNull(reference, getMessage(ERROR_BIO_NAME_NOT_FOUND, referenceName));
            allProjects = projectDao.loadProjectsByBioDataItemId(reference.getBioDataItemId());
        }

        Map<Long, List<Project>> hierarchyMap = new HashMap<>();
        Map<Long, Set<ProjectItem>> itemMap;

        if (StringUtils.isEmpty(referenceName)) {
            itemMap = projectDao.loadAllProjectItems();
        } else {
            itemMap = projectDao.loadProjectItemsByProjects(allProjects);
        }

        Map<Long, Set<ProjectNote>> noteMap = projectDao.loadAllProjectNotes(StringUtils.isEmpty(referenceName) ?
                null : allProjects);

        Map<Long, List<ProjectDescription>> descriptionsMap = StringUtils.isBlank(referenceName)
                ? projectDescriptionDao.findAll()
                : projectDescriptionDao.findByProjectIdIn(ListUtils.emptyIfNull(allProjects).stream()
                .map(Project::getId)
                .collect(Collectors.toList()));

        attachMetadata(allProjects, itemMap);

        allProjects.forEach(p -> {
            if (itemMap.containsKey(p.getId())) {
                p.setItems(new ArrayList<>(itemMap.get(p.getId())));
            }
            if (noteMap.containsKey(p.getId())) {
                p.setNotes(new ArrayList<>(noteMap.get(p.getId())));
            }
            if (descriptionsMap.containsKey(p.getId())) {
                p.setDescriptions(new ArrayList<>(descriptionsMap.get(p.getId())));
            }

            if (!hierarchyMap.containsKey(p.getParentId())) {
                hierarchyMap.put(p.getParentId(), new ArrayList<>());
            }
            hierarchyMap.get(p.getParentId()).add(p);
        });

        if (parentId != null) {
            Project topProject = this.load(parentId);
            Assert.notNull(topProject, getMessage(ERROR_PROJECT_ID_NOT_FOUND, parentId));
            topProject.setNestedProjects(hierarchyMap.get(parentId));
            attachMetadataToTopProject(parentId, topProject);
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
    @Transactional(propagation = Propagation.SUPPORTS)
    public Project load(final Long projectId) {
        final Project project = projectDao.loadProject(projectId);
        Assert.notNull(project, getMessage(ERROR_PROJECT_ID_NOT_FOUND, projectId));
        loadProjectStuff(project);
        return project;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public AbstractSecuredEntity changeOwner(final Long id, final String owner) {
        final Project project = load(id);
        project.setOwner(owner);
        projectDao.updateOwner(id, owner);
        return project;
    }

    @Override
    public AclClass getSupportedClass() {
        return AclClass.PROJECT;
    }

    /**
     * Loads a project from the database by its name, with all its items
     *
     * @param projectName {@code String} name of a project to load
     * @return a {@code Project} from the database
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public Project load(final String projectName) {
        final Project project = projectDao.loadProject(projectName);
        Assert.notNull(project, getMessage(ERROR_PROJECT_NAME_NOT_FOUND, projectName));
        loadProjectStuff(project);
        updateLastOpenedDate(project);
        return project;
    }

    /**
     * Moves a Project, specified by projectId to a parent project, specified by parentId
     * @param projectId an ID of a project, to move
     * @param parentId an ID of a parent project to move to
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void moveProjectToParent(final long projectId, final Long parentId) {
        Assert.notNull(projectDao.loadProject(projectId), getMessage(ERROR_PROJECT_ID_NOT_FOUND, projectId));
        if (parentId != null) {
            Assert.notNull(projectDao.loadProject(parentId), getMessage(ERROR_PROJECT_ID_NOT_FOUND, parentId));
        }
        projectDao.moveProjectToParent(projectId, parentId);
    }

    /**
     * Saves a new project to the database or updates an existing one. Also saves all project items passed, if they
     * are not null.
     *
     * @param project a {@code Project} to be saved
     * @return saved {@code Project} from the database
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Project create(final Project project) {
        return create(project, null);
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
    public Project create(final Project project, final Long parentId) {
        project.setOwner(authManager.getAuthorizedUser());

        Project helpProject = project;
        boolean newProject = checkNewProject(helpProject);

        projectDao.saveProject(helpProject, parentId);

        Project loadedProject = this.load(helpProject.getId());

        if (helpProject.getItems() != null) {
            List<ProjectItem> newProjectItems = helpProject.getItems()
                    .stream().distinct().collect(Collectors.toList());
            if (!newProject) {
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

                Assert.isTrue(itemsToAdd.stream().noneMatch(item ->
                                item.getBioDataItem().getFormat() == BiologicalDataItemFormat.REFERENCE),
                        getMessage(ERROR_PROJECT_INVALID_REFERENCE));
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

                helpProject = this.load(helpProject.getId());

            } else {
                List<BiologicalDataItem> dataItems = biologicalDataItemDao.loadBiologicalDataItemsByIds(
                        newProjectItems.parallelStream()
                                .map(projectItem -> projectItem.getBioDataItem().getId())
                                .collect(Collectors.toList()));

                Reference reference = findReferenceFromBioItems(dataItems);
                checkReference(reference, dataItems);

                projectDao.addProjectItems(helpProject.getId(), newProjectItems);
                helpProject = this.load(helpProject.getId());
                List<VcfFile> newVcfFiles = new ArrayList<>();
                List<GeneFile> newGeneFiles = new ArrayList<>();
                dataItems.forEach(i -> fillFileTypeLists(i, newGeneFiles, newVcfFiles));
            }
        }
        processNotes(project, helpProject, newProject, loadedProject);
        return helpProject;
    }

    /**
     * Deletes a project, specified by ID, with it's items and bookmarks
     *
     * @param projectId {@code Long} an ID of a project to delete
     * @param force {@code Boolean} if project is parent and flag is true that project will be deleted
     *                             with nested projects
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Project delete(final Long projectId, final Boolean force) throws IOException {
        // Throws an error if there is no such project
        final Project projectToDelete = this.load(projectId);
        //if force flag is disabled that we can't delete parent project
        if (!force) {
            final List<Project> nestedProjects = projectDao.loadNestedProjects(projectId);
            Assert.isTrue(nestedProjects.isEmpty(), getMessage(ERROR_PROJECT_DELETE_HAS_NESTED,
                    projectToDelete.getName())
            );
        }
        deleteProjectWithNested(projectToDelete);
        return projectToDelete;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void renameProject(final String name, final String newName, final String newPrettyName) {
        Assert.isTrue(StringUtils.isNotBlank(newName) || StringUtils.isNotBlank(newPrettyName),
                "Either new name or new pretty name should be defined.");
        if (StringUtils.isNotBlank(newName)) {
            Assert.isTrue(!name.trim().equals(newName.trim()),
                    "New project name should not be equal to old name.");
        }
        final Project project = projectDao.loadProject(name);
        Assert.notNull(project, getMessage(ERROR_PROJECT_NAME_NOT_FOUND, name));
        if (StringUtils.isNotBlank(newName)) {
            checkProjectName(newName.trim());
            project.setName(newName.trim());
        }
        if (StringUtils.isNotBlank(newPrettyName)) {
            project.setPrettyName(newPrettyName.trim());
        }
        projectDao.saveProject(project, null);
    }

    /**
     * Adds an item to a project, specified by ID
     *
     * @param projectId        {@code Long} ID of a project
     * @param biologicalItemId {@code Long} ID of an item to add
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Project addProjectItem(long projectId, long biologicalItemId) {
        Project loadedProject = load(projectId);
        Reference reference = findReference(loadedProject.getItems());
        List<BiologicalDataItem> itemsToAdd = biologicalDataItemDao
                .loadBiologicalDataItemsByIds(Collections.singletonList(biologicalItemId));
        Assert.isTrue(itemsToAdd.stream()
                        .noneMatch(item -> item.getFormat() == BiologicalDataItemFormat.REFERENCE),
                getMessage(ERROR_PROJECT_INVALID_REFERENCE));
        Set<Long> existingBioIds = loadedProject.getItems().stream()
                .map(item -> BiologicalDataItem.getBioDataItemId(item.getBioDataItem()))
                .collect(Collectors.toSet());
        if (!existingBioIds.contains(biologicalItemId)) {
            checkReference(reference, itemsToAdd);
            projectDao.addProjectItem(projectId, biologicalItemId);
        }
        return load(projectId);
    }

    /**
     * Removes an item from a project, specified by ID
     *
     * @param projectId        {@code Long} ID of a project
     * @param biologicalItemId {@code Long} ID of an item to remove
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Project removeProjectItem(long projectId, long biologicalItemId) {
        projectDao.deleteProjectItem(projectId, biologicalItemId);
        return load(projectId);
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

    private void loadProjectStuff(final Project project) {
        loadProjectItems(project);
        project.setNestedProjects(projectDao.loadNestedProjects(project.getId()));
        project.setNotes(projectDao.loadProjectNotes(project.getId()));
    }

    private void updateLastOpenedDate(Project project) {
        projectDao.updateLastOpenedDate(project.getId());
    }

    private void loadProjectItems(final Project project) {
        project.setItems(projectDao.loadProjectItemsByProjectId(project.getId()));

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

    private void deleteProjectWithNested(final Project projectToDelete) throws IOException {
        deleteNestedProjects(projectToDelete.getId());
        projectDescriptionDao.deleteByProjectId(projectToDelete.getId());
        projectDao.deleteAllProjectNotes(projectToDelete.getId());
        projectDao.deleteProjectItems(projectToDelete.getId());
        projectDao.deleteProject(projectToDelete.getId());
        fileManager.deleteProjectDirectory(projectToDelete);
        metadataManager.delete(projectToDelete.getId(), AclClass.PROJECT.name());
    }

    private void deleteNestedProjects(final Long projectId) throws IOException {
        final List<Project> nestedProjects = projectDao.loadNestedProjects(projectId);
        for (Project nestedProject : nestedProjects) {
            deleteProjectWithNested(nestedProject);
        }
    }

    private void loadSamples(final Project project, final Set<Long> vcfIds, final Set<Long> segIds) {
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

    private void fillFileTypeLists(final BiologicalDataItem biologicalDataItem,
                                   final List<GeneFile> geneFileList,
                                   final List<VcfFile> vcfFileList) {
        if (biologicalDataItem instanceof GeneFile) {
            geneFileList.add((GeneFile) biologicalDataItem);
        } else if (biologicalDataItem instanceof VcfFile) {
            vcfFileList.add((VcfFile) biologicalDataItem);
        }
    }

    private boolean checkNewProject(final Project helpProject) {
        Project existingProject = projectDao.loadProject(helpProject.getName());

        boolean newProject = false;
        if (helpProject.getId() == null) {
            helpProject.setCreatedDate(new Date());

            // for new project ensure that there is no project with this name
            Assert.isNull(existingProject, getMessage(ERROR_PROJECT_NAME_EXISTS,
                    helpProject.getName()));
            newProject = true;
        } else {
            // for updated one - ensure that if there is a project with that name,
            // its ID is equal to this project's id
            Assert.isTrue(existingProject == null || existingProject.getId().equals(helpProject.getId()),
                    getMessage(ERROR_PROJECT_NAME_EXISTS, helpProject.getName()));
        }
        return newProject;
    }

    private void attachMetadata(final List<Project> allProjects, final Map<Long, Set<ProjectItem>> itemMap) {
        attachMetadataToProjects(allProjects);
        attachMetadataToItems(itemMap);
    }

    private void attachMetadataToProjects(final List<Project> allProjects) {
        final Map<EntityVO, MetadataVO> projectsMetadata = metadataManager.getItems(allProjects.stream()
                .map(project -> EntityVO.builder()
                        .entityId(project.getId())
                        .entityClass(project.getAclClass())
                        .build())
                .collect(Collectors.toList()));
        allProjects.forEach(project -> attachMetadataToProject(project, projectsMetadata));
    }

    private void attachMetadataToProject(final Project project, final Map<EntityVO, MetadataVO> projectMetadata) {
        final MetadataVO metadataVO = projectMetadata.get(EntityVO.builder()
                .entityId(project.getId())
                .entityClass(project.getAclClass())
                .build());

        if (Objects.isNull(metadataVO)) {
            return;
        }

        project.setMetadata(metadataVO.getMetadata());
    }

    private void attachMetadataToItems(final Map<Long, Set<ProjectItem>> itemMap) {
        final Map<EntityVO, MetadataVO> projectItemsMetadata = metadataManager.getItems(itemMap.values().stream()
                .flatMap(Collection::stream)
                .map(ProjectItem::getBioDataItem)
                .map(projectItem -> EntityVO.builder()
                        .entityId(projectItem.getId())
                        .entityClass(projectItem.getAclClass())
                        .build())
                .distinct()
                .collect(Collectors.toList()));
        itemMap.values().forEach(items -> SetUtils.emptyIfNull(items).stream()
                .map(ProjectItem::getBioDataItem)
                .forEach(item -> attachMetadataToItem(item, projectItemsMetadata)));
    }

    private void attachMetadataToItem(final BiologicalDataItem item,
                                      final Map<EntityVO, MetadataVO> projectItemsMetadata) {
        final MetadataVO metadataVO = projectItemsMetadata.get(EntityVO.builder()
                .entityId(item.getId())
                .entityClass(item.getAclClass())
                .build());

        if (Objects.isNull(metadataVO)) {
            return;
        }

        item.setMetadata(metadataVO.getMetadata());
    }

    private void attachMetadataToTopProject(final Long parentId, final Project topProject) {
        final MetadataVO parentMetadata = metadataManager.get(parentId, AclClass.PROJECT.name());
        if (Objects.nonNull(parentMetadata)) {
            topProject.setMetadata(parentMetadata.getMetadata());
        }

        final Map<EntityVO, MetadataVO> parentItemsMetadata = metadataManager.getItems(
                ListUtils.emptyIfNull(topProject.getItems()).stream()
                        .map(ProjectItem::getBioDataItem)
                        .map(item -> EntityVO.builder()
                                .entityId(item.getId())
                                .entityClass(item.getAclClass())
                                .build())
                        .distinct()
                        .collect(Collectors.toList()));
        ListUtils.emptyIfNull(topProject.getItems()).stream()
                .map(ProjectItem::getBioDataItem)
                .forEach(item -> attachMetadataToItem(item, parentItemsMetadata));
    }

    private void processNotes(final Project project, final Project helpProject, final boolean newProject,
                              final Project loadedProject) {
        if (!newProject && !CollectionUtils.isEmpty(loadedProject.getNotes())) {
            final List<Long> existingNoteIds = loadedProject.getNotes().stream()
                    .map(ProjectNote::getNoteId)
                    .collect(Collectors.toList());
            final List<Long> newNoteIds = helpProject.getNotes() == null ? Collections.emptyList() :
                    helpProject.getNotes().stream()
                            .map(ProjectNote::getNoteId)
                            .collect(Collectors.toList());
            final List<Long> notesToDelete = existingNoteIds.stream()
                    .filter(o -> !newNoteIds.contains(o))
                    .collect(Collectors.toList());
            if (!notesToDelete.isEmpty()) {
                final Filter notesFilter = Filter.builder()
                        .field("note_id")
                        .operator("in")
                        .value("(" + join(notesToDelete, ",") + ")")
                        .build();
                final Filter projectFilter = Filter.builder()
                        .field("project_id")
                        .operator("=")
                        .value(String.valueOf(helpProject.getId()))
                        .build();
                projectDao.deleteProjectNotes(Arrays.asList(notesFilter, projectFilter));
            }
        }
        List<ProjectNote> notes = project.getNotes();
        if (!CollectionUtils.isEmpty(notes)) {
            List<ProjectNote> notesToAdd = notes.stream()
                    .filter(n -> n.getNoteId() == null)
                    .collect(Collectors.toList());
            List<ProjectNote> notesToUpdate = notes.stream()
                    .filter(n -> n.getNoteId() != null)
                    .collect(Collectors.toList());
            notes = projectDao.saveProjectNotes(notesToAdd, project.getId());
            notes.addAll(projectDao.updateProjectNotes(notesToUpdate));
        }
        helpProject.setNotes(notes);
    }

    private void checkReference(final Reference reference, final List<BiologicalDataItem> projectItems) {
        for (BiologicalDataItem item : projectItems) {
            if (FeatureFile.class.isAssignableFrom(item.getClass())) {
                FeatureFile file = (FeatureFile) item;
                Assert.isTrue(reference.getId().equals(file.getReferenceId()),
                        getMessage(ERROR_PROJECT_NON_MATCHING_REFERENCE, file.getName()));
            }
        }
    }

    private Reference findReference(final List<ProjectItem> projectItems) {
        List<Reference> references = projectItems.stream()
                .filter(item -> item.getBioDataItem().getFormat() == BiologicalDataItemFormat.REFERENCE)
                .map(item -> (Reference) item.getBioDataItem())
                .collect(Collectors.toList());
        Assert.isTrue(!references.isEmpty(), getMessage(ERROR_PROJECT_INVALID_REFERENCE));
        Assert.isTrue(references.size() == 1, getMessage(ERROR_PROJECT_INVALID_REFERENCE));
        return references.get(0);
    }

    private Reference findReferenceFromBioItems(final List<BiologicalDataItem> projectItems) {
        List<Reference> references = projectItems.stream()
                .filter(item -> item.getFormat() == BiologicalDataItemFormat.REFERENCE)
                .map(item -> (Reference) item)
                .collect(Collectors.toList());
        Assert.isTrue(!references.isEmpty(), getMessage(ERROR_PROJECT_INVALID_REFERENCE));
        Assert.isTrue(references.size() == 1, getMessage(ERROR_PROJECT_INVALID_REFERENCE));
        return references.get(0);
    }

    private void checkProjectName(final String name) {
        final Project project = projectDao.loadProject(name);
        Assert.isNull(project, getMessage(ERROR_PROJECT_NAME_EXISTS, name));
    }
}
