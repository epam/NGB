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

package com.epam.catgenome.dao;

import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.dao.project.ProjectDao;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.util.AuthUtils;

/**
 * Source:      ProjectDaoTest
 * Created:     21.12.15, 18:46
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class ProjectDaoTest extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired
    private ProjectDao projectDao;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveLoadProject() {
        Project project = new Project();
        project.setName("test");
        project.setCreatedBy(AuthUtils.getCurrentUserId());
        project.setCreatedDate(new Date());
        project.setOwner(EntityHelper.TEST_OWNER);

        projectDao.saveProject(project, null);

        Project loadedProject = projectDao.loadProject(project.getId());
        Assert.assertNotNull(loadedProject);
        Assert.assertEquals(project.getId(), loadedProject.getId());
        Assert.assertEquals(project.getName(), loadedProject.getName());
        Assert.assertEquals(project.getCreatedBy(), loadedProject.getCreatedBy());
        Assert.assertEquals(project.getCreatedDate(), loadedProject.getCreatedDate());
        Assert.assertNotNull(loadedProject.getLastOpenedDate());
        Assert.assertEquals(project.getOwner(), loadedProject.getOwner());

        List<Project> loadedProjects = projectDao.loadTopLevelProjectsOrderByLastOpened(project.getCreatedBy());
        Assert.assertFalse(loadedProjects.isEmpty());
        loadedProjects.forEach(p -> {
            Assert.assertNotNull(p.getId());
            Assert.assertNotNull(p.getName());
            Assert.assertNotNull(p.getCreatedBy());
            Assert.assertNotNull(p.getCreatedDate());
            Assert.assertNotNull(p.getLastOpenedDate());
            Assert.assertEquals(project.getOwner(), p.getOwner());
        });
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testUpdateProject() {
        Project project = new Project();
        project.setName("test");
        project.setCreatedBy(AuthUtils.getCurrentUserId());
        project.setCreatedDate(new Date());
        project.setOwner(EntityHelper.TEST_OWNER);

        projectDao.saveProject(project, null);

        project.setName("test2");
        projectDao.saveProject(project, null);

        Project loadedProject = projectDao.loadProject(project.getId());
        Assert.assertNotNull(loadedProject);
        Assert.assertEquals(project.getName(), loadedProject.getName());
        Assert.assertEquals(project.getOwner(), loadedProject.getOwner());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadProjectsByParentId() throws Exception {
        Project parent = new Project();
        parent.setName("testParent");
        parent.setCreatedBy(AuthUtils.getCurrentUserId());
        parent.setCreatedDate(new Date());
        parent.setOwner(EntityHelper.TEST_OWNER);

        projectDao.saveProject(parent, null);

        Project child = new Project();
        child.setName("testChild");
        child.setCreatedBy(AuthUtils.getCurrentUserId());
        child.setCreatedDate(new Date());
        child.setOwner(EntityHelper.TEST_OWNER);

        projectDao.saveProject(child, parent.getId());

        List<Project> childProjects = projectDao.loadNestedProjects(parent.getId());
        Assert.assertFalse(childProjects.isEmpty());
        Assert.assertEquals(childProjects.get(0).getId(), child.getId());
        Assert.assertEquals(childProjects.get(0).getName(), child.getName());
        Assert.assertEquals(childProjects.get(0).getOwner(), child.getOwner());

        Project child2 = new Project();
        child2.setName("testChild2");
        child2.setCreatedBy(AuthUtils.getCurrentUserId());
        child2.setCreatedDate(new Date());
        child2.setOwner(EntityHelper.TEST_OWNER);

        projectDao.saveProject(child2, null);

        projectDao.moveProjectToParent(child2.getId(), parent.getId());
        childProjects = projectDao.loadNestedProjects(parent.getId());
        Assert.assertEquals(childProjects.size(), 2);

        List<Project> topLevel = projectDao.loadTopLevelProjectsOrderByLastOpened(AuthUtils.getCurrentUserId());
        Assert.assertEquals(1, topLevel.size());
    }
}
