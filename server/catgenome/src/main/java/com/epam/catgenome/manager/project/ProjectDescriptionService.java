/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

import com.epam.catgenome.dao.project.ProjectDescriptionDao;
import com.epam.catgenome.entity.project.ProjectDescription;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.epam.catgenome.util.NgbFileUtils.getFile;

@Service
@RequiredArgsConstructor
public class ProjectDescriptionService {

    private final ProjectManager projectManager;
    private final ProjectDescriptionDao projectDescriptionDao;

    @Transactional
    public ProjectDescription upsert(final Long projectId, final String name, final String path,
                                     final MultipartFile multipartFile) throws IOException {
        Assert.isTrue(path != null || multipartFile != null, "Description file path or content should be defined");
        projectManager.load(projectId);

        String descriptionName;
        byte[] content;
        if (multipartFile == null) {
            final File file = getFile(path);
            descriptionName = StringUtils.isBlank(name) ? file.getName() : name;
            content = Files.readAllBytes(file.toPath());
        } else {
            descriptionName = StringUtils.isBlank(name) ? multipartFile.getOriginalFilename() : name;
            content = multipartFile.getBytes();
        }

        final Optional<ProjectDescription> loadedDescription = loadDescriptions(projectId).stream()
                .filter(existingDescription -> Objects.equals(existingDescription.getName(), descriptionName))
                .findAny();

        if (loadedDescription.isPresent()) {
            final ProjectDescription description = loadedDescription.get();
            projectDescriptionDao.update(description, content);
            return description;
        }

        final ProjectDescription description = ProjectDescription.builder()
                .projectId(projectId)
                .name(descriptionName)
                .build();
        projectDescriptionDao.save(description, content);
        return description;
    }

    public InputStream loadContent(final Long id) {
        return projectDescriptionDao.findContentById(id);
    }

    public ProjectDescription load(final Long id) {
        final ProjectDescription description = projectDescriptionDao.findById(id);
        Assert.notNull(description, "Project description with id '{}' doesn't exist");
        return description;
    }

    public List<ProjectDescription> loadDescriptions(final Long projectId) {
        return projectDescriptionDao.findByProjectId(projectId);
    }

    @Transactional
    public ProjectDescription deleteById(final Long id) {
        final ProjectDescription description = load(id);
        projectDescriptionDao.deleteById(id);
        return description;
    }

    @Transactional
    public List<ProjectDescription> deleteByProjectId(final Long projectId, final String name) {
        projectManager.load(projectId);
        final List<ProjectDescription> descriptions = loadDescriptions(projectId);

        if (StringUtils.isBlank(name)) {
            projectDescriptionDao.deleteByProjectId(projectId);
            return descriptions;
        }

        final ProjectDescription descriptionToDelete = descriptions.stream()
                .filter(existingDescription -> Objects.equals(existingDescription.getName(), name))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "Project description with name '%s' was not found", name)));
        projectDescriptionDao.deleteById(descriptionToDelete.getId());
        return Collections.singletonList(descriptionToDelete);
    }
}
