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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.epam.catgenome.dao.blast.BlastTaskDao;
import com.epam.catgenome.entity.blast.BlastTask;
import com.epam.catgenome.entity.blast.TaskStatus;
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
public class BlastTaskDaoTest extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired
    private BlastTaskDao blastTaskDao;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveTask() {
        BlastTask blastTask = getBlastTask();
        blastTaskDao.saveTask(blastTask);

        BlastTask loadedBlastTask = blastTaskDao.loadTaskById(blastTask.getId());
        Assert.assertNotNull(loadedBlastTask);
        Assert.assertEquals(blastTask.getId(), loadedBlastTask.getId());
        Assert.assertEquals(blastTask.getTitle(), loadedBlastTask.getTitle());
        Assert.assertEquals(blastTask.getCreatedDate(), loadedBlastTask.getCreatedDate());
    }

    @Test
    public void testUpdateTask() {
        BlastTask blastTask = getBlastTask();
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
        blastTaskDao.saveTask(getBlastTask());
        QueryParameters parameters = new QueryParameters();
        PagingInfo pagingInfo = new PagingInfo();
        parameters.setPagingInfo(pagingInfo);
        Filter queryFilter = new Filter("task_id", "=", "1");
        parameters.setFilters(Collections.singletonList(queryFilter));
        SortInfo sortInfo = new SortInfo("task_id", true);
        parameters.setSortInfos(Collections.singletonList(sortInfo));

        List<BlastTask> blastTasks = blastTaskDao.loadAllTasks(parameters);
        Assert.assertFalse(blastTasks.isEmpty());
    }

    @Test
    public void testGetTasksCount() {
        blastTaskDao.saveTask(getBlastTask());
        long count = blastTaskDao.getTasksCount(Collections.emptyList());
        Assert.assertEquals(1, count);
    }

    @NotNull
    private BlastTask getBlastTask() {
        BlastTask blastTask = new BlastTask();
        blastTask.setId(1L);
        blastTask.setTitle("test");
        blastTask.setStatus(TaskStatus.CREATED);
        blastTask.setCreatedDate(new Date());
        return blastTask;
    }
}
