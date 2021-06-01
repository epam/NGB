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

package com.epam.catgenome.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.epam.catgenome.dao.blast.BlastTaskDao;
import com.epam.catgenome.entity.blast.BlastTask;
import com.epam.catgenome.entity.blast.BlastTaskOrganism;
import com.epam.catgenome.entity.blast.TaskParameter;
import com.epam.catgenome.entity.blast.TaskStatus;
import com.epam.catgenome.manager.AuthManager;
import com.epam.catgenome.manager.blast.BlastTaskManager;
import com.epam.catgenome.manager.blast.dto.TaskPage;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.PagingInfo;
import com.epam.catgenome.util.db.QueryParameters;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BlastTaskDaoTest extends AbstractTransactionalJUnit4SpringContextTests {

    public static final String TEST = "test";
    @Autowired
    private BlastTaskDao blastTaskDao;

    @Autowired
    private BlastTaskManager blastTaskManager;
    @Autowired
    private AuthManager authManager;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveTask() {
        BlastTask blastTask = getBlastTask(1L, TEST);
        blastTaskDao.saveTask(blastTask);

        BlastTask loadedBlastTask = blastTaskDao.loadTaskById(blastTask.getId());
        Assert.assertNotNull(loadedBlastTask);
        Assert.assertEquals(blastTask.getId(), loadedBlastTask.getId());
        Assert.assertEquals(blastTask.getTitle(), loadedBlastTask.getTitle());
        Assert.assertEquals(blastTask.getCreatedDate(), loadedBlastTask.getCreatedDate());
    }

    @Test
    public void testUpdateTask() {
        BlastTask blastTask = getBlastTask(1L, TEST);
        blastTaskDao.saveTask(blastTask);

        blastTask.setStatus(TaskStatus.CANCELED);
        blastTaskDao.updateTask(blastTask);

        BlastTask loadedBlastTask = blastTaskDao.loadTaskById(blastTask.getId());
        Assert.assertNotNull(loadedBlastTask);
        Assert.assertEquals(blastTask.getId(), loadedBlastTask.getId());
        Assert.assertEquals(blastTask.getTitle(), loadedBlastTask.getTitle());
        Assert.assertEquals(blastTask.getCreatedDate(), loadedBlastTask.getCreatedDate());
    }

    @Test
    public void testLoadTasks() {
        blastTaskDao.saveTask(getBlastTask(1L, "test1"));
        blastTaskDao.saveTask(getBlastTask(2L, "test2"));
        blastTaskDao.saveTask(getBlastTask(3L, "test3"));
        blastTaskDao.saveTask(getBlastTask(4L, "test4"));
        blastTaskDao.saveTask(getBlastTask(5L, "test5"));

        QueryParameters parameters = new QueryParameters();
        PagingInfo pagingInfo = new PagingInfo(2, 1);
        parameters.setPagingInfo(pagingInfo);
//        Filter queryFilter = new Filter("task_id", ">", "3");
//        parameters.setFilters(Collections.singletonList(queryFilter));
//        SortInfo sortInfo = new SortInfo("task_id", false);
//        parameters.setSortInfos(Collections.singletonList(sortInfo));

        TaskPage taskPage = blastTaskManager.loadTasks(parameters);
        Assert.assertNotNull(taskPage);
        Assert.assertEquals(taskPage.getTotalCount(), 5);
        Assert.assertEquals(taskPage.getBlastTasks().size(), 2);
    }

    @Test
    public void testLoadTasksDefault() {
        blastTaskDao.saveTask(getBlastTask(1L, "test1"));
        blastTaskDao.saveTask(getBlastTask(2L, "test2"));
        blastTaskDao.saveTask(getBlastTask(3L, "test3"));
        blastTaskDao.saveTask(getBlastTask(4L, "test4"));
        blastTaskDao.saveTask(getBlastTask(5L, "test5"));

        QueryParameters parameters = new QueryParameters();
        TaskPage taskPage = blastTaskManager.loadTasks(parameters);
        Assert.assertNotNull(taskPage);
        Assert.assertEquals(taskPage.getTotalCount(), 5);
    }

    @Test
    public void testGetTasksCount() {
        blastTaskDao.saveTask(getBlastTask(1L, TEST));
        long count = blastTaskDao.getTasksCount(Collections.emptyList());
        Assert.assertEquals(1, count);
    }

    @Test
    public void testDeleteOrganisms() {
        blastTaskDao.saveTask(getBlastTask(1L, TEST));
        blastTaskDao.saveOrganisms(1L, Arrays.asList(1L, 2L));
        List<Filter> deleteFilters = new ArrayList<>();
        deleteFilters.add(new Filter("organism", "=", "1"));
        blastTaskDao.deleteOrganisms(deleteFilters);
        List<BlastTaskOrganism> organisms = blastTaskDao.loadOrganisms(1);
        Assert.assertNotNull(organisms);
        Assert.assertEquals(organisms.size(), 1);
    }

    @Test
    public void testDeleteExclOrganisms() {
        blastTaskDao.saveTask(getBlastTask(1L, TEST));
        blastTaskDao.saveExclOrganisms(1L, Arrays.asList(1L, 2L));
        List<Filter> deleteFilters = new ArrayList<>();
        deleteFilters.add(new Filter("organism", "=", "1"));
        blastTaskDao.deleteExclOrganisms(deleteFilters);
        List<BlastTaskOrganism> organisms = blastTaskDao.loadExclOrganisms(1);
        Assert.assertNotNull(organisms);
        Assert.assertEquals(organisms.size(), 1);
    }

    @Test
    public void testDeleteParameters() {
        blastTaskDao.saveTask(getBlastTask(1L, TEST));
        blastTaskDao.saveTaskParameters(1L, Collections.singletonMap("key", "value"));
        List<Filter> deleteFilters = new ArrayList<>();
        deleteFilters.add(new Filter("parameter", "=", "'key'"));
        blastTaskDao.deleteParameters(deleteFilters);
        List<TaskParameter> parameters = blastTaskDao.loadTaskParameters(1);
        Assert.assertNotNull(parameters);
        Assert.assertEquals(parameters.size(), 0);
    }

    @Test
    public void testDeleteTasks() {
        blastTaskDao.saveTask(getBlastTask(1L, "test1"));
        blastTaskDao.saveTask(getBlastTask(2L, "test2"));
        blastTaskDao.saveTask(getBlastTask(3L, "test3"));
        blastTaskDao.saveTask(getBlastTask(4L, "test4"));
        blastTaskDao.saveTask(getBlastTask(5L, "test5"));
        List<Filter> deleteFilters = new ArrayList<>();
        deleteFilters.add(new Filter("task_id", "in", "(1,2,3,4)"));
        blastTaskDao.deleteTasks(deleteFilters);
        long count = blastTaskDao.getTasksCount(Collections.emptyList());
        Assert.assertEquals(1, count);
    }

    @NotNull
    private BlastTask getBlastTask(final long id, final String title) {
        BlastTask blastTask = new BlastTask();
        blastTask.setId(id);
        blastTask.setTitle(title);
        blastTask.setStatus(TaskStatus.CREATED);
        blastTask.setOwner(authManager.getAuthorizedUser());
        blastTask.setCreatedDate(LocalDateTime.now());
        return blastTask;
    }
}
