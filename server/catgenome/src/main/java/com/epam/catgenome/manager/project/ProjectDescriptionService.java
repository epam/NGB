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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectDescriptionService {

    private final ProjectManager projectManager;
    private final ProjectDescriptionDao projectDescriptionDao;

    @Transactional
    public ProjectDescription save(final Long projectId, final String name, final MultipartFile file)
            throws IOException {
        projectManager.load(projectId);
        final ProjectDescription description = ProjectDescription.builder()
                .projectId(projectId)
                .name(StringUtils.isBlank(name) ? file.getOriginalFilename() : name)
                .build();

        loadDescriptions(projectId).stream()
                .map(ProjectDescription::getName)
                .filter(existingName -> Objects.equals(existingName, description.getName()))
                .findAny()
                .ifPresent(existingName -> {
                    throw new IllegalArgumentException(String.format(
                            "Project description with name '%s' already exists", description.getName()));
                });

        projectDescriptionDao.save(description, file.getBytes());
        return description;
    }

    @Transactional
    public ProjectDescription update(final Long id, final String name, final MultipartFile file)
            throws IOException {
        final ProjectDescription description = load(id);

        if (StringUtils.isNotBlank(name)) {
            description.setName(name);
        }

        projectDescriptionDao.update(description, file.getBytes());
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
}
