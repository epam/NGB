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

package com.epam.catgenome.dao.project;

import com.epam.catgenome.dao.AbstractDaoTest;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.project.ProjectDescription;
import com.epam.catgenome.helper.EntityHelper;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class ProjectDescriptionDaoTest extends AbstractDaoTest {
    private static final String NAME1 = "name1";
    private static final byte[] CONTENT1 = "content1".getBytes();
    private static final String NAME2 = "name2";
    private static final byte[] CONTENT2 = "content2".getBytes();

    private Long projectId;

    @Autowired
    private ProjectDescriptionDao projectDescriptionDao;

    @Autowired
    private ProjectDao projectDao;

    @Before
    public void setup() throws Exception {
        super.setup();
        final Project project = createProject("test");
        projectId = project.getId();
    }

    @Test
    @Transactional
    public void shouldCRUD() throws IOException {
        final ProjectDescription projectDescription1 = ProjectDescription.builder()
                .projectId(projectId)
                .name(NAME1)
                .build();

        final ProjectDescription projectDescription2 = ProjectDescription.builder()
                .projectId(projectId)
                .name(NAME2)
                .build();

        projectDescriptionDao.save(projectDescription1, CONTENT1);
        final Long projectDescription1Id = projectDescription1.getId();
        Assert.assertThat(projectDescription1Id, notNullValue());
        assertContent(projectDescription1Id, CONTENT1);

        final ProjectDescription actualDescriptionById = projectDescriptionDao.findById(projectDescription1Id);
        Assert.assertThat(actualDescriptionById.getId(), is(projectDescription1Id));
        Assert.assertThat(actualDescriptionById.getProjectId(), is(projectId));
        Assert.assertThat(actualDescriptionById.getName(), is(NAME1));

        projectDescriptionDao.update(projectDescription1, CONTENT2);
        assertContent(projectDescription1Id, CONTENT2);

        projectDescriptionDao.save(projectDescription2, CONTENT1);
        final Long projectDescription2Id = projectDescription2.getId();
        Assert.assertThat(projectDescription2Id, notNullValue());
        assertContent(projectDescription2Id, CONTENT1);

        final List<ProjectDescription> actualDescriptions = projectDescriptionDao.findByProjectId(projectId);
        Assert.assertThat(actualDescriptions, hasSize(2));
        Assert.assertThat(actualDescriptions.stream()
                .map(ProjectDescription::getId)
                .collect(Collectors.toSet()), hasItems(projectDescription1Id, projectDescription2Id));

        projectDescriptionDao.deleteById(projectDescription1Id);
        Assert.assertThat(projectDescriptionDao.findContentById(projectDescription1Id), nullValue());

        projectDescriptionDao.deleteByProjectId(projectId);
        Assert.assertThat(projectDescriptionDao.findByProjectId(projectId), hasSize(0));
    }

    @Transactional
    @Test
    public void shouldLoadAllOrIn() {
        final Project project2 = createProject("test2");
        final Long projectId2 = project2.getId();

        final ProjectDescription projectDescription11 = ProjectDescription.builder()
                .projectId(projectId)
                .name(NAME1)
                .build();
        projectDescriptionDao.save(projectDescription11, CONTENT1);

        final ProjectDescription projectDescription12 = ProjectDescription.builder()
                .projectId(projectId)
                .name(NAME2)
                .build();
        projectDescriptionDao.save(projectDescription12, CONTENT1);

        final ProjectDescription projectDescription21 = ProjectDescription.builder()
                .projectId(projectId2)
                .name(NAME1)
                .build();
        projectDescriptionDao.save(projectDescription21, CONTENT1);

        final Map<Long, List<ProjectDescription>> actualAllDescriptions = projectDescriptionDao.findAll();
        assertFindAllOrIn(projectId2, projectDescription11, projectDescription12, projectDescription21,
                actualAllDescriptions);

        final Map<Long, List<ProjectDescription>> actualInDescriptions = projectDescriptionDao
                .findByProjectIdIn(Arrays.asList(projectId, projectId2));
        assertFindAllOrIn(projectId2, projectDescription11, projectDescription12, projectDescription21,
                actualInDescriptions);
    }

    private void assertFindAllOrIn(final Long projectId2,
                                   final ProjectDescription projectDescription11,
                                   final ProjectDescription projectDescription12,
                                   final ProjectDescription projectDescription21,
                                   final Map<Long, List<ProjectDescription>> result) {
        Assert.assertThat(result.size(), is(2));

        final List<ProjectDescription> actualProject1Description = result.get(projectId);
        Assert.assertThat(actualProject1Description, hasSize(2));
        Assert.assertThat(actualProject1Description.stream()
                .map(ProjectDescription::getId)
                .collect(Collectors.toSet()), hasItems(projectDescription11.getId(), projectDescription12.getId()));

        final List<ProjectDescription> actualProject2Description = result.get(projectId2);
        Assert.assertThat(actualProject2Description, hasSize(1));
        Assert.assertThat(actualProject2Description.stream()
                .map(ProjectDescription::getId)
                .collect(Collectors.toSet()), hasItems(projectDescription21.getId()));
    }

    private void assertContent(final Long id, final byte[] expectedContent) throws IOException {
        final InputStream actualContent = projectDescriptionDao.findContentById(id);
        Assert.assertThat(expectedContent, is(IOUtils.toByteArray(actualContent)));
    }

    private Project createProject(final String name) {
        final Project project = new Project();
        project.setName(name);
        project.setCreatedDate(new Date());
        project.setOwner(EntityHelper.TEST_OWNER);
        projectDao.saveProject(project, null);
        return project;
    }
}
