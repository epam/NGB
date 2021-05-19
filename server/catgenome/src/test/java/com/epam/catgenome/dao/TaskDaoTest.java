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

import java.util.*;

import com.epam.catgenome.dao.blast.BlastTaskDao;
import com.epam.catgenome.entity.blast.*;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.PagingInfo;
import com.epam.catgenome.util.db.QueryParameters;
import com.epam.catgenome.util.db.SortInfo;
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
public class TaskDaoTest extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired
    private BlastTaskDao blastTaskDao;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveTask() {
        Task task = getBlastTask();
        blastTaskDao.saveTask(task);

        Task loadedTask = blastTaskDao.loadTaskById(task.getId());
        Assert.assertNotNull(loadedTask);
        Assert.assertEquals(task.getId(), loadedTask.getId());
        Assert.assertEquals(task.getTitle(), loadedTask.getTitle());
        Assert.assertEquals(task.getCreatedDate(), loadedTask.getCreatedDate());
    }

    @Test
    public void testUpdateTask() {
        Task task = getBlastTask();
        blastTaskDao.saveTask(task);

        task.setStatus(TaskStatus.CANCELED);
        blastTaskDao.saveTask(task);

        Task loadedTask = blastTaskDao.loadTaskById(task.getId());
        Assert.assertNotNull(loadedTask);
        Assert.assertEquals(task.getId(), loadedTask.getId());
        Assert.assertEquals(task.getTitle(), loadedTask.getTitle());
        Assert.assertEquals(task.getCreatedDate(), loadedTask.getCreatedDate());
    }

    @Test
    public void testLoadTasks() {
        blastTaskDao.saveTask(getBlastTask());
        QueryParameters parameters = new QueryParameters();
        PagingInfo pagingInfo = new PagingInfo();
        parameters.setPagingInfo(pagingInfo);
        Filter queryFilter = new Filter("task_id", "1");
        parameters.setFilters(Collections.singletonList(queryFilter));
        SortInfo sortInfo = new SortInfo("task_id", true);
        parameters.setSortInfos(Collections.singletonList(sortInfo));

        List<Task> tasks = blastTaskDao.loadAllTasks(parameters);
        Assert.assertFalse(tasks.isEmpty());
    }

    @Test
    public void testGetTasksCount() {
        blastTaskDao.saveTask(getBlastTask());
        Filter queryFilter = new Filter("task_id", "1");
//        long count = blastTaskDao.getTasksCount(Collections.singletonList(queryFilter));
        long count = blastTaskDao.getTasksCount(Collections.emptyList());
        Assert.assertEquals(1, count);
    }

    @NotNull
    private Task getBlastTask() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("test");
        task.setStatus(TaskStatus.CREATED);
        task.setCreatedDate(new Date());
        return task;
    }
}
